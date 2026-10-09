package grok_connect.utils;

import grok_connect.GrokConnect;
import grok_connect.connectors_info.DataQueryRunResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;

public class ErrorClassifierTest {
    @Test
    public void userErrors() {
        Assertions.assertTrue(ErrorClassifier.isUserError(new SQLException("relation does not exist")));
        Assertions.assertTrue(ErrorClassifier.isUserError(new SQLSyntaxErrorException("syntax error")));
        Assertions.assertTrue(ErrorClassifier.isUserError(new SQLTimeoutException("timeout")));
        Assertions.assertTrue(ErrorClassifier.isUserError(new QueryCancelledByUser()));
        Assertions.assertTrue(ErrorClassifier.isUserError(new ConnectException("refused")));
        Assertions.assertTrue(ErrorClassifier.isUserError(new UnknownHostException("db.example")));
        Assertions.assertTrue(ErrorClassifier.isUserError(new SocketTimeoutException("read timed out")));
        Assertions.assertTrue(ErrorClassifier.isUserError(new NoRouteToHostException("no route")));
        Assertions.assertTrue(ErrorClassifier.isUserError(new GrokConnectException("Credentials can't be null")));
    }

    @Test
    public void userErrorsDeepInTheCauseChain() {
        Exception wrapped = new GrokConnectException(new RuntimeException(new SQLException("permission denied")));
        Assertions.assertTrue(ErrorClassifier.isUserError(wrapped));
        Assertions.assertTrue(ErrorClassifier.isUserError(
                new GrokConnectException(new IllegalStateException(new ConnectException("refused")))));
    }

    @Test
    public void internalErrors() {
        Assertions.assertFalse(ErrorClassifier.isUserError(null));
        Assertions.assertFalse(ErrorClassifier.isUserError(new NullPointerException()));
        Assertions.assertFalse(ErrorClassifier.isUserError(new IllegalStateException("bad state")));
        Assertions.assertFalse(ErrorClassifier.isUserError(new ClassNotFoundException("org.Driver")));
        Assertions.assertFalse(ErrorClassifier.isUserError(new NoClassDefFoundError("org/Driver")));
        Assertions.assertFalse(ErrorClassifier.isUserError(new GrokConnectException(new NullPointerException())));
        Assertions.assertFalse(ErrorClassifier.isUserError(
                new GrokConnectException("JDBC driver not found", new ClassNotFoundException("org.Driver"))));
    }

    @Test
    public void selfReferencingCauseTerminates() {
        Exception a = new RuntimeException("a");
        Exception b = new RuntimeException("b", a);
        a.initCause(b);
        Assertions.assertFalse(ErrorClassifier.isUserError(b));
    }

    @Test
    public void packExceptionSetsErrorType() {
        DataQueryRunResult result = new DataQueryRunResult();
        GrokConnect.packException(result, new SQLException("syntax error"));
        Assertions.assertEquals(ErrorClassifier.USER, result.errorType);
        result = new DataQueryRunResult();
        GrokConnect.packException(result, new NullPointerException());
        Assertions.assertEquals(ErrorClassifier.INTERNAL, result.errorType);
        Assertions.assertTrue(GrokConnect.gson.toJson(result).contains("\"errorType\":\"internal\""));
    }
}
