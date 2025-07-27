package com.run.route;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.run.common.openai.request.message.UserMessage;
import com.run.common.route.IRoute;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.entity.WorkFlow;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/30  18:16}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ChatRoute implements IRoute {

    protected Router apiRoute;

    protected Pool pool;

    @Inject
    public ChatRoute(@Named("apiRoute") Router apiRoute, Pool pool) {
        this.apiRoute = apiRoute;
        this.pool = pool;
    }

    @SneakyThrows
    private void chat() {
        String w = "{\n" +
                "  \"edges\": [\n" +
                "    {\n" +
                "      \"id\": \"9270d279-1760-4aac-9545-77b9c15fd336\",\n" +
                "      \"type\": \"app-edge\",\n" +
                "      \"endPoint\": {\n" +
                "        \"x\": 790,\n" +
                "        \"y\": 3450.082\n" +
                "      },\n" +
                "      \"pointsList\": [\n" +
                "        {\n" +
                "          \"x\": 630,\n" +
                "          \"y\": 3490\n" +
                "        },\n" +
                "        {\n" +
                "          \"x\": 740,\n" +
                "          \"y\": 3490\n" +
                "        },\n" +
                "        {\n" +
                "          \"x\": 680,\n" +
                "          \"y\": 3450.082\n" +
                "        },\n" +
                "        {\n" +
                "          \"x\": 790,\n" +
                "          \"y\": 3450.082\n" +
                "        }\n" +
                "      ],\n" +
                "      \"properties\": {},\n" +
                "      \"startPoint\": {\n" +
                "        \"x\": 630,\n" +
                "        \"y\": 3490\n" +
                "      },\n" +
                "      \"sourceNodeId\": \"start-node\",\n" +
                "      \"targetNodeId\": \"1a76099c-48b3-41d0-a4d3-7b7d33d90c80\",\n" +
                "      \"sourceAnchorId\": \"start-node_right\",\n" +
                "      \"targetAnchorId\": \"1a76099c-48b3-41d0-a4d3-7b7d33d90c80_left\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"nodes\": [\n" +
                "    {\n" +
                "      \"x\": 360,\n" +
                "      \"y\": 2820.328,\n" +
                "      \"id\": \"base-node\",\n" +
                "      \"type\": \"base-node\",\n" +
                "      \"properties\": {\n" +
                "        \"config\": {},\n" +
                "        \"height\": 849.531,\n" +
                "        \"showNode\": true,\n" +
                "        \"stepName\": \"基本信息\",\n" +
                "        \"node_data\": {\n" +
                "          \"desc\": \"\",\n" +
                "          \"name\": \"胖虎专用\",\n" +
                "          \"prologue\": \"您好，我是 XXX 小助手，您可以向我提出 XXX 使用问题。\\n- XXX 主要功能有什么？\\n- XXX 如何收费？\\n- 需要转人工服务\",\n" +
                "          \"tts_type\": \"TTS\",\n" +
                "          \"stt_autosend\": true,\n" +
                "          \"stt_model_id\": \"e7397070-f602-11ef-afe8-0242ac120002\",\n" +
                "          \"tts_autoplay\": true,\n" +
                "          \"tts_model_id\": \"0a06f5a4-bddd-11ef-9f32-0242ac120003\",\n" +
                "          \"stt_model_enable\": true,\n" +
                "          \"tts_model_enable\": true,\n" +
                "          \"file_upload_enable\": true,\n" +
                "          \"file_upload_setting\": {\n" +
                "            \"audio\": false,\n" +
                "            \"image\": false,\n" +
                "            \"video\": false,\n" +
                "            \"document\": true,\n" +
                "            \"maxFiles\": 3,\n" +
                "            \"fileLimit\": 50\n" +
                "          },\n" +
                "          \"tts_model_params_setting\": {\n" +
                "            \"voice\": \"longxiaochun\",\n" +
                "            \"speech_rate\": 1.3\n" +
                "          }\n" +
                "        },\n" +
                "        \"input_field_list\": [],\n" +
                "        \"user_input_config\": {\n" +
                "          \"title\": \"用户输入\"\n" +
                "        },\n" +
                "        \"api_input_field_list\": [],\n" +
                "        \"user_input_field_list\": []\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"x\": 470,\n" +
                "      \"y\": 3490,\n" +
                "      \"id\": \"start-node\",\n" +
                "      \"type\": \"start-node\",\n" +
                "      \"properties\": {\n" +
                "        \"config\": {\n" +
                "          \"fields\": [\n" +
                "            {\n" +
                "              \"label\": \"用户问题\",\n" +
                "              \"value\": \"question\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"label\": \"文档\",\n" +
                "              \"value\": \"document\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"globalFields\": [\n" +
                "            {\n" +
                "              \"label\": \"当前时间\",\n" +
                "              \"value\": \"time\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"label\": \"历史聊天记录\",\n" +
                "              \"value\": \"history_context\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"label\": \"对话 ID\",\n" +
                "              \"value\": \"chat_id\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        \"fields\": [\n" +
                "          {\n" +
                "            \"label\": \"用户问题\",\n" +
                "            \"value\": \"question\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"height\": 408,\n" +
                "        \"showNode\": true,\n" +
                "        \"stepName\": \"开始\",\n" +
                "        \"globalFields\": [\n" +
                "          {\n" +
                "            \"label\": \"当前时间\",\n" +
                "            \"value\": \"time\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"x\": 950,\n" +
                "      \"y\": 3450.082,\n" +
                "      \"id\": \"1a76099c-48b3-41d0-a4d3-7b7d33d90c80\",\n" +
                "      \"type\": \"ai-chat-node\",\n" +
                "      \"properties\": {\n" +
                "        \"config\": {\n" +
                "          \"fields\": [\n" +
                "            {\n" +
                "              \"label\": \"AI 回答内容\",\n" +
                "              \"value\": \"answer\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"label\": \"思考过程\",\n" +
                "              \"value\": \"reasoning_content\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        \"height\": 977.383,\n" +
                "        \"showNode\": true,\n" +
                "        \"stepName\": \"AI 对话\",\n" +
                "        \"condition\": \"AND\",\n" +
                "        \"node_data\": {\n" +
                "          \"prompt\": \" {{开始.question}}\\n 中文回答\",\n" +
                "          \"system\": \"\",\n" +
                "          \"model_id\": \"92463790-e696-11ef-b3af-0242ac120002\",\n" +
                "          \"is_result\": true,\n" +
                "          \"max_tokens\": null,\n" +
                "          \"temperature\": null,\n" +
                "          \"dialogue_type\": \"WORKFLOW\",\n" +
                "          \"model_setting\": {\n" +
                "            \"reasoning_content_end\": \"</think>\",\n" +
                "            \"reasoning_content_start\": \"<think>\",\n" +
                "            \"reasoning_content_enable\": true\n" +
                "          },\n" +
                "          \"dialogue_number\": 2,\n" +
                "          \"model_params_setting\": {\n" +
                "            \"max_tokens\": 26077,\n" +
                "            \"temperature\": 0.7\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        ObjectMapper objectMapper = new ObjectMapper();

        Map map = objectMapper.readValue(w, Map.class);


        apiRoute.get("/chat")
                .handler((context) -> {
                    context.response().setChunked(true);
                    context.response().putHeader("Content-Type", "text/event-stream;charset=utf-8");
                    context.response().putHeader("Cache-Control", "no-cache");
                    context.response().putHeader("Character-Encoding", "utf-8");
                    context.response().write(Buffer.buffer("", "utf-8"));
                    WorkFlowManage workFlowManage = new WorkFlowManage(WorkFlow.of(new JsonObject(map)), List.of(new UserMessage("你好")), new JsonObject(), new JsonObject(), (chunk, isEnd) -> {
                        if (isEnd) {
                            context.response().end();
                            return;
                        }
                        String content = chunk.getChoices().get(0).getDelta().getContent();
                        if (StringUtils.isNotEmpty(content)) {
                            context.response().write(Buffer.buffer(content, "utf-8"));
                        }
                    });
                    workFlowManage.invoke();

                });
    }


    @Override
    public void initRoute() {
        chat();
    }

    @Override
    public void initOpenApi() {

    }
}
