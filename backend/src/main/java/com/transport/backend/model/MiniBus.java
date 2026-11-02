package com.transport.backend.model;

/**
 * Represents a compact transit vehicle (e.g. 15–20 seats).
 *
 * Road Restriction Policy:
 * Due to its compact wheelbase and narrow width, a MiniBus can travel on all roads,
 * including designated narrow roads (road.isNarrowRoad() == true).
 */
public class MiniBus extends Bus {

    public static final int DEFAULT_CAPACITY = 15;

    public MiniBus(String id, int capacity, Stop currentStop) {
        super(id, capacity, currentStop);
    }

    public MiniBus(String id, Stop currentStop) {
        super(id, DEFAULT_CAPACITY, currentStop);
    }

    /**
     * Polymorphic implementation: MiniBus can traverse any valid road segment.
     */
    @Override
    public boolean canUseRoad(Road road) {
        return road != null;
    }
}
