import { useState, useEffect, useCallback } from 'react';
import Badge from '../common/Badge';
import { fetchAnalytics } from '../../api/client';

export default function AnalyticsDashboard() {
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastRefreshed, setLastRefreshed] = useState(null);

  const loadAnalytics = useCallback(async () => {
    setLoading(true);
    setError(null);
    const result = await fetchAnalytics();
    if (result.success) {
      setAnalytics(result.data);
      setLastRefreshed(new Date().toLocaleTimeString());
    } else {
      setError(result.error || 'Failed to load operational analytics');
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    loadAnalytics();
  }, [loadAnalytics]);

  if (loading && !analytics) {
    return (
      <div className="analytics-loading-container">
        <div className="spinner"></div>
        <p className="loading-text">Aggregating operational analytics and benchmark telemetry...</p>
      </div>
    );
  }

  if (error && !analytics) {
    return (
      <div className="analytics-error-banner">
        <div className="error-icon">⚠️</div>
        <div className="error-content">
          <h4>Failed to Load Analytics</h4>
          <p>{error}</p>
          <button className="btn-retry" onClick={loadAnalytics}>Retry Connection</button>
        </div>
      </div>
    );
  }

  const { passengers, fleet, trips, demand, telemetry } = analytics || {};

  return (
    <div className="analytics-dashboard">
      {/* Header bar */}
      <div className="analytics-header">
        <div className="analytics-header-left">
          <h2 className="section-heading">Operational Analytics & Telemetry</h2>
          <p className="section-sub">
            Real-time passenger conservation tracking, fleet utilization, trip statistics, and Dijkstra algorithmic benchmarking.
          </p>
        </div>
        <div className="analytics-header-actions">
          {lastRefreshed && (
            <span className="last-refreshed">Last updated: {lastRefreshed}</span>
          )}
          <button
            className="action-btn btn-secondary refresh-btn"
            onClick={loadAnalytics}
            disabled={loading}
          >
            {loading ? <span className="btn-spinner"></span> : '🔄'} Refresh
          </button>
        </div>
      </div>

      {error && (
        <div className="warning-toast">
          ⚠️ Could not refresh live metrics: {error}
        </div>
      )}

      {/* 1. Passenger Conservation Invariant Card */}
      <div className="analytics-card conservation-card">
        <div className="card-top-row">
          <div className="card-title-group">
            <span className="card-icon">⚖️</span>
            <div>
              <h3 className="card-title">Passenger Conservation Invariant</h3>
              <p className="card-subtitle">
                Guaranteed zero loss across demand queue, boarded capacity, and completed deliveries.
              </p>
            </div>
          </div>
          <Badge
            variant={passengers?.conservationValid ? 'available' : 'transit'}
            text={passengers?.conservationValid ? 'Invariant Verified (Total = W + T + C)' : 'Verification Pending'}
          />
        </div>

        <div className="conservation-equation">
          <div className="equation-term highlight-total">
            <span className="term-label">Total System Demand</span>
            <span className="term-value">{passengers?.totalPassengers ?? 0}</span>
            <span className="term-sub">100% of all recorded demand</span>
          </div>
          <span className="equation-operator">=</span>
          <div className="equation-term">
            <span className="term-label">Waiting in Queue</span>
            <span className="term-value text-orange">{passengers?.waitingPassengers ?? 0}</span>
            <span className="term-sub">At terminal / en route stops</span>
          </div>
          <span className="equation-operator">+</span>
          <div className="equation-term">
            <span className="term-label">In-Transit (Boarded)</span>
            <span className="term-value text-blue">{passengers?.inTransitPassengers ?? 0}</span>
            <span className="term-sub">Occupying active bus seats</span>
          </div>
          <span className="equation-operator">+</span>
          <div className="equation-term">
            <span className="term-label">Completed Delivery</span>
            <span className="term-value text-green">{passengers?.completedPassengers ?? 0}</span>
            <span className="term-sub">Alighted at destination</span>
          </div>
        </div>
      </div>

      {/* 2. Operations Grid (Fleet Utilization & Trip Performance) */}
      <div className="analytics-two-col-grid">
        {/* Fleet Metrics */}
        <div className="analytics-card">
          <div className="card-top-row">
            <div className="card-title-group">
              <span className="card-icon">🚌</span>
              <div>
                <h3 className="card-title">Fleet Deployment & Utilization</h3>
                <p className="card-subtitle">Real-time status breakdown across all registered transit vehicles</p>
              </div>
            </div>
            <span className="metric-pill pill-orange">
              {fleet?.fleetUtilizationPercent?.toFixed(1) ?? 0}% Utilized
            </span>
          </div>

          <div className="progress-bar-container">
            <div
              className="progress-bar-fill"
              style={{ width: `${Math.min(100, fleet?.fleetUtilizationPercent ?? 0)}%` }}
            ></div>
          </div>

          <div className="metrics-subgrid">
            <div className="metric-box">
              <span className="box-val text-green">{fleet?.availableBuses ?? 0}</span>
              <span className="box-label">Available Buses</span>
            </div>
            <div className="metric-box">
              <span className="box-val text-blue">{fleet?.inTransitBuses ?? 0}</span>
              <span className="box-label">In-Transit Buses</span>
            </div>
            <div className="metric-box">
              <span className="box-val text-red">{fleet?.maintenanceBuses ?? 0}</span>
              <span className="box-label">In Maintenance</span>
            </div>
            <div className="metric-box">
              <span className="box-val">{fleet?.totalBuses ?? 0}</span>
              <span className="box-label">Total Fleet Size</span>
            </div>
          </div>

          <div className="capacity-summary-row">
            <span>Seat Capacity Utilization:</span>
            <strong>
              {fleet?.occupiedSeats ?? 0} / {fleet?.totalFleetCapacity ?? 0} seats ({fleet?.seatUtilizationPercent?.toFixed(1) ?? 0}%)
            </strong>
          </div>
        </div>

        {/* Trip Performance */}
        <div className="analytics-card">
          <div className="card-top-row">
            <div className="card-title-group">
              <span className="card-icon">🗺️</span>
              <div>
                <h3 className="card-title">Trip Execution & Dispatch</h3>
                <p className="card-subtitle">Dispatch volume, completion rate, and average trip duration</p>
              </div>
            </div>
            <span className="metric-pill pill-cyan">
              {trips?.totalDispatchedTrips ?? 0} Dispatched
            </span>
          </div>

          <div className="metrics-subgrid">
            <div className="metric-box">
              <span className="box-val text-blue">{trips?.activeTripsCount ?? 0}</span>
              <span className="box-label">Active En-Route</span>
            </div>
            <div className="metric-box">
              <span className="box-val text-green">{trips?.completedTripsCount ?? 0}</span>
              <span className="box-label">Completed Trips</span>
            </div>
            <div className="metric-box">
              <span className="box-val text-red">{trips?.cancelledTripsCount ?? 0}</span>
              <span className="box-label">Cancelled / Incident</span>
            </div>
            <div className="metric-box">
              <span className="box-val text-orange">
                {trips?.averageEstimatedTripTimeMinutes ? `${trips.averageEstimatedTripTimeMinutes.toFixed(1)}m` : '0.0m'}
              </span>
              <span className="box-label">Avg Travel Time</span>
            </div>
          </div>

          <div className="trip-status-summary">
            <div className="status-item">
              <span className="status-dot dot-green"></span>
              <span>Completion Rate:</span>
              <strong>
                {trips?.totalDispatchedTrips > 0
                  ? Math.round(((trips.completedTripsCount || 0) / trips.totalDispatchedTrips) * 100)
                  : 100}%
              </strong>
            </div>
            <div className="status-item">
              <span className="status-dot dot-cyan"></span>
              <span>Active Dispatches:</span>
              <strong>{trips?.activeTripsCount ?? 0}</strong>
            </div>
          </div>
        </div>
      </div>

      {/* 3. Dijkstra Shortest-Path Algorithmic Telemetry */}
      <div className="analytics-card telemetry-card">
        <div className="card-top-row">
          <div className="card-title-group">
            <span className="card-icon">⚡</span>
            <div>
              <h3 className="card-title">Dijkstra Shortest-Path Algorithmic Telemetry</h3>
              <p className="card-subtitle">
                Monotonic high-resolution timer (<code>System.nanoTime()</code>) profiling runtime graph traversal efficiency.
              </p>
            </div>
          </div>
          <Badge variant="orange" text="High-Resolution Profiler" />
        </div>

        <div className="telemetry-grid">
          <div className="telemetry-box">
            <span className="telemetry-number">{telemetry?.totalCalculations ?? 0}</span>
            <span className="telemetry-label">Total Dijkstra Calculations</span>
            <span className="telemetry-sub">Across simulation & interactive queries</span>
          </div>
          <div className="telemetry-box highlight-telemetry">
            <span className="telemetry-number text-orange">
              {telemetry?.averageLatencyMicros ? telemetry.averageLatencyMicros.toFixed(1) : '0.0'} µs
            </span>
            <span className="telemetry-label">Average Computation Latency</span>
            <span className="telemetry-sub">Microsecond execution benchmark</span>
          </div>
          <div className="telemetry-box">
            <span className="telemetry-number">
              {telemetry?.minLatencyMicros ? telemetry.minLatencyMicros.toFixed(1) : '0.0'} / {telemetry?.maxLatencyMicros ? telemetry.maxLatencyMicros.toFixed(1) : '0.0'} µs
            </span>
            <span className="telemetry-label">Min / Max Recorded Latency</span>
            <span className="telemetry-sub">Fastest cached vs cold path search</span>
          </div>
          <div className="telemetry-box">
            <span className="telemetry-number text-cyan">
              {telemetry?.lastLatencyMicros ? telemetry.lastLatencyMicros.toFixed(1) : '0.0'} µs
            </span>
            <span className="telemetry-label">Latest Path Computation</span>
            <span className="telemetry-sub">Most recent Dijkstra invocation</span>
          </div>
        </div>
      </div>

      {/* 4. Top Waiting Demand Corridors */}
      <div className="analytics-card">
        <div className="card-top-row">
          <div className="card-title-group">
            <span className="card-icon">🔥</span>
            <div>
              <h3 className="card-title">Top Waiting Transit Corridors</h3>
              <p className="card-subtitle">Corridors with the highest passenger demand volume waiting in the PriorityQueue</p>
            </div>
          </div>
          <span className="metric-pill pill-orange">
            {demand?.totalWaitingPassengers ?? 0} Total Waiting
          </span>
        </div>

        {demand?.topCorridors?.length > 0 ? (
          <div className="corridor-table-wrapper">
            <table className="analytics-table">
              <thead>
                <tr>
                  <th>Origin Stop</th>
                  <th>Destination Stop</th>
                  <th>Queued Batches</th>
                  <th>Waiting Passengers</th>
                  <th>Allocation Priority</th>
                </tr>
              </thead>
              <tbody>
                {demand.topCorridors.map((c, idx) => (
                  <tr key={`${c.pickupStopId}-${c.destinationStopId}-${idx}`}>
                    <td>
                      <span className="stop-code-badge">{c.pickupStopId}</span> {c.pickupStopName}
                    </td>
                    <td>
                      <span className="stop-code-badge">{c.destinationStopId}</span> {c.destinationStopName}
                    </td>
                    <td>{c.batchCount} batch{c.batchCount > 1 ? 'es' : ''}</td>
                    <td>
                      <strong className="text-orange">{c.waitingPassengers} passengers</strong>
                    </td>
                    <td>
                      <Badge
                        variant={idx === 0 ? 'orange' : idx < 3 ? 'transit' : 'default'}
                        text={idx === 0 ? 'Highest Demand' : idx < 3 ? 'High Priority' : 'Normal'}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="analytics-empty-state">
            <p>✅ All queued demand has been allocated. No passengers currently waiting at transit stops.</p>
          </div>
        )}
      </div>
    </div>
  );
}
