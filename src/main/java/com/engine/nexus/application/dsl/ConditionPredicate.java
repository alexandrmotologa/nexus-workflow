package com.engine.nexus.application.dsl;

@FunctionalInterface
public interface ConditionPredicate {
    boolean evaluate(WorkflowContext context);
}
