package com.transport.backend.model;

/**
 * Represents a full-size public transit bus (e.g. 40–50 seats).
 *
 * Road Restriction Policy:
 * Standard transit buses have a wide frame and large turning radius.
 * Therefore, they are prohibited from using narrow roads (road.isNarrowRoad() == true).
 */
public class StandardBus extends Bus {

    public static final int DEFAULT_CAPACITY = 40;

    public StandardBus(String id, int capacity, Stop currentStop) {
        super(id, capacity, currentStop);
    }

    public StandardBus(String id, Stop currentStop) {
        super(id, DEFAULT_CAPACITY, currentStop);
    }

    /**
     * Polymorphic implementation: StandardBus cannot use narrow roads.
     */
    @Override
    public boolean canUseRoad(Road road) {
        if (road == null) {
            return false;
        }
        return !road.isNarrowRoad();
    }
}
