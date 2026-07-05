package io.crest.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.MapperFeature;
import io.crest.jackson.JacksonConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class JsonUtilSecurityTest {

    @Test
    void jsonUtilShouldNotEnableCaseInsensitiveProperties() {
        CaseInsensitiveProbe parsed = JsonUtil.parseObject("{\"SeCrEt\":\"hidden\"}", CaseInsensitiveProbe.class);

        assertNull(parsed.secret);
    }

    @Test
    void springObjectMapperShouldKeepCaseInsensitivePropertiesDisabled() {
        JacksonConfig config = new JacksonConfig();
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder()
                .featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
        config.customJackson().customize(builder);

        assertFalse(config.jacksonObjectMapper(builder).isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
    }

    @JsonIgnoreProperties({"secret"})
    static class CaseInsensitiveProbe {
        public String secret;
    }
}
