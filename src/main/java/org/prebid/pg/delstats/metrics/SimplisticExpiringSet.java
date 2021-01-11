package org.prebid.pg.delstats.metrics;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Implements expiration in a simplistic manner by use of two sets. When the 'most recent' set is past the expiration
 * amount of time, it is moved to the 'ready to expire' set. This means some entries might be retained for twice the
 * expiration interval.
 *
 * Remove and retain operations not implemented. Should be, thread safe but not guaranteed.
 *
 * @param <T>
 */
public class SimplisticExpiringSet<T> {
    private final int expirationTimeAmount;
    private final TemporalUnit expirationTimeUnit;

    private Set<T> readyToExpireSet;
    private Set<T> recentSet;
    private Instant lastRecentSetCreated;

    public SimplisticExpiringSet(int expirationTimeAmount, TemporalUnit expirationTimeUnit) {
        this.expirationTimeAmount = expirationTimeAmount;
        this.expirationTimeUnit = Objects.requireNonNull(expirationTimeUnit);
        this.readyToExpireSet = new HashSet<>();
        this.recentSet = new HashSet<>();
        this.lastRecentSetCreated = Instant.now();
    }

    public SimplisticExpiringSet() {
        this(1, ChronoUnit.HOURS);
    }

    public boolean add(T entry) {
        return getRecent().add(entry);
    }

    public boolean addAll(Collection<? extends T> entries) {
        return getRecent().addAll(entries);
    }

    public int size() {
        return getAll().size();
    }

    public boolean isEmpty() {
        return getAll().isEmpty();
    }

    public Set<T> getAll() {
        Set<T> mergedSet = new HashSet<>(getRecent());
        mergedSet.addAll(this.readyToExpireSet);
        return mergedSet;
    }

    public Set<T> getRecent() {
        return getRecent(Instant.now());
    }

    public synchronized Set<T> getRecent(Instant now) {
        if (hasExpired(now)) {
            this.readyToExpireSet = this.recentSet;
            this.recentSet = new HashSet<>();
            this.lastRecentSetCreated = now;
        }
        return this.recentSet;
    }

    public boolean hasExpired(Instant now) {
        Instant then = now.minus(expirationTimeAmount, expirationTimeUnit);
        return lastRecentSetCreated.isBefore(then);
    }
}
