package org.prebid.pg.delstats.utils;

import java.util.Base64;

public class HttpUtils {

    private HttpUtils() {
    }

    public static String generateBasicAuthHeaderEntry(String user, String password) {
        return String.format("Basic %s",
                Base64.getEncoder().encodeToString(String.format("%s:%s", user, password).getBytes()));
    }
}
