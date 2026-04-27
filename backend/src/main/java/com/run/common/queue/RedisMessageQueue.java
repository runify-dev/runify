package com.run.common.queue;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RedisMessageQueue implements MessageQueue<String> {

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_INDEX = "index";
    private static final String FIELD_DATA = "data";

    private static final String TYPE_MESSAGE = "MESSAGE";
    private static final String TYPE_COMPLETE = "COMPLETE";

    private static final String STATE_RUNNING = "RUNNING";
    private static final String STATE_COMPLETED = "COMPLETED";

    /**
     * complete 使用最大 Stream ID，保证它排在所有正常 index 后面。
     */
    private static final String COMPLETE_STREAM_ID = Long.MAX_VALUE + "-0";

    private static final long ACTIVE_TTL_SECONDS = 10 * 60;
    private static final long ACTIVE_TTL_MS = ACTIVE_TTL_SECONDS * 1000;

    private static final long COMPLETE_TTL_SECONDS = 60;
    private static final long COMPLETE_TTL_MS = COMPLETE_TTL_SECONDS * 1000;

    /**
     * 同一个 eventId 30 秒内最多刷新一次 Redis TTL。
     */
    private static final long TOUCH_INTERVAL_MS = 30_000;

    /**
     * 没有消息时轮询间隔。
     * 不使用 XREAD BLOCK，避免 consumer 占住 Redis 连接池。
     */
    private static final long POLL_INTERVAL_MS = 200;

    private static final long RETRY_INTERVAL_MS = 1_000;
    private static final long CLEAN_INTERVAL_MS = 5_000;

    private static final int BATCH_SIZE = 50;

    /**
     * create:
     * 1. 删除旧 stream
     * 2. 设置 state=RUNNING，TTL=10分钟
     */
    private static final String CREATE_SCRIPT = """
            redis.call('DEL', KEYS[1])
            redis.call('SET', KEYS[2], ARGV[1])
            redis.call('EXPIRE', KEYS[2], ARGV[2])
            return 1
            """;

    /**
     * publish:
     * 1. 如果 state=COMPLETED，返回 0
     * 2. XADD index-0
     * 3. 设置 state=RUNNING
     * 4. stream / state 都续期 10 分钟
     * <p>
     * KEYS:
     * 1 streamKey
     * 2 stateKey
     * <p>
     * ARGV:
     * 1 streamId
     * 2 FIELD_TYPE
     * 3 TYPE_MESSAGE
     * 4 FIELD_INDEX
     * 5 index
     * 6 FIELD_DATA
     * 7 message
     * 8 STATE_COMPLETED
     * 9 STATE_RUNNING
     * 10 ACTIVE_TTL_SECONDS
     */
    private static final String PUBLISH_SCRIPT = """
            local state = redis.call('GET', KEYS[2])
            if state == ARGV[8] then
                return 0
            end
            
            redis.call(
                'XADD',
                KEYS[1],
                ARGV[1],
                ARGV[2], ARGV[3],
                ARGV[4], ARGV[5],
                ARGV[6], ARGV[7]
            )
            
            redis.call('SET', KEYS[2], ARGV[9])
            redis.call('EXPIRE', KEYS[1], ARGV[10])
            redis.call('EXPIRE', KEYS[2], ARGV[10])
            return 1
            """;

    /**
     * complete:
     * 1. 如果 state != COMPLETED，写入 COMPLETE
     * 2. 设置 state=COMPLETED
     * 3. stream / state 保留 1 分钟
     * <p>
     * KEYS:
     * 1 streamKey
     * 2 stateKey
     * <p>
     * ARGV:
     * 1 COMPLETE_STREAM_ID
     * 2 FIELD_TYPE
     * 3 TYPE_COMPLETE
     * 4 COMPLETE_TTL_SECONDS
     * 5 STATE_COMPLETED
     */
    private static final String COMPLETE_SCRIPT = """
            local state = redis.call('GET', KEYS[2])
            
            if state ~= ARGV[5] then
                redis.call(
                    'XADD',
                    KEYS[1],
                    ARGV[1],
                    ARGV[2], ARGV[3]
                )
            end
            
            redis.call('SET', KEYS[2], ARGV[5])
            redis.call('EXPIRE', KEYS[1], ARGV[4])
            redis.call('EXPIRE', KEYS[2], ARGV[4])
            return 1
            """;

    private final RedisAPI redis;
    private final Vertx vertx;

    /**
     * publisher 所在机器有完整本地日志。
     */
    private final ConcurrentMap<String, EventState> stateMap = new ConcurrentHashMap<>();

    /**
     * 同一个 eventId 的 Redis 写操作串行化，避免 complete 跑到 publish 前面。
     */
    private final ConcurrentMap<String, CompletableFuture<Void>> writeChainMap = new ConcurrentHashMap<>();

    /**
     * 同一个 JVM 内，同一个 eventId + consumerId 新连接替换旧连接。
     */
    private final ConcurrentMap<String, ActiveConsumer> activeConsumerMap = new ConcurrentHashMap<>();

    private final AtomicLong tokenGenerator = new AtomicLong(0);

    private final long cleanerTimerId;

    public RedisMessageQueue(RedisAPI redis, Vertx vertx) {
        this.redis = Objects.requireNonNull(redis, "redis can not be null");
        this.vertx = Objects.requireNonNull(vertx, "vertx can not be null");

        this.cleanerTimerId = vertx.setPeriodic(CLEAN_INTERVAL_MS, ignored -> cleanExpiredStates());
    }

    @Override
    public void create(String eventId) {
        requireNotBlank("eventId", eventId);

        completeActiveConsumers(eventId);

        EventState state = new EventState(System.currentTimeMillis() + ACTIVE_TTL_MS);
        stateMap.put(eventId, state);

        enqueueWrite(
                eventId,
                () -> redis.eval(List.of(
                        CREATE_SCRIPT,
                        "2",
                        streamKey(eventId),
                        stateKey(eventId),
                        STATE_RUNNING,
                        String.valueOf(ACTIVE_TTL_SECONDS)
                )),
                response -> {
                },
                err -> System.err.println("create failed, eventId=" + eventId + ", err=" + err.getMessage())
        );
    }

    @Override
    public void publish(String eventId, Long index, String message) {
        requireNotBlank("eventId", eventId);
        Objects.requireNonNull(index, "index can not be null");
        Objects.requireNonNull(message, "message can not be null");

        if (index <= 0) {
            throw new IllegalArgumentException("index must be greater than 0");
        }

        EventState state = stateMap.get(eventId);
        if (state == null) {
            throw new IllegalStateException("EventId not found: " + eventId + ", please call create() first");
        }

        if (state.completed) {
            throw new IllegalStateException("Queue already completed: " + eventId);
        }

        long now = System.currentTimeMillis();
        state.expireAt = now + ACTIVE_TTL_MS;

        /*
         * 本地先写。
         * 如果 consumer 刚好在本机，就可以直接从内存读取。
         */
        state.messageLog.put(index, message);

        /*
         * Redis 异步串行写。
         * 其他机器 consumer 依赖 Redis 兜底。
         */
        enqueueWrite(
                eventId,
                () -> redis.eval(List.of(
                        PUBLISH_SCRIPT,
                        "2",
                        streamKey(eventId),
                        stateKey(eventId),

                        toStreamId(index),

                        FIELD_TYPE,
                        TYPE_MESSAGE,

                        FIELD_INDEX,
                        String.valueOf(index),

                        FIELD_DATA,
                        message,

                        STATE_COMPLETED,
                        STATE_RUNNING,
                        String.valueOf(ACTIVE_TTL_SECONDS)
                )),
                response -> touchActiveThrottled(eventId),
                err -> System.err.println("publish failed, eventId=" + eventId
                        + ", index=" + index
                        + ", err=" + err.getMessage())
        );
    }

    @Override
    public void complete(String eventId) {
        requireNotBlank("eventId", eventId);

        EventState state = stateMap.get(eventId);
        if (state == null) {
            throw new IllegalStateException("EventId not found: " + eventId);
        }

        if (state.completed) {
            return;
        }

        state.completed = true;
        state.expireAt = System.currentTimeMillis() + COMPLETE_TTL_MS;

        enqueueWrite(
                eventId,
                () -> redis.eval(List.of(
                        COMPLETE_SCRIPT,
                        "2",
                        streamKey(eventId),
                        stateKey(eventId),

                        COMPLETE_STREAM_ID,

                        FIELD_TYPE,
                        TYPE_COMPLETE,

                        String.valueOf(COMPLETE_TTL_SECONDS),
                        STATE_COMPLETED
                )),
                response -> {
                },
                err -> {
                    System.err.println("complete failed, eventId=" + eventId + ", err=" + err.getMessage());
                    expire(streamKey(eventId), COMPLETE_TTL_SECONDS);
                    expire(stateKey(eventId), COMPLETE_TTL_SECONDS);
                }
        );
    }

    @Override
    public void consumer(String eventId,
                         String consumerId,
                         Long index,
                         Consumer<String> onNext,
                         Runnable onComplete) {
        requireNotBlank("eventId", eventId);
        requireNotBlank("consumerId", consumerId);
        Objects.requireNonNull(onNext, "onNext can not be null");
        Objects.requireNonNull(onComplete, "onComplete can not be null");

        long afterIndex = index == null ? 0L : index;
        if (afterIndex < 0) {
            throw new IllegalArgumentException("index can not be negative");
        }

        String activeKey = activeConsumerKey(eventId, consumerId);

        ActiveConsumer session = new ActiveConsumer(
                activeKey,
                tokenGenerator.incrementAndGet(),
                onComplete
        );

        ActiveConsumer old = activeConsumerMap.put(activeKey, session);
        if (old != null) {
            old.complete();
        }

        /*
         * 本机有 EventState，优先走本地。
         * 本机没有，说明 consumer 落在其他机器，走 Redis。
         */
        EventState localState = stateMap.get(eventId);
        if (localState != null) {
            localLoop(eventId, consumerId, session, afterIndex, onNext);
            return;
        }

        checkRemoteReadableOrComplete(
                eventId,
                () -> redisLoop(eventId, consumerId, session, afterIndex, onNext),
                session::complete
        );
    }

    @Override
    public void delete(String eventId) {
        requireNotBlank("eventId", eventId);

        EventState state = stateMap.remove(eventId);
        if (state != null) {
            state.completed = true;
            state.expireAt = System.currentTimeMillis();
        }

        completeActiveConsumers(eventId);

        enqueueWrite(
                eventId,
                () -> redis.del(List.of(streamKey(eventId), stateKey(eventId))),
                response -> writeChainMap.remove(eventId),
                err -> System.err.println("delete failed, eventId=" + eventId + ", err=" + err.getMessage())
        );
    }

    @Override
    public void exists(String eventId, Consumer<Boolean> callback) {
        requireNotBlank("eventId", eventId);
        Objects.requireNonNull(callback, "callback can not be null");

        if (stateMap.containsKey(eventId)) {
            callback.accept(true);
            return;
        }

        redis.exists(List.of(stateKey(eventId), streamKey(eventId)))
                .onSuccess(response -> callback.accept(responseToLong(response) > 0))
                .onFailure(err -> {
                    System.err.println("exists failed, eventId=" + eventId + ", err=" + err.getMessage());
                    callback.accept(false);
                });
    }

    public void shutdown() {
        vertx.cancelTimer(cleanerTimerId);

        activeConsumerMap.forEach((key, session) -> session.complete());
        activeConsumerMap.clear();

        stateMap.clear();
        writeChainMap.clear();
    }

    // -------------------------------------------------------------------------
    // local first
    // -------------------------------------------------------------------------

    private void localLoop(String eventId,
                           String consumerId,
                           ActiveConsumer session,
                           long afterIndex,
                           Consumer<String> onNext) {
        if (!isConsumerActive(session)) {
            return;
        }

        EventState state = stateMap.get(eventId);
        if (state == null) {
            /*
             * 本地状态被清理了，兜底走 Redis。
             */
            checkRemoteReadableOrComplete(
                    eventId,
                    () -> redisLoop(eventId, consumerId, session, afterIndex, onNext),
                    session::complete
            );
            return;
        }

        long currentIndex = afterIndex;

        for (var entry : state.messageLog.tailMap(afterIndex, false).entrySet()) {
            if (!isConsumerActive(session)) {
                return;
            }

            Long messageIndex = entry.getKey();
            String message = entry.getValue();

            if (messageIndex == null || messageIndex <= currentIndex) {
                continue;
            }

            try {
                onNext.accept(message);
            } catch (Exception e) {
                System.err.println("local onNext failed, eventId=" + eventId
                        + ", consumerId=" + consumerId
                        + ", index=" + messageIndex
                        + ", err=" + e.getMessage());

                long retryIndex = currentIndex;
                vertx.setTimer(RETRY_INTERVAL_MS, ignored ->
                        localLoop(eventId, consumerId, session, retryIndex, onNext));
                return;
            }

            currentIndex = messageIndex;
        }

        if (state.completed) {
            session.complete();
            return;
        }

        long nextIndex = currentIndex;
        vertx.setTimer(POLL_INTERVAL_MS, ignored ->
                localLoop(eventId, consumerId, session, nextIndex, onNext));
    }

    // -------------------------------------------------------------------------
    // Redis fallback
    // -------------------------------------------------------------------------

    private void redisLoop(String eventId,
                           String consumerId,
                           ActiveConsumer session,
                           long afterIndex,
                           Consumer<String> onNext) {
        if (!isConsumerActive(session)) {
            return;
        }

        redis.xread(List.of(
                "COUNT", String.valueOf(BATCH_SIZE),
                "STREAMS", streamKey(eventId), toStartStreamId(afterIndex)
        )).onSuccess(response -> {
            if (!isConsumerActive(session)) {
                return;
            }

            List<Response> entries = extractEntries(response, streamKey(eventId));

            if (entries == null || entries.isEmpty()) {
                checkRemoteContinueOrComplete(eventId, consumerId, session, afterIndex, onNext);
                return;
            }

            processRemoteEntries(eventId, consumerId, session, afterIndex, entries, 0, onNext);
        }).onFailure(err -> {
            if (!isConsumerActive(session)) {
                return;
            }

            System.err.println("XREAD failed, eventId=" + eventId
                    + ", consumerId=" + consumerId
                    + ", index=" + afterIndex
                    + ", err=" + err.getMessage());

            vertx.setTimer(RETRY_INTERVAL_MS, ignored ->
                    checkRemoteContinueOrComplete(eventId, consumerId, session, afterIndex, onNext));
        });
    }

    private void processRemoteEntries(String eventId,
                                      String consumerId,
                                      ActiveConsumer session,
                                      long currentIndex,
                                      List<Response> entries,
                                      int entryIndex,
                                      Consumer<String> onNext) {
        if (!isConsumerActive(session)) {
            return;
        }

        if (entryIndex >= entries.size()) {
            redisLoop(eventId, consumerId, session, currentIndex, onNext);
            return;
        }

        Response entry = entries.get(entryIndex);
        if (entry == null || entry.size() < 2) {
            processRemoteEntries(eventId, consumerId, session, currentIndex, entries, entryIndex + 1, onNext);
            return;
        }

        String streamId = entry.get(0).toString();
        Response fields = entry.get(1);

        String type = getField(fields, FIELD_TYPE);

        if (TYPE_COMPLETE.equals(type)) {
            session.complete();
            return;
        }

        if (TYPE_MESSAGE.equals(type)) {
            long messageIndex = resolveIndex(streamId, fields);
            String message = getField(fields, FIELD_DATA);

            if (messageIndex <= currentIndex) {
                processRemoteEntries(eventId, consumerId, session, currentIndex, entries, entryIndex + 1, onNext);
                return;
            }

            try {
                onNext.accept(message);
            } catch (Exception e) {
                System.err.println("redis onNext failed, eventId=" + eventId
                        + ", consumerId=" + consumerId
                        + ", index=" + messageIndex
                        + ", err=" + e.getMessage());

                vertx.setTimer(RETRY_INTERVAL_MS, ignored ->
                        redisLoop(eventId, consumerId, session, currentIndex, onNext));
                return;
            }

            processRemoteEntries(eventId, consumerId, session, messageIndex, entries, entryIndex + 1, onNext);
            return;
        }

        long nextIndex = parseIndexFromStreamId(streamId);
        if (nextIndex <= currentIndex) {
            nextIndex = currentIndex;
        }

        processRemoteEntries(eventId, consumerId, session, nextIndex, entries, entryIndex + 1, onNext);
    }

    private void checkRemoteReadableOrComplete(String eventId,
                                               Runnable onReadable,
                                               Runnable onComplete) {
        redis.exists(List.of(stateKey(eventId), streamKey(eventId)))
                .onSuccess(response -> {
                    if (responseToLong(response) > 0) {
                        onReadable.run();
                    } else {
                        onComplete.run();
                    }
                })
                .onFailure(err -> {
                    System.err.println("check readable failed, eventId=" + eventId
                            + ", err=" + err.getMessage());
                    onComplete.run();
                });
    }

    private void checkRemoteContinueOrComplete(String eventId,
                                               String consumerId,
                                               ActiveConsumer session,
                                               long afterIndex,
                                               Consumer<String> onNext) {
        if (!isConsumerActive(session)) {
            return;
        }

        redis.get(stateKey(eventId))
                .onSuccess(state -> {
                    if (!isConsumerActive(session)) {
                        return;
                    }

                    if (state == null) {
                        session.complete();
                        return;
                    }

                    String stateValue = state.toString();

                    if (STATE_COMPLETED.equals(stateValue)) {
                        /*
                         * state 已 completed，但 COMPLETE 消息可能还没被本轮读到。
                         * 再尝试读一次，读不到再 complete。
                         */
                        drainRemoteAfterComplete(eventId, consumerId, session, afterIndex, onNext);
                        return;
                    }

                    vertx.setTimer(POLL_INTERVAL_MS, ignored ->
                            redisLoop(eventId, consumerId, session, afterIndex, onNext));
                })
                .onFailure(err -> {
                    if (!isConsumerActive(session)) {
                        return;
                    }

                    System.err.println("check remote state failed, eventId=" + eventId
                            + ", consumerId=" + consumerId
                            + ", err=" + err.getMessage());

                    vertx.setTimer(RETRY_INTERVAL_MS, ignored ->
                            redisLoop(eventId, consumerId, session, afterIndex, onNext));
                });
    }

    private void drainRemoteAfterComplete(String eventId,
                                          String consumerId,
                                          ActiveConsumer session,
                                          long afterIndex,
                                          Consumer<String> onNext) {
        if (!isConsumerActive(session)) {
            return;
        }

        redis.xread(List.of(
                "COUNT", String.valueOf(BATCH_SIZE),
                "STREAMS", streamKey(eventId), toStartStreamId(afterIndex)
        )).onSuccess(response -> {
            if (!isConsumerActive(session)) {
                return;
            }

            List<Response> entries = extractEntries(response, streamKey(eventId));

            if (entries == null || entries.isEmpty()) {
                session.complete();
                return;
            }

            processRemoteEntries(eventId, consumerId, session, afterIndex, entries, 0, onNext);
        }).onFailure(err -> {
            System.err.println("drain after complete failed, eventId=" + eventId
                    + ", consumerId=" + consumerId
                    + ", err=" + err.getMessage());
            session.complete();
        });
    }

    // -------------------------------------------------------------------------
    // write chain
    // -------------------------------------------------------------------------

    private void enqueueWrite(String eventId,
                              Supplier<Future<Response>> operation,
                              Consumer<Response> onSuccess,
                              Consumer<Throwable> onFailure) {
        writeChainMap.compute(eventId, (key, previous) -> {
            CompletableFuture<Void> base = previous == null
                    ? CompletableFuture.completedFuture(null)
                    : previous;

            CompletableFuture<Void> next = base
                    .handle((value, error) -> null)
                    .thenCompose(ignored -> {
                        try {
                            return operation.get().toCompletionStage();
                        } catch (Throwable e) {
                            CompletableFuture<Response> failed = new CompletableFuture<>();
                            failed.completeExceptionally(e);
                            return failed;
                        }
                    })
                    .thenAccept(response -> {
                        if (onSuccess != null) {
                            onSuccess.accept(response);
                        }
                    })
                    .exceptionally(error -> {
                        Throwable real = unwrap(error);
                        if (onFailure != null) {
                            onFailure.accept(real);
                        }
                        return null;
                    })
                    .toCompletableFuture();

            return next;
        });
    }

    private void touchActiveThrottled(String eventId) {
        EventState state = stateMap.get(eventId);
        if (state == null || state.completed) {
            return;
        }

        if (TOUCH_INTERVAL_MS > 0) {
            long now = System.currentTimeMillis();

            if (now - state.lastTouchTime < TOUCH_INTERVAL_MS) {
                return;
            }

            state.lastTouchTime = now;
        }

        expire(streamKey(eventId), ACTIVE_TTL_SECONDS);
        expire(stateKey(eventId), ACTIVE_TTL_SECONDS);
    }

    private void expire(String key, long seconds) {
        redis.expire(List.of(key, String.valueOf(seconds)))
                .onFailure(err ->
                        System.err.println("EXPIRE failed, key=" + key + ", err=" + err.getMessage())
                );
    }

    // -------------------------------------------------------------------------
    // cleanup / utilities
    // -------------------------------------------------------------------------

    private void cleanExpiredStates() {
        long now = System.currentTimeMillis();

        stateMap.forEach((eventId, state) -> {
            if (now >= state.expireAt) {
                boolean removed = stateMap.remove(eventId, state);
                if (removed) {
                    writeChainMap.remove(eventId);
                    completeActiveConsumers(eventId);
                }
            }
        });
    }

    private void completeActiveConsumers(String eventId) {
        String prefix = eventId + "::";

        activeConsumerMap.forEach((key, session) -> {
            if (key.startsWith(prefix)) {
                session.complete();
            }
        });
    }

    private boolean isConsumerActive(ActiveConsumer session) {
        ActiveConsumer current = activeConsumerMap.get(session.activeKey);
        return current == session && !session.completed;
    }

    private List<Response> extractEntries(Response response, String key) {
        if (response == null) {
            return null;
        }

        for (Response streamResp : response) {
            if (streamResp == null || streamResp.size() < 2) {
                continue;
            }

            if (key.equals(streamResp.get(0).toString())) {
                Response messages = streamResp.get(1);
                if (messages == null) {
                    return null;
                }
                return messages.stream().toList();
            }
        }

        return null;
    }

    private String getField(Response fields, String fieldName) {
        if (fields == null) {
            return null;
        }

        for (int i = 0; i + 1 < fields.size(); i += 2) {
            if (fieldName.equals(fields.get(i).toString())) {
                return fields.get(i + 1).toString();
            }
        }

        return null;
    }

    private long resolveIndex(String streamId, Response fields) {
        String indexValue = getField(fields, FIELD_INDEX);

        if (indexValue != null && !indexValue.isBlank()) {
            try {
                return Long.parseLong(indexValue);
            } catch (NumberFormatException ignored) {
                // fallback
            }
        }

        return parseIndexFromStreamId(streamId);
    }

    private long parseIndexFromStreamId(String streamId) {
        if (streamId == null || streamId.isBlank()) {
            return 0;
        }

        int dashIndex = streamId.indexOf('-');
        if (dashIndex <= 0) {
            return 0;
        }

        try {
            return Long.parseLong(streamId.substring(0, dashIndex));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long responseToLong(Response response) {
        if (response == null) {
            return 0;
        }

        try {
            return Long.parseLong(response.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Throwable unwrap(Throwable error) {
        if (error instanceof CompletionException && error.getCause() != null) {
            return error.getCause();
        }
        return error;
    }

    private static String toStreamId(Long index) {
        return index + "-0";
    }

    private static String toStartStreamId(long afterIndex) {
        if (afterIndex <= 0) {
            return "0-0";
        }
        return afterIndex + "-0";
    }

    private static String activeConsumerKey(String eventId, String consumerId) {
        return eventId + "::" + consumerId;
    }

    private static String streamKey(String eventId) {
        return "mq:{" + eventId + "}:stream";
    }

    private static String stateKey(String eventId) {
        return "mq:{" + eventId + "}:state";
    }

    private static void requireNotBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " can not be blank");
        }
    }

    private static class EventState {

        private final ConcurrentSkipListMap<Long, String> messageLog = new ConcurrentSkipListMap<>();

        private volatile boolean completed = false;

        private volatile long expireAt;

        private volatile long lastTouchTime = 0L;

        private EventState(long expireAt) {
            this.expireAt = expireAt;
        }
    }

    private class ActiveConsumer {

        private final String activeKey;

        private final long token;

        private final Runnable onComplete;

        private volatile boolean completed = false;

        private ActiveConsumer(String activeKey, long token, Runnable onComplete) {
            this.activeKey = activeKey;
            this.token = token;
            this.onComplete = onComplete;
        }

        private void complete() {
            if (completed) {
                return;
            }

            completed = true;
            activeConsumerMap.remove(activeKey, this);

            try {
                onComplete.run();
            } catch (Exception e) {
                System.err.println("onComplete failed, activeKey=" + activeKey
                        + ", token=" + token
                        + ", err=" + e.getMessage());
            }
        }
    }
}