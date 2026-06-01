package io.crest.home;

import io.crest.result.ResultMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @Test
    void livenessShouldReturnUp() {
        HealthController controller = new HealthController(null);

        ResponseEntity<ResultMessage> response = controller.liveness();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", status(response).get("status"));
    }

    @Test
    void readinessShouldReturnUpWhenDatabaseConnectionIsValid() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        HealthController controller = new HealthController(dataSource);
        ResponseEntity<ResultMessage> response = controller.readiness();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", status(response).get("status"));
        verify(connection).close();
    }

    @Test
    void readinessShouldReturnUnavailableWhenDatabaseConnectionFails() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("connection failed"));

        HealthController controller = new HealthController(dataSource);
        ResponseEntity<ResultMessage> response = controller.readiness();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getBody().getCode());
        assertEquals("DOWN", status(response).get("status"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> status(ResponseEntity<ResultMessage> response) {
        return (Map<String, Object>) response.getBody().getData();
    }
}
