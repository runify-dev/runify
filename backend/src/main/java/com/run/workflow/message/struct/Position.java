package com.run.workflow.message.struct;

public record Position(String id, Integer index, Position children) {
    public Position(String id, Integer index) {
        this(id, index, null);
    }

    public Position(String id) {
        this(id, null, null);
    }

}
