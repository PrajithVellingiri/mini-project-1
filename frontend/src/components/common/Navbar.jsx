export default function Navbar({ activeTab, onSelectTab, healthState, onRefresh }) {
  const tabs = [
    { id: 'overview', label: 'Operations Center', icon: '⚡' },
    { id: 'network', label: 'Transit Network Map', icon: '🗺️' },
    { id: 'routing', label: 'Route Optimization Lab', icon: '🧭' },
    { id: 'traffic', label: 'Traffic Simulation', icon: '🚦' },
    { id: 'fleet', label: 'Fleet & Demand Console', icon: '🚌' },
    { id: 'analytics', label: 'Analytics & Telemetry', icon: '📊' },
  ];

  return (
    <header className="navbar">
      <div className="navbar-left">
        <div className="navbar-brand">
          <div className="brand-logo">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="2" y="5" width="20" height="13" rx="3" />
              <path d="M4 18v2a1 1 0 0 0 1 1h1" />
              <path d="M18 21h1a1 1 0 0 0 1-1v-2" />
              <circle cx="7" cy="15" r="1.5" />
              <circle cx="17" cy="15" r="1.5" />
              <path d="M2 11h20" />
            </svg>
          </div>
          <div>
            <div className="brand-title">
              <span className="brand-highlight">Smart</span>Transit
            </div>
            <div className="brand-subtitle">Vetrinagar Route Optimization & Dispatch</div>
          </div>
        </div>

        <nav className="navbar-tabs">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => onSelectTab(tab.id)}
              className={`nav-tab ${activeTab === tab.id ? 'active' : ''}`}
            >
              <span className="tab-icon">{tab.icon}</span>
              <span className="tab-label">{tab.label}</span>
            </button>
          ))}
        </nav>
      </div>

      <div className="navbar-right">
        <div className="system-pill" onClick={onRefresh} title="Click to refresh system status">
          <span
            className={`status-dot ${
              healthState.status === 'success'
                ? 'dot-green'
                : healthState.status === 'loading'
                ? 'dot-amber'
                : 'dot-red'
            }`}
          />
          <span className="system-label">
            {healthState.status === 'success'
              ? `Backend Online (${healthState.latencyMs}ms)`
              : healthState.status === 'loading'
              ? 'Checking status...'
              : 'Backend Offline'}
          </span>
          <button className="refresh-mini-btn" title="Refresh state">↻</button>
        </div>
      </div>
    </header>
  );
}
