import { useState } from 'react';
import {
  Bus,
  CheckCircle2,
  Wrench,
  RotateCcw,
  MapPin,
  Users,
  AlertTriangle,
  Compass,
} from 'lucide-react';

export default function DriverCockpit({
  simState,
  onCompleteTrip,
  onUpdateStatus,
  onRestoreBus,
}) {
  const buses = simState?.buses || [];
  const activeTrips = simState?.trips || [];
  const roads = simState?.roads || [];

  const [selectedBusId, setSelectedBusId] = useState(buses[0]?.id || 'BUS-01');

  const selectedBus = buses.find((b) => b.id === selectedBusId) || buses[0];
  const busTrip = activeTrips.find((t) => t.busId === selectedBus?.id);

  const closedRoads = roads.filter((r) => !r.open);

  return (
    <div className="driver-cockpit-container">
      {/* Top Header */}
      <div className="driver-header-banner">
        <div>
          <div className="driver-badge">
            <Bus size={14} />
            <span>Driver Operations Cockpit</span>
          </div>
          <h2 className="driver-banner-title">Assigned Vehicle Manifest & Controls</h2>
          <p className="driver-banner-sub">
            Monitor real-time passenger occupancy, track assigned relief trips, and report operational vehicle status.
          </p>
        </div>

        {/* Bus Selector */}
        <div className="bus-selector-box">
          <label className="text-xs font-semibold text-gray-700 block mb-1">Select Active Bus:</label>
          <select
            value={selectedBusId}
            onChange={(e) => setSelectedBusId(e.target.value)}
            className="driver-bus-select"
          >
            {buses.map((b) => (
              <option key={b.id} value={b.id}>
                {b.id} ({b.type}) — {b.status}
              </option>
            ))}
          </select>
        </div>
      </div>

      {selectedBus && (
        <div className="driver-dashboard-grid">
          {/* Main Bus Status & KPI Card */}
          <div className="driver-card driver-bus-card">
            <div className="card-top-row">
              <div className="flex items-center gap-3">
                <div className="bus-avatar">
                  <Bus size={28} className="text-indigo-600" />
                </div>
                <div>
                  <h3 className="bus-headline">{selectedBus.id}</h3>
                  <span className="bus-subtag">{selectedBus.type ? selectedBus.type.replace('_', ' ') : 'Standard Bus'}</span>
                </div>
              </div>
              <span className={`status-pill status-${selectedBus.status?.toLowerCase()}`}>
                {selectedBus.status}
              </span>
            </div>

            <div className="driver-stats-grid">
              <div className="driver-stat-box">
                <span className="stat-label">Current Location</span>
                <span className="stat-val text-indigo-600 flex items-center gap-1">
                  <MapPin size={16} />
                  {selectedBus.currentStopId || 'Transit Depot'}
                </span>
              </div>
              <div className="driver-stat-box">
                <span className="stat-label">Passenger Load</span>
                <span className="stat-val flex items-center gap-1">
                  <Users size={16} />
                  {selectedBus.passengerOccupancy} / {selectedBus.capacity}
                </span>
              </div>
              <div className="driver-stat-box">
                <span className="stat-label">Narrow Road Capable</span>
                <span className="stat-val text-gray-700">
                  {selectedBus.canNavigateNarrowRoads ? 'Yes (All Roads)' : 'No (Standard Arterials)'}
                </span>
              </div>
            </div>

            {/* Driver Actions */}
            <div className="driver-actions-panel">
              <h4 className="actions-panel-title">Permitted Driver Controls</h4>
              <div className="driver-btn-group">
                {selectedBus.status === 'IN_TRANSIT' && busTrip && (
                  <button
                    onClick={() => onCompleteTrip(busTrip.tripId)}
                    className="driver-btn btn-complete-trip"
                  >
                    <CheckCircle2 size={16} />
                    <span>Complete Assigned Trip ({busTrip.tripId})</span>
                  </button>
                )}

                {selectedBus.status !== 'MAINTENANCE' && (
                  <button
                    onClick={() => onUpdateStatus(selectedBus.id, 'MAINTENANCE')}
                    className="driver-btn btn-report-issue"
                  >
                    <Wrench size={16} />
                    <span>Report Vehicle Issue (Maintenance)</span>
                  </button>
                )}

                {selectedBus.status === 'MAINTENANCE' && (
                  <button
                    onClick={() => onRestoreBus(selectedBus.id)}
                    className="driver-btn btn-restore-vehicle"
                  >
                    <RotateCcw size={16} />
                    <span>Issue Resolved (Restore to Available)</span>
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* Active Trip Details or Idle Info */}
          <div className="driver-card driver-trip-card">
            <h3 className="card-heading flex items-center gap-2">
              <Compass size={18} className="text-indigo-600" />
              <span>Assigned Trip Manifest</span>
            </h3>

            {busTrip ? (
              <div className="trip-manifest-body">
                <div className="manifest-item">
                  <span className="manifest-label">Trip Identifier:</span>
                  <span className="font-mono font-bold text-indigo-700">{busTrip.tripId}</span>
                </div>
                <div className="manifest-item">
                  <span className="manifest-label">Route Origin:</span>
                  <span className="font-semibold">{busTrip.originStopName || busTrip.originStopId}</span>
                </div>
                <div className="manifest-item">
                  <span className="manifest-label">Route Destination:</span>
                  <span className="font-semibold text-emerald-700">{busTrip.destinationStopName || busTrip.destinationStopId}</span>
                </div>
                <div className="manifest-item">
                  <span className="manifest-label">Passengers Onboard:</span>
                  <span className="font-bold">{busTrip.boardedPassengerCount} Passengers</span>
                </div>
                <div className="manifest-item">
                  <span className="manifest-label">Estimated Travel Time:</span>
                  <span className="font-semibold">{busTrip.estimatedTravelTimeMinutes} minutes</span>
                </div>

                <div className="route-stops-manifest">
                  <span className="text-xs font-semibold text-gray-500 block mb-2">Sequential Route Waypoints:</span>
                  <div className="waypoints-flow">
                    {busTrip.routeStops?.map((stop, i) => (
                      <span key={i} className="waypoint-badge">
                        {stop.name || stop.id}
                        {i < busTrip.routeStops.length - 1 && ' &rarr; '}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              <div className="no-active-trip-box">
                <p className="text-gray-500 text-sm">
                  Vehicle <strong>{selectedBus.id}</strong> currently has no dispatched in-progress relief trip.
                </p>
                <p className="text-xs text-gray-400 mt-2">
                  Once the Transport Manager executes an allocation pass, dispatched route manifests will appear here.
                </p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Driver Traffic Advisories */}
      <div className="driver-advisories-card">
        <h4 className="card-heading flex items-center gap-2 text-sm text-gray-700">
          <AlertTriangle size={16} className="text-amber-500" />
          <span>Active Route Advisories for Drivers ({closedRoads.length} Closures)</span>
        </h4>
        {closedRoads.length === 0 ? (
          <p className="text-xs text-gray-500">All corridors open. Drive safely.</p>
        ) : (
          <div className="advisories-list">
            {closedRoads.map((r, i) => (
              <span key={i} className="advisory-pill">
                Closed: {r.fromStopId} &harr; {r.toStopId}
              </span>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
