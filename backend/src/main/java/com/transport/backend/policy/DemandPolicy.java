package com.transport.backend.policy;

import com.transport.backend.model.DemandRequest;
import java.util.Comparator;

/**
 * Strategy abstraction for ranking passenger demand batches.
 * Extends Comparator for direct use with Java's PriorityQueue and sorting utilities.
 */
@FunctionalInterface
public interface DemandPolicy extends Comparator<DemandRequest> {

    /**
     * Compares two demand requests according to a specific ranking policy.
     */
    @Override
    int compare(DemandRequest d1, DemandRequest d2);
}
