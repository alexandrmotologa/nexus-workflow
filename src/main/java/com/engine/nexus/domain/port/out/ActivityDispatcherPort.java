package com.engine.nexus.domain.port.out;

import java.util.Map;

public interface ActivityDispatcherPort {
    Map<String, Object> dispatch(String activityName, Map<String, Object> inputs);
}
