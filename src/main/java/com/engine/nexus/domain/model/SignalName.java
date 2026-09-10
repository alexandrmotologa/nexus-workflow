package com.engine.nexus.domain.model;

import java.util.Objects;

public record SignalName(String value) {
    public SignalName {
        Objects.requireNonNull(value, "SignalName value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("SignalName value must not be blank");
        }
    }

    public static SignalName of(String value) {
        return new SignalName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
