/**
 * Reusable HTTP client and API functions for Smart Public Transportation Route Optimization.
 * Configured with dynamic VITE_API_BASE_URL support for seamless local and production deployments.
 */

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '');

export async function fetchHealth() {
  const startTime = performance.now();
  try {
    const response = await fetch(`${API_BASE}/api/health`, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
    });

    const latencyMs = Math.round(performance.now() - startTime);

    if (!response.ok) {
      throw new Error(`Server returned HTTP ${response.status} (${response.statusText || 'Error'})`);
    }

    const data = await response.json();
    return {
      success: true,
      data,
      latencyMs,
      timestamp: new Date().toLocaleTimeString(),
    };
  } catch (error) {
    const latencyMs = Math.round(performance.now() - startTime);
    return {
      success: false,
      error: error.message || 'Unable to connect to backend server',
      latencyMs,
      timestamp: new Date().toLocaleTimeString(),
    };
  }
}

export async function fetchSimulationState() {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/state`, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`Failed to fetch simulation state: HTTP ${response.status}`);
    }
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function runSimulation() {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/run`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`Failed to execute simulation run: HTTP ${response.status}`);
    }
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function resetSimulation() {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/reset`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`Failed to reset simulation: HTTP ${response.status}`);
    }
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function completeActiveTrips() {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/complete-trips`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`Failed to complete active trips: HTTP ${response.status}`);
    }
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function completeSpecificTrip(tripId) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/trips/${encodeURIComponent(tripId)}/complete`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`Failed to complete trip ${tripId}: HTTP ${response.status}`);
    }
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function fetchRoute(source, destination, vehicleType = '') {
  try {
    const params = new URLSearchParams({ source, destination });
    if (vehicleType && vehicleType !== 'GENERAL') {
      params.append('vehicleType', vehicleType);
    }

    const response = await fetch(`${API_BASE}/api/simulation/routes?${params.toString()}`, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errJson = await response.json().catch(() => null);
      throw new Error(errJson?.message || `Failed to calculate route: HTTP ${response.status}`);
    }
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function updateRoadTravelTime(fromStopId, toStopId, travelTimeMinutes) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/roads/travel-time`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({
        fromStopId,
        toStopId,
        travelTimeMinutes: parseFloat(travelTimeMinutes),
      }),
    });
    if (!response.ok) {
      const errJson = await response.json().catch(() => null);
      throw new Error(errJson?.message || `Failed to update road travel time: HTTP ${response.status}`);
    }
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function updateRoadStatus(fromStopId, toStopId, open, bidirectional = true) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/roads/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({
        fromStopId,
        toStopId,
        open,
        bidirectional,
      }),
    });
    if (!response.ok) {
      const errJson = await response.json().catch(() => null);
      throw new Error(errJson?.message || `Failed to update road availability: HTTP ${response.status}`);
    }
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function updateBusStatus(busId, status) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/buses/${encodeURIComponent(busId)}/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ status }),
    });
    if (!response.ok) {
      const errJson = await response.json().catch(() => null);
      throw new Error(errJson?.message || `Failed to update bus status: HTTP ${response.status}`);
    }
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function createDemandRequest(pickupStopId, destinationStopId, passengerCount) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/demand`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({
        pickupStopId,
        destinationStopId,
        passengerCount: parseInt(passengerCount, 10),
      }),
    });
    if (!response.ok) {
      const errJson = await response.json().catch(() => null);
      throw new Error(errJson?.message || `Failed to create demand batch: HTTP ${response.status}`);
    }
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function fetchAnalytics() {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/analytics`, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errJson = await response.json().catch(() => null);
      throw new Error(errJson?.message || `Failed to fetch analytics: HTTP ${response.status}`);
    }
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function triggerBusBreakdown(busId) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/buses/${encodeURIComponent(busId)}/breakdown`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errJson = await response.json().catch(() => null);
      throw new Error(errJson?.message || `Failed to trigger breakdown for bus ${busId}: HTTP ${response.status}`);
    }
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function restoreBus(busId) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/buses/${encodeURIComponent(busId)}/restore`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errJson = await response.json().catch(() => null);
      throw new Error(errJson?.message || `Failed to restore bus ${busId}: HTTP ${response.status}`);
    }
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

