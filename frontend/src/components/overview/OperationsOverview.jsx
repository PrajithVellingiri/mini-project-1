import StatCard from '../common/StatCard';
import Badge from '../common/Badge';

export default function OperationsOverview({
  summary = {},
  buses = [],
  trips = [],
  demands = [],
  onRunSimulation,
  onResetSimulation,
  onCompleteAllTrips,
  onSelectTab,
  simLoading,
}) {
  const activeTripsList = trips.filter((t) => t.status === 'IN_PROGRESS');
  const availableBuses = buses.filter((b) => b.status === 'AVAILABLE' && b.availableSeats > 0);

  return (
    <div className="operations-overview">
      {/* KPI Cards Row */}
      <div className="stats-kpi-grid">
        <StatCard
          title="Passenger Demand"
          value={summary.waitingPassengers || 0}
          subtitle={`${summary.waitingDemandBatches || 0} queued batches waiting`}
          highlight={true}
          badgeText="Waiting"
          badgeVariant="orange"
          icon="👥"
        />
        <StatCard
          title="Active Relief Trips"
          value={activeTripsList.length}
          subtitle={`${summary.allocatedPassengers || 0} passengers boarded`}
          badgeText={activeTripsList.length > 0 ? 'En Route' : 'Idle'}
          badgeVariant={activeTripsList.length > 0 ? 'transit' : 'available'}
          icon="⚡"
        />
        <StatCard
          title="Available Fleet"
          value={`${availableBuses.length} / ${buses.length}`}
          subtitle={`${buses.filter((b) => b.status === 'MAINTENANCE').length} in maintenance`}
          badgeText="Readiness"
          badgeVariant="available"
          icon="🚌"
        />
        <StatCard
          title="Network Transit Stops"
          value={summary.totalStops || 13}
          subtitle={`${summary.totalRoads || 38} directed road edges`}
          badgeText="1 Central Depot"
          badgeVariant="default"
          icon="📍"
        />
      </div>

      {/* Primary Action Banner */}
      <div className="action-control-banner">
        <div className="banner-text">
          <h3 className="banner-heading">Simulation Dispatch Engine</h3>
          <p className="banner-sub">
            Trigger greedy capacity-aware allocation passes or reset the deterministic dataset.
          </p>
        </div>
        <div className="banner-actions">
          <button
            className="action-btn btn-run"
            onClick={onRunSimulation}
            disabled={simLoading || availableBuses.length === 0}
            title={availableBuses.length === 0 ? 'No buses currently available' : 'Dispatch available buses to highest priority demand'}
          >
            {simLoading ? 'Dispatching...' : '▶ Run Allocation Pass'}
          </button>
          {activeTripsList.length > 0 && (
            <button
              className="action-btn btn-complete"
              onClick={onCompleteAllTrips}
              disabled={simLoading}
            >
              ✓ Complete All Trips
            </button>
          )}
          <button
            className="action-btn btn-reset"
            onClick={onResetSimulation}
            disabled={simLoading}
          >
            ↺ Reset Simulation
          </button>
        </div>
      </div>

      {/* Two Column Quick View: Active Trips & Top Corridor Demand */}
      <div className="overview-split-grid">
        {/* Active Trips Quick View */}
        <div className="overview-card">
          <div className="overview-card-header">
            <div>
              <h4 className="card-title">Live Dispatched Trips ({activeTripsList.length})</h4>
              <span className="card-subtitle">Vehicles currently in transit</span>
            </div>
            <button className="view-all-link" onClick={() => onSelectTab('fleet')}>
              View Fleet Console →
            </button>
          </div>

          {activeTripsList.length === 0 ? (
            <div className="empty-overview-card">
              <span>All buses idle at stations. Click "Run Allocation Pass" to dispatch relief trips.</span>
            </div>
          ) : (
            <div className="active-trips-list">
              {activeTripsList.map((trip) => (
                <div key={trip.tripId} className="mini-trip-card">
                  <div className="trip-card-top">
                    <span className="trip-id font-mono">{trip.tripId}</span>
                    <Badge variant="transit">{trip.status}</Badge>
                  </div>
                  <div className="trip-card-mid">
                    <span className="trip-bus font-mono">{trip.busId} ({trip.busType})</span>
                    <span className="trip-pax font-semibold text-orange">{trip.passengerCount} passengers</span>
                  </div>
                  <div className="trip-card-route font-mono">
                    {trip.routeStopIds ? trip.routeStopIds.join(' → ') : trip.tripId}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Top Waiting Corridors */}
        <div className="overview-card">
          <div className="overview-card-header">
            <div>
              <h4 className="card-title">Pending Demand Queue ({demands.length} Batches)</h4>
              <span className="card-subtitle">Corridor FIFO + Priority Queue head batches</span>
            </div>
            <button className="view-all-link" onClick={() => onSelectTab('fleet')}>
              Manage Demand →
            </button>
          </div>

          <div className="demands-queue-table-wrapper">
            <table className="custom-data-table mini-table">
              <thead>
                <tr>
                  <th>Req ID</th>
                  <th>Corridor</th>
                  <th>Pax</th>
                  <th>Arrival Rank</th>
                </tr>
              </thead>
              <tbody>
                {demands.slice(0, 5).map((d, index) => (
                  <tr key={d.requestId}>
                    <td className="font-mono">{d.requestId}</td>
                    <td className="font-mono">{d.pickupStopId} → {d.destinationStopId}</td>
                    <td className="font-semibold text-orange">{d.passengerCount}</td>
                    <td>
                      <span className="font-mono rank-badge">#{d.originalArrivalOrder}</span>
                      {index === 0 && <span className="fifo-head-pill">Global Head</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
