export default function Badge({ children, variant = 'default', size = 'md' }) {
  const variantStyles = {
    default: 'badge-default',
    available: 'badge-available',
    transit: 'badge-transit',
    warning: 'badge-warning',
    danger: 'badge-danger',
    orange: 'badge-orange',
    narrow: 'badge-narrow',
  };

  return (
    <span className={`custom-badge ${variantStyles[variant] || 'badge-default'} badge-${size}`}>
      {children}
    </span>
  );
}
