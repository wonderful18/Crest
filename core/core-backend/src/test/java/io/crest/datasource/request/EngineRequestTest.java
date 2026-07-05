package io.crest.datasource.request;

import io.crest.datasource.security.SqlSecurityPolicy;
import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EngineRequestTest {

    @Test
    void nullQueryShouldBeRejectedBySqlPolicyInsteadOfThrowingNpe() {
        EngineRequest request = new EngineRequest();

        assertNull(request.getQuery());
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateSingleStatement(request.getQuery()));
    }
}
