import { useState } from 'react';
import {
  Navigation,
  Bus,
  AlertTriangle,
  MapPin,
  Clock,
  ArrowRight,
  ShieldAlert,
} from 'lucide-react';

export default function PassengerHub({ simState, onNavigateToRoute }) {
  const [selectedPickup, setSelectedPickup] = useState('S1');
  const [selectedDest, setSelectedDest] = useState('S9');

  const stops = simState?.stops || [];
  const buses = simState?.buses || [];
  const roads = simState?.roads || [];

  // Filter service alerts (closed roads or roads with high delay)
  const closedRoads = roads.filter((r) => !r.open);
  const congestedRoads = roads.filter((r) => r.open && r.travelTimeMinutes > 15);

  return (
    <div className="passenger-hub-container">
      {/* Welcome Banner */}
      <div className="passenger-banner">
        <div className="banner-content">
          <div className="banner-badge">
            <Navigation size={14} />
            <span>Passenger Transit Portal</span>
          </div>
          <h2 className="banner-title">Vetrinagar Smart Public Transit</h2>
          <p className="banner-desc">
            Check real-time bus schedules, find optimal routes between city hubs, and view live transit service alerts.
          </p>
        </div>
      </div>

      {/* Quick Route Search Card */}
      <div className="passenger-search-card">
        <h3 className="card-heading">
          <Navigation size={18} className="text-indigo-600" />
          <span>Quick Route Planner</span>
        </h3>
        <p className="card-subtext">Find the fastest transit connection across Vetrinagar</p>

        <div className="passenger-route-inputs">
          <div className="route-select-field">
            <label className="field-label">
              <MapPin size={14} className="text-green-600" /> Origin Stop
            </label>
            <select
              value={selectedPickup}
              onChange={(e) => setSelectedPickup(e.target.value)}
              className="route-select"
            >
              {stops.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.id}: {s.name}
                </option>
              ))}
            </select>
          </div>

          <div className="route-arrow-icon">
            <ArrowRight size={20} className="text-gray-400" />
          </div>

          <div className="route-select-field">
            <label className="field-label">
              <MapPin size={14} className="text-red-600" /> Destination
            </label>
            <select
              value={selectedDest}
              onChange={(e) => setSelectedDest(e.target.value)}
              className="route-select"
            >
              {stops.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.id}: {s.name}
                </option>
              ))}
            </select>
          </div>

          <button
            onClick={() => onNavigateToRoute(selectedPickup, selectedDest)}
            className="find-route-btn"
          >
            <span>Calculate Route</span>
            <ArrowRight size={16} />
          </button>
        </div>
      </div>

      {/* Grid: Service Alerts + Live Buses */}
      <div className="passenger-grid-two">
        {/* Service Alerts Card */}
        <div className="passenger-subcard">
          <div className="subcard-header">
            <div className="flex items-center gap-2">
              <ShieldAlert size={18} className="text-amber-500" />
              <h3 className="subcard-title">Live Service Alerts & Disruptions</h3>
            </div>
            <span className="alert-count-pill">{closedRoads.length + congestedRoads.length} Active</span>
          </div>

          {closedRoads.length === 0 && congestedRoads.length === 0 ? (
            <div className="text-sm text-green-700 bg-green-50 p-4 rounded-lg flex items-center gap-2">
              <span>All road corridors are fully operational with normal travel times.</span>
            </div>
          ) : (
            <div className="alerts-list">
              {closedRoads.map((r, idx) => (
                <div key={idx} className="alert-item alert-closed">
                  <AlertTriangle size={16} className="text-red-600 shrink-0" />
                  <div>
                    <strong className="text-red-900">Road Closure:</strong> Corridor {r.fromStopId} &harr; {r.toStopId} is temporarily closed. Buses are diverted via alternate paths.
                  </div>
                </div>
              ))}
              {congestedRoads.map((r, idx) => (
                <div key={idx} className="alert-item alert-delayed">
                  <Clock size={16} className="text-amber-600 shrink-0" />
                  <div>
                    <strong className="text-amber-900">Traffic Congestion:</strong> Road {r.fromStopId} &rarr; {r.toStopId} has elevated travel time ({r.travelTimeMinutes} mins).
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Available Fleet Buses Card */}
        <div className="passenger-subcard">
          <div className="subcard-header">
            <div className="flex items-center gap-2">
              <Bus size={18} className="text-indigo-600" />
              <h3 className="subcard-title">Fleet Bus Availability</h3>
            </div>
            <span className="bus-count-pill">{buses.length} Fleet Vehicles</span>
          </div>

          <div className="passenger-bus-list">
            {buses.map((b) => (
              <div key={b.id} className="passenger-bus-item">
                <div className="bus-info-left">
                  <span className="bus-id-tag">{b.id}</span>
                  <div>
                    <span className="bus-type-text">{b.type ? b.type.replace('_', ' ') : 'Standard Bus'}</span>
                    <span className="bus-location-text">
                      <MapPin size={12} className="inline mr-1" />
                      At {b.currentStopId || 'Transit Depot'}
                    </span>
                  </div>
                </div>

                <div className="bus-info-right">
                  <span className={`bus-status-badge status-${b.status?.toLowerCase()}`}>
                    {b.status}
                  </span>
                  <span className="bus-seats-text">
                    Cap: {b.capacity} | Pax: {b.passengerOccupancy}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
