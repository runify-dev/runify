package com.run.common.apispec;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

import java.util.*;

public class ApiSpec {

    private final LinkedHashMap<String, Schema<?>> pathParams;
    private final LinkedHashMap<String, Schema<?>> queryParams;
    private final LinkedHashMap<String, Schema<?>> fileParams;
    private final LinkedHashMap<String, Schema<?>> formParams;
    private final Schema<?> body;

    private ApiSpec(LinkedHashMap<String, Schema<?>> pathParams,
                    LinkedHashMap<String, Schema<?>> queryParams,
                    LinkedHashMap<String, Schema<?>> fileParams,
                    LinkedHashMap<String, Schema<?>> formParams,
                    Schema<?> body) {
        this.pathParams = pathParams;
        this.queryParams = queryParams;
        this.fileParams = fileParams;
        this.formParams = formParams;
        this.body = body;
    }

    /**
     * 校验 RoutingContext 中的 path params、query params、body、file params、form params
     *
     * @throws ValidationException 校验失败时抛出
     */
    public void validate(RoutingContext context) {
        List<ValidationException.ValidationError> errors = validateQuietly(context);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /**
     * 校验 RoutingContext，返回错误列表，不抛异常
     */
    public List<ValidationException.ValidationError> validateQuietly(RoutingContext context) {
        List<ValidationException.ValidationError> errors = new ArrayList<>();

        // Validate path params
        if (!pathParams.isEmpty()) {
            JsonObject pathData = new JsonObject();
            for (String name : pathParams.keySet()) {
                String value = context.pathParam(name);
                if (value != null) pathData.put(name, value);
            }
            errors.addAll(SchemaValidator.validateQuietly("path", toObjectSchema(pathParams), pathData));
        }

        // Validate query params
        if (!queryParams.isEmpty()) {
            JsonObject queryData = new JsonObject();
            for (String name : queryParams.keySet()) {
                String value = context.queryParams().get(name);
                if (value != null) queryData.put(name, value);
            }
            errors.addAll(SchemaValidator.validateQuietly("query", toObjectSchema(queryParams), queryData));
        }

        // Validate body (JSON)
        if (body != null) {
            try {
                JsonObject bodyData = context.body().asJsonObject();
                if (bodyData == null) bodyData = new JsonObject();
                errors.addAll(SchemaValidator.validateQuietly("body", body, bodyData));
            } catch (Exception e) {
                errors.add(new ValidationException.ValidationError("body", "", "请求体格式不正确，需要JSON格式"));
            }
        }

        // Validate file params
        if (!fileParams.isEmpty()) {
            List<FileUpload> uploads = context.fileUploads();
            for (Map.Entry<String, Schema<?>> entry : fileParams.entrySet()) {
                SchemaValidator.validateFileQuietly(entry.getKey(), entry.getValue(), uploads, errors);
            }
        }

        // Validate form params (multipart form fields)
        if (!formParams.isEmpty()) {
            JsonObject formData = new JsonObject();
            for (String name : formParams.keySet()) {
                String value = context.request().getFormAttribute(name);
                if (value != null) formData.put(name, value);
            }
            errors.addAll(SchemaValidator.validateQuietly("form", toObjectSchema(formParams), formData));
        }

        return errors;
    }

    // ===== OpenAPI 转换 =====

    /**
     * 生成 OpenAPI Operation 对象
     */
    public Operation toOperation(String description, List<String> tags) {
        Operation op = new Operation();
        if (description != null) op.description(description);
        if (tags != null) tags.forEach(op::addTagsItem);

        // Path params → parameters(in=path)
        for (Map.Entry<String, Schema<?>> entry : pathParams.entrySet()) {
            op.addParametersItem(toParameter(entry.getKey(), entry.getValue(), "path"));
        }
        // Query params → parameters(in=query)
        for (Map.Entry<String, Schema<?>> entry : queryParams.entrySet()) {
            op.addParametersItem(toParameter(entry.getKey(), entry.getValue(), "query"));
        }

        // File / form params → requestBody(multipart/form-data)
        if (!fileParams.isEmpty() || !formParams.isEmpty()) {
            op.requestBody(buildMultipartRequestBody());
        }
        // JSON body → requestBody(application/json)
        else if (body != null) {
            op.requestBody(new RequestBody()
                    .content(new Content()
                            .addMediaType("application/json", new MediaType().schema(body))));
        }
        return op;
    }

    /**
     * 生成带安全认证的 OpenAPI Operation 对象
     */
    public Operation toSecuredOperation(String description, List<String> tags,
                                        List<io.swagger.v3.oas.models.security.SecurityRequirement> security) {
        Operation op = toOperation(description, tags);
        if (security != null) op.security(security);
        return op;
    }

    // ===== Builder =====

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final LinkedHashMap<String, Schema<?>> pathParams = new LinkedHashMap<>();
        private final LinkedHashMap<String, Schema<?>> queryParams = new LinkedHashMap<>();
        private final LinkedHashMap<String, Schema<?>> fileParams = new LinkedHashMap<>();
        private final LinkedHashMap<String, Schema<?>> formParams = new LinkedHashMap<>();
        private Schema<?> body;

        public Builder pathParam(String name, Schema<?> schema) {
            pathParams.put(name, schema);
            return this;
        }

        public Builder queryParam(String name, Schema<?> schema) {
            queryParams.put(name, schema);
            return this;
        }

        public Builder fileParam(String name, Schema<?> schema) {
            fileParams.put(name, schema);
            return this;
        }

        public Builder formParam(String name, Schema<?> schema) {
            formParams.put(name, schema);
            return this;
        }

        public Builder body(Schema<?> schema) {
            this.body = schema;
            return this;
        }

        public ApiSpec build() {
            return new ApiSpec(
                    new LinkedHashMap<>(pathParams),
                    new LinkedHashMap<>(queryParams),
                    new LinkedHashMap<>(fileParams),
                    new LinkedHashMap<>(formParams),
                    body
            );
        }
    }

