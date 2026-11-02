import { useState, useEffect, useCallback } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import LandingPage from './components/landing/LandingPage';
import LoginPage from './components/auth/LoginPage';
import Navbar from './components/common/Navbar';
import AccessDenied from './components/common/AccessDenied';
import OperationsOverview from './components/overview/OperationsOverview';
import PassengerHub from './components/passenger/PassengerHub';
import DriverCockpit from './components/driver/DriverCockpit';
import UserManagement from './components/admin/UserManagement';
import TransportationMap from './components/network/TransportationMap';
import RouteOptimizationLab from './components/routing/RouteOptimizationLab';
import TrafficConsole from './components/traffic/TrafficConsole';
import FleetDemandManager from './components/fleet/FleetDemandManager';
import AnalyticsDashboard from './components/analytics/AnalyticsDashboard';
import {
  fetchHealth,
  fetchSimulationState,
  runSimulation,
  resetSimulation,
  completeActiveTrips,
  completeSpecificTrip,
  updateRoadTravelTime,
  updateRoadStatus,
  updateBusStatus,
  createDemandRequest,
  triggerBusBreakdown,
  restoreBus,
} from './api/client';
import { Loader2, AlertCircle } from 'lucide-react';
import './App.css';

function AuthenticatedDashboard({ onNavigateHome }) {
  const { user, role, isAuthenticated } = useAuth();

  const [activeTab, setActiveTab] = useState('overview');
  const [healthState, setHealthState] = useState({
    status: 'loading',
    data: null,
    error: null,
    latencyMs: null,
    timestamp: null,
  });

  const [simState, setSimState] = useState(null);
  const [simLoading, setSimLoading] = useState(false);
  const [simError, setSimError] = useState(null);
  const [toastMessage, setToastMessage] = useState(null);
  const [highlightedPath, setHighlightedPath] = useState([]);

  const checkHealth = useCallback(async () => {
    setHealthState((prev) => ({ ...prev, status: 'loading', error: null }));
    const result = await fetchHealth();
    if (result.success) {
      setHealthState({
        status: 'success',
        data: result.data,
        error: null,
        latencyMs: result.latencyMs,
        timestamp: result.timestamp,
      });
    } else {
      setHealthState({
        status: 'error',
        data: null,
        error: result.error,
        latencyMs: result.latencyMs,
        timestamp: result.timestamp,
      });
    }
  }, []);

  const loadSimulationState = useCallback(async () => {
    if (!isAuthenticated) return;
    setSimLoading(true);
    setSimError(null);
    const result = await fetchSimulationState();
    if (result.success) {
      setSimState(result.data);
    } else {
      setSimError(result.error);
    }
    setSimLoading(false);
  }, [isAuthenticated]);

  useEffect(() => {
    checkHealth();
  }, [checkHealth]);

  useEffect(() => {
    if (isAuthenticated) {
      loadSimulationState();
    }
  }, [isAuthenticated, loadSimulationState]);

  // Adjust activeTab if role changes and tab is not allowed
  useEffect(() => {
    if (role === 'PASSENGER' || role === 'DRIVER') {
      if (['traffic', 'fleet', 'analytics', 'users'].includes(activeTab)) {
        setActiveTab('overview');
      }
    } else if (role === 'TRANSPORT_MANAGER') {
      if (activeTab === 'users') {
        setActiveTab('overview');
      }
    }
  }, [role, activeTab]);

  const showToast = (message, type = 'info') => {
    setToastMessage({ message, type });
    setTimeout(() => setToastMessage(null), 5000);
  };

  const handleRunSimulation = async () => {
    setSimLoading(true);
    const result = await runSimulation();
    if (result.success) {
      const tripsCount = result.data.assignedTripCount || 0;
      const allocated = result.data.allocatedPassengerCount || 0;
      showToast(
        tripsCount > 0
          ? `Dispatched ${tripsCount} relief trips; ${allocated} passengers boarded.`
          : 'Allocation pass completed. No available buses or pending demands.',
        'success'
      );
      await loadSimulationState();
    } else {
      showToast(result.error || 'Allocation pass failed.', 'error');
    }
    setSimLoading(false);
  };

  const handleResetSimulation = async () => {
    setSimLoading(true);
    const result = await resetSimulation();
    if (result.success) {
      setSimState(result.data);
      setHighlightedPath([]);
      showToast('Simulation successfully reset to baseline state.', 'info');
    } else {
      showToast(result.error || 'Reset failed.', 'error');
    }
    setSimLoading(false);
  };

  const handleCompleteAllTrips = async () => {
    setSimLoading(true);
    const result = await completeActiveTrips();
    if (result.success) {
      setSimState(result.data);
      showToast('All active trips completed. Buses arrived at destinations.', 'success');
    } else {
      showToast(result.error || 'Failed to complete trips.', 'error');
    }
    setSimLoading(false);
  };

  const handleCompleteSpecificTrip = async (tripId) => {
    setSimLoading(true);
    const result = await completeSpecificTrip(tripId);
    if (result.success) {
      setSimState(result.data);
      showToast(`Trip ${tripId} completed successfully.`, 'success');
    } else {
      showToast(result.error || `Failed to complete trip ${tripId}.`, 'error');
    }
    setSimLoading(false);
  };

  const handleUpdateRoadTravelTime = async (fromId, toId, travelTimeMinutes) => {
    const result = await updateRoadTravelTime(fromId, toId, travelTimeMinutes);
    if (result.success) {
      await loadSimulationState();
    }
    return result;
  };

  const handleUpdateRoadStatus = async (fromId, toId, open, bidirectional = true) => {
    const result = await updateRoadStatus(fromId, toId, open, bidirectional);
    if (result.success) {
      showToast(
        open
          ? `Road ${fromId} ↔ ${toId} reopened to transit.`
          : `Road ${fromId} ↔ ${toId} CLOSED to all traffic.`,
        open ? 'success' : 'warning'
      );
      await loadSimulationState();
    }
    return result;
  };

  const handleUpdateBusStatus = async (busId, status) => {
    const result = await updateBusStatus(busId, status);
    if (result.success) {
      showToast(`Bus ${busId} status set to ${status}.`, 'info');
      await loadSimulationState();
    }
    return result;
  };

  const handleCreateDemand = async (pickup, dest, pax) => {
    const result = await createDemandRequest(pickup, dest, pax);
    if (result.success) {
      await loadSimulationState();
    }
    return result;
  };

  const handleTriggerBreakdown = async (busId) => {
    const result = await triggerBusBreakdown(busId);
    if (result.success) {
      await loadSimulationState();
    }
    return result;
  };

  const handleRestoreBus = async (busId) => {
    const result = await restoreBus(busId);
    if (result.success) {
      await loadSimulationState();
    }
    return result;
  };

  const stops = simState?.stops || [];
  const roads = simState?.roads || [];
  const buses = simState?.buses || [];
  const trips = simState?.trips || [];
  const pendingDemands = simState?.pendingDemands || [];
  const summary = simState?.summary || {};

  return (
    <div className="app-layout">
      {/* Top Navbar */}
      <Navbar
        activeTab={activeTab}
        onSelectTab={setActiveTab}
        healthState={healthState}
        onRefresh={() => {
          checkHealth();
          loadSimulationState();
        }}
        onNavigateHome={onNavigateHome}
      />

      {/* Floating Toast Notification */}
      {toastMessage && (
        <div className={`toast-alert toast-${toastMessage.type}`}>
          <span className="toast-dot" />
          <span className="toast-content">{toastMessage.message}</span>
          <button className="toast-close" onClick={() => setToastMessage(null)}>×</button>
        </div>
      )}

      {/* Main Content Area */}
      <main className="main-content">
        {simError && (
          <div className="error-banner">
            <AlertCircle size={20} className="error-icon text-red-500" />
            <span>{simError}</span>
            <button className="retry-btn" onClick={loadSimulationState}>Retry</button>
          </div>
        )}

        {/* Tab 1: Overview / Role Hub */}
        {activeTab === 'overview' && (
          role === 'PASSENGER' ? (
            <PassengerHub
              simState={simState}
              onNavigateToRoute={() => setActiveTab('routing')}
            />
          ) : role === 'DRIVER' ? (
            <DriverCockpit
              simState={simState}
              onCompleteTrip={handleCompleteSpecificTrip}
              onUpdateStatus={handleUpdateBusStatus}
              onRestoreBus={handleRestoreBus}
            />
          ) : (
            <OperationsOverview
              summary={summary}
              buses={buses}
              trips={trips}
              demands={pendingDemands}
              onRunSimulation={handleRunSimulation}
              onResetSimulation={handleResetSimulation}
              onCompleteAllTrips={handleCompleteAllTrips}
              onSelectTab={setActiveTab}
              simLoading={simLoading}
              canReset={role === 'ADMIN'}
            />
          )
        )}

        {/* Tab 2: Network Map */}
        {activeTab === 'network' && (
          <TransportationMap
            stops={stops}
            roads={roads}
            buses={buses}
            trips={trips}
            highlightPath={highlightedPath}
            onSelectStop={(stopId) => showToast(`Station selected: ${stopId}`)}
            onSelectRoad={() => {
              if (['ADMIN', 'TRANSPORT_MANAGER'].includes(role)) {
                setActiveTab('traffic');
              } else {
                showToast('Traffic configuration console requires Transport Manager or Admin role.', 'warning');
              }
            }}
          />
        )}

        {/* Tab 3: Route Optimization Lab */}
        {activeTab === 'routing' && (
          <RouteOptimizationLab
            stops={stops}
            onHighlightRoute={(stopIds) => {
              setHighlightedPath(stopIds);
              showToast(`Highlighted route: ${stopIds.join(' → ')} on Map`, 'info');
            }}
          />
        )}

        {/* Tab 4: Traffic Console (Admin & Transport Manager only) */}
        {activeTab === 'traffic' && (
          ['ADMIN', 'TRANSPORT_MANAGER'].includes(role) ? (
            <TrafficConsole
              roads={roads}
              onUpdateTravelTime={handleUpdateRoadTravelTime}
              onUpdateRoadStatus={handleUpdateRoadStatus}
              onResetRoads={handleResetSimulation}
            />
          ) : (
            <AccessDenied onReturn={() => setActiveTab('overview')} requiredRole="TRANSPORT_MANAGER" />
          )
        )}

        {/* Tab 5: Fleet & Demand Console (Admin & Transport Manager only) */}
        {activeTab === 'fleet' && (
          ['ADMIN', 'TRANSPORT_MANAGER'].includes(role) ? (
            <FleetDemandManager
              buses={buses}
              stops={stops}
              demands={pendingDemands}
              trips={trips}
              onUpdateBusStatus={handleUpdateBusStatus}
              onCreateDemand={handleCreateDemand}
              onCompleteTrip={handleCompleteSpecificTrip}
              onCompleteAllTrips={handleCompleteAllTrips}
              onTriggerBreakdown={handleTriggerBreakdown}
              onRestoreBus={handleRestoreBus}
            />
          ) : (
            <AccessDenied onReturn={() => setActiveTab('overview')} requiredRole="TRANSPORT_MANAGER" />
          )
        )}

        {/* Tab 6: Analytics & Telemetry (Admin & Transport Manager only) */}
        {activeTab === 'analytics' && (
          ['ADMIN', 'TRANSPORT_MANAGER'].includes(role) ? (
            <AnalyticsDashboard />
          ) : (
            <AccessDenied onReturn={() => setActiveTab('overview')} requiredRole="TRANSPORT_MANAGER" />
          )
        )}

        {/* Tab 7: User Management (Admin only) */}
        {activeTab === 'users' && (
          role === 'ADMIN' ? (
            <UserManagement />
          ) : (
            <AccessDenied onReturn={() => setActiveTab('overview')} requiredRole="ADMIN" />
          )
        )}
      </main>

      {/* Footer */}
      <footer className="app-footer">
        <div>
          <strong>Smart Public Transportation Route Optimization</strong> — Vetrinagar Urban Transit Network
        </div>
        <div className="footer-meta">
          <span>Logged in as: <strong>{user?.fullName}</strong> ({role}) &bull; Active Role-Based Session</span>
        </div>
      </footer>
    </div>
  );
}

