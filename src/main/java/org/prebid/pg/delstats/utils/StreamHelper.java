package org.prebid.pg.delstats.utils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class StreamHelper {
    public static <T> Stream<T> getStream(List<T> list) {
        return Optional.ofNullable(list).map(List::stream).orElseGet(Stream::empty);
    }

    private StreamHelper() {
    }
}
