package grok_connect.connectors_info;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataConnectionTest {
    @Test
    void ssl_isOffWhenTheParameterIsAbsentOrNull() {
        DataConnection conn = new DataConnection();
        Assertions.assertFalse(conn.ssl());
        conn.parameters.put(DbCredentials.SSL, null);
        Assertions.assertFalse(conn.ssl());
        conn.parameters.put(DbCredentials.SSL, true);
        Assertions.assertTrue(conn.ssl());
    }
}
