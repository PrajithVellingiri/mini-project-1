package com.transport.backend.network;

import com.transport.backend.model.Road;
import com.transport.backend.model.Stop;

import java.util.*;

/**
 * Weighted graph representing the public transit road network.
 * Vertices represent transit stops and the central depot.
 * Edges represent directional roads with strictly positive travel times.
 */
public class RoadNetwork {

    private final Map<String, Stop> stops;
    private final Map<String, Map<String, Road>> adjacencyList;

    public RoadNetwork() {
        this.stops = new LinkedHashMap<>();
        this.adjacencyList = new LinkedHashMap<>();
    }

    /**
     * Adds a stop (vertex) to the road network.
     *
     * @param stop Stop to add
     * @throws IllegalArgumentException if stop is null or a stop with the same ID already exists
     */
    public void addStop(Stop stop) {
        if (stop == null) {
            throw new IllegalArgumentException("Stop cannot be null");
        }
        if (stops.containsKey(stop.getId())) {
            throw new IllegalArgumentException("Stop with ID '" + stop.getId() + "' already exists in the network");
        }
        stops.put(stop.getId(), stop);
        adjacencyList.put(stop.getId(), new LinkedHashMap<>());
    }

    public boolean containsStop(String stopId) {
        return stopId != null && stops.containsKey(stopId.trim());
    }

    public Stop getStop(String stopId) {
        if (stopId == null) return null;
        return stops.get(stopId.trim());
    }

    public Collection<Stop> getAllStops() {
        return Collections.unmodifiableCollection(stops.values());
    }

    public int getStopCount() {
        return stops.size();
    }

    /**
     * Adds a directional road (edge) between two existing stops.
     *
     * @param fromStopId        Origin stop ID
     * @param toStopId          Destination stop ID
     * @param travelTimeMinutes Estimated travel time (> 0)
     * @param narrowRoad        True if the road is restricted to smaller vehicles
     * @return The created Road instance
     */
    public Road addRoad(String fromStopId, String toStopId, double travelTimeMinutes, boolean narrowRoad) {
        if (fromStopId == null || !stops.containsKey(fromStopId.trim())) {
            throw new IllegalArgumentException("Source stop '" + fromStopId + "' does not exist in network");
        }
        if (toStopId == null || !stops.containsKey(toStopId.trim())) {
            throw new IllegalArgumentException("Destination stop '" + toStopId + "' does not exist in network");
        }
        if (fromStopId.trim().equalsIgnoreCase(toStopId.trim())) {
            throw new IllegalArgumentException("Road cannot connect a stop to itself: " + fromStopId);
        }
        if (travelTimeMinutes <= 0.0) {
            throw new IllegalArgumentException("Travel time must be strictly positive (> 0), got: " + travelTimeMinutes);
        }

        String from = fromStopId.trim();
        String to = toStopId.trim();

        Map<String, Road> outgoing = adjacencyList.get(from);
        if (outgoing.containsKey(to)) {
            throw new IllegalArgumentException("Road from '" + from + "' to '" + to + "' already exists");
        }

        Road road = new Road(from, to, travelTimeMinutes, narrowRoad);
        outgoing.put(to, road);
        return road;
    }

    /**
     * Convenience method to add a standard (non-narrow) directional road.
     */
    public Road addRoad(String fromStopId, String toStopId, double travelTimeMinutes) {
        return addRoad(fromStopId, toStopId, travelTimeMinutes, false);
    }

    /**
     * Adds a bidirectional road between two stops with identical travel times.
     */
    public void addBidirectionalRoad(String stopIdA, String stopIdB, double travelTimeMinutes, boolean narrowRoad) {
        addRoad(stopIdA, stopIdB, travelTimeMinutes, narrowRoad);
        addRoad(stopIdB, stopIdA, travelTimeMinutes, narrowRoad);
    }

    /**
     * Updates the travel time on an existing road edge (e.g. following dynamic traffic reports).
     *
     * @param fromStopId        Source stop ID
     * @param toStopId          Destination stop ID
     * @param newTravelTime     Updated positive travel time
     */
    public void updateTravelTime(String fromStopId, String toStopId, double newTravelTime) {
        if (newTravelTime <= 0.0) {
            throw new IllegalArgumentException("Updated travel time must be strictly positive (> 0), got: " + newTravelTime);
        }
        Road road = getRoad(fromStopId, toStopId);
        if (road == null) {
            throw new IllegalArgumentException("Cannot update travel time: road from '" + fromStopId + "' to '" + toStopId + "' does not exist");
        }
        road.setTravelTimeMinutes(newTravelTime);
    }

