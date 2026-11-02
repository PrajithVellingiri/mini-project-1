import StatCard from '../common/StatCard';
import Badge from '../common/Badge';
import {
  Users,
  Zap,
  Bus,
  MapPin,
  Play,
  CheckCircle2,
  RotateCcw,
  ArrowRight,
  Clock,
} from 'lucide-react';

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
  canReset = true,
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
          icon={<Users size={20} className="text-amber-500" />}
        />
        <StatCard
          title="Active Relief Trips"
          value={activeTripsList.length}
          subtitle={`${summary.allocatedPassengers || 0} passengers boarded`}
          badgeText={activeTripsList.length > 0 ? 'En Route' : 'Idle'}
          badgeVariant={activeTripsList.length > 0 ? 'transit' : 'available'}
          icon={<Zap size={20} className="text-indigo-500" />}
        />
        <StatCard
          title="Available Fleet"
          value={`${availableBuses.length} / ${buses.length}`}
          subtitle={`${buses.filter((b) => b.status === 'MAINTENANCE').length} in maintenance`}
          badgeText="Readiness"
          badgeVariant="available"
          icon={<Bus size={20} className="text-emerald-500" />}
        />
        <StatCard
          title="Network Transit Stops"
          value={summary.totalStops || 13}
          subtitle={`${summary.totalRoads || 38} directed road edges`}
          badgeText="1 Central Depot"
          badgeVariant="default"
          icon={<MapPin size={20} className="text-blue-500" />}
        />
      </div>

      {/* Primary Action Banner */}
      <div className="action-control-banner">
        <div className="banner-text">
          <h3 className="banner-heading">Simulation Dispatch Engine</h3>
          <p className="banner-sub">
            Trigger greedy capacity-aware allocation passes or manage operational fleet dispatches.
          </p>
        </div>
        <div className="banner-actions">
          <button
            className="action-btn btn-run"
            onClick={onRunSimulation}
            disabled={simLoading || availableBuses.length === 0}
            title={availableBuses.length === 0 ? 'No buses currently available' : 'Dispatch available buses to highest priority demand'}
          >
            <Play size={15} />
            <span>{simLoading ? 'Dispatching...' : 'Run Allocation Pass'}</span>
          </button>
          {activeTripsList.length > 0 && (
            <button
              className="action-btn btn-complete"
              onClick={onCompleteAllTrips}
              disabled={simLoading}
            >
              <CheckCircle2 size={15} />
              <span>Complete All Trips</span>
            </button>
          )}
          {canReset && (
            <button
              className="action-btn btn-reset"
              onClick={onResetSimulation}
              disabled={simLoading}
              title="Restores the deterministic reference dataset (Admin only)"
            >
              <RotateCcw size={15} />
              <span>Reset Simulation</span>
            </button>
          )}
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
              <span>View Fleet Console</span>
              <ArrowRight size={14} className="inline ml-1" />
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
                    <span className="trip-pax font-semibold">{trip.boardedPassengerCount} Pax</span>
                  </div>
                  <div className="trip-card-route">
                    <span className="route-endpoints">
                      {trip.originStopId} &rarr; {trip.destinationStopId}
                    </span>
                    <span className="route-eta">
                      <Clock size={12} className="inline mr-1" />
                      {trip.estimatedTravelTimeMinutes}m
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Top Waiting Passenger Demand */}
        <div className="overview-card">
          <div className="overview-card-header">
            <div>
              <h4 className="card-title">Top Waiting Demand Queues ({demands.length})</h4>
              <span className="card-subtitle">Aggregated by corridor in priority order</span>
            </div>
            <button className="view-all-link" onClick={() => onSelectTab('fleet')}>
              <span>View Demand Queue</span>
              <ArrowRight size={14} className="inline ml-1" />
            </button>
          </div>

          {demands.length === 0 ? (
            <div className="empty-overview-card">
              <span>All passenger demand has been fully satisfied. System is clear.</span>
            </div>
          ) : (
            <div className="mini-demand-list">
              {demands.slice(0, 5).map((d) => (
                <div key={d.requestId} className="mini-demand-row">
                  <div className="demand-row-left">
                    <span className="demand-req-id font-mono">{d.requestId}</span>
                    <span className="demand-corridor">
                      {d.pickupStopName || d.pickupStopId} &rarr; {d.destinationStopName || d.destinationStopId}
                    </span>
                  </div>
                  <div className="demand-row-right">
                    <span className="demand-pax-count font-mono">{d.passengerCount} passengers</span>
                    <Badge variant={d.originalArrivalOrder === 0 ? 'orange' : 'default'}>
                      {d.originalArrivalOrder === 0 ? 'Emergency' : `#${d.originalArrivalOrder}`}
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
