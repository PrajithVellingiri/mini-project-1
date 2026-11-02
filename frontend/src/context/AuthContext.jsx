import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import {
  login as apiLogin,
  fetchCurrentUser,
  logoutUser,
  setAuthToken,
  getAuthToken,
  setUnauthorizedHandler,
} from '../api/client';

const AuthContext = createContext(null);

export const DEMO_ACCOUNTS = {
  ADMIN: {
    roleName: 'Administrator',
    roleKey: 'ADMIN',
    email: 'admin@smarttransport.demo',
    password: 'Review@2026',
    description: 'Full system control, simulation reset, user management & fleet governance.',
    iconName: 'ShieldCheck',
    badgeColor: 'badge-purple',
  },
  PASSENGER: {
    roleName: 'Passenger',
    roleKey: 'PASSENGER',
    email: 'passenger@smarttransport.demo',
    password: 'Review@2026',
    description: 'Route search, bus schedules, arrival times & active transit alerts.',
    iconName: 'Navigation',
    badgeColor: 'badge-blue',
  },
  DRIVER: {
    roleName: 'Bus Driver',
    roleKey: 'DRIVER',
    email: 'driver@smarttransport.demo',
    password: 'Review@2026',
    description: 'Assigned bus cockpit, active trip manifest, route stops & status updates.',
    iconName: 'BusFront',
    badgeColor: 'badge-amber',
  },
  TRANSPORT_MANAGER: {
    roleName: 'Transport Manager',
    roleKey: 'TRANSPORT_MANAGER',
    email: 'manager@smarttransport.demo',
    password: 'Review@2026',
    description: 'Fleet monitoring, dispatch management, traffic congestion & analytics.',
    iconName: 'SlidersHorizontal',
    badgeColor: 'badge-emerald',
  },
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setTokenState] = useState(() => getAuthToken());
  const [loading, setLoading] = useState(true);

  const logout = useCallback(async () => {
    await logoutUser();
    setTokenState(null);
    setUser(null);
  }, []);

  // Listen to 401s from client.js
  useEffect(() => {
    setUnauthorizedHandler(() => {
      logout();
    });
  }, [logout]);

  // Initial user profile hydration from persistent token
  useEffect(() => {
    async function initAuth() {
      const savedToken = getAuthToken();
      if (!savedToken) {
        setLoading(false);
        return;
      }

      const result = await fetchCurrentUser();
      if (result.success && result.data) {
        setUser(result.data);
      } else {
        // Token expired or invalid
        setAuthToken(null);
        setTokenState(null);
        setUser(null);
      }
      setLoading(false);
    }

    initAuth();
  }, []);

  const login = useCallback(async (email, password) => {
    const result = await apiLogin(email, password);
    if (result.success) {
      setTokenState(result.data.token);
      setUser(result.data.user);
      return { success: true };
    }
    return { success: false, error: result.error };
  }, []);

  const quickLogin = useCallback(async (roleKey) => {
    const demo = DEMO_ACCOUNTS[roleKey];
    if (!demo) {
      return { success: false, error: `Unknown demo role: ${roleKey}` };
    }
    return login(demo.email, demo.password);
  }, [login]);

  const hasRole = useCallback((requiredRoles) => {
    if (!user) return false;
    if (Array.isArray(requiredRoles)) {
      return requiredRoles.includes(user.role);
    }
    return user.role === requiredRoles;
  }, [user]);

  const value = {
    user,
    token,
    role: user?.role || null,
    isAuthenticated: Boolean(user && token),
    loading,
    login,
    quickLogin,
    logout,
    hasRole,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
