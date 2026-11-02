package com.transport.backend.policy;

import com.transport.backend.model.DemandRequest;
import com.transport.backend.model.Stop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

class DemandRequestAndPolicyTest {

    private Stop stopA;
    private Stop stopB;
    private Stop stopC;
    private DemandPolicy policy;

    @BeforeEach
    void setUp() {
        stopA = new Stop("STOP_A", "Terminal A");
        stopB = new Stop("STOP_B", "Terminal B");
        stopC = new Stop("STOP_C", "Terminal C");
        policy = new DefaultDemandPolicy();
    }

    @Test
    @DisplayName("Should preserve arrival order during partial serving and batch reduction")
    void shouldPreserveArrivalOrderOnPartialService() {
        long originalArrivalOrder = 101L;
        DemandRequest batch = new DemandRequest("REQ-1", stopA, stopB, 25, originalArrivalOrder);

        assertEquals(25, batch.getPassengerCount());
        assertEquals(originalArrivalOrder, batch.getOriginalArrivalOrder());

        // Partially serve 10 passengers
        batch.servePartial(10);
        assertEquals(15, batch.getPassengerCount());
        assertEquals(originalArrivalOrder, batch.getOriginalArrivalOrder(), "Arrival order must be preserved after partial service");

        // Split remaining into a new batch
        DemandRequest remaining = batch.createRemainingBatch("REQ-1-REM", 5);
        assertEquals(10, remaining.getPassengerCount());
        assertEquals(originalArrivalOrder, remaining.getOriginalArrivalOrder(), "Split batch must retain identical original arrival order");
    }

    @Test
    @DisplayName("Should validate demand request parameters")
    void shouldValidateDemandRequestParameters() {
        assertThrows(IllegalArgumentException.class, () -> new DemandRequest("", stopA, stopB, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> new DemandRequest("REQ", null, stopB, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> new DemandRequest("REQ", stopA, null, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> new DemandRequest("REQ", stopA, stopA, 10, 1)); // same stop
        assertThrows(IllegalArgumentException.class, () -> new DemandRequest("REQ", stopA, stopB, 0, 1)); // count <= 0
        assertThrows(IllegalArgumentException.class, () -> new DemandRequest("REQ", stopA, stopB, -5, 1));
        assertThrows(IllegalArgumentException.class, () -> new DemandRequest("REQ", stopA, stopB, 10, -1)); // negative arrival order
    }

    @Test
    @DisplayName("DemandPolicy should prioritize larger passenger counts, then earlier arrival order")
    void shouldRankByPassengerCountThenArrivalOrder() {
        DemandRequest d1 = new DemandRequest("D1", stopA, stopB, 15, 10L); // 15 pax, order 10
        DemandRequest d2 = new DemandRequest("D2", stopA, stopC, 30, 20L); // 30 pax, order 20 (larger count -> higher priority)
        DemandRequest d3 = new DemandRequest("D3", stopB, stopC, 30, 5L);  // 30 pax, order 5 (same count as d2, but arrived earlier)
        DemandRequest d4 = new DemandRequest("D4", stopA, stopB, 10, 1L);  // 10 pax, order 1

        PriorityQueue<DemandRequest> queue = new PriorityQueue<>(policy);
        queue.add(d1);
        queue.add(d2);
        queue.add(d3);
        queue.add(d4);

        // Expected polling order:
        // 1st: d3 (30 passengers, arrival order 5)
        // 2nd: d2 (30 passengers, arrival order 20)
        // 3rd: d1 (15 passengers, arrival order 10)
        // 4th: d4 (10 passengers, arrival order 1)
        assertEquals("D3", queue.poll().getRequestId());
        assertEquals("D2", queue.poll().getRequestId());
        assertEquals("D1", queue.poll().getRequestId());
        assertEquals("D4", queue.poll().getRequestId());
        assertTrue(queue.isEmpty());
    }
}
