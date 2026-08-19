import { useEffect, useState } from 'react';
import { getMyRooms } from '../services/api';
import type { RoomSummary } from '../services/api';

interface MyRoomsProps {
  token: string;
  onSelectRoom: (roomId: string) => void;
  refreshKey: number;
}

export default function MyRooms({ token, onSelectRoom, refreshKey }: MyRoomsProps) {
  const [rooms, setRooms] = useState<RoomSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    getMyRooms(token)
      .then((result) => {
        if (!cancelled) {
          setRooms(result);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Could not load rooms');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [token, refreshKey]);

  if (loading) {
    return <p className="my-rooms-status">Loading your rooms…</p>;
  }

  if (error) {
    return <p className="my-rooms-status error">{error}</p>;
  }

  if (rooms.length === 0) {
    return <p className="my-rooms-status">No saved rooms yet — create one to get started.</p>;
  }

  return (
    <ul className="my-rooms-list">
      {rooms.map((room) => (
        <li key={room.roomId}>
          <button type="button" className="my-room-item" onClick={() => onSelectRoom(room.roomId)}>
            <span className="my-room-id">{room.roomId}</span>
            <span className="my-room-preview">{room.preview}</span>
            <span className="my-room-time">{new Date(room.updatedAt).toLocaleString()}</span>
          </button>
        </li>
      ))}
    </ul>
  );
}
