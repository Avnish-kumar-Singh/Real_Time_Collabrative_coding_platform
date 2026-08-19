import { useEffect, useState } from 'react';
import Editor from '@monaco-editor/react';
import { createRoom } from './services/api';
import { useCodeSync } from './hooks/useCodeSync';
import { useAuth } from './hooks/useAuth';
import AuthPanel from './components/AuthPanel';
import MyRooms from './components/MyRooms';
import RunPanel from './components/RunPanel';
import ScreenShare from './components/ScreenShare';
import FileExplorer from './components/FileExplorer';
import Logo from './components/Logo';
import { languageFromPath, monacoLanguageFor } from './utils/language';
import './App.css';

function randomGuestName() {
  return `Guest-${Math.floor(1000 + Math.random() * 9000)}`;
}

export default function App() {
  const { auth, authError, authLoading, doRegister, doLogin, doLogout, setAuthError } = useAuth();

  const [roomId, setRoomId] = useState<string | null>(null);
  const [inputRoomId, setInputRoomId] = useState('');
  const [username, setUsername] = useState(randomGuestName());
  const [loading, setLoading] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [showAuthPanel, setShowAuthPanel] = useState(false);
  const [myRoomsRefreshKey, setMyRoomsRefreshKey] = useState(0);

  const {
    files,
    activeFile,
    setActiveFile,
    status,
    error,
    users,
    activity,
    sendCodeUpdate,
    createFile,
    deleteFile,
    isConnected,
  } = useCodeSync(roomId, username);

  // Language is derived from the active file's extension — no manual picker needed.
  // Falls back to JavaScript for files with an unrecognized/missing extension.
  const language = languageFromPath(activeFile) ?? 'javascript';

  useEffect(() => {
    document.title = roomId ? `CodeSync · ${roomId}` : 'CodeSync';
  }, [roomId]);

  useEffect(() => {
    if (auth) {
      setUsername(auth.username);
    }
  }, [auth]);

  const handleCreateRoom = async () => {
    setLoading(true);
    setFormError(null);
    try {
      const room = await createRoom(auth?.token ?? null);
      setRoomId(room.roomId);
      setInputRoomId(room.roomId);
      setMyRoomsRefreshKey((key) => key + 1);
    } catch {
      setFormError('Could not create room. Is the backend running on port 8080?');
    } finally {
      setLoading(false);
    }
  };

  const handleJoinRoom = () => {
    const trimmed = inputRoomId.trim();
    if (!trimmed) {
      setFormError('Enter a room ID.');
      return;
    }
    if (!username.trim()) {
      setFormError('Enter a display name.');
      return;
    }
    setFormError(null);
    setRoomId(trimmed);
  };

  const handleLeaveRoom = () => {
    setRoomId(null);
    setFormError(null);
  };

  const handleSelectSavedRoom = (savedRoomId: string) => {
    setInputRoomId(savedRoomId);
    setFormError(null);
    setRoomId(savedRoomId);
  };

  return (
    <div className="app">
      <header className="header">
        <div className="brand">
          <Logo className="brand-logo" />
          <div className="brand-text">
            <h1 className="brand-title">CodeSync</h1>
            <p className="brand-tagline">Real-time coding platform</p>
          </div>
        </div>
        <div className="header-right">
          {auth ? (
            <div className="account-pill">
              <span>Signed in as {auth.username}</span>
              <button type="button" className="link-button" onClick={doLogout}>
                Log out
              </button>
            </div>
          ) : (
            <button type="button" className="link-button" onClick={() => setShowAuthPanel((v) => !v)}>
              {showAuthPanel ? 'Hide login' : 'Log in / Sign up'}
            </button>
          )}
          <div className="status-pill" data-status={status}>
            <span className="status-dot" aria-hidden="true" />
            {roomId ? `Room ${roomId} · ${status}` : 'No room selected'}
          </div>
        </div>
      </header>

      {!auth && showAuthPanel && (
        <AuthPanel
          onLogin={doLogin}
          onRegister={doRegister}
          loading={authLoading}
          error={authError}
          onDismiss={() => {
            setAuthError(null);
            setShowAuthPanel(false);
          }}
        />
      )}

      <section className="toolbar">
        <button type="button" onClick={handleCreateRoom} disabled={loading}>
          {loading ? 'Creating…' : 'Create Room'}
        </button>
        <input
          value={inputRoomId}
          onChange={(event) => setInputRoomId(event.target.value)}
          placeholder="Room ID"
        />
        <input
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          placeholder="Your display name"
          maxLength={24}
        />
        <button type="button" onClick={handleJoinRoom}>
          Join Room
        </button>
        {roomId && (
          <button type="button" className="secondary" onClick={handleLeaveRoom}>
            Leave
          </button>
        )}
      </section>

      {(formError || error) && (
        <div className="error-banner">{formError ?? error}</div>
      )}

      {auth && !roomId && (
        <section className="my-rooms-panel">
          <h2>Your rooms</h2>
          <MyRooms token={auth.token} onSelectRoom={handleSelectSavedRoom} refreshKey={myRoomsRefreshKey} />
        </section>
      )}

      {roomId && (
        <section className="presence-bar">
          <span className="presence-label">Online ({users.length}):</span>
          <div className="presence-avatars">
            {users.map((user) => (
              <span key={user} className={`presence-chip${user === username ? ' self' : ''}`}>
                {user}
                {user === username ? ' (you)' : ''}
              </span>
            ))}
          </div>
          {activity.length > 0 && (
            <div className="activity-feed">
              {activity.map((entry) => (
                <span key={entry.id} className="activity-item">
                  {entry.text}
                </span>
              ))}
            </div>
          )}
        </section>
      )}

      <section className="workspace">
        <FileExplorer
          paths={Object.keys(files)}
          activeFile={activeFile}
          onSelect={setActiveFile}
          onCreate={createFile}
          onDelete={deleteFile}
        />

        <div className="editor-panel">
          {activeFile ? (
            <Editor
              height="60vh"
              language={monacoLanguageFor(language)}
              theme="vs-dark"
              value={files[activeFile] ?? ''}
              onChange={(value) => sendCodeUpdate(activeFile, value ?? '')}
              options={{
                minimap: { enabled: false },
                fontSize: 14,
                readOnly: !isConnected,
                wordWrap: 'on',
              }}
            />
          ) : (
            <div className="editor-empty">
              {roomId ? 'Select or create a file to start editing.' : 'Create or join a room to start.'}
            </div>
          )}
        </div>
      </section>

      <RunPanel
        language={language}
        code={activeFile ? files[activeFile] ?? '' : ''}
        path={activeFile}
      />

      <ScreenShare />

      {!roomId && (
        <p className="hint">
          {auth
            ? 'Create a new room or pick one of your saved rooms above to resume.'
            : 'Create a room or join with an ID. Log in to save your rooms and resume them later.'}
        </p>
      )}
    </div>
  );
}
