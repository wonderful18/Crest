package io.crest.runtime;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Locale;

public enum CrestRuntimeRole {
    ALL("all"),
    API("api"),
    SCHEDULER("scheduler"),
    WORKER("worker");

    private final String value;

    CrestRuntimeRole(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean servesApi() {
        return this == ALL || this == API;
    }

    public boolean runsScheduler() {
        return this == ALL || this == SCHEDULER;
    }

    public boolean runsWorker() {
        return this == ALL || this == WORKER;
    }

    public static CrestRuntimeRole from(Environment environment) {
        String configured = null;
        if (environment != null) {
            configured = firstText(
                    environment.getProperty("CREST_RUNTIME_ROLE"),
                    environment.getProperty("crest.runtime.role")
            );
        }
        return from(configured);
    }

    public static CrestRuntimeRole from(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : ALL.value;
        for (CrestRuntimeRole role : values()) {
            if (role.value.equals(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unsupported CREST_RUNTIME_ROLE: " + value);
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
