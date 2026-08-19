export type MessageType =
  | 'CODE_UPDATE'
  | 'SYNC'
  | 'ERROR'
  | 'USER_JOINED'
  | 'USER_LEFT'
  | 'PRESENCE'
  | 'LANGUAGE_UPDATE'
  | 'FILE_CREATE'
  | 'FILE_DELETE';

export interface CodeMessage {
  type: MessageType;
  content?: string | null;
  username?: string | null;
  users?: string[] | null;
  language?: string | null;
  path?: string | null;
  paths?: string[] | null;
  files?: Record<string, string> | null;
}

export interface Room {
  roomId: string;
  code: string;
}

export interface RoomSummary {
  roomId: string;
  updatedAt: string;
  preview: string;
}

export interface AuthResult {
  token: string;
  username: string;
}

const API_BASE = '';
const TOKEN_KEY = 'codesync_token';
const USERNAME_KEY = 'codesync_username';

export function getStoredAuth(): AuthResult | null {
  const token = localStorage.getItem(TOKEN_KEY);
  const username = localStorage.getItem(USERNAME_KEY);
  if (!token || !username) {
    return null;
  }
  return { token, username };
}

export function storeAuth(auth: AuthResult) {
  localStorage.setItem(TOKEN_KEY, auth.token);
  localStorage.setItem(USERNAME_KEY, auth.username);
}

export function clearStoredAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USERNAME_KEY);
}

async function handleJsonResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      if (body?.error) {
        message = body.error;
      }
    } catch {
      // ignore body parse failure, use default message
    }
    throw new Error(message);
  }
  return response.json();
}

function authHeaders(token: string | null): HeadersInit {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function register(username: string, password: string): Promise<AuthResult> {
  const response = await fetch(`${API_BASE}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  return handleJsonResponse<AuthResult>(response);
}

export async function login(username: string, password: string): Promise<AuthResult> {
  const response = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  return handleJsonResponse<AuthResult>(response);
}

export async function logout(token: string): Promise<void> {
  await fetch(`${API_BASE}/auth/logout`, {
    method: 'POST',
    headers: authHeaders(token),
  });
}

export async function createRoom(token: string | null): Promise<Room> {
  const response = await fetch(`${API_BASE}/room/create`, {
    method: 'POST',
    headers: authHeaders(token),
  });
  return handleJsonResponse<Room>(response);
}

export async function getRoom(roomId: string): Promise<Room> {
  const response = await fetch(`${API_BASE}/room/${encodeURIComponent(roomId)}`);
  return handleJsonResponse<Room>(response);
}

export interface ExecuteResult {
  stdout: string;
  stderr: string;
  output: string;
  exitCode: number;
  language: string;
  version: string;
}

export async function executeCode(
  language: string,
  code: string,
  stdin = '',
  path?: string | null,
  args: string[] = [],
): Promise<ExecuteResult> {
  const payload: any = { language, code, stdin };
  if (path) {
    payload.path = path;
  }
  if (args.length > 0) {
    payload.args = args;
  }

  const response = await fetch(`${API_BASE}/execute`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return handleJsonResponse<ExecuteResult>(response);
}

export async function getMyRooms(token: string): Promise<RoomSummary[]> {
  const response = await fetch(`${API_BASE}/rooms/mine`, {
    headers: authHeaders(token),
  });
  return handleJsonResponse<RoomSummary[]>(response);
}
