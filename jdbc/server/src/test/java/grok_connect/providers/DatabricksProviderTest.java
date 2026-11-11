package grok_connect.providers;

import grok_connect.connectors_info.Credentials;
import grok_connect.connectors_info.DataConnection;
import grok_connect.connectors_info.DbCredentials;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class DatabricksProviderTest {
    @Test
    void schemaGoesIntoUrlPath() {
        DatabricksProvider provider = new DatabricksProvider();
        DataConnection conn = new DataConnection();
        conn.credentials = new Credentials();
        conn.parameters.put("workspaceURL", "dbc-1.cloud.databricks.com");
        Assertions.assertTrue(provider.getConnectionStringImpl(conn).endsWith(":443/default"));
        conn.parameters.put(DbCredentials.SCHEMA, "sales");
        Assertions.assertTrue(provider.getConnectionStringImpl(conn).endsWith(":443/sales"));
        Assertions.assertNull(provider.getProperties(conn).getProperty("ConnSchema"));
    }

    // The two views with the columns Unity Catalog documents, in SQLite: the query itself is run, not read
    private static final String[] CONSTRAINT_VIEWS = {
            "CREATE TABLE referential_constraints (constraint_catalog TEXT, constraint_schema TEXT, constraint_name TEXT, " +
                    "unique_constraint_catalog TEXT, unique_constraint_schema TEXT, unique_constraint_name TEXT)",
            "CREATE TABLE key_column_usage (constraint_catalog TEXT, constraint_schema TEXT, constraint_name TEXT, " +
                    "table_catalog TEXT, table_schema TEXT, table_name TEXT, column_name TEXT, " +
                    "ordinal_position INTEGER, position_in_unique_constraint INTEGER)",
            // primary keys: one column, two columns, and one in another schema
            "INSERT INTO key_column_usage VALUES ('main', 'sales', 'customers_pk', 'main', 'sales', 'customers', 'customer_id', 1, NULL)",
            "INSERT INTO key_column_usage VALUES ('main', 'sales', 'order_lines_pk', 'main', 'sales', 'order_lines', 'order_id', 1, NULL)",
            "INSERT INTO key_column_usage VALUES ('main', 'sales', 'order_lines_pk', 'main', 'sales', 'order_lines', 'line_no', 2, NULL)",
            "INSERT INTO key_column_usage VALUES ('main', 'crm', 'regions_pk', 'main', 'crm', 'regions', 'region_id', 1, NULL)",
            // a key of one column
            "INSERT INTO referential_constraints VALUES ('main', 'sales', 'orders_customer_fk', 'main', 'sales', 'customers_pk')",
            "INSERT INTO key_column_usage VALUES ('main', 'sales', 'orders_customer_fk', 'main', 'sales', 'orders', 'customer_id', 1, 1)",
            // a key of two columns, declared in the other order than the key it points at
            "INSERT INTO referential_constraints VALUES ('main', 'sales', 'shipments_line_fk', 'main', 'sales', 'order_lines_pk')",
            "INSERT INTO key_column_usage VALUES ('main', 'sales', 'shipments_line_fk', 'main', 'sales', 'shipments', 'line_no', 1, 2)",
            "INSERT INTO key_column_usage VALUES ('main', 'sales', 'shipments_line_fk', 'main', 'sales', 'shipments', 'order_id', 2, 1)",
            // a key that points into another schema
            "INSERT INTO referential_constraints VALUES ('main', 'sales', 'orders_region_fk', 'main', 'crm', 'regions_pk')",
            "INSERT INTO key_column_usage VALUES ('main', 'sales', 'orders_region_fk', 'main', 'sales', 'orders', 'region_id', 1, 1)",
            // a key of another schema
            "INSERT INTO referential_constraints VALUES ('main', 'crm', 'contacts_region_fk', 'main', 'crm', 'regions_pk')",
            "INSERT INTO key_column_usage VALUES ('main', 'crm', 'contacts_region_fk', 'main', 'crm', 'contacts', 'region_id', 1, 1)",
    };

    private static List<String> foreignKeys(String schema) throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = db.createStatement()) {
                for (String sql : CONSTRAINT_VIEWS)
                    st.executeUpdate(sql);
            }
            List<String> rows = new ArrayList<>();
            try (PreparedStatement ps = db.prepareStatement(DatabricksProvider.getForeignKeysSql("main", schema != null))) {
                if (schema != null)
                    ps.setString(1, schema);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        rows.add(String.join(" ", rs.getString("table_schema"), rs.getString("constraint_name"),
                                rs.getString("table_name") + "." + rs.getString("column_name"), "->",
                                rs.getString("foreign_table_name") + "." + rs.getString("foreign_column_name")));
                }
            }
            return rows;
        }
    }

    @Test
    void foreignKeysOfSchemaPairEachColumnWithTheKeyColumnItPointsAt() throws SQLException, ClassNotFoundException {
        Assertions.assertEquals(Arrays.asList(
                "sales orders_customer_fk orders.customer_id -> customers.customer_id",
                "sales shipments_line_fk shipments.line_no -> order_lines.line_no",
                "sales shipments_line_fk shipments.order_id -> order_lines.order_id"), foreignKeys("SALES"));
    }

    @Test
    void foreignKeysOfCatalogLeaveOutTheOnesThatPointIntoAnotherSchema() throws SQLException, ClassNotFoundException {
        Assertions.assertEquals(Arrays.asList(
                "crm contacts_region_fk contacts.region_id -> regions.region_id",
                "sales orders_customer_fk orders.customer_id -> customers.customer_id",
                "sales shipments_line_fk shipments.line_no -> order_lines.line_no",
                "sales shipments_line_fk shipments.order_id -> order_lines.order_id"), foreignKeys(null));
    }
}
