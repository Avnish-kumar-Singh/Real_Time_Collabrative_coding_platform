import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';

import { WS_BASE } from '../services/api';

type ScreenShareRole =
  | 'idle'
  | 'sharing'
  | 'viewing';

interface SignalMessage {
  type:
    | 'PEER_JOINED'
    | 'PEER_LEFT'
    | 'OFFER'
    | 'ANSWER'
    | 'ICE_CANDIDATE';

  senderId?: string;
  targetId?: string;
  payload?: string;
}

const ICE_SERVERS = [
  {
    urls: 'stun:stun.l.google.com:19302',
  },
];

export function useScreenShare() {
  const [role, setRole] =
    useState<ScreenShareRole>('idle');

  const [shareRoomId, setShareRoomId] =
    useState<string | null>(null);

  const [viewerCount, setViewerCount] =
    useState(0);

  const [remoteStream, setRemoteStream] =
    useState<MediaStream | null>(null);

  const [error, setError] =
    useState<string | null>(null);

  const socketRef =
    useRef<WebSocket | null>(null);

  const localStreamRef =
    useRef<MediaStream | null>(null);

  const peersRef =
    useRef<
      Map<string, RTCPeerConnection>
    >(new Map());

  const cleanup = useCallback(() => {
    peersRef.current.forEach(
      (pc) => pc.close()
    );

    peersRef.current.clear();

    if (localStreamRef.current) {
      localStreamRef.current
        .getTracks()
        .forEach((track) => {
          track.stop();
        });

      localStreamRef.current = null;
    }

    socketRef.current?.close();
    socketRef.current = null;

    setRole('idle');
    setShareRoomId(null);
    setViewerCount(0);
    setRemoteStream(null);
  }, []);

  useEffect(
    () => () => cleanup(),
    [cleanup]
  );

  /*
   * Connect to the backend WebSocket
   * signaling endpoint.
   *
   * Local:
   *   ws://localhost:8080/screen
   *
   * Production:
   *   wss://codesync-backend-740b.onrender.com/screen
   */
  const openSocket = (
    roomId: string
  ) => {
    const socket = new WebSocket(
      `${WS_BASE}/screen?shareRoomId=${encodeURIComponent(
        roomId
      )}`
    );

    socketRef.current = socket;

    return socket;
  };

  const send = (
    socket: WebSocket,
    message: SignalMessage
  ) => {
    if (
      socket.readyState ===
      WebSocket.OPEN
    ) {
      socket.send(
        JSON.stringify(message)
      );
    }
  };

  const createPeerConnection = (
    peerId: string,
    socket: WebSocket,
    isSharer: boolean
  ) => {
    const pc =
      new RTCPeerConnection({
        iceServers: ICE_SERVERS,
      });

    pc.onicecandidate = (event) => {
      if (event.candidate) {
        send(socket, {
          type: 'ICE_CANDIDATE',
          targetId: peerId,
          payload:
            JSON.stringify(
              event.candidate
            ),
        });
      }
    };

    if (
      isSharer &&
      localStreamRef.current
    ) {
      localStreamRef.current
        .getTracks()
        .forEach((track) => {
          pc.addTrack(
            track,
            localStreamRef.current!
          );
        });
    } else {
      pc.ontrack = (event) => {
        setRemoteStream(
          event.streams[0]
        );
      };
    }

    peersRef.current.set(
      peerId,
      pc
    );

    return pc;
  };

  const startSharing =
    useCallback(async () => {
      setError(null);

      try {
        const stream =
          await navigator.mediaDevices.getDisplayMedia(
            {
              video: true,
              audio: false,
            }
          );

        localStreamRef.current =
          stream;

        stream
          .getVideoTracks()[0]
          .addEventListener(
            'ended',
            cleanup
          );

        const roomId =
          crypto
            .randomUUID()
            .replace(/-/g, '')
            .slice(0, 8);

        const socket =
          openSocket(roomId);

        socket.onmessage =
          async (event) => {
            const message =
              JSON.parse(
                event.data
              ) as SignalMessage;

            if (
              message.type ===
                'PEER_JOINED' &&
              message.senderId
            ) {
              const pc =
                createPeerConnection(
                  message.senderId,
                  socket,
                  true
                );

              const offer =
                await pc.createOffer();

              await pc.setLocalDescription(
                offer
              );

              send(socket, {
                type: 'OFFER',
                targetId:
                  message.senderId,
                payload:
                  JSON.stringify(
                    offer
                  ),
              });

              setViewerCount(
                peersRef.current.size
              );

              return;
            }

            if (
              message.type ===
                'ANSWER' &&
              message.senderId &&
              message.payload
            ) {
              const pc =
                peersRef.current.get(
                  message.senderId
                );

              await pc?.setRemoteDescription(
                JSON.parse(
                  message.payload
                )
              );

              return;
            }

            if (
              message.type ===
                'ICE_CANDIDATE' &&
              message.senderId &&
              message.payload
            ) {
              const pc =
                peersRef.current.get(
                  message.senderId
                );

              await pc?.addIceCandidate(
                JSON.parse(
                  message.payload
                )
              );

              return;
            }

            if (
              message.type ===
                'PEER_LEFT' &&
              message.senderId
            ) {
              peersRef.current
                .get(
                  message.senderId
                )
                ?.close();

              peersRef.current.delete(
                message.senderId
              );

              setViewerCount(
                peersRef.current.size
              );
            }
          };

        socket.onopen = () => {
          setRole('sharing');
          setShareRoomId(roomId);
        };

        socket.onerror = () => {
          setError(
            'Connection to signaling server failed.'
          );
        };
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : 'Could not start screen share. Permission may have been denied.'
        );
      }
    }, [cleanup]);

  const watchShare =
    useCallback(
      (roomId: string) => {
        setError(null);

        const socket =
          openSocket(roomId);

        socket.onmessage =
          async (event) => {
            const message =
              JSON.parse(
                event.data
              ) as SignalMessage;

            if (
              message.type ===
                'OFFER' &&
              message.senderId &&
              message.payload
            ) {
              const pc =
                createPeerConnection(
                  message.senderId,
                  socket,
                  false
                );

              await pc.setRemoteDescription(
                JSON.parse(
                  message.payload
                )
              );

              const answer =
                await pc.createAnswer();

              await pc.setLocalDescription(
                answer
              );

              send(socket, {
                type: 'ANSWER',
                targetId:
                  message.senderId,
                payload:
                  JSON.stringify(
                    answer
                  ),
              });

              return;
            }

            if (
              message.type ===
                'ICE_CANDIDATE' &&
              message.senderId &&
              message.payload
            ) {
              const pc =
                peersRef.current.get(
                  message.senderId
                );

              await pc?.addIceCandidate(
                JSON.parse(
                  message.payload
                )
              );

              return;
            }

            if (
              message.type ===
              'PEER_LEFT'
            ) {
              setError(
                'The sharer stopped sharing.'
              );

              cleanup();
            }
          };

        socket.onopen = () => {
          setRole('viewing');
          setShareRoomId(roomId);
        };

        socket.onerror = () => {
          setError(
            'Could not connect to that share room.'
          );
        };
      },
      [cleanup]
    );

  const stop =
    useCallback(
      () => cleanup(),
      [cleanup]
    );

  return {
    role,
    shareRoomId,
    viewerCount,
    remoteStream,
    error,
    startSharing,
    watchShare,
    stop,
  };
}