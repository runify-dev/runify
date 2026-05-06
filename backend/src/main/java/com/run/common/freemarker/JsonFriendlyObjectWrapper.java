package com.run.common.freemarker;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.*;

import java.lang.reflect.Array;
import java.util.*;

public class JsonFriendlyObjectWrapper extends DefaultObjectWrapper {

    private final ObjectMapper objectMapper;

    public JsonFriendlyObjectWrapper(Version incompatibleImprovements, ObjectMapper objectMapper) {
        super(incompatibleImprovements);
        this.objectMapper = objectMapper;
    }

    @Override
    public TemplateModel wrap(Object obj) throws TemplateModelException {
        if (obj instanceof io.vertx.core.json.JsonObject jsonObject) {
            obj = jsonObject.getMap();
        }

        if (obj instanceof io.vertx.core.json.JsonArray jsonArray) {
            obj = jsonArray.getList();
        }

        if (obj instanceof Map<?, ?> map) {
            return new JsonHashModel(map, this, objectMapper);
        }

        if (obj instanceof Collection<?> collection) {
            return new JsonSequenceModel(new ArrayList<>(collection), this, objectMapper);
        }

        if (obj != null && obj.getClass().isArray()) {
            int length = Array.getLength(obj);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(Array.get(obj, i));
            }
            return new JsonSequenceModel(list, this, objectMapper);
        }

        return super.wrap(obj);
    }
}