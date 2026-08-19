import { useState, type FormEvent } from 'react';

interface AuthPanelProps {
  onLogin: (username: string, password: string) => Promise<boolean>;
  onRegister: (username: string, password: string) => Promise<boolean>;
  loading: boolean;
  error: string | null;
  onDismiss: () => void;
}

export default function AuthPanel({ onLogin, onRegister, loading, error, onDismiss }: AuthPanelProps) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const success = mode === 'login' ? await onLogin(username, password) : await onRegister(username, password);
    if (success) {
      setPassword('');
    }
  };

  return (
    <div className="auth-panel">
      <div className="auth-tabs">
        <button
          type="button"
          className={mode === 'login' ? 'auth-tab active' : 'auth-tab'}
          onClick={() => setMode('login')}
        >
          Log in
        </button>
        <button
          type="button"
          className={mode === 'register' ? 'auth-tab active' : 'auth-tab'}
          onClick={() => setMode('register')}
        >
          Create account
        </button>
        <button type="button" className="auth-dismiss" onClick={onDismiss}>
          Continue as guest
        </button>
      </div>

      <form className="auth-form" onSubmit={handleSubmit}>
        <input
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          placeholder="Username"
          autoComplete="username"
        />
        <input
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="Password"
          type="password"
          autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Please wait…' : mode === 'login' ? 'Log in' : 'Create account'}
        </button>
      </form>

      {error && <div className="auth-error">{error}</div>}
    </div>
  );
}
