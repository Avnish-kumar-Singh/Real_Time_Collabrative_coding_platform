import { useEffect, useRef, useState } from 'react';
import { useScreenShare } from '../hooks/useScreenShare';

export default function ScreenShare() {
  const { role, shareRoomId, viewerCount, remoteStream, error, startSharing, watchShare, stop } = useScreenShare();
  const [watchInput, setWatchInput] = useState('');
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.srcObject = remoteStream;
    }
  }, [remoteStream]);

  const handleWatch = () => {
    const trimmed = watchInput.trim();
    if (trimmed) {
      watchShare(trimmed);
    }
  };

  return (
    <section className="screen-share-panel">
      <div className="screen-share-header">
        <h2>Screen sharing</h2>
        {role === 'idle' ? (
          <div className="screen-share-controls">
            <button type="button" onClick={startSharing}>
              Share my screen
            </button>
            <span className="screen-share-or">or</span>
            <input
              value={watchInput}
              onChange={(event) => setWatchInput(event.target.value)}
              placeholder="Share room ID to watch"
            />
            <button type="button" onClick={handleWatch}>
              Watch
            </button>
          </div>
        ) : (
          <button type="button" className="secondary" onClick={stop}>
            {role === 'sharing' ? 'Stop sharing' : 'Stop watching'}
          </button>
        )}
      </div>

      {error && <div className="error-banner">{error}</div>}

      {role === 'sharing' && shareRoomId && (
        <div className="screen-share-status">
          <p>
            Sharing live — give this ID to whoever should watch:{' '}
            <strong className="share-room-id">{shareRoomId}</strong> · Viewers: {viewerCount}
          </p>
          <p className="screen-share-hint">
            When your browser just asked what to share: <em>"Chrome Tab"</em> shares only this browser
            tab, while <em>"Window"</em> or <em>"Entire Screen"</em> shares everything in that window or
            display — including apps like VS Code — exactly as you selected.
          </p>
        </div>
      )}

      {role === 'viewing' && (
        <div className="screen-share-status">
          <p>
            Watching room <strong className="share-room-id">{shareRoomId}</strong>
          </p>
          <video ref={videoRef} autoPlay playsInline className="screen-share-video" />
        </div>
      )}
    </section>
  );
}
