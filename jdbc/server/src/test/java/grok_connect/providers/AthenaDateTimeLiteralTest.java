package grok_connect.providers;

import grok_connect.connectors_info.FuncParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AthenaDateTimeLiteralTest {
    private final AthenaDataProvider provider = new AthenaDataProvider();

    @Test
    void isoInstantBecomesTrinoTimestampLiteral() {
        Assertions.assertEquals("TIMESTAMP '2022-01-01 00:00:00.000'",
                provider.castParamValueToSqlDateTime(new FuncParam("datetime", "date", "2022-01-01T00:00:00.000Z")));
    }

    @Test
    void offsetIsConvertedToUtc() {
        Assertions.assertEquals("TIMESTAMP '2021-12-31 22:30:15.250'",
                provider.castParamValueToSqlDateTime(new FuncParam("datetime", "date", "2022-01-01T01:30:15.250+03:00")));
    }

    @Test
    void nonIsoValueIsPassedThrough() {
        Assertions.assertEquals("TIMESTAMP '2022-01-01 10:00:00'",
                provider.castParamValueToSqlDateTime(new FuncParam("datetime", "date", "2022-01-01 10:00:00")));
    }
}
