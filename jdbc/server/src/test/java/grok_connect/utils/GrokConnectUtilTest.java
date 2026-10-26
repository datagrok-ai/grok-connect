package grok_connect.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GrokConnectUtilTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "jdbc:virtuoso://host:1111/TIMEOUT=100/UID=dba/PWD=s3cr/et|jdbc:virtuoso://host:1111/TIMEOUT=100/UID=dba/PWD=***",
            "jdbc:sqlserver://h:1433;databaseName=db;user=u;password=p@ss;encrypt=true|jdbc:sqlserver://h:1433;databaseName=db;user=u;password=***;encrypt=true",
            "jdbc:postgresql://h/db?user=u&Password=p&ssl=true|jdbc:postgresql://h/db?user=u&Password=***&ssl=true",
            "jdbc:spark://h:443/default;AuthMech=3;Auth_AccessToken=dapi123;transportMode=http|jdbc:spark://h:443/default;AuthMech=3;Auth_AccessToken=***;transportMode=http",
            "jdbc:awsathena://AwsRegion=us-east-1;AWSAccessKey=AK;AWSSecretKey=SK;|jdbc:awsathena://AwsRegion=us-east-1;AWSAccessKey=AK;AWSSecretKey=***;",
            "mongodb://user:p@ss:w@host:27017/db|mongodb://user:***@host:27017/db",
            "jdbc:postgresql://host:5432/db|jdbc:postgresql://host:5432/db",
    })
    void maskCredentialsHidesSecrets(String url, String expected) {
        Assertions.assertEquals(expected, GrokConnectUtil.maskCredentials(url));
    }

    @Test
    void maskCredentialsKeepsEmptyInput() {
        Assertions.assertNull(GrokConnectUtil.maskCredentials(null));
        Assertions.assertEquals("", GrokConnectUtil.maskCredentials(""));
    }
}
