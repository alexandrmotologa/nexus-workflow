package com.engine.nexus.domain.model;

import java.util.Map;

@FunctionalInterface
public interface StepPredicate {
    boolean test(Map<String, Object> state);
}
