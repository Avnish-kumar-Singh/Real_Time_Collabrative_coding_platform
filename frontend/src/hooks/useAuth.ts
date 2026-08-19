import { useCallback, useState } from 'react';
import * as api from '../services/api';
import type { AuthResult } from '../services/api';

export function useAuth() {
  const [auth, setAuth] = useState<AuthResult | null>(() => api.getStoredAuth());
  const [authError, setAuthError] = useState<string | null>(null);
  const [authLoading, setAuthLoading] = useState(false);

  const doRegister = useCallback(async (username: string, password: string) => {
    setAuthLoading(true);
    setAuthError(null);
    try {
      const result = await api.register(username, password);
      api.storeAuth(result);
      setAuth(result);
      return true;
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Registration failed');
      return false;
    } finally {
      setAuthLoading(false);
    }
  }, []);

  const doLogin = useCallback(async (username: string, password: string) => {
    setAuthLoading(true);
    setAuthError(null);
    try {
      const result = await api.login(username, password);
      api.storeAuth(result);
      setAuth(result);
      return true;
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Login failed');
      return false;
    } finally {
      setAuthLoading(false);
    }
  }, []);

  const doLogout = useCallback(async () => {
    if (auth) {
      try {
        await api.logout(auth.token);
      } catch {
        // best-effort — clear local state regardless
      }
    }
    api.clearStoredAuth();
    setAuth(null);
  }, [auth]);

  return { auth, authError, authLoading, doRegister, doLogin, doLogout, setAuthError };
}
