/**
 * Reusable HTTP client and API functions for Smart Public Transportation Route Optimization.
 * Configured with JWT Bearer authentication, dynamic VITE_API_BASE_URL support,
 * and role-based user management endpoints.
 */

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '');

let authToken = localStorage.getItem('smart_transit_token') || null;
let onUnauthorizedHandler = null;

export function setAuthToken(token) {
  authToken = token;
  if (token) {
    localStorage.setItem('smart_transit_token', token);
  } else {
    localStorage.removeItem('smart_transit_token');
  }
}

export function getAuthToken() {
  return authToken;
}

export function setUnauthorizedHandler(handler) {
  onUnauthorizedHandler = handler;
}

function getAuthHeaders(customHeaders = {}) {
  const headers = {
    'Accept': 'application/json',
    ...customHeaders,
  };
  if (authToken) {
    headers['Authorization'] = `Bearer ${authToken}`;
  }
  return headers;
}

async function handleResponse(response, defaultErrorMessage) {
  if (response.status === 401) {
    if (onUnauthorizedHandler) {
      onUnauthorizedHandler();
    }
  }

  if (!response.ok) {
    const errJson = await response.json().catch(() => null);
    const message = errJson?.message || `${defaultErrorMessage} (HTTP ${response.status})`;
    throw new Error(message);
  }

  return response.json();
}

// ── Authentication Endpoints ────────────────────────────────────────────────

export async function login(email, password) {
  try {
    const response = await fetch(`${API_BASE}/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ email: email.trim().toLowerCase(), password }),
    });

    if (!response.ok) {
      const errJson = await response.json().catch(() => null);
      throw new Error(errJson?.message || `Login failed with HTTP ${response.status}`);
    }

    const data = await response.json();
    if (data.token) {
      setAuthToken(data.token);
    }
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function fetchCurrentUser() {
  try {
    const response = await fetch(`${API_BASE}/api/auth/me`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });

    const data = await handleResponse(response, 'Failed to fetch current user profile');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function logoutUser() {
  try {
    await fetch(`${API_BASE}/api/auth/logout`, {
      method: 'POST',
      headers: getAuthHeaders(),
    }).catch(() => {});
  } finally {
    setAuthToken(null);
  }
  return { success: true };
}

// ── Admin User Management Endpoints ─────────────────────────────────────────

export async function fetchUsers(search = '', role = '') {
  try {
    const params = new URLSearchParams();
    if (search) params.append('search', search);
    if (role && role !== 'ALL') params.append('role', role);

    const queryString = params.toString() ? `?${params.toString()}` : '';
    const response = await fetch(`${API_BASE}/api/users${queryString}`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });

    const data = await handleResponse(response, 'Failed to fetch users');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function createUser(userData) {
  try {
    const response = await fetch(`${API_BASE}/api/users`, {
      method: 'POST',
      headers: getAuthHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify(userData),
    });

    const data = await handleResponse(response, 'Failed to create user');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function updateUser(id, userData) {
  try {
    const response = await fetch(`${API_BASE}/api/users/${id}`, {
      method: 'PUT',
      headers: getAuthHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify(userData),
    });

    const data = await handleResponse(response, 'Failed to update user');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function toggleUserStatus(id, enabled) {
  try {
    const response = await fetch(`${API_BASE}/api/users/${id}/status`, {
      method: 'PATCH',
      headers: getAuthHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ enabled }),
    });

    const data = await handleResponse(response, 'Failed to update user status');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function resetUserPassword(id, newPassword) {
  try {
    const response = await fetch(`${API_BASE}/api/users/${id}/reset-password`, {
      method: 'POST',
      headers: getAuthHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ newPassword }),
    });

    const data = await handleResponse(response, 'Failed to reset password');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

// ── Public System Health Endpoint ───────────────────────────────────────────

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

// ── Protected Simulation Endpoints ──────────────────────────────────────────

export async function fetchSimulationState() {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/state`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });
    const data = await handleResponse(response, 'Failed to fetch simulation state');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function runSimulation() {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/run`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    const data = await handleResponse(response, 'Failed to execute simulation run');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function resetSimulation() {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/reset`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    const data = await handleResponse(response, 'Failed to reset simulation');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function completeActiveTrips() {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/complete-trips`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    const data = await handleResponse(response, 'Failed to complete active trips');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function completeSpecificTrip(tripId) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/trips/${encodeURIComponent(tripId)}/complete`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    const data = await handleResponse(response, `Failed to complete trip ${tripId}`);
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
      headers: getAuthHeaders(),
    });
    const data = await handleResponse(response, 'Failed to calculate route');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function updateRoadTravelTime(fromStopId, toStopId, travelTimeMinutes) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/roads/travel-time`, {
      method: 'PATCH',
      headers: getAuthHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({
        fromStopId,
        toStopId,
        travelTimeMinutes: parseFloat(travelTimeMinutes),
      }),
    });
    const data = await handleResponse(response, 'Failed to update road travel time');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function updateRoadStatus(fromStopId, toStopId, open, bidirectional = true) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/roads/status`, {
      method: 'PATCH',
      headers: getAuthHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({
        fromStopId,
        toStopId,
        open,
        bidirectional,
      }),
    });
    const data = await handleResponse(response, 'Failed to update road availability');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function updateBusStatus(busId, status) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/buses/${encodeURIComponent(busId)}/status`, {
      method: 'PATCH',
      headers: getAuthHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ status }),
    });
    const data = await handleResponse(response, `Failed to update status for bus ${busId}`);
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function createDemandRequest(pickupStopId, destinationStopId, passengerCount) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/demand`, {
      method: 'POST',
      headers: getAuthHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({
        pickupStopId,
        destinationStopId,
        passengerCount: parseInt(passengerCount, 10),
      }),
    });
    const data = await handleResponse(response, 'Failed to create demand batch');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function fetchAnalytics() {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/analytics`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });
    const data = await handleResponse(response, 'Failed to fetch analytics');
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function triggerBusBreakdown(busId) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/buses/${encodeURIComponent(busId)}/breakdown`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    const data = await handleResponse(response, `Failed to trigger breakdown for bus ${busId}`);
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function restoreBus(busId) {
  try {
    const response = await fetch(`${API_BASE}/api/simulation/buses/${encodeURIComponent(busId)}/restore`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    const data = await handleResponse(response, `Failed to restore bus ${busId}`);
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error.message };
  }
}
