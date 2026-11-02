import { useState, useEffect, useCallback } from 'react';
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
import Navbar from './components/common/Navbar';
import OperationsOverview from './components/overview/OperationsOverview';
import TransportationMap from './components/network/TransportationMap';
import RouteOptimizationLab from './components/routing/RouteOptimizationLab';
import TrafficConsole from './components/traffic/TrafficConsole';
import FleetDemandManager from './components/fleet/FleetDemandManager';
import AnalyticsDashboard from './components/analytics/AnalyticsDashboard';
import './App.css';

export default function App() {
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
    setSimLoading(true);
    setSimError(null);
    const result = await fetchSimulationState();
    if (result.success) {
      setSimState(result.data);
    } else {
      setSimError(result.error);
    }
    setSimLoading(false);
  }, []);

  useEffect(() => {
    checkHealth();
    loadSimulationState();
  }, [checkHealth, loadSimulationState]);

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
            <span className="error-icon">⚠️</span>
            <span>{simError}</span>
            <button className="retry-btn" onClick={loadSimulationState}>Retry</button>
          </div>
        )}

        {activeTab === 'overview' && (
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
          />
        )}

        {activeTab === 'network' && (
          <TransportationMap
            stops={stops}
            roads={roads}
            buses={buses}
            trips={trips}
            highlightPath={highlightedPath}
            onSelectStop={(stopId) => showToast(`Station selected: ${stopId}`)}
            onSelectRoad={(road) => {
              setActiveTab('traffic');
            }}
          />
        )}

        {activeTab === 'routing' && (
          <RouteOptimizationLab
            stops={stops}
            onHighlightRoute={(stopIds) => {
              setHighlightedPath(stopIds);
              showToast(`Highlighted route: ${stopIds.join(' → ')} on Map`, 'info');
            }}
          />
        )}

        {activeTab === 'traffic' && (
          <TrafficConsole
            roads={roads}
            onUpdateTravelTime={handleUpdateRoadTravelTime}
            onUpdateRoadStatus={handleUpdateRoadStatus}
            onResetRoads={handleResetSimulation}
          />
        )}

        {activeTab === 'fleet' && (
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
        )}

        {activeTab === 'analytics' && (
          <AnalyticsDashboard />
        )}
      </main>

      {/* Footer */}
      <footer className="app-footer">
        <div>
          <strong>Smart Public Transportation Route Optimization</strong> — Vetrinagar Urban Transit Network
        </div>
        <div className="footer-meta">
          <span>Java 21 &bull; Spring Boot 4.1.1 &bull; React 19 &bull; MySQL 8.0.46</span>
        </div>
      </footer>
    </div>
  );
}
