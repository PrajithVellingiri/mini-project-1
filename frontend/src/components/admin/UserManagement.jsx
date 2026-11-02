import { useState, useEffect, useCallback } from 'react';
import {
  fetchUsers,
  createUser,
  updateUser,
  toggleUserStatus,
  resetUserPassword,
} from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import {
  UserPlus,
  Search,
  KeyRound,
  Edit3,
  UserCheck,
  UserX,
  Shield,
  AlertCircle,
  CheckCircle2,
  X,
  Loader2,
  RefreshCw,
} from 'lucide-react';

const ROLES = ['ADMIN', 'PASSENGER', 'DRIVER', 'TRANSPORT_MANAGER'];

export default function UserManagement() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  // Modals state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [resettingUser, setResettingUser] = useState(null);

  // Create form state
  const [createForm, setCreateForm] = useState({
    fullName: '',
    email: '',
    password: '',
    role: 'PASSENGER',
    enabled: true,
  });

  // Edit form state
  const [editForm, setEditForm] = useState({
    fullName: '',
    email: '',
    role: 'PASSENGER',
    enabled: true,
  });

  // Reset password state
  const [newPassword, setNewPassword] = useState('');

  const loadUsersList = useCallback(async () => {
    setLoading(true);
    setActionError(null);
    const result = await fetchUsers(search, roleFilter);
    if (result.success) {
      setUsers(result.data);
    } else {
      setActionError(result.error);
    }
    setLoading(false);
  }, [search, roleFilter]);

  useEffect(() => {
    loadUsersList();
  }, [loadUsersList]);

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    setActionError(null);
    setActionSuccess(null);

    const result = await createUser(createForm);
    if (result.success) {
      setActionSuccess(`User account '${createForm.fullName}' created successfully.`);
      setShowCreateModal(false);
      setCreateForm({ fullName: '', email: '', password: '', role: 'PASSENGER', enabled: true });
      loadUsersList();
    } else {
      setActionError(result.error);
    }
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    if (!editingUser) return;
    setActionError(null);
    setActionSuccess(null);

    const result = await updateUser(editingUser.id, editForm);
    if (result.success) {
      setActionSuccess(`User '${editForm.fullName}' updated successfully.`);
      setEditingUser(null);
      loadUsersList();
    } else {
      setActionError(result.error);
    }
  };

  const handleToggleStatus = async (user) => {
    setActionError(null);
    setActionSuccess(null);
    const newStatus = !user.enabled;

    const result = await toggleUserStatus(user.id, newStatus);
    if (result.success) {
      setActionSuccess(`User '${user.fullName}' has been ${newStatus ? 'enabled' : 'disabled'}.`);
      loadUsersList();
    } else {
      setActionError(result.error);
    }
  };

  const handleResetPasswordSubmit = async (e) => {
    e.preventDefault();
    if (!resettingUser) return;
    setActionError(null);
    setActionSuccess(null);

    const result = await resetUserPassword(resettingUser.id, newPassword);
    if (result.success) {
      setActionSuccess(`Password reset successfully for ${resettingUser.fullName}.`);
      setResettingUser(null);
      setNewPassword('');
    } else {
      setActionError(result.error);
    }
  };

  const getRoleBadgeClass = (role) => {
    switch (role) {
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
    <div className="user-management-container">
      {/* Top Header */}
      <div className="section-header">
        <div>
          <h2 className="section-title">
            <Shield size={22} className="inline-icon text-indigo-600" />
            User & Role Governance
          </h2>
          <p className="section-subtitle">
            Admin console to manage registered user accounts, assign RBAC roles, and control access permissions.
          </p>
        </div>
        <div className="header-actions">
          <button onClick={loadUsersList} className="btn-secondary" title="Refresh user list">
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>
          <button onClick={() => setShowCreateModal(true)} className="btn-primary">
            <UserPlus size={16} />
            <span>Add New User</span>
          </button>
        </div>
      </div>

      {/* Alerts */}
      {actionError && (
        <div className="login-alert login-alert-error mb-4">
          <AlertCircle size={18} className="alert-icon" />
          <span>{actionError}</span>
        </div>
      )}
      {actionSuccess && (
        <div className="login-alert login-alert-success mb-4">
          <CheckCircle2 size={18} className="alert-icon" />
          <span>{actionSuccess}</span>
        </div>
      )}

      {/* Filter Toolbar */}
      <div className="filters-bar">
        <div className="search-input-group">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name or email..."
            className="filter-search-input"
          />
        </div>

        <div className="role-filter-group">
          <label className="filter-label">Filter Role:</label>
          <select
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
            className="filter-select"
          >
            <option value="ALL">All Roles ({users.length})</option>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {r.replace('_', ' ')}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Users Table */}
      <div className="table-responsive">
        <table className="custom-table">
          <thead>
            <tr>
              <th>User</th>
              <th>Email</th>
              <th>Assigned Role</th>
              <th>Status</th>
              <th>Account Created</th>
              <th className="text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading && users.length === 0 ? (
              <tr>
                <td colSpan="6" className="text-center py-8">
                  <Loader2 size={24} className="animate-spin text-indigo-600 mx-auto" />
                  <p className="mt-2 text-sm text-gray-500">Loading user database...</p>
                </td>
              </tr>
            ) : users.length === 0 ? (
              <tr>
                <td colSpan="6" className="text-center py-8 text-gray-500">
                  No matching user accounts found.
                </td>
              </tr>
            ) : (
              users.map((u) => {
                const isSelf = currentUser && currentUser.id === u.id;
                return (
                  <tr key={u.id} className={!u.enabled ? 'row-disabled' : ''}>
                    <td>
                      <div className="user-name-cell">
                        <span className="font-semibold text-gray-900">{u.fullName}</span>
                        {isSelf && <span className="self-tag">(You)</span>}
                      </div>
                    </td>
                    <td className="text-gray-600 font-mono text-xs">{u.email}</td>
                    <td>
                      <span className={`role-badge ${getRoleBadgeClass(u.role)}`}>
                        {u.role.replace('_', ' ')}
                      </span>
                    </td>
                    <td>
                      <span className={`status-pill ${u.enabled ? 'status-active' : 'status-disabled'}`}>
                        {u.enabled ? 'Active' : 'Disabled'}
                      </span>
                    </td>
                    <td className="text-gray-500 text-xs">
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : 'Baseline'}
                    </td>
                    <td className="text-right">
                      <div className="table-action-buttons">
                        <button
                          onClick={() => {
                            setEditingUser(u);
                            setEditForm({
                              fullName: u.fullName,
                              email: u.email,
                              role: u.role,
                              enabled: u.enabled,
                            });
                          }}
                          className="action-btn action-edit"
                          title="Edit user details"
                        >
                          <Edit3 size={15} />
                        </button>

                        <button
                          onClick={() => setResettingUser(u)}
                          className="action-btn action-key"
                          title="Reset password"
                        >
                          <KeyRound size={15} />
                        </button>

                        <button
                          onClick={() => handleToggleStatus(u)}
                          disabled={isSelf && u.enabled}
                          className={`action-btn ${u.enabled ? 'action-disable' : 'action-enable'}`}
                          title={isSelf ? 'Cannot disable your own account' : u.enabled ? 'Disable user account' : 'Enable user account'}
                        >
                          {u.enabled ? <UserX size={15} /> : <UserCheck size={15} />}
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Modal: Create User */}
      {showCreateModal && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <div className="modal-header">
              <h3 className="modal-title">Create New User Account</h3>
              <button onClick={() => setShowCreateModal(false)} className="modal-close-btn">
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleCreateSubmit} className="modal-form">
              <div className="form-group">
                <label className="form-label">Full Name</label>
                <input
                  type="text"
                  required
                  value={createForm.fullName}
                  onChange={(e) => setCreateForm({ ...createForm, fullName: e.target.value })}
                  placeholder="e.g. Ramesh Kumar"
                  className="form-input"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Email Address</label>
                <input
                  type="email"
                  required
                  value={createForm.email}
                  onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                  placeholder="user@smarttransport.demo"
                  className="form-input"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Temporary Password</label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={createForm.password}
                  onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                  placeholder="Minimum 6 characters"
                  className="form-input"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Role Assignment</label>
                <select
                  value={createForm.role}
                  onChange={(e) => setCreateForm({ ...createForm, role: e.target.value })}
                  className="form-input"
                >
                  {ROLES.map((r) => (
                    <option key={r} value={r}>
                      {r.replace('_', ' ')}
                    </option>
                  ))}
                </select>
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="btn-secondary"
                >
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Create User
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Edit User */}
      {editingUser && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <div className="modal-header">
              <h3 className="modal-title">Edit User: {editingUser.fullName}</h3>
              <button onClick={() => setEditingUser(null)} className="modal-close-btn">
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleEditSubmit} className="modal-form">
              <div className="form-group">
                <label className="form-label">Full Name</label>
                <input
                  type="text"
                  required
                  value={editForm.fullName}
                  onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })}
                  className="form-input"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Email Address</label>
                <input
                  type="email"
                  required
                  value={editForm.email}
                  onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                  className="form-input"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Role Assignment</label>
                <select
                  value={editForm.role}
                  onChange={(e) => setEditForm({ ...editForm, role: e.target.value })}
                  className="form-input"
                >
                  {ROLES.map((r) => (
                    <option key={r} value={r}>
                      {r.replace('_', ' ')}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={editForm.enabled}
                    onChange={(e) => setEditForm({ ...editForm, enabled: e.target.checked })}
                  />
                  <span>Account Active & Enabled</span>
                </label>
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  onClick={() => setEditingUser(null)}
                  className="btn-secondary"
                >
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Reset Password */}
      {resettingUser && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <div className="modal-header">
              <h3 className="modal-title">Reset Password: {resettingUser.fullName}</h3>
              <button onClick={() => setResettingUser(null)} className="modal-close-btn">
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleResetPasswordSubmit} className="modal-form">
              <p className="text-sm text-gray-600 mb-4">
                Enter a new secure password for <strong>{resettingUser.email}</strong>. The user will be required to authenticate with this new credential.
              </p>

              <div className="form-group">
                <label className="form-label">New Password</label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Minimum 6 characters"
                  className="form-input"
                />
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  onClick={() => setResettingUser(null)}
                  className="btn-secondary"
                >
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Update Password
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
