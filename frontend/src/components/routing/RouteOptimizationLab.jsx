import { useState, useEffect } from 'react';
import { fetchRoute } from '../../api/client';

export default function RouteOptimizationLab({ stops = [], onHighlightRoute }) {
  const [source, setSource] = useState('DEPOT');
  const [destination, setDestination] = useState('S4');
  const [vehicleType, setVehicleType] = useState('GENERAL');
  const [loading, setLoading] = useState(false);
  const [primaryRoute, setPrimaryRoute] = useState(null);
  const [alternateRoute, setAlternateRoute] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);

  const calculateOptimalRoutes = async (src, dst, veh) => {
    if (!src || !dst || src === dst) {
      setErrorMessage(src === dst ? 'Source and destination cannot be identical.' : 'Please select both source and destination.');
      return;
    }

    setLoading(true);
    setErrorMessage(null);

    // 1. Fetch Primary Route using specified vehicle type
    const resPrimary = await fetchRoute(src, dst, veh);
    if (!resPrimary.success) {
      setErrorMessage(resPrimary.error);
      setPrimaryRoute(null);
      setAlternateRoute(null);
      setLoading(false);
      return;
    }

    setPrimaryRoute(resPrimary.data);
    if (onHighlightRoute && resPrimary.data.reachable && resPrimary.data.stopIds) {
      onHighlightRoute(resPrimary.data.stopIds);
    }

    // 2. Fetch Alternate Route with contrasting vehicle restriction
    // If user asked for STANDARD, compare with MINIBUS (which can take narrow shortcuts)
    // If user asked for MINIBUS, compare with STANDARD (which must detour around narrow roads)
    // If user asked for GENERAL, compare with STANDARD to check if arterial constraints impact route
    const altVehicle = veh === 'STANDARD' ? 'MINIBUS' : 'STANDARD';
    const resAlt = await fetchRoute(src, dst, altVehicle);

    if (resAlt.success) {
      const altData = resAlt.data;
      const primData = resPrimary.data;

      // Case A: Alternate vehicle type has NO valid route
      if (!altData.reachable) {
        setAlternateRoute({
          reachable: false,
          identical: false,
          vehicleTypeUsed: altVehicle,
          comparisonLabel: altVehicle === 'MINIBUS' ? 'MiniBus Alternative' : 'StandardBus Alternative',
          unreachableReason: `${altVehicle === 'MINIBUS' ? 'MiniBus' : 'StandardBus'} cannot reach ${dst} from ${src} due to road accessibility restrictions or closed corridors.`,
        });
      }
      // Case B: Both are reachable, but routes are identical in stop sequence
      else if (primData.reachable && JSON.stringify(altData.stopIds) === JSON.stringify(primData.stopIds)) {
        setAlternateRoute({
          reachable: true,
          identical: true,
          vehicleTypeUsed: altVehicle,
          travelTimeMinutes: altData.travelTimeMinutes,
          distanceKm: altData.distanceKm,
        });
      }
      // Case C: Distinct alternative route found
      else {
        setAlternateRoute({
          ...altData,
          reachable: true,
          identical: false,
          comparisonLabel: altVehicle === 'MINIBUS' ? 'MiniBus Shortcut' : 'StandardBus Arterial Detour',
        });
      }
    } else {
      setAlternateRoute(null);
    }

    setLoading(false);
  };

  const handleSwapStops = () => {
    const temp = source;
    setSource(destination);
    setDestination(temp);
    calculateOptimalRoutes(destination, temp, vehicleType);
  };

  useEffect(() => {
    calculateOptimalRoutes('DEPOT', 'S4', 'GENERAL');
  }, []);

  return (
    <div className="route-lab-panel">
      <div className="panel-header">
        <div>
          <h3 className="panel-title">Dijkstra Route Optimization & Alternate Analysis</h3>
          <p className="panel-subtitle">
            Computes deterministic minimum-travel-time paths with vehicle accessibility constraints and road closures.
          </p>
        </div>
      </div>

      {/* Control Form */}
      <div className="route-controls-card">
        <div className="route-input-group">
          <label className="input-label">Origin Station</label>
          <select
            className="custom-select"
            value={source}
            onChange={(e) => setSource(e.target.value)}
          >
            {stops.map((s) => (
              <option key={s.id} value={s.id}>
                {s.id} — {s.name}
              </option>
            ))}
          </select>
        </div>

        <button className="swap-stops-btn" onClick={handleSwapStops} title="Swap Origin & Destination">
          ⇄
        </button>

        <div className="route-input-group">
          <label className="input-label">Destination Station</label>
          <select
            className="custom-select"
            value={destination}
            onChange={(e) => setDestination(e.target.value)}
          >
            {stops.map((s) => (
              <option key={s.id} value={s.id}>
                {s.id} — {s.name}
              </option>
            ))}
          </select>
        </div>

        <div className="route-input-group">
          <label className="input-label">Vehicle Restriction</label>
          <select
            className="custom-select"
            value={vehicleType}
            onChange={(e) => setVehicleType(e.target.value)}
          >
            <option value="GENERAL">All Vehicles (Unrestricted)</option>
            <option value="STANDARD">StandardBus (Restricted from Narrow Roads)</option>
            <option value="MINIBUS">MiniBus (Access to All Roads)</option>
          </select>
        </div>

        <button
          className="calculate-route-btn"
          onClick={() => calculateOptimalRoutes(source, destination, vehicleType)}
          disabled={loading}
        >
          {loading ? 'Evaluating...' : 'Find Fastest Route'}
        </button>
      </div>

      {errorMessage && (
        <div className="console-alert alert-error">{errorMessage}</div>
      )}

      {/* Results Comparison Grid */}
      {primaryRoute && (
        <div className="route-results-grid">
          {/* Primary Route Card */}
          <div className={`route-result-card primary-route-card ${!primaryRoute.reachable ? 'card-unreachable' : ''}`}>
            <div className="card-top-pill">
              <span className={`pill-badge ${primaryRoute.reachable ? 'pill-recommended' : 'pill-danger'}`}>
                {primaryRoute.reachable ? 'Optimal Dijkstra Route' : 'Destination Unreachable'}
              </span>
              {primaryRoute.reachable && (
                <div className="route-header-metrics">
                  {primaryRoute.executionTimeMicros > 0 && (
                    <span className="telemetry-pill" title="Dijkstra monotonic execution latency">
                      ⚡ {primaryRoute.executionTimeMicros.toFixed(1)} µs
                    </span>
                  )}
                  <span className="route-time-val">
                    {primaryRoute.travelTimeMinutes} min
                    {primaryRoute.distanceKm ? ` (${primaryRoute.distanceKm} km)` : ''}
                  </span>
                </div>
              )}
            </div>

            {primaryRoute.reachable ? (
              <>
                <div className="route-stops-summary">
                  <span className="route-stops-count">{primaryRoute.stops?.length || 0} stops traversed</span>
                  <div className="route-chips-chain">
                    {primaryRoute.stops?.map((stop, idx) => (
                      <span key={stop.id} className="chain-node">
                        <span className="node-badge">{stop.id}</span>
                        {idx < primaryRoute.stops.length - 1 && <span className="chain-arrow">→</span>}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="route-details-list">
                  <div className="detail-row">
                    <span className="detail-label">Status</span>
                    <span className="detail-val text-available">Reachable (Shortest Path)</span>
                  </div>
                  <div className="detail-row">
                    <span className="detail-label">Estimated Distance</span>
                    <span className="detail-val">{primaryRoute.distanceKm || 0} km</span>
                  </div>
                  <div className="detail-row">
                    <span className="detail-label">Vehicle Mode</span>
                    <span className="detail-val">{primaryRoute.vehicleTypeUsed || vehicleType}</span>
                  </div>
                </div>

                <button
                  className="highlight-map-action-btn"
                  onClick={() => onHighlightRoute && onHighlightRoute(primaryRoute.stopIds)}
                >
                  Highlight Route on Network Map
                </button>
              </>
            ) : (
              <div style={{ padding: '1.25rem 0' }}>
                <p style={{ color: '#f87171', fontSize: '0.9rem', marginBottom: '0.5rem' }}>
                  <strong>No Path Available:</strong> All routes from {source} to {destination} are currently blocked by road closures or vehicle access restrictions.
                </p>
                <span style={{ fontSize: '0.8rem', color: '#8b949e' }}>
                  Check the Traffic & Incidents console to ensure connecting corridors are open.
                </span>
              </div>
            )}
          </div>

          {/* Alternative Route Card */}
          {alternateRoute ? (
            alternateRoute.identical ? (
              <div className="route-result-card placeholder-card">
                <span className="placeholder-title">Alternative Path Analysis</span>
                <p className="placeholder-desc">
                  No distinct alternative route needed. The shortest path is unique and identical for both StandardBus and MiniBus ({alternateRoute.travelTimeMinutes} min, {alternateRoute.distanceKm || 0} km).
                </p>
              </div>
            ) : !alternateRoute.reachable ? (
              <div className="route-result-card placeholder-card" style={{ borderColor: 'rgba(239, 68, 68, 0.4)' }}>
                <span className="placeholder-title" style={{ color: '#f87171' }}>
                  Alternative Route: No Valid Path
                </span>
                <p className="placeholder-desc" style={{ color: '#fca5a5' }}>
                  {alternateRoute.unreachableReason}
                </p>
              </div>
            ) : (
              <div className="route-result-card alternate-route-card">
                <div className="card-top-pill">
                  <span className="pill-badge pill-alternate">{alternateRoute.comparisonLabel || 'Alternative Route'}</span>
                  <span className="route-time-val">
                    {alternateRoute.travelTimeMinutes} min
                    {alternateRoute.distanceKm ? ` (${alternateRoute.distanceKm} km)` : ''}
                  </span>
                </div>

                <div className="route-stops-summary">
                  <span className="route-stops-count">{alternateRoute.stops?.length || 0} stops traversed</span>
                  <div className="route-chips-chain">
                    {alternateRoute.stops?.map((stop, idx) => (
                      <span key={stop.id} className="chain-node">
                        <span className="node-badge node-alt">{stop.id}</span>
                        {idx < alternateRoute.stops.length - 1 && <span className="chain-arrow">→</span>}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="route-details-list">
                  <div className="detail-row">
                    <span className="detail-label">Vehicle Evaluated</span>
                    <span className="detail-val">{alternateRoute.vehicleTypeUsed}</span>
                  </div>
                  <div className="detail-row">
                    <span className="detail-label">Time Differential</span>
                    <span className={`detail-val ${alternateRoute.travelTimeMinutes > primaryRoute.travelTimeMinutes ? 'text-warning' : 'text-available'}`}>
                      {alternateRoute.travelTimeMinutes > primaryRoute.travelTimeMinutes
                        ? `+${(alternateRoute.travelTimeMinutes - primaryRoute.travelTimeMinutes).toFixed(1)} min slower`
                        : `${(alternateRoute.travelTimeMinutes - primaryRoute.travelTimeMinutes).toFixed(1)} min faster`}
                    </span>
                  </div>
                  {alternateRoute.distanceKm && primaryRoute.distanceKm && (
                    <div className="detail-row">
                      <span className="detail-label">Distance Differential</span>
                      <span className="detail-val">
                        {(alternateRoute.distanceKm - primaryRoute.distanceKm) >= 0
                          ? `+${(alternateRoute.distanceKm - primaryRoute.distanceKm).toFixed(1)} km longer`
                          : `${(alternateRoute.distanceKm - primaryRoute.distanceKm).toFixed(1)} km shorter`}
                      </span>
                    </div>
                  )}
                </div>

                <button
                  className="highlight-map-action-btn"
                  onClick={() => onHighlightRoute && onHighlightRoute(alternateRoute.stopIds)}
                >
                  Highlight Alternative on Map
                </button>
              </div>
            )
          ) : (
            <div className="route-result-card placeholder-card">
              <span className="placeholder-title">Alternative Path Analysis</span>
              <p className="placeholder-desc">
                Select an origin and destination to compare vehicle routing restrictions.
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
