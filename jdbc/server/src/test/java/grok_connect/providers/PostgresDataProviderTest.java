package grok_connect.providers;

import grok_connect.connectors_info.DbCredentials;
import grok_connect.connectors_info.FuncCall;
import grok_connect.providers.utils.NamedArgumentConverter;
import grok_connect.providers.utils.Provider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.MethodSource;
import serialization.DataFrame;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Test class for PostgresDataProvider
 */
class PostgresDataProviderTest extends ContainerizedProviderBaseTest {
    private static final String INIT_SCHEMA_NAME = "public";
    private static final String INIT_TABLE_NAME = "mock_data";

    protected PostgresDataProviderTest(Provider provider) {
        super(provider);
    }

    @DisplayName("Test of getCatalogs() method with correct DataConnection")
    @Test
    public void getCatalogs_ok() {
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.getCatalogs(connection));
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(1, actual.getColumnCount());
        Assertions.assertEquals("catalog_name", actual.getColumn(0).getName());
        Assertions.assertTrue(actual.rowCount > 0);
    }

    @DisplayName("Test of getSchemas() method with correct DataConnection")
    @ParameterizedTest(name = "CORRECT ARGUMENTS")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#getSchemas_ok")
    public void getSchemas_ok(DataFrame expected) {
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.getSchemas(connection));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @Disabled
    @Test
    public void getSchemas_notOk() {
        // method probably should throw something when bad input
    }

    @DisplayName("Test of getSchema() method with correct DataConnection")
    @ParameterizedTest(name = "CORRECT ARGUMENTS")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#getSchema_ok")
    public void getSchema_ok(DataFrame expected) {
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.getSchema(connection,
                INIT_SCHEMA_NAME, INIT_TABLE_NAME, false));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @DisplayName("getSchema() with key info sees the keys of a table the user may only read")
    @Test
    public void getSchema_keyInfo_readOnlyUser() throws Exception {
        execDirect("CREATE TABLE key_ro (id int PRIMARY KEY, code text UNIQUE, note text)");
        execDirect("CREATE ROLE key_reader LOGIN PASSWORD 'reader'");
        execDirect("GRANT SELECT ON key_ro TO key_reader");
        try {
            connection.credentials.parameters.put(DbCredentials.LOGIN, "key_reader");
            connection.credentials.parameters.put(DbCredentials.PASSWORD, "reader");
            DataFrame schema = provider.getSchema(connection, INIT_SCHEMA_NAME, "key_ro", true);
            Map<String, String> keys = new HashMap<>();
            for (int i = 0; i < schema.rowCount; i++)
                keys.put(String.valueOf(schema.getColumn("column_name").get(i)),
                        schema.getColumn("is_primary_key").get(i) + "/" + schema.getColumn("is_unique").get(i));
            Assertions.assertEquals("1/1", keys.get("id"));
            Assertions.assertEquals("0/1", keys.get("code"));
            Assertions.assertEquals("0/0", keys.get("note"));
        } finally {
            execDirect("DROP TABLE key_ro");
            execDirect("DROP ROLE key_reader");
        }
    }

    private void execDirect(String sql) throws Exception {
        try (Connection c = DriverManager.getConnection(container.getJdbcUrl(), container.getUsername(), container.getPassword());
             Statement statement = c.createStatement()) {
            statement.execute(sql);
        }
    }

    @Disabled
    @Test
    public void getSchema_notOk() {
        // method probably should throw something when bad input
    }

    @DisplayName("Output support for postgresql array type")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#checkOutputDataFrame_arrayType_ok")
    public void checkOutputDataFrame_arrayType_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @DisplayName("Output support for postgresql basic types")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#checkOutputDataFrame_basicTypes_ok")
    public void checkOutputDataFrame_basicTypes_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @DisplayName("Output support for postgresql bit string type")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#checkOutputDataFrame_bitStringType_ok")
    public void checkOutputDataFrame_bitStringType_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

// bytea types - supported as string if in sql query use encode()
    @DisplayName("Output support for postgresql bytea type")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#checkOutputDataFrame_byteAType_ok")
    public void checkOutputDataFrame_byteAType_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @DisplayName("Output support for postgresql composite custom type")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#checkOutputDataFrame_compositeType_ok")
    public void checkOutputDataFrame_compositeType_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @DisplayName("Output support for postgresql date, time, timestamp, interval types")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#checkOutputDataFrame_dateTypes_ok")
    public void checkOutputDataFrame_dateTypes_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @DisplayName("Output support for postgresql jsonb")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#checkOutputDataFrame_jsonbType_ok")
    public void checkOutputDataFrame_jsonbType_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @DisplayName("Output support for postgresql numeric, real, double precision, bigint")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#checkOutputDataFrame_numericType_ok")
    public void checkOutputDataFrame_numericType_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @DisplayName("Output support for serial type")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#checkOutputDataFrame_serialType_ok")
    public void checkOutputDataFrame_serialType_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @DisplayName("Output support for uuid type")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#checkOutputDataFrame_uuidType_ok")
    public void checkOutputDataFrame_uuidType_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @DisplayName("Output support for xml type")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#checkOutputDataFrame_xmlType_ok")
    public void checkOutputDataFrame_xmlType_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @DisplayName("Parameters support")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource({"grok_connect.providers.arguments_provider.CommonObjectsMother#checkParameterSupport_ok",
            "grok_connect.providers.arguments_provider.CommonObjectsMother#checkMultipleParametersSupport_ok",
            "grok_connect.providers.arguments_provider.CommonObjectsMother#checkListParameterSupport_ok",
            "grok_connect.providers.arguments_provider.CommonObjectsMother#checkRegexSupport_ok"})
    public void checkParameterSupport_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @DisplayName("Parameters support for datetime")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.CommonObjectsMother#checkDatesParameterSupport_ok")
    public void checkDatesParameterSupport_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }

    @DisplayName("Postgres Null safety")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.CommonObjectsMother#checkNullSupport_ok")
    public void checkNullSupport_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall) {
        funcCall.func.connection = connection;
        Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
    }

    @DisplayName("Postgresql operators support")
    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("grok_connect.providers.arguments_provider.PostgresObjectsMother#checkPostgresOperatorsSupport_ok")
    public void checkPostgresOperatorsSupport_ok(@ConvertWith(NamedArgumentConverter.class) FuncCall funcCall, DataFrame expected) {
        funcCall.func.connection = connection;
        DataFrame actual = Assertions.assertDoesNotThrow(() -> provider.execute(funcCall));
        Assertions.assertTrue(dataFrameComparator.isDataFramesEqual(expected, actual));
    }
}