    /**
     * Updates the availability status of an existing road edge (open or closed).
     *
     * @param fromStopId Source stop ID
     * @param toStopId   Destination stop ID
     * @param open       True if the road is open to traffic, false if closed
     */
    public void setRoadOpen(String fromStopId, String toStopId, boolean open) {
        Road road = getRoad(fromStopId, toStopId);
        if (road == null) {
            throw new IllegalArgumentException("Cannot update road availability: road from '" + fromStopId + "' to '" + toStopId + "' does not exist");
        }
        road.setOpen(open);
    }

    /**
     * Checks if an existing road edge is currently open to traffic.
     */
    public boolean isRoadOpen(String fromStopId, String toStopId) {
        Road road = getRoad(fromStopId, toStopId);
        if (road == null) {
            throw new IllegalArgumentException("No direct road between '" + fromStopId + "' and '" + toStopId + "'");
        }
        return road.isOpen();
    }

    /**
     * Retrieves the road edge between two stops, or null if no direct edge exists.
     */
    public Road getRoad(String fromStopId, String toStopId) {
        if (fromStopId == null || toStopId == null) return null;
        Map<String, Road> outgoing = adjacencyList.get(fromStopId.trim());
        if (outgoing == null) return null;
        return outgoing.get(toStopId.trim());
    }

    /**
     * Retrieves the travel time between two connected stops.
     */
    public double getTravelTime(String fromStopId, String toStopId) {
        Road road = getRoad(fromStopId, toStopId);
        if (road == null) {
            throw new IllegalArgumentException("No direct road between '" + fromStopId + "' and '" + toStopId + "'");
        }
        return road.getTravelTimeMinutes();
    }

    /**
     * Retrieves all outgoing road edges from a given stop.
     */
    public Collection<Road> getAdjacentRoads(String stopId) {
        if (stopId == null || !stops.containsKey(stopId.trim())) {
            throw new IllegalArgumentException("Stop '" + stopId + "' not found in network");
        }
        return Collections.unmodifiableCollection(adjacencyList.get(stopId.trim()).values());
    }

    /**
     * Retrieves all adjacent neighboring stop IDs reachable from the given stop.
     */
    public Set<String> getNeighborStopIds(String stopId) {
        if (stopId == null || !stops.containsKey(stopId.trim())) {
            throw new IllegalArgumentException("Stop '" + stopId + "' not found in network");
        }
        return Collections.unmodifiableSet(adjacencyList.get(stopId.trim()).keySet());
    }

    /**
     * Disconnects a stop from the network by removing all outgoing and incoming roads.
     * The stop vertex itself remains in the graph as a disconnected stop.
     *
     * @param stopId ID of the stop to disconnect
     */
    public void disconnectStop(String stopId) {
        if (stopId == null || !stops.containsKey(stopId.trim())) {
            throw new IllegalArgumentException("Stop '" + stopId + "' not found in network");
        }
        String id = stopId.trim();

        // 1. Clear outgoing edges
        adjacencyList.get(id).clear();

        // 2. Remove incoming edges from other stops
        for (Map<String, Road> outgoing : adjacencyList.values()) {
            outgoing.remove(id);
        }
    }

    /**
     * Checks if a stop vertex is completely isolated (no outgoing or incoming edges).
     */
    public boolean isStopDisconnected(String stopId) {
        if (stopId == null || !stops.containsKey(stopId.trim())) {
            throw new IllegalArgumentException("Stop '" + stopId + "' not found in network");
        }
        String id = stopId.trim();

        // Check outgoing edges
        if (!adjacencyList.get(id).isEmpty()) {
            return false;
        }

        // Check incoming edges
        for (Map<String, Road> outgoing : adjacencyList.values()) {
            if (outgoing.containsKey(id)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Retrieves all roads currently active in the network.
     */
    public List<Road> getAllRoads() {
        List<Road> all = new ArrayList<>();
        for (Map<String, Road> outgoing : adjacencyList.values()) {
            all.addAll(outgoing.values());
        }
        return Collections.unmodifiableList(all);
    }
}
