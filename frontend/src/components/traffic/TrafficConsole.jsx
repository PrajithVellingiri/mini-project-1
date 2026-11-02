import { useState } from 'react';

export default function TrafficConsole({ roads = [], onUpdateTravelTime, onUpdateRoadStatus, onResetRoads }) {
  const [selectedRoadKey, setSelectedRoadKey] = useState(roads.length ? `${roads[0].fromStopId}->${roads[0].toStopId}` : '');
  const [targetTravelTime, setTargetTravelTime] = useState(8.0);
  const [isUpdating, setIsUpdating] = useState(false);
  const [trafficMessage, setTrafficMessage] = useState(null);

  const currentRoad = roads.find((r) => `${r.fromStopId}->${r.toStopId}` === selectedRoadKey) || roads[0];
  const isRoadOpen = currentRoad ? (currentRoad.open !== false) : true;

  const handleSelectRoad = (key) => {
    setSelectedRoadKey(key);
    const r = roads.find((road) => `${road.fromStopId}->${road.toStopId}` === key);
    if (r) {
      setTargetTravelTime(r.travelTimeMinutes);
    }
  };

  const handleApplyCongestion = async (multiplier) => {
    if (!currentRoad) return;
    const newTime = Math.max(1, Math.round(currentRoad.travelTimeMinutes * multiplier * 10) / 10);
    setTargetTravelTime(newTime);
    await executeTravelTimeUpdate(currentRoad.fromStopId, currentRoad.toStopId, newTime);
  };

  const handleToggleRoadAvailability = async () => {
    if (!currentRoad || !onUpdateRoadStatus) return;
    setIsUpdating(true);
    setTrafficMessage(null);

    const newOpenState = !isRoadOpen;
    const result = await onUpdateRoadStatus(currentRoad.fromStopId, currentRoad.toStopId, newOpenState, true);

    if (result.success) {
      setTrafficMessage({
        type: newOpenState ? 'success' : 'warning',
        text: newOpenState
          ? `Road ${currentRoad.fromStopId} ↔ ${currentRoad.toStopId} reopened. Available for routing.`
          : `EMERGENCY CLOSURE: Road ${currentRoad.fromStopId} ↔ ${currentRoad.toStopId} closed. Excluded from Dijkstra routes.`,
      });
    } else {
      setTrafficMessage({
        type: 'error',
        text: result.error || 'Failed to update road closure state.',
      });
    }
    setIsUpdating(false);
  };

  const handleCustomSubmit = async (e) => {
    e.preventDefault();
    if (!currentRoad) return;
    await executeTravelTimeUpdate(currentRoad.fromStopId, currentRoad.toStopId, targetTravelTime);
  };

  const executeTravelTimeUpdate = async (from, to, timeMinutes) => {
    setIsUpdating(true);
    setTrafficMessage(null);
    const result = await onUpdateTravelTime(from, to, timeMinutes);
    if (result.success) {
      setTrafficMessage({
        type: 'success',
        text: `Travel time updated: ${from} → ${to} is now ${timeMinutes} min.`,
      });
    } else {
      setTrafficMessage({
        type: 'error',
        text: result.error || 'Failed to update road travel time.',
      });
    }
    setIsUpdating(false);
  };

  return (
    <div className="traffic-console-panel">
      <div className="panel-header">
        <div>
          <h3 className="panel-title">Dynamic Traffic & Road Incident Simulation</h3>
          <p className="panel-subtitle">
            Inject traffic spikes or trigger explicit road closures to evaluate dynamic Dijkstra re-routing.
          </p>
        </div>
      </div>

      {trafficMessage && (
        <div className={`console-alert alert-${trafficMessage.type}`}>
          {trafficMessage.text}
        </div>
      )}

      {!isRoadOpen && (
        <div className="console-alert alert-error" style={{ marginBottom: '1.25rem' }}>
          <strong>ROAD CLOSED TO ALL TRAFFIC:</strong> Road segment {currentRoad?.fromStopId} ↔ {currentRoad?.toStopId} is currently blocked. The Dijkstra route optimization engine strictly excludes this corridor and automatically routes around it.
        </div>
      )}

      <div className="traffic-grid">
        {/* Left: Road Selector & Quick Presets */}
        <div className="traffic-card">
          <label className="input-label">Select Road Segment</label>
          <select
            className="custom-select"
            value={selectedRoadKey}
            onChange={(e) => handleSelectRoad(e.target.value)}
          >
            {roads.map((road) => (
              <option key={`${road.fromStopId}->${road.toStopId}`} value={`${road.fromStopId}->${road.toStopId}`}>
                {road.fromStopId} → {road.toStopId} ({road.open === false ? 'CLOSED' : `${road.travelTimeMinutes}m`}) {road.narrowRoad ? '[Narrow]' : ''}
              </option>
            ))}
          </select>

          {currentRoad && (
            <div className="road-metrics-summary">
              <div className="metric-box">
                <span className="metric-label">Operational Status</span>
                <span className={`metric-val ${isRoadOpen ? 'text-available' : 'text-danger'}`}>
                  {isRoadOpen ? 'OPEN (Active)' : 'CLOSED (Blocked)'}
                </span>
              </div>
              <div className="metric-box">
                <span className="metric-label">Travel Time</span>
                <span className={`metric-val ${!isRoadOpen ? 'text-danger' : currentRoad.travelTimeMinutes > 15 ? 'text-warning' : 'text-primary'}`}>
                  {isRoadOpen ? `${currentRoad.travelTimeMinutes} min` : 'Blocked'}
                </span>
              </div>
              <div className="metric-box">
                <span className="metric-label">Road Type</span>
                <span className="metric-val">
                  {currentRoad.narrowRoad ? 'Narrow Corridor' : 'Standard Arterial'}
                </span>
              </div>
            </div>
          )}

          {/* Explicit Road Closure Control */}
          <div style={{ marginTop: '1.25rem', paddingTop: '1.25rem', borderTop: '1px solid #30363d' }}>
            <span className="section-subtitle">Road Incident / Availability Control</span>
            <p style={{ fontSize: '0.82rem', color: '#8b949e', marginBottom: '0.75rem' }}>
              Explicitly open or close this bidirectional corridor. Closed roads are completely removed from Dijkstra graph traversals.
            </p>
            <button
              className={`preset-btn ${isRoadOpen ? 'btn-closure' : 'btn-reopen'}`}
              style={{ width: '100%', padding: '0.65rem 1rem', fontSize: '0.9rem', fontWeight: 600 }}
              disabled={isUpdating}
              onClick={handleToggleRoadAvailability}
            >
              {isRoadOpen ? 'Emergency Close Road (Block All Transit)' : 'Reopen Road (Restore Transit Flow)'}
            </button>
          </div>

          <div className="congestion-presets-section">
            <span className="section-subtitle">Traffic Multiplier Presets</span>
            <div className="preset-buttons-row">
              <button
                className="preset-btn"
                disabled={isUpdating || !isRoadOpen}
                onClick={() => handleApplyCongestion(1.0)}
                title="Reset to 1x normal speed"
              >
                Normal (1x)
              </button>
              <button
                className="preset-btn preset-moderate"
                disabled={isUpdating || !isRoadOpen}
                onClick={() => handleApplyCongestion(1.25)}
              >
                +25% Slow
              </button>
              <button
                className="preset-btn preset-moderate"
                disabled={isUpdating || !isRoadOpen}
                onClick={() => handleApplyCongestion(1.5)}
              >
                +50% Delay
              </button>
              <button
                className="preset-btn preset-heavy"
                disabled={isUpdating || !isRoadOpen}
                onClick={() => handleApplyCongestion(2.0)}
              >
                +100% Rush
              </button>
            </div>
          </div>
        </div>

        {/* Right: Custom Slider Control */}
        <div className="traffic-card">
          <span className="section-subtitle">Fine-Grained Travel Time Slider</span>
          <form onSubmit={handleCustomSubmit} className="slider-form">
            <div className="slider-container">
              <div className="slider-header">
                <span>Travel Time</span>
                <span className="slider-value-display">{targetTravelTime} min</span>
              </div>
              <input
                type="range"
                min="1"
                max="45"
                step="0.5"
                value={targetTravelTime > 45 ? 45 : targetTravelTime}
                onChange={(e) => setTargetTravelTime(parseFloat(e.target.value))}
                className="custom-range-slider"
                disabled={isUpdating || !isRoadOpen}
              />
              <div className="slider-ticks">
                <span>1 min (Free Flow)</span>
                <span>20 min (Moderate)</span>
                <span>45 min (Congested)</span>
              </div>
            </div>

            <button type="submit" className="primary-action-btn" disabled={isUpdating || !isRoadOpen}>
              {isUpdating ? 'Updating Road Edge...' : 'Apply Travel Time Change'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
