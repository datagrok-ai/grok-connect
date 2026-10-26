package grok_connect.utils;

import java.util.regex.Pattern;

public class GrokConnectUtil {
    private static final Pattern URL_SECRET_PARAM = Pattern.compile(
            "(?i)([\\w.-]*(?:password|passwd|pwd|secret|token|credential)[\\w.-]*\\s*=)[^;&\\s]*");
    private static final Pattern URL_USER_INFO_PASSWORD = Pattern.compile("(://[^/@:;?&\\s]*:)[^/;?&\\s]*@");

    /** Masks passwords and secrets in a JDBC URL or custom connection string before it is logged. */
    public static String maskCredentials(String url) {
        if (isEmpty(url))
            return url;
        String masked = URL_SECRET_PARAM.matcher(url).replaceAll("$1***");
        return URL_USER_INFO_PASSWORD.matcher(masked).replaceAll("$1***@");
    }

    public static boolean isNotEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String capitalize(String s) {
        return isEmpty(s) ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }
}
