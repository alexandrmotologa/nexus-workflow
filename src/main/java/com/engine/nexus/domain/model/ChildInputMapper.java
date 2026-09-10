package com.engine.nexus.domain.model;

import java.util.Map;

@FunctionalInterface
public interface ChildInputMapper {
    Map<String, Object> map(Map<String, Object> parentState);
}
