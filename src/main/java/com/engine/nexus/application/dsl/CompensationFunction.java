package com.engine.nexus.application.dsl;

@FunctionalInterface
public interface CompensationFunction {
    void compensate(WorkflowContext context) throws Exception;
}
