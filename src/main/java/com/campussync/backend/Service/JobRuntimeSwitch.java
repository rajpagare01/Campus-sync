package com.campussync.backend.Service;

import com.campussync.backend.config.JobRuntimeProperties;
import org.springframework.stereotype.Component;

@Component
public class JobRuntimeSwitch {

    private final JobRuntimeProperties properties;

    public JobRuntimeSwitch(JobRuntimeProperties properties) {
        this.properties = properties;
    }

    public boolean useRabbit() {
        return properties.useRabbit();
    }
}