    // ===== Internal helpers =====

    private static Parameter toParameter(String name, Schema<?> schema, String in) {
        Parameter p = new Parameter().name(name).in(in).schema(schema);
        if ("path".equals(in)) p.required(true);
        return p;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static JsonSchema toObjectSchema(LinkedHashMap<String, Schema<?>> params) {
        JsonSchema s = new JsonSchema();
        LinkedHashMap<String, Schema> raw = new LinkedHashMap<>();
        for (Map.Entry<String, Schema<?>> e : params.entrySet()) {
            raw.put(e.getKey(), (Schema) e.getValue());
        }
        s.setProperties(raw);
        s.setRequired(new ArrayList<>(params.keySet()));
        return s;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private RequestBody buildMultipartRequestBody() {
        List<String> required = new ArrayList<>();
        LinkedHashMap<String, Schema> properties = new LinkedHashMap<>();

        for (Map.Entry<String, Schema<?>> entry : fileParams.entrySet()) {
            properties.put(entry.getKey(), (Schema) entry.getValue());
            if (entry.getValue().getExtensions() != null
                    && entry.getValue().getExtensions().containsKey("x-required-message")) {
                required.add(entry.getKey());
            }
        }
        for (Map.Entry<String, Schema<?>> entry : formParams.entrySet()) {
            properties.put(entry.getKey(), (Schema) entry.getValue());
            if (entry.getValue().getExtensions() != null
                    && entry.getValue().getExtensions().containsKey("x-required-message")) {
                required.add(entry.getKey());
            }
        }

        ObjectSchema multipartSchema = new ObjectSchema();
        multipartSchema.setProperties(properties);
        if (!required.isEmpty()) {
            multipartSchema.setRequired(required);
        }

        return new RequestBody()
                .content(new Content()
                        .addMediaType("multipart/form-data", new MediaType().schema(multipartSchema)));
    }
}
