package com.transport.backend.service;

import com.transport.backend.dto.AlgorithmBenchmarkDto;
import com.transport.backend.model.Bus;
import com.transport.backend.model.Road;
import com.transport.backend.model.RouteResult;
import com.transport.backend.model.Stop;
import com.transport.backend.network.RoadNetwork;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dijkstra's shortest-path algorithm implementation for minimum-travel-time route planning.
 *
 * Utilizes a binary min-heap (Java PriorityQueue) to optimize travel times across
 * the transit road network, respecting vehicle-specific accessibility restrictions.
 */
public class RoutePlanner {

    private static final double EPSILON = 1e-9;
    private final RoadNetwork roadNetwork;

    // High-resolution rolling performance telemetry
    private final AtomicLong totalCalculations = new AtomicLong(0);
    private final AtomicLong totalDurationNanos = new AtomicLong(0);
    private final AtomicLong minDurationNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxDurationNanos = new AtomicLong(0);
    private final AtomicLong lastDurationNanos = new AtomicLong(0);

    public RoutePlanner(RoadNetwork roadNetwork) {
        if (roadNetwork == null) {
            throw new IllegalArgumentException("RoadNetwork cannot be null");
        }
        this.roadNetwork = roadNetwork;
    }

    /**
     * Records execution latency into thread-safe rolling telemetry counters.
     */
    private void recordExecutionMetric(long nanos) {
        totalCalculations.incrementAndGet();
        totalDurationNanos.addAndGet(nanos);
        lastDurationNanos.set(nanos);
        minDurationNanos.accumulateAndGet(nanos, Math::min);
        maxDurationNanos.accumulateAndGet(nanos, Math::max);
    }

    /**
     * Retrieves current aggregate benchmark metrics.
     */
    public AlgorithmBenchmarkDto getBenchmarkMetrics() {
        long count = totalCalculations.get();
        if (count == 0) {
            return new AlgorithmBenchmarkDto(0, 0.0, 0.0, 0.0, 0.0);
        }
        double avgMicros = Math.round(((double) totalDurationNanos.get() / count / 1000.0) * 10.0) / 10.0;
        double minMicros = Math.round((minDurationNanos.get() / 1000.0) * 10.0) / 10.0;
        double maxMicros = Math.round((maxDurationNanos.get() / 1000.0) * 10.0) / 10.0;
        double lastMicros = Math.round((lastDurationNanos.get() / 1000.0) * 10.0) / 10.0;

        return new AlgorithmBenchmarkDto(count, avgMicros, minMicros, maxMicros, lastMicros);
    }

