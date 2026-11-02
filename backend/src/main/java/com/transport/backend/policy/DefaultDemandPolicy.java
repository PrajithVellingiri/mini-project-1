package com.transport.backend.policy;

import com.transport.backend.model.DemandRequest;

/**
 * Standard demand ranking policy for the Smart Public Transportation system.
 *
 * Ranking Rules:
 * 1. Emergency relief requests (arrivalOrder == 0 or DEM-EMG-*) strictly preempt standard requests.
 * 2. If both are emergency or both are regular: Larger passenger count first (descending order).
 * 3. If passenger counts are equal: Earlier arrival order first (ascending FIFO order).
 */
public class DefaultDemandPolicy implements DemandPolicy {

    @Override
    public int compare(DemandRequest d1, DemandRequest d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return 1;
        if (d2 == null) return -1;

        // Rule 1: Emergency relief requests strictly preempt standard requests
        boolean d1Emergency = d1.isEmergency();
        boolean d2Emergency = d2.isEmergency();
        if (d1Emergency != d2Emergency) {
            return d1Emergency ? -1 : 1;
        }

        // Rule 2: Larger passenger counts first
        int countComparison = Integer.compare(d2.getPassengerCount(), d1.getPassengerCount());
        if (countComparison != 0) {
            return countComparison;
        }

        // Rule 3: Earlier arrival order first (FIFO tie-breaker)
        return Long.compare(d1.getOriginalArrivalOrder(), d2.getOriginalArrivalOrder());
    }
}
