package com.engine.nexus.infrastructure.adapter.in.rest.dto;

import java.util.Map;

public record SignalRequest(
        Map<String, Object> payload
) {}
