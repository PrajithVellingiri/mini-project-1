import { ShieldAlert, ArrowLeft } from 'lucide-react';

export default function AccessDenied({ onReturn, requiredRole }) {
  return (
    <div className="access-denied-card">
      <div className="access-denied-icon">
        <ShieldAlert size={48} className="text-red-500 mx-auto" />
      </div>
      <h2 className="access-denied-title">Access Restricted</h2>
      <p className="access-denied-message">
        Your authenticated role does not have authorization to access this operational module.
        {requiredRole && ` This view requires ${requiredRole.replace('_', ' ')} privileges.`}
      </p>
      <button onClick={onReturn} className="btn-primary mt-4">
        <ArrowLeft size={16} />
        <span>Return to Dashboard</span>
      </button>
    </div>
  );
}
