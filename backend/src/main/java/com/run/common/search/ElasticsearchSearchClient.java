package com.run.common.search;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.util.NamedValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public class ElasticsearchSearchClient implements SearchClient {

    private final ElasticsearchAsyncClient client;
    private final ObjectMapper objectMapper;
    private final List<String> defaultSearchFields;

    public ElasticsearchSearchClient(
            ElasticsearchAsyncClient client,
            ObjectMapper objectMapper,
            List<String> defaultSearchFields
    ) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.defaultSearchFields = defaultSearchFields == null ? List.of() : List.copyOf(defaultSearchFields);
    }

    @Override
    public CompletionStage<Void> index(SearchDocument document) {
        return client.index(i -> i
                        .index(document.getIndex())
                        .id(document.getId())
                        .document(document.getFields()))
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        throw wrap(throwable);
                    }
                    return null;
                });
    }

    @Override
    public CompletionStage<Void> bulkIndex(Collection<SearchDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        BulkRequest.Builder builder = new BulkRequest.Builder();
        for (SearchDocument document : documents) {
            builder.operations(op -> op.index(idx -> idx
                    .index(document.getIndex())
                    .id(document.getId())
                    .document(document.getFields())));
        }

        return client.bulk(builder.build())
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        throw wrap(throwable);
                    }
                    ensureBulkSuccess(response);
                    return null;
                });
    }

    @Override
    public CompletionStage<Void> updateById(String index, String id, Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return client.update(u -> u
                        .index(index)
                        .id(id)
                        .doc(fields), ObjectNode.class)
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        throw wrap(throwable);
                    }
                    return null;
                });
    }

    @Override
    public CompletionStage<Void> deleteById(String index, String id) {
        return client.delete(d -> d
                        .index(index)
                        .id(id))
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        throw wrap(throwable);
                    }
                    return null;
                });
    }

    @Override
    public CompletionStage<Void> deleteByIds(String index, Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        BulkRequest.Builder builder = new BulkRequest.Builder();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                builder.operations(op -> op.delete(d -> d.index(index).id(id)));
            }
        }

        return client.bulk(builder.build())
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        throw wrap(throwable);
                    }
                    ensureBulkSuccess(response);
                    return null;
                });
    }

    @Override
    public CompletionStage<Void> deleteByQuery(com.run.common.search.SearchQuery query) {
        com.run.common.search.SearchRequest searchRequest = com.run.common.search.SearchRequest.from(query);
        return client.deleteByQuery(d -> d
                        .index(searchRequest.getIndex())
                        .query(buildQuery(searchRequest)))
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        throw wrap(throwable);
                    }
                    return null;
                });
    }

    @Override
    public CompletionStage<Boolean> exists(String index, String id) {
        return client.exists(e -> e
                        .index(index)
                        .id(id))
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        throw wrap(throwable);
                    }
                    return response.value();
                });
    }

    @Override
    public CompletionStage<SearchResult<SearchDocument>> search(com.run.common.search.SearchRequest request) {
        if (request.getKnn() != null) {
            throw new SearchException("Elasticsearch vector search is not implemented in this artifact yet. Lucene kNN is supported.");
        }
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(request.getIndex())
                .from(request.getFrom())
                .size(request.getPageSize())
                .query(buildQuery(request));

        if (!request.getReturnFields().isEmpty()) {
            builder.source(s -> s.filter(f -> f.includes(request.getReturnFields())));
        }

        for (SearchSort sort : request.getSorts()) {
            if (sort.getType() == SearchSort.Type.SCORE) {
                builder.sort(s -> s.score(sc -> sc.order(sort.isDesc() ? SortOrder.Desc : SortOrder.Asc)));
            } else {
                builder.sort(s -> s.field(f -> f
                        .field(sortField(sort.getField(), sort))
                        .order(sort.isDesc() ? SortOrder.Desc : SortOrder.Asc)));
            }
        }

        if (request.getHighlight() != null && !request.getHighlight().getFields().isEmpty()) {
            builder.highlight(h -> {
                h.preTags(request.getHighlight().getPreTag());
                h.postTags(request.getHighlight().getPostTag());
                for (String field : request.getHighlight().getFields()) {
                    h.fields(NamedValue.of(field, HighlightField.of(f -> f)));
                }
                return h;
            });
        }

        return client.search(builder.build(), ObjectNode.class)
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        throw wrap(throwable);
                    }

                    long total = response.hits().total() == null
                            ? response.hits().hits().size()
                            : response.hits().total().value();

                    List<SearchHit<SearchDocument>> hits = new ArrayList<>();
                    response.hits().hits().forEach(hit -> {
                        Map<String, Object> source = hit.source() == null
                                ? Map.of()
                                : objectMapper.convertValue(hit.source(), new TypeReference<Map<String, Object>>() {
                        });

                        float score = hit.score() == null ? 0F : hit.score().floatValue();
                        String index = hit.index();
                        String id = hit.id();

                        Map<String, List<String>> highlights = new LinkedHashMap<>();
                        if (hit.highlight() != null) {
                            hit.highlight().forEach((field, fragments) -> highlights.put(field, List.copyOf(fragments)));
                        }

                        hits.add(new SearchHit<>(
                                index,
                                id,
                                score,
                                new SearchDocument(index, id, source),
                                highlights
                        ));
                    });

                    return new SearchResult<>(total, request.getPageNo(), request.getPageSize(), hits);
                });
    }

    @Override
    public void close() throws Exception {
        this.client.close();
    }

    private Query buildQuery(com.run.common.search.SearchRequest request) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        boolean hasClause = false;

        if (!request.getIds().isEmpty()) {
            bool.filter(f -> f.ids(ids -> ids.values(request.getIds())));
            hasClause = true;
        }

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            List<String> fields = request.getKeywordFields().isEmpty()
                    ? defaultSearchFields
                    : request.getKeywordFields();

            if (fields == null || fields.isEmpty()) {
                bool.must(m -> m.simpleQueryString(s -> s.query(request.getKeyword())));
            } else {
                bool.must(m -> m.multiMatch(mm -> mm
                        .query(request.getKeyword())
                        .fields(fields)));
            }
            hasClause = true;
        }

        for (Map.Entry<String, List<String>> entry : request.getExactFilters().entrySet()) {
            List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }

            String field = exactField(entry.getKey());
            if (values.size() == 1) {
                bool.filter(f -> f.term(t -> t
                        .field(field)
                        .value(values.get(0))));
            } else {
                List<FieldValue> fieldValues = values.stream().map(FieldValue::of).toList();
                bool.filter(f -> f.terms(t -> t
                        .field(field)
                        .terms(v -> v.value(fieldValues))));
            }
            hasClause = true;
        }

        for (SearchExactBoost exactBoost : request.getExactBoosts()) {
            bool.should(s -> s.term(t -> t
                    .field(exactField(exactBoost.getField()))
                    .value(exactBoost.getValue())
                    .boost(exactBoost.getBoost())));
            hasClause = true;
        }

        if (!hasClause) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        return Query.of(q -> q.bool(bool.build()));
    }

    private void ensureBulkSuccess(BulkResponse response) {
        if (!response.errors()) {
            return;
        }

        String message = response.items().stream()
                .filter(item -> item.error() != null)
                .map(item -> {
                    String id = item.id() == null ? "" : item.id();
                    String reason = item.error().reason() == null ? "unknown error" : item.error().reason();
                    return id + ": " + reason;
                })
                .reduce((a, b) -> a + "; " + b)
                .orElse("bulk request failed");

        throw new SearchException("bulk request failed: " + message);
    }

    private static String exactField(String field) {
        return field.endsWith(".keyword") ? field : field + ".keyword";
    }

    private static String sortField(String field, SearchSort sort) {
        return sort.getType() == SearchSort.Type.STRING ? exactField(field) : field;
    }

    private RuntimeException wrap(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;
        return cause instanceof RuntimeException ? (RuntimeException) cause : new SearchException(cause);
    }
}
