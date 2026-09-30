---
paths:
  - jdbc/**
---

## GrokConnect Connectors

GrokConnect is a Java/Maven project providing JDBC-based database connectors.

```bash
cd jdbc
mvn package -DskipTests          # Build
mvn test                          # Run all tests
mvn test -Dtest=ClassName -DfailIfNoTests=false         # Run specific test class
mvn test -Dtest=ClassName#method -DfailIfNoTests=false  # Run specific test method
java -Xmx4g -classpath "$(ls server/target/grok_connect-*.jar):server/lib/*" grok_connect.GrokConnect  # Run GrokConnect server locally
```

Source is in `jdbc/server/src/main/java/grok_connect/`.
Tests are in `jdbc/server/src/test/java/grok_connect/`.
