package com.transport.backend.service;

import com.transport.backend.model.DemandRequest;
import com.transport.backend.model.Stop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DemandQueueServiceTest {

    private DemandQueueService queueService;
    private Stop stopA;
    private Stop stopB;
    private Stop stopC;

    @BeforeEach
    void setUp() {
        queueService = new DemandQueueService();
        stopA = new Stop("STOP_A", "Downtown");
        stopB = new Stop("STOP_B", "Uptown");
        stopC = new Stop("STOP_C", "Suburbs");
    }

    @Test
    @DisplayName("Should maintain FIFO within identical pickup-destination corridor regardless of batch size")
    void shouldMaintainFifoWithinSameCorridor() {
        // Corridor: STOP_A -> STOP_B
        // Batch 1 arrives at t=1 with small size 10
        DemandRequest req1 = new DemandRequest("REQ-1", stopA, stopB, 10, 1L);
        // Batch 2 arrives at t=2 with huge size 60 in the SAME corridor
        DemandRequest req2 = new DemandRequest("REQ-2", stopA, stopB, 60, 2L);

        queueService.enqueue(req1);
        queueService.enqueue(req2);

        assertEquals(2, queueService.size());
        assertEquals(70, queueService.getTotalWaitingPassengers());

        // Eligible requests must contain ONLY req1 because req2 cannot overtake earlier req1 in the same corridor
        List<DemandRequest> eligible = queueService.getEligibleRequestsInPriorityOrder();
        assertEquals(1, eligible.size());
        assertEquals("REQ-1", eligible.get(0).getRequestId());

        // Once req1 is marked served, req2 becomes eligible
        queueService.markServed(req1);
        assertEquals(1, queueService.size());
        assertEquals(60, queueService.getTotalWaitingPassengers());

        List<DemandRequest> eligibleAfter = queueService.getEligibleRequestsInPriorityOrder();
        assertEquals(1, eligibleAfter.size());
        assertEquals("REQ-2", eligibleAfter.get(0).getRequestId());
    }

    @Test
    @DisplayName("Should rank different corridors globally by passenger count then arrival order")
    void shouldRankDifferentCorridorsByGlobalPriority() {
        // Corridor 1: STOP_A -> STOP_B with 15 passengers (t=1)
        DemandRequest req1 = new DemandRequest("R1", stopA, stopB, 15, 1L);
        // Corridor 2: STOP_A -> STOP_C with 40 passengers (t=5)
        DemandRequest req2 = new DemandRequest("R2", stopA, stopC, 40, 5L);
        // Corridor 3: STOP_B -> STOP_C with 40 passengers (t=2, earlier than R2)
        DemandRequest req3 = new DemandRequest("R3", stopB, stopC, 40, 2L);

        queueService.enqueue(req1);
        queueService.enqueue(req2);
        queueService.enqueue(req3);

        List<DemandRequest> eligible = queueService.getEligibleRequestsInPriorityOrder();
        assertEquals(3, eligible.size());

        // Global order:
        // 1st: R3 (40 pax, t=2)
        // 2nd: R2 (40 pax, t=5)
        // 3rd: R1 (15 pax, t=1)
        assertEquals("R3", eligible.get(0).getRequestId());
        assertEquals("R2", eligible.get(1).getRequestId());
        assertEquals("R1", eligible.get(2).getRequestId());
    }

    @Test
    @DisplayName("Should preserve arrival order when a batch is partially served")
    void shouldPreserveArrivalOrderOnPartialService() {
        DemandRequest req1 = new DemandRequest("R1", stopA, stopB, 30, 100L);
        queueService.enqueue(req1);

        DemandRequest rem = req1.createRemainingBatch("R1-REM", 18); // 12 passengers remain
        queueService.markPartiallyServed(req1, rem);

        assertEquals(1, queueService.size());
        assertEquals(12, queueService.getTotalWaitingPassengers());

        List<DemandRequest> eligible = queueService.getEligibleRequestsInPriorityOrder();
        assertEquals(1, eligible.size());
        assertEquals("R1-REM", eligible.get(0).getRequestId());
        assertEquals(12, eligible.get(0).getPassengerCount());
        assertEquals(100L, eligible.get(0).getOriginalArrivalOrder(), "Must preserve original arrival order");
    }

    @Test
    @DisplayName("Should reject duplicate request IDs")
    void shouldRejectDuplicateRequestIds() {
        DemandRequest req1 = new DemandRequest("REQ-DUP", stopA, stopB, 10, 1L);
        queueService.enqueue(req1);

        DemandRequest reqDuplicate = new DemandRequest("REQ-DUP", stopA, stopC, 20, 2L);
        assertThrows(IllegalArgumentException.class, () -> queueService.enqueue(reqDuplicate));
    }
}
