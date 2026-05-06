package com.run.common.freemarker;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.*;

import java.util.ArrayList;
import java.util.Map;

public class JsonHashModel implements TemplateHashModelEx, TemplateScalarModel {

    private final Map<?, ?> map;
    private final ObjectWrapper wrapper;
    private final ObjectMapper objectMapper;

    public JsonHashModel(Map<?, ?> map, ObjectWrapper wrapper, ObjectMapper objectMapper) {
        this.map = map;
        this.wrapper = wrapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        if (!map.containsKey(key)) {
            return null;
        }
        return wrapper.wrap(map.get(key));
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public TemplateCollectionModel keys() {
        return new JsonSequenceModel(new ArrayList<>(map.keySet()), wrapper, objectMapper);
    }

    @Override
    public TemplateCollectionModel values() {
        return new JsonSequenceModel(new ArrayList<>(map.values()), wrapper, objectMapper);
    }

    @Override
    public String getAsString() {
        return JsonStringUtils.toJsonString(objectMapper, map);
    }
}