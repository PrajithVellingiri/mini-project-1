package com.transport.backend.service;

import com.transport.backend.model.DemandRequest;
import com.transport.backend.model.Stop;
import com.transport.backend.policy.DefaultDemandPolicy;
import com.transport.backend.policy.DemandPolicy;

import java.util.*;

/**
 * Manages passenger travel demand queues ensuring:
 * 1. Global Priority: Evaluated across different pickup-destination corridors via PriorityQueue
 *    ranking (larger batches first, tie-broken by arrival order).
 * 2. Group-Level FIFO: Within the same pickup-destination corridor, earlier arriving batches
 *    must strictly never be overtaken by later batches, enforced via per-group ArrayDeque.
 *
 * Coexistence Architecture:
 * - Each unique corridor (pickup -> destination) has an ArrayDeque storing requests in strict FIFO order.
 * - Only the HEAD of each corridor's ArrayDeque is entered into the global PriorityQueue.
 * - Later batches waiting in the same corridor stay queued behind the head in the ArrayDeque.
 * - When a head batch is served, the next batch in that corridor's ArrayDeque is promoted to the PriorityQueue.
 * - When a batch is partially served, the remaining batch replaces the head with its original arrival order.
 */
public class DemandQueueService {

    private final DemandPolicy demandPolicy;
    private final PriorityQueue<DemandRequest> globalQueue;
    private final Map<String, ArrayDeque<DemandRequest>> groupQueues;
    private final Set<String> activeRequestIds;

    public DemandQueueService(DemandPolicy demandPolicy) {
        if (demandPolicy == null) {
            throw new IllegalArgumentException("DemandPolicy cannot be null");
        }
        this.demandPolicy = demandPolicy;
        this.globalQueue = new PriorityQueue<>(demandPolicy);
        this.groupQueues = new LinkedHashMap<>();
        this.activeRequestIds = new HashSet<>();
    }

    public DemandQueueService() {
        this(new DefaultDemandPolicy());
    }

    /**
     * Enqueues a new demand request.
     *
     * @param request The passenger batch request to enqueue
     * @throws IllegalArgumentException if request is null or ID is duplicate
     */
    public synchronized void enqueue(DemandRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("DemandRequest cannot be null");
        }
        if (activeRequestIds.contains(request.getRequestId())) {
            throw new IllegalArgumentException("Duplicate request ID: " + request.getRequestId());
        }

        String groupKey = getGroupKey(request.getPickupStop(), request.getDestinationStop());
        ArrayDeque<DemandRequest> groupQueue = groupQueues.computeIfAbsent(groupKey, k -> new ArrayDeque<>());

        if (groupQueue.isEmpty()) {
            // First batch in this corridor: becomes the corridor head and enters global PriorityQueue
            groupQueue.addLast(request);
            globalQueue.add(request);
        } else {
            // Corridor already has waiting batches: queue at the tail to preserve strict FIFO
            groupQueue.addLast(request);
        }

        activeRequestIds.add(request.getRequestId());
    }

    /**
     * Returns a snapshot list of currently eligible demand requests (the heads of each corridor),
     * sorted according to global priority.
     */
    public synchronized List<DemandRequest> getEligibleRequestsInPriorityOrder() {
        List<DemandRequest> eligible = new ArrayList<>(globalQueue);
        eligible.sort(demandPolicy);
        return Collections.unmodifiableList(eligible);
    }

    /**
     * Marks a demand request as fully served and removes it from the queues.
     * Promotes the next batch in the same corridor to the global PriorityQueue, if present.
     *
     * @param request The completed demand request
     */
    public synchronized void markServed(DemandRequest request) {
        if (request == null) return;

        String groupKey = getGroupKey(request.getPickupStop(), request.getDestinationStop());
        ArrayDeque<DemandRequest> groupQueue = groupQueues.get(groupKey);

        if (groupQueue != null && !groupQueue.isEmpty()) {
            // Remove head if it matches
            if (groupQueue.peekFirst().equals(request)) {
                groupQueue.pollFirst();
            } else {
                groupQueue.remove(request);
            }

            // Remove from global queue
            globalQueue.remove(request);
            activeRequestIds.remove(request.getRequestId());

            // Promote next batch in corridor to global queue
            if (!groupQueue.isEmpty()) {
                DemandRequest nextHead = groupQueue.peekFirst();
                if (!globalQueue.contains(nextHead)) {
                    globalQueue.add(nextHead);
                }
            } else {
                groupQueues.remove(groupKey);
            }
        } else {
            globalQueue.remove(request);
            activeRequestIds.remove(request.getRequestId());
        }
    }

    /**
     * Replaces a partially served demand batch with its remaining passenger batch.
     * The remaining batch retains the exact same original arrival order and position at the head of its corridor.
     *
     * @param originalRequest  The original demand batch
     * @param remainingRequest The remainder batch with preserved arrival order
     */
    public synchronized void markPartiallyServed(DemandRequest originalRequest, DemandRequest remainingRequest) {
        if (originalRequest == null || remainingRequest == null) {
            throw new IllegalArgumentException("Requests cannot be null");
        }
        if (originalRequest.getOriginalArrivalOrder() != remainingRequest.getOriginalArrivalOrder()) {
            throw new IllegalArgumentException("Remaining request must preserve identical originalArrivalOrder");
        }

        String groupKey = getGroupKey(originalRequest.getPickupStop(), originalRequest.getDestinationStop());
        ArrayDeque<DemandRequest> groupQueue = groupQueues.get(groupKey);

        if (groupQueue == null || groupQueue.isEmpty()) {
            throw new IllegalStateException("Corridor queue for " + groupKey + " is empty");
        }

        // Rule 6: Remove from PriorityQueue before updating/reinserting
        globalQueue.remove(originalRequest);
        activeRequestIds.remove(originalRequest.getRequestId());

        // Replace head in corridor queue
        groupQueue.pollFirst();
        groupQueue.addFirst(remainingRequest);

        // Reinsert updated record into PriorityQueue
        globalQueue.add(remainingRequest);
        activeRequestIds.add(remainingRequest.getRequestId());
    }

    /**
     * Checks if all queues are empty.
     */
    public synchronized boolean isEmpty() {
        return activeRequestIds.isEmpty();
    }

    /**
     * Total number of waiting demand batches across all corridors.
     */
    public synchronized int size() {
        return activeRequestIds.size();
    }

    /**
     * Computes total unserved passenger count across all corridors.
     */
    public synchronized int getTotalWaitingPassengers() {
        int total = 0;
        for (ArrayDeque<DemandRequest> deque : groupQueues.values()) {
            for (DemandRequest req : deque) {
                total += req.getPassengerCount();
            }
        }
        return total;
    }

    /**
     * Retrieves all waiting demand requests in arrival order per corridor.
     */
    public synchronized List<DemandRequest> getAllWaitingRequests() {
        List<DemandRequest> all = new ArrayList<>();
        for (ArrayDeque<DemandRequest> deque : groupQueues.values()) {
            all.addAll(deque);
        }
        return Collections.unmodifiableList(all);
    }

    public synchronized void clear() {
        globalQueue.clear();
        groupQueues.clear();
        activeRequestIds.clear();
    }

    private String getGroupKey(Stop pickup, Stop destination) {
        return pickup.getId().trim().toUpperCase() + "->" + destination.getId().trim().toUpperCase();
    }
}
