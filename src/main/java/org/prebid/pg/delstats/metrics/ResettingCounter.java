package org.prebid.pg.delstats.metrics;

import com.codahale.metrics.Counter;

public class ResettingCounter extends Counter {
    @Override
    public long getCount() {
        final long currentCount = super.getCount();
        dec(currentCount);
        return currentCount;
    }

}
