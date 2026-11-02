package com.transport.backend.policy;

import com.transport.backend.model.DemandRequest;

/**
 * Standard demand ranking policy for the Smart Public Transportation system.
 *
 * Ranking Rules:
 * 1. Larger passenger count first (descending order).
 * 2. If passenger counts are equal, earlier arrival order first (ascending FIFO order).
 */
public class DefaultDemandPolicy implements DemandPolicy {

    @Override
    public int compare(DemandRequest d1, DemandRequest d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return 1;
        if (d2 == null) return -1;

        // Rule 1: Larger passenger counts first
        int countComparison = Integer.compare(d2.getPassengerCount(), d1.getPassengerCount());
        if (countComparison != 0) {
            return countComparison;
        }

        // Rule 2: Earlier arrival order first (FIFO tie-breaker)
        return Long.compare(d1.getOriginalArrivalOrder(), d2.getOriginalArrivalOrder());
    }
}
