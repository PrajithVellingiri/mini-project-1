import { useState } from 'react';
import { useAuth, DEMO_ACCOUNTS } from '../../context/AuthContext';
import {
  ShieldCheck,
  Navigation,
  BusFront,
  SlidersHorizontal,
  Lock,
  Mail,
  ArrowRight,
  ArrowLeft,
  AlertCircle,
  Loader2,
  Sparkles,
} from 'lucide-react';

export default function LoginPage({ onNavigateHome, onLoginSuccess }) {
  const { login, quickLogin } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [quickLoadingRole, setQuickLoadingRole] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      setError('Please provide both email and password.');
      return;
    }

    setError(null);
    setLoading(true);
    const result = await login(email, password);
    if (result.success) {
      if (onLoginSuccess) onLoginSuccess();
    } else {
      setError(result.error || 'Authentication failed. Please verify credentials.');
    }
    setLoading(false);
  };

  const handleQuickLogin = async (roleKey) => {
    setError(null);
    setQuickLoadingRole(roleKey);
    const result = await quickLogin(roleKey);
    if (result.success) {
      if (onLoginSuccess) onLoginSuccess();
    } else {
      setError(result.error || `Quick login failed for ${roleKey}.`);
    }
    setQuickLoadingRole(null);
  };

  const getRoleIcon = (iconName) => {
    switch (iconName) {
      case 'ShieldCheck':
        return <ShieldCheck className="role-card-icon text-indigo-600" size={24} />;
      case 'Navigation':
        return <Navigation className="role-card-icon text-blue-600" size={24} />;
      case 'BusFront':
        return <BusFront className="role-card-icon text-amber-600" size={24} />;
      case 'SlidersHorizontal':
        return <SlidersHorizontal className="role-card-icon text-emerald-600" size={24} />;
      default:
        return <Lock className="role-card-icon text-indigo-600" size={24} />;
    }
  };

  return (
    <div className="login-page-container">
      <div className="login-card-wrapper">
        {onNavigateHome && (
          <div className="login-back-wrapper">
            <button
              type="button"
              onClick={onNavigateHome}
              className="login-back-home-btn"
            >
              <ArrowLeft size={16} />
              <span>Back to Homepage</span>
            </button>
          </div>
        )}

        {/* Header / Brand identity */}
        <div className="login-header">
          <div className="login-brand-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="2" y="5" width="20" height="13" rx="3" />
              <path d="M4 18v2a1 1 0 0 0 1 1h1" />
              <path d="M18 21h1a1 1 0 0 0 1-1v-2" />
              <circle cx="7" cy="15" r="1.5" />
              <circle cx="17" cy="15" r="1.5" />
              <path d="M2 11h20" />
            </svg>
          </div>
          <h1 className="login-title">
            <span className="brand-highlight">Smart</span>Transit Portal
          </h1>
          <p className="login-subtitle">
            Public Transportation Route Optimization & Operations Management
          </p>
        </div>

        {/* Error notification */}
        {error && (
          <div className="login-alert login-alert-error">
            <AlertCircle size={18} className="alert-icon" />
            <span>{error}</span>
          </div>
        )}

        {/* Standard Credentials Form */}
        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label className="form-label">Email Address</label>
            <div className="input-with-icon">
              <Mail className="input-icon" size={18} />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@smarttransport.demo"
                required
                className="form-input"
                autoComplete="email"
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <div className="input-with-icon">
              <Lock className="input-icon" size={18} />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••••"
                required
                className="form-input"
                autoComplete="current-password"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading || Boolean(quickLoadingRole)}
            className="login-submit-btn"
          >
            {loading ? (
              <>
                <Loader2 size={18} className="animate-spin" />
                <span>Authenticating...</span>
              </>
            ) : (
              <>
                <span>Sign In to System</span>
                <ArrowRight size={18} />
              </>
            )}
          </button>
        </form>

        {/* Quick Login — Review Demo Accounts Section */}
        <div className="quick-login-divider">
          <span className="divider-line" />
          <span className="divider-text">
            <Sparkles size={14} className="sparkle-icon" />
            Quick Login — Review Demo Accounts
          </span>
          <span className="divider-line" />
        </div>

        <p className="quick-login-hint">
          Select any verified review account to test role-specific workflows and RBAC permissions:
        </p>

        <div className="quick-login-grid">
          {Object.entries(DEMO_ACCOUNTS).map(([key, demo]) => {
            const isThisLoading = quickLoadingRole === key;
            return (
              <div key={key} className="role-demo-card">
                <div className="role-card-header">
                  <div className="role-icon-container">
                    {getRoleIcon(demo.iconName)}
                  </div>
                  <div>
                    <h3 className="role-card-title">{demo.roleName}</h3>
                    <span className="role-card-email">{demo.email}</span>
                  </div>
                </div>

                <p className="role-card-desc">{demo.description}</p>

                <button
                  type="button"
                  onClick={() => handleQuickLogin(key)}
                  disabled={loading || Boolean(quickLoadingRole)}
                  className="role-quick-login-btn"
                >
                  {isThisLoading ? (
                    <>
                      <Loader2 size={15} className="animate-spin" />
                      <span>Signing in...</span>
                    </>
                  ) : (
                    <>
                      <span>Quick Login as {demo.roleName.split(' ')[0]}</span>
                      <ArrowRight size={14} />
                    </>
                  )}
                </button>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
