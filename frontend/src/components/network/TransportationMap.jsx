import { useState } from 'react';

// Topological coordinates on an 860x600 normalized canvas
const STOP_COORDINATES = {
  DEPOT: { x: 430, y: 300, label: 'Central Depot', isDepot: true },
  S1: { x: 320, y: 200, label: 'Downtown Hub' },
  S2: { x: 430, y: 170, label: 'Central Station' },
  S3: { x: 210, y: 160, label: 'University' },
  S4: { x: 340, y: 70, label: 'Tech Park N' },
  S5: { x: 120, y: 280, label: 'City Hospital' },
  S6: { x: 160, y: 420, label: 'Market Square' },
  S7: { x: 260, y: 330, label: 'Old Town', isNarrowSector: true },
  S8: { x: 300, y: 530, label: 'South Harbor' },
  S9: { x: 550, y: 60, label: 'Airport Term' },
  S10: { x: 640, y: 210, label: 'West End Mall' },
  S11: { x: 600, y: 400, label: 'Industrial Zone' },
  S12: { x: 470, y: 520, label: 'Riverside Park' },
};

export default function TransportationMap({
  stops = [],
  roads = [],
  buses = [],
  trips = [],
  highlightPath = [],
  onSelectStop,
  onSelectRoad,
}) {
  const [selectedStopId, setSelectedStopId] = useState(null);
  const [hoveredRoadKey, setHoveredRoadKey] = useState(null);

  // Group buses by their current stop
  const busesByStop = buses.reduce((acc, bus) => {
    const stopId = bus.currentStopId || 'DEPOT';
    if (!acc[stopId]) acc[stopId] = [];
    acc[stopId].push(bus);
    return acc;
  }, {});

  const handleStopClick = (stopId) => {
    setSelectedStopId(stopId === selectedStopId ? null : stopId);
    if (onSelectStop) onSelectStop(stopId);
  };

  const isEdgeInHighlightedPath = (fromId, toId) => {
    if (!highlightPath || highlightPath.length < 2) return false;
    for (let i = 0; i < highlightPath.length - 1; i++) {
      if (highlightPath[i] === fromId && highlightPath[i + 1] === toId) {
        return true;
      }
    }
    return false;
  };

  // Group directed roads into pairs or unique lines with offsets
  const renderedRoads = roads.map((road) => {
    const from = STOP_COORDINATES[road.fromStopId] || { x: 0, y: 0 };
    const to = STOP_COORDINATES[road.toStopId] || { x: 0, y: 0 };
    const isHighlighted = isEdgeInHighlightedPath(road.fromStopId, road.toStopId);

    // Calculate slight perpendicular offset for bidirectional distinction
    const dx = to.x - from.x;
    const dy = to.y - from.y;
    const len = Math.sqrt(dx * dx + dy * dy) || 1;
    const offsetX = (-dy / len) * 4;
    const offsetY = (dx / len) * 4;

    const startX = from.x + offsetX;
    const startY = from.y + offsetY;
    const endX = to.x + offsetX;
    const endY = to.y + offsetY;

    const midX = (startX + endX) / 2;
    const midY = (startY + endY) / 2;

    const roadKey = `${road.fromStopId}->${road.toStopId}`;
    const isHovered = hoveredRoadKey === roadKey;

    return {
      ...road,
      roadKey,
      startX,
      startY,
      endX,
      endY,
      midX,
      midY,
      isHighlighted,
      isHovered,
    };
  });

  return (
    <div className="network-map-container">
      <div className="map-toolbar">
        <div className="map-legend">
          <span className="legend-item">
            <span className="legend-line line-primary" /> Standard Road
          </span>
          <span className="legend-item">
            <span className="legend-line line-narrow" /> Narrow Heritage Corridor (MiniBus Only)
          </span>
          <span className="legend-item">
            <span className="legend-line line-highlight" /> Active / Selected Route
          </span>
          <span className="legend-item">
            <span className="legend-node node-depot" /> Depot
          </span>
          <span className="legend-item">
            <span className="legend-node node-stop" /> Station Stop
          </span>
        </div>
      </div>

      <div className="svg-map-wrapper">
        <svg viewBox="0 0 760 590" className="transit-svg-canvas">
          <defs>
            {/* Arrow marker for standard directed roads */}
            <marker
              id="arrow-primary"
              viewBox="0 0 10 10"
              refX="18"
              refY="5"
              markerWidth="6"
              markerHeight="6"
              orient="auto-start-reverse"
            >
              <path d="M 0 1 L 9 5 L 0 9 z" fill="#475569" />
            </marker>

            {/* Arrow marker for narrow roads */}
            <marker
              id="arrow-narrow"
              viewBox="0 0 10 10"
              refX="18"
              refY="5"
              markerWidth="6"
              markerHeight="6"
              orient="auto-start-reverse"
            >
              <path d="M 0 1 L 9 5 L 0 9 z" fill="#f59e0b" />
            </marker>

            {/* Arrow marker for highlighted routes */}
            <marker
              id="arrow-highlight"
              viewBox="0 0 10 10"
              refX="18"
              refY="5"
              markerWidth="7"
              markerHeight="7"
              orient="auto-start-reverse"
            >
              <path d="M 0 1 L 9 5 L 0 9 z" fill="#ea580c" />
            </marker>

            {/* Arrow marker for closed roads */}
            <marker
              id="arrow-closed"
              viewBox="0 0 10 10"
              refX="18"
              refY="5"
              markerWidth="6"
              markerHeight="6"
              orient="auto-start-reverse"
            >
              <path d="M 0 1 L 9 5 L 0 9 z" fill="#ef4444" />
            </marker>
          </defs>

          {/* Grid lines background decoration */}
          <pattern id="subtle-grid" width="40" height="40" patternUnits="userSpaceOnUse">
            <path d="M 40 0 L 0 0 0 40" fill="none" stroke="rgba(255, 255, 255, 0.025)" strokeWidth="1" />
          </pattern>
          <rect width="100%" height="100%" fill="url(#subtle-grid)" />

          {/* 1. Road Edges */}
          <g className="road-edges-layer">
            {renderedRoads.map((road) => {
              const isClosed = road.open === false;
              let strokeClass = 'road-line-primary';
              let marker = 'url(#arrow-primary)';

              if (isClosed) {
                strokeClass = 'road-line-closed';
                marker = 'url(#arrow-closed)';
              } else if (road.isHighlighted) {
                strokeClass = 'road-line-highlight';
                marker = 'url(#arrow-highlight)';
              } else if (road.narrowRoad) {
                strokeClass = 'road-line-narrow';
                marker = 'url(#arrow-narrow)';
              } else if (road.travelTimeMinutes > 15) {
                strokeClass = 'road-line-congested';
              }

              return (
                <g
                  key={road.roadKey}
                  className="road-group"
                  onMouseEnter={() => setHoveredRoadKey(road.roadKey)}
                  onMouseLeave={() => setHoveredRoadKey(null)}
                  onClick={() => onSelectRoad && onSelectRoad(road)}
                >
                  {/* Invisible thicker stroke for easy hovering/clicking */}
                  <line
                    x1={road.startX}
                    y1={road.startY}
                    x2={road.endX}
                    y2={road.endY}
                    stroke="transparent"
                    strokeWidth="14"
                  />
                  {/* Visible styled line */}
                  <line
                    x1={road.startX}
                    y1={road.startY}
                    x2={road.endX}
                    y2={road.endY}
                    className={`road-svg-path ${strokeClass} ${road.isHovered ? 'hovered' : ''}`}
                    markerEnd={marker}
                  />
                  {/* Travel time or closure badge along the edge */}
                  {(isClosed || road.isHovered || road.isHighlighted || road.narrowRoad) && (
                    <g transform={`translate(${road.midX}, ${road.midY})`}>
                      <rect
                        x={isClosed ? "-26" : "-18"}
                        y="-10"
                        width={isClosed ? "52" : "36"}
                        height="18"
                        rx="4"
                        className={`edge-time-bg ${isClosed ? 'bg-closed' : road.isHighlighted ? 'bg-highlight' : road.narrowRoad ? 'bg-narrow' : ''}`}
                      />
                      <text x="0" y="3" textAnchor="middle" className={`edge-time-text ${isClosed ? 'text-closed' : ''}`}>
                        {isClosed ? 'CLOSED' : `${road.travelTimeMinutes}m`}
                      </text>
                    </g>
                  )}
                </g>
              );
            })}
          </g>

          {/* 2. Stop Nodes */}
          <g className="stop-nodes-layer">
            {Object.entries(STOP_COORDINATES).map(([stopId, coord]) => {
              const isSelected = selectedStopId === stopId;
              const isHighlighted = highlightPath && highlightPath.includes(stopId);
              const busesHere = busesByStop[stopId] || [];

              return (
                <g
                  key={stopId}
                  transform={`translate(${coord.x}, ${coord.y})`}
                  className={`stop-node-group ${isSelected ? 'selected' : ''} ${isHighlighted ? 'path-stop' : ''}`}
                  onClick={() => handleStopClick(stopId)}
                >
                  {/* Outer pulse for depot or selected */}
                  {(coord.isDepot || isSelected || isHighlighted) && (
                    <circle r={coord.isDepot ? 22 : 18} className="stop-pulse" />
                  )}

                  {/* Main Node Circle */}
                  <circle
                    r={coord.isDepot ? 14 : 10}
                    className={`stop-circle ${coord.isDepot ? 'depot-node' : coord.isNarrowSector ? 'narrow-sector-node' : ''}`}
                  />

                  {/* Stop ID label */}
                  <text y={coord.isDepot ? 4 : 3} textAnchor="middle" className="stop-code-text">
                    {coord.isDepot ? 'D' : stopId}
                  </text>

                  {/* Stop Name Tag */}
                  <text y={coord.isDepot ? 28 : 22} textAnchor="middle" className="stop-name-text">
                    {coord.label}
                  </text>

                  {/* Station bus indicators */}
                  {busesHere.length > 0 && (
                    <g transform="translate(12, -12)">
                      <rect x="-8" y="-8" width="22" height="16" rx="4" className="station-bus-pill" />
                      <text x="3" y="4" textAnchor="middle" className="station-bus-text">
                        🚌{busesHere.length}
                      </text>
                    </g>
                  )}
                </g>
              );
            })}
          </g>
        </svg>
      </div>

      {/* Selected Stop Details Drawer */}
      {selectedStopId && (
        <div className="map-stop-drawer">
          <div className="drawer-header">
            <div>
              <span className="drawer-title">Station Details: {selectedStopId}</span>
              <span className="drawer-subtitle">
                {STOP_COORDINATES[selectedStopId]?.label || 'Transit Stop'}
              </span>
            </div>
            <button className="close-drawer-btn" onClick={() => setSelectedStopId(null)}>×</button>
          </div>
          <div className="drawer-body">
            <div className="drawer-section">
              <span className="section-title">Parked / Visiting Buses ({busesByStop[selectedStopId]?.length || 0})</span>
              {busesByStop[selectedStopId]?.length ? (
                <div className="drawer-buses-grid">
                  {busesByStop[selectedStopId].map((bus) => (
                    <div key={bus.id} className="drawer-bus-chip">
                      <span className="bus-chip-id">{bus.id}</span>
                      <span className="bus-chip-type">{bus.type === 'MINIBUS' ? 'MiniBus (25)' : 'Standard (50)'}</span>
                      <span className={`bus-chip-status status-${bus.status.toLowerCase()}`}>{bus.status}</span>
                      <span className="bus-chip-occ">{bus.passengerOccupancy}/{bus.capacity} seats</span>
                    </div>
                  ))}
                </div>
              ) : (
                <span className="drawer-empty-text">No buses currently at this station.</span>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
