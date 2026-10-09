package grok_connect.utils;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Tells Datlas whether a failure is the user's (bad SQL, unreachable or rejecting database, cancellation)
 * or grok_connect's own (a bug or a broken deployment), via {@code DataQueryRunResult.errorType}.
 */
public class ErrorClassifier {
    public static final String USER = "user";
    public static final String INTERNAL = "internal";

    public static String errorType(Throwable ex) {
        return isUserError(ex) ? USER : INTERNAL;
    }

    public static boolean isUserError(Throwable ex) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable t = ex; t != null && seen.add(t); t = t.getCause()) {
            if (t instanceof SQLException || t instanceof QueryCancelledByUser || t instanceof ConnectException
                    || t instanceof UnknownHostException || t instanceof SocketTimeoutException
                    || t instanceof NoRouteToHostException)
                return true;
            // a causeless GrokConnectException is a deliberate refusal (missing credentials, unsupported operation)
            if (t.getClass().equals(GrokConnectException.class) && t.getCause() == null)
                return true;
        }
        return false;
    }
}
