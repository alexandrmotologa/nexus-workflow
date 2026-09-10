package com.engine.nexus.application.dsl;

import java.util.Map;

@FunctionalInterface
public interface ActivityFunction {
    Map<String, Object> execute(WorkflowContext context) throws Exception;
}