function AppContent() {
  const { isAuthenticated, loading: authLoading } = useAuth();
  const [currentPath, setCurrentPath] = useState(() => {
    return window.location.pathname || '/';
  });

  const navigate = useCallback((targetPath) => {
    if (window.location.pathname !== targetPath) {
      window.history.pushState({}, '', targetPath);
    }
    setCurrentPath(targetPath);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  useEffect(() => {
    const handlePopState = () => {
      setCurrentPath(window.location.pathname || '/');
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  // When auth state settles, if authenticated user is on /login, forward to /dashboard
  useEffect(() => {
    if (!authLoading && isAuthenticated && currentPath === '/login') {
      navigate('/dashboard');
    }
  }, [authLoading, isAuthenticated, currentPath, navigate]);

  // Initial token verification screen
  if (authLoading) {
    return (
      <div className="full-screen-loader">
        <div className="loader-box">
          <Loader2 size={36} className="animate-spin text-indigo-600 mx-auto" />
          <h2 className="loader-title">SmartTransit Portal</h2>
          <p className="loader-sub">Verifying security credentials...</p>
        </div>
      </div>
    );
  }

  // 1. Path is /login
  if (currentPath === '/login') {
    if (isAuthenticated) {
      return <AuthenticatedDashboard onNavigateHome={() => navigate('/')} />;
    }
    return (
      <LoginPage
        onNavigateHome={() => navigate('/')}
        onLoginSuccess={() => navigate('/dashboard')}
      />
    );
  }

  // 2. Path is /dashboard
  if (currentPath === '/dashboard') {
    if (!isAuthenticated) {
      return (
        <LoginPage
          onNavigateHome={() => navigate('/')}
          onLoginSuccess={() => navigate('/dashboard')}
        />
      );
    }
    return <AuthenticatedDashboard onNavigateHome={() => navigate('/')} />;
  }

  // 3. Path is / (or any other path) -> Public Landing Dashboard
  return (
    <LandingPage
      onNavigate={navigate}
      onLogin={() => navigate('/login')}
      onGoToDashboard={() => navigate('/dashboard')}
    />
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
