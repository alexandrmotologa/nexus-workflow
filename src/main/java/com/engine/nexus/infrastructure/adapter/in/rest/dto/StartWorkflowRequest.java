package com.engine.nexus.infrastructure.adapter.in.rest.dto;

import java.util.Map;

public record StartWorkflowRequest(
        String customId,
        Map<String, Object> input
) {}