    /**
     * Finds the minimum-travel-time route between two stops for a specific vehicle type.
     *
     * @param sourceStopId      ID of the origin stop
     * @param destinationStopId ID of the target stop
     * @param bus               The bus executing the route, or null if no vehicle restrictions apply
     * @return RouteResult with ordered stops, total travel time, and reachability status
     * @throws IllegalArgumentException if source or destination stop does not exist in the network
     */
    public RouteResult findShortestRoute(String sourceStopId, String destinationStopId, Bus bus) {
        long startNanos = System.nanoTime();

        if (sourceStopId == null || sourceStopId.trim().isEmpty()) {
            throw new IllegalArgumentException("Source stop ID must not be null or blank");
        }
        if (destinationStopId == null || destinationStopId.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination stop ID must not be null or blank");
        }

        String source = sourceStopId.trim();
        String destination = destinationStopId.trim();

        if (!roadNetwork.containsStop(source)) {
            throw new IllegalArgumentException("Source stop '" + source + "' does not exist in road network");
        }
        if (!roadNetwork.containsStop(destination)) {
            throw new IllegalArgumentException("Destination stop '" + destination + "' does not exist in road network");
        }

        // Trivial case: source is identical to destination
        if (source.equalsIgnoreCase(destination)) {
            Stop singleStop = roadNetwork.getStop(source);
            long elapsed = System.nanoTime() - startNanos;
            recordExecutionMetric(elapsed);
            return RouteResult.reachable(List.of(singleStop), 0.0, 0.0, elapsed);
        }

        // Distance map: stores the shortest known travel time from source to each stop
        Map<String, Double> distances = new HashMap<>();

        // Predecessor map: stores the predecessor stop on the optimal path for route reconstruction
        Map<String, String> predecessors = new HashMap<>();

        // Min-heap PriorityQueue: stores candidate vertices ordered by minimum travel time
        PriorityQueue<NodeDistance> minHeap = new PriorityQueue<>();

        // Visited set: tracks vertices whose shortest path has already been settled
        Set<String> settled = new HashSet<>();

        // Initialize source vertex
        distances.put(source, 0.0);
        minHeap.add(new NodeDistance(source, 0.0));

        while (!minHeap.isEmpty()) {
            NodeDistance current = minHeap.poll();
            String u = current.stopId();
            double uDist = current.distance();

            // Skip stale heap entries
            if (settled.contains(u)) {
                continue;
            }
            settled.add(u);

            // Early exit if we have settled the target destination
            if (u.equalsIgnoreCase(destination)) {
                break;
            }

            // Relax all accessible outgoing roads
            for (Road road : roadNetwork.getAdjacentRoads(u)) {
                // Exclude closed roads from route calculations
                if (!road.isOpen()) {
                    continue;
                }

                // Vehicle accessibility restriction check
                if (bus != null && !bus.canUseRoad(road)) {
                    continue;
                }

                String v = road.getToStopId();
                if (settled.contains(v)) {
                    continue;
                }

                double weight = road.getTravelTimeMinutes();
                double newDist = uDist + weight;
                double currentDist = distances.getOrDefault(v, Double.POSITIVE_INFINITY);

                // Strictly faster path found
                if (newDist < currentDist - EPSILON) {
                    distances.put(v, newDist);
                    predecessors.put(v, u);
                    minHeap.add(new NodeDistance(v, newDist));
                }
                // Deterministic tie-breaker for equal-cost routes: favor smaller predecessor ID
                else if (Math.abs(newDist - currentDist) <= EPSILON) {
                    String existingPred = predecessors.get(v);
                    if (existingPred != null && u.compareTo(existingPred) < 0) {
                        predecessors.put(v, u);
                    }
                }
            }
        }

        // If destination was not reached or distance is infinite, return unreachable
        if (!distances.containsKey(destination) || Double.isInfinite(distances.get(destination))) {
            long elapsed = System.nanoTime() - startNanos;
            recordExecutionMetric(elapsed);
            return RouteResult.unreachable(elapsed);
        }

        // Reconstruct the ordered path from destination back to source
        LinkedList<Stop> path = new LinkedList<>();
        String step = destination;
        double totalDist = 0.0;
        while (step != null) {
            Stop stop = roadNetwork.getStop(step);
            if (stop != null) {
                path.addFirst(stop);
            }
            String pred = predecessors.get(step);
            if (pred != null) {
                Road road = roadNetwork.getRoad(pred, step);
                if (road != null) {
                    totalDist += road.getDistanceKm();
                }
            }
            step = pred;
        }

        long elapsed = System.nanoTime() - startNanos;
        recordExecutionMetric(elapsed);

        // Integrity check: path must start at source
        if (path.isEmpty() || !path.getFirst().getId().equalsIgnoreCase(source)) {
            return RouteResult.unreachable(elapsed);
        }

        double totalTravelTime = distances.get(destination);
        return RouteResult.reachable(path, totalTravelTime, totalDist, elapsed);
    }

    /**
     * Convenience method to find the shortest route without specific bus constraints.
     */
    public RouteResult findShortestRoute(String sourceStopId, String destinationStopId) {
        return findShortestRoute(sourceStopId, destinationStopId, null);
    }

    public RouteResult findShortestRoute(Stop sourceStop, Stop destinationStop, Bus bus) {
        if (sourceStop == null) {
            throw new IllegalArgumentException("Source stop cannot be null");
        }
        if (destinationStop == null) {
            throw new IllegalArgumentException("Destination stop cannot be null");
        }
        return findShortestRoute(sourceStop.getId(), destinationStop.getId(), bus);
    }

    public RouteResult findShortestRoute(Stop sourceStop, Stop destinationStop) {
        return findShortestRoute(sourceStop, destinationStop, null);
    }

    /**
     * Helper record for the min-heap PriorityQueue.
     * Orders vertices primarily by travel time (ascending), with deterministic lexicographical tie-breaking.
     */
    private record NodeDistance(String stopId, double distance) implements Comparable<NodeDistance> {
        @Override
        public int compareTo(NodeDistance other) {
            int cmp = Double.compare(this.distance, other.distance);
            if (cmp != 0) {
                return cmp;
            }
            // Deterministic tie-breaker
            return this.stopId.compareTo(other.stopId);
        }
    }
}
