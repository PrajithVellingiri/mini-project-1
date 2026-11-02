import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  Map,
  Navigation,
  Activity,
  Bus,
  BarChart3,
  Shield,
  Compass,
  BusFront,
  LogOut,
  User,
  RotateCw,
  Home,
} from 'lucide-react';

export default function Navbar({ activeTab, onSelectTab, healthState, onRefresh, onNavigateHome }) {
  const { user, role, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
    if (onNavigateHome) {
      onNavigateHome();
    }
  };

  const getTabsForRole = (currentRole) => {
    switch (currentRole) {
      case 'PASSENGER':
        return [
          { id: 'overview', label: 'Transit Guide', icon: <Compass size={16} /> },
          { id: 'network', label: 'Network Map', icon: <Map size={16} /> },
          { id: 'routing', label: 'Route Planner', icon: <Navigation size={16} /> },
        ];
      case 'DRIVER':
        return [
          { id: 'overview', label: 'Driver Cockpit', icon: <BusFront size={16} /> },
          { id: 'network', label: 'Network Map', icon: <Map size={16} /> },
          { id: 'routing', label: 'Navigation Lab', icon: <Navigation size={16} /> },
        ];
      case 'TRANSPORT_MANAGER':
        return [
          { id: 'overview', label: 'Operations Center', icon: <LayoutDashboard size={16} /> },
          { id: 'network', label: 'Network Map', icon: <Map size={16} /> },
          { id: 'routing', label: 'Route Lab', icon: <Navigation size={16} /> },
          { id: 'traffic', label: 'Traffic Console', icon: <Activity size={16} /> },
          { id: 'fleet', label: 'Fleet & Demand', icon: <Bus size={16} /> },
          { id: 'analytics', label: 'Analytics', icon: <BarChart3 size={16} /> },
        ];
      case 'ADMIN':
      default:
        return [
          { id: 'overview', label: 'Operations Center', icon: <LayoutDashboard size={16} /> },
          { id: 'network', label: 'Network Map', icon: <Map size={16} /> },
          { id: 'routing', label: 'Route Lab', icon: <Navigation size={16} /> },
          { id: 'traffic', label: 'Traffic Console', icon: <Activity size={16} /> },
          { id: 'fleet', label: 'Fleet & Demand', icon: <Bus size={16} /> },
          { id: 'analytics', label: 'Analytics', icon: <BarChart3 size={16} /> },
          { id: 'users', label: 'User Governance', icon: <Shield size={16} /> },
        ];
    }
  };

  const tabs = getTabsForRole(role);

  const getRoleBadgeColor = (r) => {
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
    <header className="navbar">
      <div className="navbar-left">
        <div
          className="navbar-brand"
          onClick={onNavigateHome}
          title={onNavigateHome ? 'Return to Public Homepage' : undefined}
          style={{ cursor: onNavigateHome ? 'pointer' : 'default' }}
        >
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
            <div className="brand-subtitle">Vetrinagar Route Optimization</div>
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
        {/* Link to public homepage */}
        {onNavigateHome && (
          <button
            type="button"
            onClick={onNavigateHome}
            className="portal-home-nav-btn"
            title="View Public Homepage"
          >
            <Home size={15} />
            <span className="portal-btn-label">Public Portal</span>
          </button>
        )}

        {/* System status pill */}
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
              ? `Online (${healthState.latencyMs}ms)`
              : healthState.status === 'loading'
              ? 'Checking...'
              : 'Offline'}
          </span>
          <RotateCw size={13} className="text-gray-400" />
        </div>

        {/* Authenticated user pill */}
        {user && (
          <div className="user-profile-widget">
            <div className="user-avatar-mini">
              <User size={15} />
            </div>
            <div className="user-meta">
              <span className="user-display-name">{user.fullName || user.email}</span>
              <span className={`user-role-badge ${getRoleBadgeColor(role)}`}>
                {role ? role.replace('_', ' ') : 'USER'}
              </span>
            </div>
            <button onClick={handleLogout} className="logout-btn" title="Sign out to Homepage">
              <LogOut size={16} />
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
