package com.run.common.freemarker;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.*;

import java.util.List;

public class JsonSequenceModel implements TemplateSequenceModel, TemplateCollectionModel, TemplateScalarModel {

    private final List<?> list;
    private final ObjectWrapper wrapper;
    private final ObjectMapper objectMapper;

    public JsonSequenceModel(List<?> list, ObjectWrapper wrapper, ObjectMapper objectMapper) {
        this.list = list;
        this.wrapper = wrapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public TemplateModel get(int index) throws TemplateModelException {
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return wrapper.wrap(list.get(index));
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public TemplateModelIterator iterator() {
        return new TemplateModelIterator() {
            private int index = 0;

            @Override
            public TemplateModel next() throws TemplateModelException {
                return wrapper.wrap(list.get(index++));
            }

            @Override
            public boolean hasNext() {
                return index < list.size();
            }
        };
    }

    @Override
    public String getAsString() {
        return JsonStringUtils.toJsonString(objectMapper, list);
    }
}