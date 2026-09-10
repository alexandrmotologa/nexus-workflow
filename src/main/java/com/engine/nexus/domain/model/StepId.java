package com.engine.nexus.domain.model;

import java.util.Objects;

public record StepId(String value) {
    public StepId {
        Objects.requireNonNull(value, "StepId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("StepId value must not be blank");
        }
    }

    public static StepId of(String value) {
        return new StepId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
