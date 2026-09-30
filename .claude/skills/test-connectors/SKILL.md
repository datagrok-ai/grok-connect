---
name: test-connectors
description: Run Java/Maven tests for the GrokConnect connectors project
when-to-use: When user asks to test connectors, run connector tests, or verify GrokConnect
context: fork
effort: medium
argument-hint: "[all|<ClassName>|<ClassName>#<method>]"
---

# Test Connectors (GrokConnect)

Run Java/Maven tests for the GrokConnect connectors project.

## Usage

```
/test-connectors [target]
```

Where `target` is one of:
- `all` - Run all tests (default)
- `<ClassName>` - Run a specific test class (e.g., `PostgresDataProviderTest`, `D42DartFixtureTest`)
- `<ClassName>#<method>` - Run a specific test method

## Prerequisites

- **JDK 8** - Required by the project
- **Maven 3+** - Build tool
- **Docker** - Required for TestContainers-based integration tests (most provider tests)

## Instructions

When this skill is invoked, help the user run the appropriate tests for the connectors project.

### Project Location

```
jdbc/
```

### Run All Tests

```bash
cd jdbc && mvn test
```

### Run a Specific Test Class

```bash
cd jdbc && mvn -Dtest=PostgresDataProviderTest -DfailIfNoTests=false test
```

### Run a Specific Test Method

```bash
cd jdbc && mvn -Dtest=PostgresDataProviderTest#testMethodName -DfailIfNoTests=false test
```

### Build Without Tests (for quick compilation check)

```bash
cd jdbc && mvn -Dmaven.test.skip=true package
```

### Run Only Serialization Tests (no Docker needed)

```bash
cd jdbc && mvn -pl serialization test
```

### Run Only grok_connect Module Tests

```bash
cd jdbc && mvn -pl server test
```

## Available Test Classes

### Provider Tests (require Docker for TestContainers)

Located in `server/src/test/java/grok_connect/providers/`:

| Test Class | Database |
|------------|----------|
| `PostgresDataProviderTest` | PostgreSQL |
| `MySqlDataProviderTest` | MySQL |
| `MariaDbDataProviderTest` | MariaDB |
| `MsSqlDataProviderTest` | MS SQL Server |
| `OracleDataProviderTest` | Oracle |
| `ClickHouseDataProviderTest` | ClickHouse |
| `MongoDbDataProviderTest` | MongoDB |
| `Neo4jDataProviderTest` | Neo4j |
| `CassandraDataProviderTest` | Cassandra |
| `Db2DataProviderTest` | DB2 |
| `VirtuosoDataProviderTest` | Virtuoso |
| `VerticaDataProviderTest` | Vertica |
| `Hive2DataProviderTest` | Hive2 |
| `ImpalaDataProviderTest` | Impala |
| `SnowflakeDataProviderTest` | Snowflake |
| `RedshiftDataProviderTest` | Redshift |
| `AthenaDataProviderTest` | Athena |
| `TeradataDataProviderTest` | Teradata |
| `PIDataProviderTest` | PI Data |

### Unit Tests (no Docker needed)

| Test Class | Location |
|------------|----------|
| `JdbcDataProviderTest` | `server/src/test/.../providers/` |
| `ComplexTypeConverterManagerTest` | `server/src/test/.../managers/complex_column/` |
| `TableQueryTest` | `server/src/test/.../table_query/` |
| `MsSqlTableQueryTest` | `server/src/test/.../table_query/` |
| `PostgresTableQueryTest` | `server/src/test/.../table_query/` |
| `SqlAnnotatorTest` | `server/src/test/.../utils/` |
| `D42DartFixtureTest`, `D42JavaFixtureWriterTest` | `serialization/src/test/.../serialization/` |

## Behavior

1. Ask the user which tests to run if not specified
2. Check if Docker is running when integration tests are requested
3. Run the appropriate Maven test commands from `jdbc/`
4. Report test results and any failures
