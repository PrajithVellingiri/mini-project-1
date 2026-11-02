export default function StatCard({ title, value, subtitle, icon, highlight = false, badgeText, badgeVariant }) {
  return (
    <div className={`stat-card ${highlight ? 'stat-card-highlight' : ''}`}>
      <div className="stat-card-header">
        <span className="stat-card-title">{title}</span>
        {icon && <div className="stat-card-icon">{icon}</div>}
      </div>
      <div className="stat-card-body">
        <div className="stat-card-value">{value}</div>
        {badgeText && (
          <span className={`stat-card-badge badge-${badgeVariant || 'orange'}`}>
            {badgeText}
          </span>
        )}
      </div>
      {subtitle && <div className="stat-card-subtitle">{subtitle}</div>}
    </div>
  );
}
