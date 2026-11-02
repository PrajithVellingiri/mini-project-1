import { useState } from 'react';
import Badge from '../common/Badge';

export default function FleetDemandManager({
  buses = [],
  stops = [],
  demands = [],
  trips = [],
  onUpdateBusStatus,
  onCreateDemand,
  onCompleteTrip,
  onCompleteAllTrips,
  onTriggerBreakdown,
  onRestoreBus,
}) {
  const [newPickup, setNewPickup] = useState('S1');
  const [newDest, setNewDest] = useState('S4');
  const [newPaxCount, setNewPaxCount] = useState(20);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedbackMsg, setFeedbackMsg] = useState(null);
  const [actionLoadingBusId, setActionLoadingBusId] = useState(null);

  const handleCreateDemandSubmit = async (e) => {
    e.preventDefault();
    if (newPickup === newDest) {
      setFeedbackMsg({ type: 'error', text: 'Pickup and destination stops must be distinct.' });
      return;
    }
    if (newPaxCount <= 0) {
      setFeedbackMsg({ type: 'error', text: 'Passenger count must be at least 1.' });
      return;
    }

    setIsSubmitting(true);
    setFeedbackMsg(null);
    const result = await onCreateDemand(newPickup, newDest, newPaxCount);
    if (result.success) {
      setFeedbackMsg({
        type: 'success',
        text: `Demand batch created: ${result.data.requestId || 'Success'} (${newPaxCount} pax on ${newPickup} -> ${newDest})`,
      });
      setNewPaxCount(20);
    } else {
      setFeedbackMsg({ type: 'error', text: result.error || 'Failed to create demand batch.' });
    }
    setIsSubmitting(false);
  };

  const handleBusStatusChange = async (busId, newStatus) => {
    const res = await onUpdateBusStatus(busId, newStatus);
    if (!res.success) {
      setFeedbackMsg({ type: 'error', text: res.error || `Failed to update status for ${busId}` });
    }
  };

  const handleTriggerBreakdown = async (busId) => {
    setActionLoadingBusId(busId);
    setFeedbackMsg(null);
    if (onTriggerBreakdown) {
      const res = await onTriggerBreakdown(busId);
      if (res.success && res.data) {
        const d = res.data;
        if (d.activeTripCancelled) {
          setFeedbackMsg({
            type: 'warning',
            text: `🚨 Breakdown Incident: ${busId} placed in MAINTENANCE. Trip ${d.cancelledTripId} cancelled. ${d.strandedPassengers} stranded passengers de-boarded at ${d.strandedStopName} (${d.strandedStopId}) and re-queued as emergency relief demand ${d.reliefDemandId} (Priority: 0).`,
          });
        } else {
          setFeedbackMsg({
            type: 'warning',
            text: `⚠️ Vehicle ${busId} moved to MAINTENANCE. No active passengers stranded.`,
          });
        }
      } else {
        setFeedbackMsg({ type: 'error', text: res.error || `Failed to trigger breakdown for ${busId}` });
      }
    }
    setActionLoadingBusId(null);
  };

  const handleRestoreBus = async (busId) => {
    setActionLoadingBusId(busId);
    setFeedbackMsg(null);
    if (onRestoreBus) {
      const res = await onRestoreBus(busId);
      if (res.success) {
        setFeedbackMsg({
          type: 'success',
          text: `✅ Vehicle ${busId} restored from maintenance back to AVAILABLE operational status.`,
        });
      } else {
        setFeedbackMsg({ type: 'error', text: res.error || `Failed to restore ${busId}` });
      }
    }
    setActionLoadingBusId(null);
  };

  return (
    <div className="fleet-demand-panel">
      {feedbackMsg && (
        <div className={`console-alert alert-${feedbackMsg.type}`}>
          {feedbackMsg.text}
        </div>
      )}

      {/* Grid: Fleet Table + Demand Creation Form */}
      <div className="fleet-demand-grid">
        {/* Fleet Table */}
        <div className="manager-card fleet-table-card">
          <div className="card-header-row">
            <div>
              <h3 className="card-heading">Transit Vehicle Fleet</h3>
              <p className="card-subheading">Manage operational readiness and vehicle health status.</p>
            </div>
          </div>

          <div className="table-responsive">
            <table className="custom-data-table">
              <thead>
                <tr>
                  <th>Bus ID</th>
                  <th>Vehicle Type</th>
                  <th>Location</th>
                  <th>Occupancy / Seats</th>
                  <th>Status Control</th>
                </tr>
              </thead>
              <tbody>
                {buses.map((bus) => {
                  const isMini = bus.type === 'MINIBUS' || bus.type.includes('Mini');
                  const occupancyRatio = bus.capacity ? Math.round((bus.passengerOccupancy / bus.capacity) * 100) : 0;

                  return (
                    <tr key={bus.id}>
                      <td className="bus-id-cell">
                        <span className="bus-icon">{isMini ? '🚐' : '🚌'}</span>
                        <span className="bus-label">{bus.id}</span>
                      </td>
                      <td>
                        <Badge variant={isMini ? 'narrow' : 'default'}>
                          {isMini ? 'MiniBus (25)' : 'Standard (50)'}
                        </Badge>
                      </td>
                      <td className="font-mono">{bus.currentStopId || 'DEPOT'}</td>
                      <td>
                        <div className="capacity-bar-container">
                          <div className="capacity-text">
                            <span>{bus.passengerOccupancy} / {bus.capacity}</span>
                            <span className="percentage-muted">{occupancyRatio}%</span>
                          </div>
                          <div className="progress-track">
                            <div
                              className={`progress-fill ${occupancyRatio > 80 ? 'fill-danger' : occupancyRatio > 0 ? 'fill-orange' : ''}`}
                              style={{ width: `${occupancyRatio}%` }}
                            />
                          </div>
                        </div>
                      </td>
                      <td>
                        <div className="bus-action-cell">
                          <select
                            className="status-selector"
                            value={bus.status}
                            disabled={bus.status === 'IN_TRANSIT'}
                            onChange={(e) => handleBusStatusChange(bus.id, e.target.value)}
                          >
                            <option value="AVAILABLE">AVAILABLE</option>
                            <option value="MAINTENANCE">MAINTENANCE</option>
                            <option value="OUT_OF_SERVICE">OUT OF SERVICE</option>
                            {bus.status === 'IN_TRANSIT' && <option value="IN_TRANSIT">IN TRANSIT</option>}
                          </select>
                          {bus.status === 'MAINTENANCE' ? (
                            <button
                              type="button"
                              className="btn-bus-action btn-restore"
                              onClick={() => handleRestoreBus(bus.id)}
                              disabled={actionLoadingBusId === bus.id}
                              title="Restore vehicle to available fleet"
                            >
                              {actionLoadingBusId === bus.id ? '...' : '🔧 Restore'}
                            </button>
                          ) : (
                            <button
                              type="button"
                              className="btn-bus-action btn-breakdown"
                              onClick={() => handleTriggerBreakdown(bus.id)}
                              disabled={actionLoadingBusId === bus.id}
                              title="Simulate roadside breakdown and passenger de-boarding"
                            >
                              {actionLoadingBusId === bus.id ? '...' : '💥 Breakdown'}
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>

        {/* Dynamic Demand Creation Form */}
        <div className="manager-card demand-create-card">
          <div className="card-header-row">
            <div>
              <h3 className="card-heading">Inject Passenger Demand</h3>
              <p className="card-subheading">Generate real-time passenger crowd batches.</p>
            </div>
          </div>

          <form onSubmit={handleCreateDemandSubmit} className="demand-form">
            <div className="form-group">
              <label className="input-label">Pickup Station</label>
              <select
                className="custom-select"
                value={newPickup}
                onChange={(e) => setNewPickup(e.target.value)}
              >
                {stops.map((s) => (
                  <option key={s.id} value={s.id}>{s.id} — {s.name}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="input-label">Destination Station</label>
              <select
                className="custom-select"
                value={newDest}
                onChange={(e) => setNewDest(e.target.value)}
              >
                {stops.map((s) => (
                  <option key={s.id} value={s.id}>{s.id} — {s.name}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="input-label">Passenger Batch Count</label>
              <input
                type="number"
                min="1"
                max="100"
                className="custom-input"
                value={newPaxCount}
                onChange={(e) => setNewPaxCount(e.target.value)}
              />
            </div>

            <button type="submit" className="primary-action-btn" disabled={isSubmitting}>
              {isSubmitting ? 'Injecting Batch...' : '+ Inject Demand to Queue'}
            </button>
          </form>
        </div>
      </div>

      {/* Active Relief Trips Table */}
      <div className="manager-card trips-monitor-card">
        <div className="card-header-row">
          <div>
            <h3 className="card-heading">Active Relief Trips Monitor</h3>
            <p className="card-subheading">Track dispatched vehicles and trigger trip completion.</p>
          </div>
          {trips.some((t) => t.status === 'IN_PROGRESS') && (
            <button className="complete-all-btn" onClick={onCompleteAllTrips}>
              ✓ Complete All Active Trips
            </button>
          )}
        </div>

        {trips.length === 0 ? (
          <div className="empty-state-notice">
            <span>No relief trips currently dispatched. Run an allocation pass from the Operations Center.</span>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="custom-data-table">
              <thead>
                <tr>
                  <th>Trip ID</th>
                  <th>Assigned Bus</th>
                  <th>Origin → Destination</th>
                  <th>Boarded Pax</th>
                  <th>Est. Time</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {trips.map((trip) => {
                  const isInProgress = trip.status === 'IN_PROGRESS';
                  return (
                    <tr key={trip.tripId} className={isInProgress ? 'row-active' : ''}>
                      <td className="font-mono">{trip.tripId}</td>
                      <td>
                        <span className="font-mono">{trip.busId}</span>
                        <span className="table-sub-label">({trip.busType})</span>
                      </td>
                      <td>
                        <span className="font-mono">{trip.routeStopIds ? `${trip.routeStopIds[0]} → ${trip.routeStopIds[trip.routeStopIds.length - 1]}` : trip.tripId}</span>
                      </td>
                      <td className="font-semibold text-orange">{trip.passengerCount} pax</td>
                      <td className="font-mono">{trip.estimatedTravelTimeMinutes} min</td>
                      <td>
                        <Badge variant={isInProgress ? 'transit' : 'available'}>
                          {trip.status}
                        </Badge>
                      </td>
                      <td>
                        {isInProgress && (
                          <button
                            className="single-complete-btn"
                            onClick={() => onCompleteTrip(trip.tripId)}
                          >
                            Arrive & Complete
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
