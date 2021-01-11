package org.prebid.pg.delstats.utils;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ResourceUtil {

    private ResourceUtil() {
    }

    /**
     * Reads files from classpath. Throws {@link IllegalArgumentException} if file was not found.
     */
    public static String readFromClasspath(String path) throws IOException {
        final InputStream resourceAsStream = ResourceUtil.class.getClassLoader().getResourceAsStream(path);

        if (resourceAsStream == null) {
            throw new IllegalArgumentException(String.format("Could not find file at path: %s", path));
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,
                StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public static byte[] readBytesFromClasspath(String path) throws IOException {
        final InputStream resourceAsStream = ResourceUtil.class.getClassLoader().getResourceAsStream(path);
        return IOUtils.toByteArray(resourceAsStream);
    }
}

