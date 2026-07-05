package io.crest.visualization.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.crest.api.visualization.request.DataVisualizationBaseRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataVisualizationBaseRequestJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldAcceptBooleanStatusFromCopyPayload() throws JsonProcessingException {
        DataVisualizationBaseRequest request = objectMapper.readValue("""
                {
                  "id": 10,
                  "pid": 0,
                  "nodeType": "leaf",
                  "name": "经营大屏-copy",
                  "type": "dataV",
                  "mobileLayout": false,
                  "status": true
                }
                """, DataVisualizationBaseRequest.class);

        assertEquals(1, request.getStatus());
    }

    @Test
    void shouldAcceptNumericStatusFromVisualizationPayload() throws JsonProcessingException {
        DataVisualizationBaseRequest request = objectMapper.readValue("""
                {
                  "id": 10,
                  "type": "dashboard",
                  "status": 0
                }
                """, DataVisualizationBaseRequest.class);

        assertEquals(0, request.getStatus());
    }
}
