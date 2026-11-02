import { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { fetchHealth } from '../../api/client';
import {
  MapPin,
  Bus,
  BusFront,
  Activity,
  Users,
  Cpu,
  ShieldCheck,
  CheckCircle2,
  ArrowRight,
  GitFork,
  Sparkles,
  BarChart3,
  Clock,
  Compass,
  Layers,
  AlertTriangle,
  ChevronRight,
  User,
  LogOut,
  LayoutDashboard,
} from 'lucide-react';
import './LandingPage.css';

// Canonical Vetrinagar Network Reference Data for Public Display
const STATIONS = [
  {
    id: 'DEPOT',
    name: 'Central Transit Depot',
    x: 430,
    y: 300,
    type: 'Depot',
    desc: 'Central fleet operations headquarters, maintenance bay, and primary vehicle dispatch depot.',
    connections: [
      { to: 'S1', name: 'Downtown Hub', time: 8, isNarrow: false },
      { to: 'S2', name: 'Central Station', time: 10, isNarrow: false },
      { to: 'S11', name: 'Industrial Zone', time: 12, isNarrow: false },
    ],
  },
  {
    id: 'S1',
    name: 'Downtown Hub',
    x: 320,
    y: 200,
    type: 'Terminal',
    desc: 'High-density commercial commuter hub and central transfer terminal.',
    connections: [
      { to: 'DEPOT', name: 'Central Transit Depot', time: 8, isNarrow: false },
      { to: 'S2', name: 'Central Station', time: 6, isNarrow: false },
      { to: 'S3', name: 'University Campus', time: 9, isNarrow: false },
      { to: 'S7', name: 'Old Town Historic', time: 5, isNarrow: true },
    ],
  },
  {
    id: 'S2',
    name: 'Central Station',
    x: 430,
    y: 170,
    type: 'Interchange',
    desc: 'Primary multimodal rail and rapid bus interchange station.',
    connections: [
      { to: 'DEPOT', name: 'Central Transit Depot', time: 10, isNarrow: false },
      { to: 'S1', name: 'Downtown Hub', time: 6, isNarrow: false },
      { to: 'S4', name: 'Tech Park North', time: 11, isNarrow: false },
      { to: 'S10', name: 'West End Mall', time: 13, isNarrow: false },
    ],
  },
  {
    id: 'S3',
    name: 'University Campus',
    x: 210,
    y: 160,
    type: 'Station',
    desc: 'Major academic center with high morning and evening student commuter surges.',
    connections: [
      { to: 'S1', name: 'Downtown Hub', time: 9, isNarrow: false },
      { to: 'S4', name: 'Tech Park North', time: 7, isNarrow: false },
      { to: 'S5', name: 'City Hospital', time: 8, isNarrow: false },
    ],
  },
  {
    id: 'S4',
    name: 'Tech Park North',
    x: 340,
    y: 70,
    type: 'Station',
    desc: 'High-tech commercial office corridor and northern arterial gateway.',
    connections: [
      { to: 'S2', name: 'Central Station', time: 11, isNarrow: false },
      { to: 'S3', name: 'University Campus', time: 7, isNarrow: false },
      { to: 'S9', name: 'Airport Terminal', time: 14, isNarrow: false },
    ],
  },
  {
    id: 'S5',
    name: 'City Hospital',
    x: 120,
    y: 280,
    type: 'Station',
    desc: 'Regional medical complex requiring 24/7 prioritized, unobstructed transit corridors.',
    connections: [
      { to: 'S3', name: 'University Campus', time: 8, isNarrow: false },
      { to: 'S6', name: 'Market Square', time: 5, isNarrow: false },
    ],
  },
  {
    id: 'S6',
    name: 'Market Square',
    x: 160,
    y: 420,
    type: 'Station',
    desc: 'Historic trade marketplace and western junction connecting old and new districts.',
    connections: [
      { to: 'S5', name: 'City Hospital', time: 5, isNarrow: false },
      { to: 'S7', name: 'Old Town Historic', time: 4, isNarrow: true },
      { to: 'S8', name: 'South Harbor', time: 10, isNarrow: false },
    ],
  },
  {
    id: 'S7',
    name: 'Old Town Historic',
    x: 260,
    y: 330,
    type: 'Narrow Sector',
    desc: 'Historic pedestrian sector with medieval cobblestone streets. Standard buses strictly prohibited; MiniBuses permitted.',
    connections: [
      { to: 'S6', name: 'Market Square', time: 4, isNarrow: true },
      { to: 'S1', name: 'Downtown Hub', time: 5, isNarrow: true },
    ],
  },
  {
    id: 'S8',
    name: 'South Harbor',
    x: 300,
    y: 530,
    type: 'Terminal',
    desc: 'Maritime ferry passenger terminal and coastal district link.',
    connections: [
      { to: 'S6', name: 'Market Square', time: 10, isNarrow: false },
      { to: 'S9', name: 'Airport Terminal', time: 12, isNarrow: false },
      { to: 'S12', name: 'Riverside Park', time: 7, isNarrow: false },
    ],
  },
  {
    id: 'S9',
    name: 'Airport Terminal',
    x: 550,
    y: 60,
    type: 'Terminal',
    desc: 'International airport passenger terminal with high-capacity express transit demands.',
    connections: [
      { to: 'S4', name: 'Tech Park North', time: 14, isNarrow: false },
      { to: 'S8', name: 'South Harbor', time: 12, isNarrow: false },
      { to: 'S10', name: 'West End Mall', time: 15, isNarrow: false },
    ],
  },
  {
    id: 'S10',
    name: 'West End Mall',
    x: 640,
    y: 210,
    type: 'Station',
    desc: 'Regional commercial retail center and eastern commuter transfer stop.',
    connections: [
      { to: 'S2', name: 'Central Station', time: 13, isNarrow: false },
      { to: 'S9', name: 'Airport Terminal', time: 15, isNarrow: false },
      { to: 'S11', name: 'Industrial Zone', time: 8, isNarrow: false },
    ],
  },
  {
    id: 'S11',
    name: 'Industrial Zone',
    x: 600,
    y: 400,
    type: 'Station',
    desc: 'Manufacturing and logistics district with peak shift-based passenger crowds.',
    connections: [
      { to: 'DEPOT', name: 'Central Transit Depot', time: 12, isNarrow: false },
      { to: 'S10', name: 'West End Mall', time: 8, isNarrow: false },
      { to: 'S12', name: 'Riverside Park', time: 9, isNarrow: false },
    ],
  },
  {
    id: 'S12',
    name: 'Riverside Park',
    x: 470,
    y: 520,
    type: 'Station',
    desc: 'Southern recreational parkway and waterfront residential community link.',
    connections: [
      { to: 'S8', name: 'South Harbor', time: 7, isNarrow: false },
      { to: 'S11', name: 'Industrial Zone', time: 9, isNarrow: false },
    ],
  },
];

// 19 Bidirectional Corridors = 38 Directed Edges
const CORRIDORS = [
  { from: 'DEPOT', to: 'S1', time: 8, isNarrow: false },
  { from: 'DEPOT', to: 'S2', time: 10, isNarrow: false },
  { from: 'DEPOT', to: 'S11', time: 12, isNarrow: false },
  { from: 'S1', to: 'S2', time: 6, isNarrow: false },
  { from: 'S1', to: 'S3', time: 9, isNarrow: false },
  { from: 'S2', to: 'S4', time: 11, isNarrow: false },
  { from: 'S3', to: 'S4', time: 7, isNarrow: false },
  { from: 'S3', to: 'S5', time: 8, isNarrow: false },
  { from: 'S4', to: 'S9', time: 14, isNarrow: false },
  { from: 'S5', to: 'S6', time: 5, isNarrow: false },
  { from: 'S6', to: 'S7', time: 4, isNarrow: true },
  { from: 'S7', to: 'S1', time: 5, isNarrow: true },
  { from: 'S6', to: 'S8', time: 10, isNarrow: false },
  { from: 'S8', to: 'S9', time: 12, isNarrow: false },
  { from: 'S8', to: 'S12', time: 7, isNarrow: false },
  { from: 'S11', to: 'S12', time: 9, isNarrow: false },
  { from: 'S10', to: 'S11', time: 8, isNarrow: false },
  { from: 'S2', to: 'S10', time: 13, isNarrow: false },
  { from: 'S9', to: 'S10', time: 15, isNarrow: false },
];

export default function LandingPage({ _onNavigate, onLogin, onGoToDashboard }) {
  const { user, role, isAuthenticated, logout } = useAuth();
  const [selectedStationId, setSelectedStationId] = useState('DEPOT');
  const [corridorFilter, setCorridorFilter] = useState('all'); // 'all', 'narrow', 'depot'
  const [apiHealth, setApiHealth] = useState({ online: false, latency: null, checking: true });

  useEffect(() => {
    let isMounted = true;
    async function checkApi() {
      const res = await fetchHealth();
      if (isMounted) {
        if (res.success) {
          setApiHealth({ online: true, latency: res.latencyMs, checking: false });
        } else {
          setApiHealth({ online: false, latency: null, checking: false });
        }
      }
    }
    checkApi();
    return () => {
      isMounted = false;
    };
  }, []);

  const selectedStation = STATIONS.find((s) => s.id === selectedStationId) || STATIONS[0];

  const filteredCorridors = CORRIDORS.filter((c) => {
    if (corridorFilter === 'narrow') return c.isNarrow;
    if (corridorFilter === 'depot') return c.from === 'DEPOT' || c.to === 'DEPOT';
    return true;
  });

  const scrollToSection = (sectionId) => {
    const el = document.getElementById(sectionId);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const getRoleBadgeClass = (r) => {
    switch (r) {
      case 'ADMIN':
        return 'badge-purple';
      case 'TRANSPORT_MANAGER':
        return 'badge-emerald';
      case 'DRIVER':
        return 'badge-amber';
      case 'PASSENGER':
        return 'badge-blue';
      default:
        return 'badge-gray';
    }
  };

  return (
    <div className="landing-layout">
      {/* ── Top Navigation Bar ────────────────────────────────────────── */}
      <header className="landing-navbar">
        <div className="landing-nav-container">
          <div className="landing-brand" onClick={() => scrollToSection('hero')}>
            <div className="landing-brand-icon">
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
              <span className="landing-brand-title">
                <span className="brand-gradient">Smart</span>Transport
              </span>
              <span className="landing-brand-tag">Urban Optimization</span>
            </div>
          </div>

          <nav className="landing-nav-links">
            <button type="button" onClick={() => scrollToSection('hero')} className="landing-nav-link">
              Home
            </button>
            <button type="button" onClick={() => scrollToSection('features')} className="landing-nav-link">
              Features
            </button>
            <button type="button" onClick={() => scrollToSection('network')} className="landing-nav-link">
              Network Overview
            </button>
            <button type="button" onClick={() => scrollToSection('how-it-works')} className="landing-nav-link">
              How It Works
            </button>
            <button type="button" onClick={() => scrollToSection('about')} className="landing-nav-link">
              About
            </button>
          </nav>

          <div className="landing-nav-actions">
            {isAuthenticated ? (
              <div className="landing-auth-user">
                <div className="landing-user-pill">
                  <div className="landing-user-avatar">
                    <User size={15} />
                  </div>
                  <div className="landing-user-info">
                    <span className="landing-user-name">{user?.fullName || user?.email}</span>
                    <span className={`landing-role-badge ${getRoleBadgeClass(role)}`}>
                      {role?.replace('_', ' ')}
                    </span>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={onGoToDashboard}
                  className="landing-btn landing-btn-primary"
                >
                  <LayoutDashboard size={16} />
                  <span>Dashboard</span>
                </button>
                <button
                  type="button"
                  onClick={logout}
                  className="landing-btn landing-btn-ghost-icon"
                  title="Sign Out"
                >
                  <LogOut size={16} />
                </button>
              </div>
            ) : (
              <div className="landing-guest-actions">
                <button
                  type="button"
                  onClick={onLogin}
                  className="landing-btn landing-btn-outline"
                >
                  Sign In
                </button>
                <button
                  type="button"
                  onClick={onLogin}
                  className="landing-btn landing-btn-primary"
                >
                  <span>Get Started</span>
                  <ChevronRight size={16} />
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* ── Hero Section ──────────────────────────────────────────────── */}
      <section id="hero" className="landing-hero-section">
        <div className="landing-hero-container">
          <div className="landing-hero-content">
            <div className="landing-announcement-pill">
              <Sparkles size={15} className="text-purple-600" />
              <span>Intelligent Urban Transit Management</span>
              {apiHealth.checking ? (
                <span className="health-ping-pill ping-checking">Checking API...</span>
              ) : apiHealth.online ? (
                <span className="health-ping-pill ping-online">
                  <span className="ping-dot" />
                  API Online ({apiHealth.latency}ms)
                </span>
              ) : (
                <span className="health-ping-pill ping-offline">Offline</span>
              )}
            </div>

            <h1 className="landing-hero-title">
              Smarter Routes. Better Connections.{' '}
              <span className="brand-gradient">Efficient Transportation.</span>
            </h1>

            <p className="landing-hero-description">
              An intelligent public transportation route optimization platform that helps
              manage transit networks, optimize journeys, monitor fleet operations and improve
              passenger mobility.
            </p>

            <div className="landing-hero-cta">
              <button
                type="button"
                onClick={() => scrollToSection('network')}
                className="landing-btn landing-btn-secondary"
              >
                <Compass size={18} />
                <span>Explore Network</span>
              </button>

              <button
                type="button"
                onClick={isAuthenticated ? onGoToDashboard : onLogin}
                className="landing-btn landing-btn-primary landing-btn-lg"
              >
                <span>Access Dashboard</span>
                <ArrowRight size={18} />
              </button>
            </div>

            <div className="landing-hero-badges">
              <div className="hero-feature-badge">
                <Clock size={15} className="text-purple-600" />
                <span>Microsecond Dijkstra Telemetry</span>
              </div>
              <div className="hero-feature-badge">
                <CheckCircle2 size={15} className="text-emerald-600" />
                <span>100% Passenger Conservation</span>
              </div>
              <div className="hero-feature-badge">
                <ShieldCheck size={15} className="text-indigo-600" />
                <span>Role-Based Access Control</span>
              </div>
            </div>
          </div>

          {/* Interactive Network Card in Hero */}
          <div className="landing-hero-visual">
            <div className="hero-visual-card">
              <div className="hero-visual-header">
                <div className="hero-visual-title-box">
                  <Activity size={18} className="text-purple-600" />
                  <span className="hero-visual-title">Vetrinagar Transit Corridor Matrix</span>
                </div>
                <span className="hero-visual-status">Live Dijkstra Engine</span>
              </div>

              <div className="hero-visual-preview">
                <svg viewBox="0 0 760 400" className="hero-svg-preview">
                  <defs>
                    <linearGradient id="heroEdgeGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#818cf8" stopOpacity="0.7" />
                      <stop offset="100%" stopColor="#c084fc" stopOpacity="0.7" />
                    </linearGradient>
                    <linearGradient id="heroActiveGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                      <stop offset="0%" stopColor="#6366f1" />
                      <stop offset="100%" stopColor="#a855f7" />
                    </linearGradient>
                  </defs>

                  {/* Background grid */}
                  <pattern id="heroGrid" width="30" height="30" patternUnits="userSpaceOnUse">
                    <circle cx="15" cy="15" r="1" fill="#e2e8f0" />
                  </pattern>
                  <rect width="760" height="400" fill="url(#heroGrid)" opacity="0.8" />

                  {/* Render Connections */}
                  {CORRIDORS.slice(0, 14).map((c, idx) => {
                    const from = STATIONS.find((s) => s.id === c.from);
                    const to = STATIONS.find((s) => s.id === c.to);
                    if (!from || !to) return null;
                    return (
                      <line
                        key={`hero-c-${idx}`}
                        x1={from.x * 0.85}
                        y1={from.y * 0.72}
                        x2={to.x * 0.85}
                        y2={to.y * 0.72}
                        stroke={c.isNarrow ? '#f59e0b' : '#cbd5e1'}
                        strokeWidth={c.isNarrow ? 2 : 2.5}
                        strokeDasharray={c.isNarrow ? '4 3' : 'none'}
                        opacity="0.8"
                      />
                    );
                  })}

                  {/* Highlighted active path: DEPOT -> S1 -> S3 -> S4 */}
                  <polyline
                    points="365,216 272,144 178,115 289,50"
                    fill="none"
                    stroke="url(#heroActiveGradient)"
                    strokeWidth="4"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="animated-path-stroke"
                  />

                  {/* Render Station Nodes */}
                  {STATIONS.map((s) => {
                    const sx = s.x * 0.85;
                    const sy = s.y * 0.72;
                    const isSelected = s.id === selectedStationId;
                    const isDepot = s.id === 'DEPOT';
                    return (
                      <g
                        key={`hero-node-${s.id}`}
                        className="hero-node-group"
                        onClick={() => setSelectedStationId(s.id)}
                      >
                        {isSelected && (
                          <circle cx={sx} cy={sy} r="18" fill="#8b5cf6" opacity="0.18" />
                        )}
                        <circle
                          cx={sx}
                          cy={sy}
                          r={isDepot ? 11 : 8}
                          fill={isDepot ? '#4f46e5' : isSelected ? '#8b5cf6' : s.type === 'Narrow Sector' ? '#f59e0b' : '#3b82f6'}
                          stroke="#ffffff"
                          strokeWidth="2.5"
                        />
                        <text
                          x={sx}
                          y={sy - 12}
                          textAnchor="middle"
                          className="hero-node-text"
                        >
                          {s.id}
                        </text>
                      </g>
                    );
                  })}
                </svg>
              </div>

              <div className="hero-visual-footer">
                <div className="hero-metric-item">
                  <span className="hero-metric-label">Graph Dimension</span>
                  <span className="hero-metric-val">13 Stops / 38 Roads</span>
                </div>
                <div className="hero-metric-divider" />
                <div className="hero-metric-item">
                  <span className="hero-metric-label">Sample Shortest Path</span>
                  <span className="hero-metric-val">DEPOT → S1 → S3 → S4 (24 min)</span>
                </div>
                <div className="hero-metric-divider" />
                <div className="hero-metric-item">
                  <span className="hero-metric-label">Execution Latency</span>
                  <span className="hero-metric-val text-purple-600">⚡ 124 µs</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── Key Statistics Section ───────────────────────────────────── */}
      <section className="landing-stats-section">
        <div className="landing-container">
          <div className="section-header text-center">
            <span className="section-eyebrow">Verified Baseline Architecture</span>
            <h2 className="section-title">Network Infrastructure at a Glance</h2>
            <p className="section-subtitle">
              Configured deterministic graph parameters representing Vetrinagar&apos;s urban transit grid.
            </p>
          </div>

          <div className="landing-stats-grid">
            <div className="landing-stat-card">
              <div className="stat-card-icon-wrapper bg-indigo-50 text-indigo-600">
                <MapPin size={28} />
              </div>
              <div className="stat-card-number">13</div>
              <h3 className="stat-card-label">Total Bus Stops</h3>
              <p className="stat-card-desc">
                Central transit depot and 12 strategic passenger nodes covering downtown, campus, airport, and old town sectors.
              </p>
            </div>

            <div className="landing-stat-card">
              <div className="stat-card-icon-wrapper bg-purple-50 text-purple-600">
                <GitFork size={28} />
              </div>
              <div className="stat-card-number">38</div>
              <h3 className="stat-card-label">Road Connections</h3>
              <p className="stat-card-desc">
                19 bidirectional travel corridors, including high-speed arterials and width-restricted historic corridors.
              </p>
            </div>

            <div className="landing-stat-card">
              <div className="stat-card-icon-wrapper bg-blue-50 text-blue-600">
                <Bus size={28} />
              </div>
              <div className="stat-card-number">4</div>
              <h3 className="stat-card-label">Fleet Buses</h3>
              <p className="stat-card-desc">
                Multi-capacity fleet combining Standard 50-passenger cruisers and agile 25-passenger MiniBuses for narrow roads.
              </p>
            </div>

            <div className="landing-stat-card">
              <div className="stat-card-icon-wrapper bg-amber-50 text-amber-600">
                <Users size={28} />
              </div>
              <div className="stat-card-number">22</div>
              <h3 className="stat-card-label">Demand Batches</h3>
              <p className="stat-card-desc">
                150+ commuters queued across travel corridors, ordered deterministically by batch size and arrival order.
              </p>
            </div>
          </div>

          <div className="stats-verification-banner">
            <CheckCircle2 size={16} className="text-emerald-600" />
            <span>
              Verified baseline dataset loaded from MySQL / TiDB schema &bull; No simulated placeholder values
            </span>
          </div>
        </div>
      </section>

      {/* ── Network Overview Section ─────────────────────────────────── */}
      <section id="network" className="landing-network-section">
        <div className="landing-container">
          <div className="section-header">
            <span className="section-eyebrow">Interactive Topology</span>
            <h2 className="section-title">Transportation Network Overview</h2>
            <p className="section-subtitle">
              Inspect the 13 transit stations, 38 road connections, and corridor width restrictions across Vetrinagar.
            </p>
          </div>

          <div className="network-showcase-box">
            {/* Filter and Legend Bar */}
            <div className="network-showcase-toolbar">
              <div className="network-filters">
                <span className="filter-title">Filter Corridors:</span>
                <button
                  type="button"
                  onClick={() => setCorridorFilter('all')}
                  className={`filter-btn ${corridorFilter === 'all' ? 'active' : ''}`}
                >
                  All Corridors (38)
                </button>
                <button
                  type="button"
                  onClick={() => setCorridorFilter('narrow')}
                  className={`filter-btn ${corridorFilter === 'narrow' ? 'active' : ''}`}
                >
                  Narrow Historic (4)
                </button>
                <button
                  type="button"
                  onClick={() => setCorridorFilter('depot')}
                  className={`filter-btn ${corridorFilter === 'depot' ? 'active' : ''}`}
                >
                  Depot Links (6)
                </button>
              </div>

              <div className="network-legend">
                <span className="legend-entry">
                  <span className="legend-chip bg-indigo-500" /> Standard Road
                </span>
                <span className="legend-entry">
                  <span className="legend-chip bg-amber-500 border-dashed" /> Narrow Corridor (MiniBus Only)
                </span>
                <span className="legend-entry">
                  <span className="legend-chip bg-purple-600 rounded-full" /> Central Depot
                </span>
              </div>
            </div>

            {/* Network Map & Station Inspector Split Layout */}
            <div className="network-display-grid">
              <div className="network-svg-wrapper">
                <svg viewBox="0 0 860 600" className="network-main-svg">
                  {/* Grid background */}
                  <defs>
                    <pattern id="netGrid" width="40" height="40" patternUnits="userSpaceOnUse">
                      <circle cx="20" cy="20" r="1" fill="#e2e8f0" />
                    </pattern>
                  </defs>
                  <rect width="860" height="600" fill="url(#netGrid)" />

                  {/* Render Corridors */}
                  {filteredCorridors.map((c, i) => {
                    const from = STATIONS.find((s) => s.id === c.from);
                    const to = STATIONS.find((s) => s.id === c.to);
                    if (!from || !to) return null;
                    const isSelectedEdge =
                      from.id === selectedStationId || to.id === selectedStationId;

                    return (
                      <g key={`corridor-${i}`}>
                        <line
                          x1={from.x}
                          y1={from.y}
                          x2={to.x}
                          y2={to.y}
                          stroke={c.isNarrow ? '#f59e0b' : isSelectedEdge ? '#7c3aed' : '#94a3b8'}
                          strokeWidth={isSelectedEdge ? 3.5 : c.isNarrow ? 2.5 : 2}
                          strokeDasharray={c.isNarrow ? '6 4' : 'none'}
                          opacity={isSelectedEdge ? 1 : 0.65}
                          className="network-edge-line"
                        />
                        {/* Travel time label at midpoint */}
                        <circle
                          cx={(from.x + to.x) / 2}
                          cy={(from.y + to.y) / 2}
                          r="10"
                          fill="#ffffff"
                          stroke={isSelectedEdge ? '#7c3aed' : '#cbd5e1'}
                          strokeWidth="1.5"
                        />
                        <text
                          x={(from.x + to.x) / 2}
                          y={(from.y + to.y) / 2 + 3.5}
                          textAnchor="middle"
                          fontSize="9"
                          fontWeight="600"
                          fill="#475569"
                        >
                          {c.time}m
                        </text>
                      </g>
                    );
                  })}

                  {/* Render Stations */}
                  {STATIONS.map((s) => {
                    const isSelected = s.id === selectedStationId;
                    const isDepot = s.id === 'DEPOT';
                    const isNarrow = s.type === 'Narrow Sector';

                    return (
                      <g
                        key={`station-${s.id}`}
                        className="network-station-group"
                        onClick={() => setSelectedStationId(s.id)}
                      >
                        {isSelected && (
                          <circle
                            cx={s.x}
                            cy={s.y}
                            r="22"
                            fill="#8b5cf6"
                            opacity="0.22"
                            className="pulse-aura"
                          />
                        )}
                        <circle
                          cx={s.x}
                          cy={s.y}
                          r={isDepot ? 14 : 10}
                          fill={
                            isDepot
                              ? '#4f46e5'
                              : isSelected
                              ? '#7c3aed'
                              : isNarrow
                              ? '#d97706'
                              : '#2563eb'
                          }
                          stroke="#ffffff"
                          strokeWidth="3"
                          className="station-node-circle"
                        />
                        <text
                          x={s.x}
                          y={s.y + (isDepot ? 26 : 22)}
                          textAnchor="middle"
                          className={`station-svg-label ${isSelected ? 'selected' : ''}`}
                        >
                          {s.id}: {s.name}
                        </text>
                      </g>
                    );
                  })}
                </svg>
              </div>

              {/* Station Inspector Detail Card */}
              <div className="station-inspector-card">
                <div className="inspector-header">
                  <div className="inspector-id-badge">
                    <MapPin size={18} className="text-purple-600" />
                    <span>{selectedStation.id}</span>
                  </div>
                  <span className={`station-type-pill ${selectedStation.type === 'Narrow Sector' ? 'type-warning' : selectedStation.type === 'Depot' ? 'type-depot' : 'type-standard'}`}>
                    {selectedStation.type}
                  </span>
                </div>

                <h3 className="inspector-name">{selectedStation.name}</h3>
                <p className="inspector-desc">{selectedStation.desc}</p>

                {selectedStation.type === 'Narrow Sector' && (
                  <div className="narrow-restriction-warning">
                    <AlertTriangle size={16} className="text-amber-600 shrink-0" />
                    <span>
                      <strong>Narrow Street Restriction</strong>: Standard 50-passenger buses cannot traverse this sector. Dijkstra planner automatically reroutes heavy buses via arterial roads.
                    </span>
                  </div>
                )}

                <div className="inspector-connections">
                  <h4 className="connections-title">
                    <span>Direct Transit Corridors</span>
                    <span className="count-badge">{selectedStation.connections.length} Links</span>
                  </h4>
                  <div className="connections-list">
                    {selectedStation.connections.map((conn, idx) => (
                      <div
                        key={`conn-${idx}`}
                        className="connection-item"
                        onClick={() => setSelectedStationId(conn.to)}
                      >
                        <div className="connection-info">
                          <span className="conn-target">
                            {conn.to}: {conn.name}
                          </span>
                          <span className="conn-sub">
                            {conn.isNarrow ? 'Narrow Historic Corridor' : 'Standard 2-Way Road'}
                          </span>
                        </div>
                        <div className="connection-metric">
                          <Clock size={13} className="text-gray-400" />
                          <span>{conn.time} min</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="inspector-footer">
                  <span className="hint-text">Click any station node or connection to inspect.</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── Features Section ─────────────────────────────────────────── */}
      <section id="features" className="landing-features-section">
        <div className="landing-container">
          <div className="section-header text-center">
            <span className="section-eyebrow">Enterprise Capabilities</span>
            <h2 className="section-title">Engineered for Modern Urban Mobility</h2>
            <p className="section-subtitle">
              Comprehensive algorithmic solvers, real-time telemetry, and secure multi-role workflows.
            </p>
          </div>

          <div className="landing-features-grid">
            {/* Feature 1 */}
            <div className="landing-feature-card">
              <div className="feature-icon-box bg-purple-50 text-purple-600">
                <Cpu size={26} />
              </div>
              <h3 className="feature-title">Intelligent Route Optimization</h3>
              <p className="feature-desc">
                Min-heap Dijkstra shortest-travel-time solver with microsecond-level ($\mu s$)
                execution profiling, dynamic traffic weighting, and vehicle-type accessibility constraints.
              </p>
            </div>

            {/* Feature 2 */}
            <div className="landing-feature-card">
              <div className="feature-icon-box bg-indigo-50 text-indigo-600">
                <BusFront size={26} />
              </div>
              <h3 className="feature-title">Fleet Management & Relief Dispatch</h3>
              <p className="feature-desc">
                Greedy capacity-aware passenger dispatching pairs queued demand with available buses,
                featuring automated roadside breakdown workflows and immediate emergency relief re-queuing.
              </p>
            </div>

            {/* Feature 3 */}
            <div className="landing-feature-card">
              <div className="feature-icon-box bg-blue-50 text-blue-600">
                <Activity size={26} />
              </div>
              <h3 className="feature-title">Dynamic Traffic Monitoring</h3>
              <p className="feature-desc">
                Simulate peak-hour congestion factors, adjust individual road travel times, and
                evaluate instantaneous algorithmic rerouting around emergency bidirectional road closures.
              </p>
            </div>

            {/* Feature 4 */}
            <div className="landing-feature-card">
              <div className="feature-icon-box bg-amber-50 text-amber-600">
                <Users size={26} />
              </div>
              <h3 className="feature-title">Passenger Demand Analysis</h3>
              <p className="feature-desc">
                Dual-tier PriorityQueue architecture groups waiting passengers by corridor with strict
                First-In-First-Out arrival ordering and mathematically proven passenger conservation.
              </p>
            </div>

            {/* Feature 5 */}
            <div className="landing-feature-card">
              <div className="feature-icon-box bg-emerald-50 text-emerald-600">
                <BarChart3 size={26} />
              </div>
              <h3 className="feature-title">Real-Time Operational Monitoring</h3>
              <p className="feature-desc">
                Interactive SVG network visualization, live vehicle tracking, passenger occupancy gauges,
                active trip manifests, and comprehensive algorithmic performance telemetry.
              </p>
            </div>

            {/* Feature 6 */}
            <div className="landing-feature-card">
              <div className="feature-icon-box bg-violet-50 text-violet-600">
                <ShieldCheck size={26} />
              </div>
              <h3 className="feature-title">Role-Based Access Control</h3>
              <p className="feature-desc">
                Database-backed security with HMAC-SHA256 JWT tokens and BCrypt hashing, providing
                tailored workflows for Passengers, Drivers, Transport Managers, and System Administrators.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ── How It Works Section ─────────────────────────────────────── */}
      <section id="how-it-works" className="landing-workflow-section">
        <div className="landing-container">
          <div className="section-header text-center">
            <span className="section-eyebrow">Deterministic Operations</span>
            <h2 className="section-title">How Route Optimization Works</h2>
            <p className="section-subtitle">
              Three synchronized algorithmic steps delivering optimal urban transit journeys.
            </p>
          </div>

          <div className="workflow-steps-grid">
            <div className="workflow-step-card">
              <div className="step-badge">01</div>
              <div className="step-icon-wrapper text-indigo-600">
                <MapPin size={32} />
              </div>
              <h3 className="step-title">Select Source & Destination</h3>
              <p className="step-desc">
                Commuters or operators select origin and target stations across Vetrinagar&apos;s
                13 network stops, defining the desired passenger transit corridor.
              </p>
            </div>

            <div className="workflow-step-card">
              <div className="step-badge">02</div>
              <div className="step-icon-wrapper text-purple-600">
                <GitFork size={32} />
              </div>
              <h3 className="step-title">Analyze Routing Constraints</h3>
              <p className="step-desc">
                The Dijkstra planner evaluates real-time traffic congestion, active road closures,
                and vehicle road-type restrictions to construct optimal traversal paths.
              </p>
            </div>

            <div className="workflow-step-card">
              <div className="step-badge">03</div>
              <div className="step-icon-wrapper text-emerald-600">
                <CheckCircle2 size={32} />
              </div>
              <h3 className="step-title">Dispatch & Optimize Journey</h3>
              <p className="step-desc">
                The greedy engine dispatches available fleet vehicles, boards prioritized passenger
                batches, and illuminates the shortest journey on the live transit map.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ── Call to Action Section ───────────────────────────────────── */}
      <section className="landing-cta-section">
        <div className="landing-container">
          <div className="landing-cta-box">
            <div className="cta-content">
              <h2 className="cta-title">Ready to Explore Smarter Transportation?</h2>
              <p className="cta-description">
                Experience intelligent route planning, simulate urban traffic incidents, or test multi-role governance with pre-configured demo accounts.
              </p>
            </div>

            <div className="cta-actions">
              <button
                type="button"
                onClick={isAuthenticated ? onGoToDashboard : onLogin}
                className="landing-btn landing-btn-white"
              >
                <span>{isAuthenticated ? 'Open Operations Dashboard' : 'Login to Platform'}</span>
                <ArrowRight size={18} />
              </button>
              <button
                type="button"
                onClick={() => scrollToSection('features')}
                className="landing-btn landing-btn-outline-white"
              >
                <span>Explore Features</span>
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* ── Footer ───────────────────────────────────────────────────── */}
      <footer id="about" className="landing-footer">
        <div className="landing-container">
          <div className="landing-footer-grid">
            {/* Column 1: Brand & Overview */}
            <div className="footer-brand-col">
              <div className="landing-brand">
                <div className="landing-brand-icon">
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
                  <span className="landing-brand-title">Smart Transport</span>
                </div>
              </div>
              <p className="footer-brand-desc">
                An intelligent urban transit management and route optimization system built with Dijkstra shortest-path algorithms, dynamic traffic simulation, and capacity-aware fleet dispatching.
              </p>
              <div className="footer-academic-pill">
                <Layers size={14} className="text-purple-600" />
                <span>Academic Mini Project Main &bull; Computer Science &amp; Engineering</span>
              </div>
            </div>

            {/* Column 2: Navigation Links */}
            <div className="footer-links-col">
              <h4 className="footer-heading">Platform Navigation</h4>
              <ul className="footer-links-list">
                <li>
                  <button type="button" onClick={() => scrollToSection('hero')} className="footer-link">
                    Home
                  </button>
                </li>
                <li>
                  <button type="button" onClick={() => scrollToSection('features')} className="footer-link">
                    Features
                  </button>
                </li>
                <li>
                  <button type="button" onClick={() => scrollToSection('network')} className="footer-link">
                    Network Overview
                  </button>
                </li>
                <li>
                  <button type="button" onClick={() => scrollToSection('how-it-works')} className="footer-link">
                    How It Works
                  </button>
                </li>
                <li>
                  <button type="button" onClick={isAuthenticated ? onGoToDashboard : onLogin} className="footer-link">
                    {isAuthenticated ? 'Operations Dashboard' : 'Sign In Portal'}
                  </button>
                </li>
              </ul>
            </div>

            {/* Column 3: Tech Stack */}
            <div className="footer-links-col">
              <h4 className="footer-heading">Technology Architecture</h4>
              <ul className="footer-tech-list">
                <li>
                  <span className="tech-badge">Java 21 LTS</span>
                  <span className="tech-desc">Core Dijkstra &amp; Graph Engine</span>
                </li>
                <li>
                  <span className="tech-badge">Spring Boot 4.1.1</span>
                  <span className="tech-desc">REST APIs &amp; JPA Persistence</span>
                </li>
                <li>
                  <span className="tech-badge">Spring Security 6</span>
                  <span className="tech-desc">JWT HMAC-SHA256 &amp; BCrypt RBAC</span>
                </li>
                <li>
                  <span className="tech-badge">React 19 + Vite 8</span>
                  <span className="tech-desc">Reactive SPA &amp; SVG Map Telemetry</span>
                </li>
                <li>
                  <span className="tech-badge">MySQL 8.0 / TiDB</span>
                  <span className="tech-desc">Relational Persistence</span>
                </li>
              </ul>
            </div>
          </div>

          <div className="landing-footer-bottom">
            <div className="footer-attribution">
              <span>Smart Public Transportation Route Optimization &bull; College Evaluation Demonstration</span>
            </div>
            <div className="footer-meta">
              <span>All transit models, graph structures, and algorithm metrics verified for academic review.</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
