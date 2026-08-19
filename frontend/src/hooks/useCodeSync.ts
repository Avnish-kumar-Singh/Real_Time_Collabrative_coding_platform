import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';

import {
  WS_BASE,
} from '../services/api';

import type {
  CodeMessage,
} from '../services/api';

type ConnectionStatus =
  | 'idle'
  | 'connecting'
  | 'connected'
  | 'disconnected'
  | 'error';

interface ActivityEntry {
  id: string;
  text: string;
}

export function useCodeSync(
  roomId: string | null,
  username: string
) {
  const [files, setFiles] = useState<Record<string, string>>({});
  const [activeFile, setActiveFile] = useState<string | null>(null);
  const [language, setLanguageState] = useState('javascript');

  const [status, setStatus] =
    useState<ConnectionStatus>('idle');

  const [error, setError] =
    useState<string | null>(null);

  const [users, setUsers] =
    useState<string[]>([]);

  const [activity, setActivity] =
    useState<ActivityEntry[]>([]);

  const socketRef =
    useRef<WebSocket | null>(null);

  const applyingRemoteRef =
    useRef(false);

  const activeFileRef =
    useRef<string | null>(null);

  useEffect(() => {
    activeFileRef.current = activeFile;
  }, [activeFile]);

  const pushActivity = useCallback(
    (text: string) => {
      const entry = {
        id: `${Date.now()}-${Math.random()}`,
        text,
      };

      setActivity((prev) => [
        ...prev.slice(-4),
        entry,
      ]);
    },
    []
  );

  useEffect(() => {
    if (!roomId) {
      return;
    }

    setStatus('connecting');
    setError(null);
    setUsers([]);
    setFiles({});
    setActiveFile(null);

    /*
     * Connect directly to the deployed backend.
     *
     * Local:
     *   ws://localhost:8080
     *
     * Production:
     *   wss://codesync-backend-740b.onrender.com
     */
    const usernameParam =
      encodeURIComponent(username || '');

    const socket = new WebSocket(
      `${WS_BASE}/code?roomId=${encodeURIComponent(
        roomId
      )}&username=${usernameParam}`
    );

    socketRef.current = socket;

    socket.onopen = () => {
      setStatus('connected');
    };

    socket.onmessage = (event) => {
      const message =
        JSON.parse(event.data) as CodeMessage;

      if (message.type === 'ERROR') {
        setError(
          message.content ?? 'Unknown error'
        );
        return;
      }

      if (message.type === 'SYNC') {
        const incomingFiles =
          message.files ?? {};

        setFiles(incomingFiles);

        if (message.language) {
          setLanguageState(
            message.language
          );
        }

        setActiveFile((prev) => {
          if (
            prev &&
            incomingFiles[prev] !== undefined
          ) {
            return prev;
          }

          const firstPath =
            Object.keys(incomingFiles)[0];

          return firstPath ?? null;
        });

        return;
      }

      if (
        message.type === 'CODE_UPDATE' &&
        message.path
      ) {
        applyingRemoteRef.current = true;

        setFiles((prev) => ({
          ...prev,
          [message.path as string]:
            message.content ?? '',
        }));

        applyingRemoteRef.current = false;

        return;
      }

      if (
        message.type === 'LANGUAGE_UPDATE' &&
        message.language
      ) {
        setLanguageState(
          message.language
        );

        return;
      }

      if (
        message.type === 'FILE_CREATE' &&
        message.path
      ) {
        setFiles((prev) =>
          message.path &&
          message.path in prev
            ? prev
            : {
                ...prev,
                [message.path as string]:
                  '',
              }
        );

        return;
      }

      if (
        message.type === 'FILE_DELETE' &&
        message.paths
      ) {
        const removed =
          new Set(message.paths);

        setFiles((prev) => {
          const next = { ...prev };

          removed.forEach((path) => {
            delete next[path];
          });

          return next;
        });

        setActiveFile((prev) => {
          if (
            prev &&
            removed.has(prev)
          ) {
            return null;
          }

          return prev;
        });

        return;
      }

      if (message.type === 'PRESENCE') {
        setUsers(
          message.users ?? []
        );

        return;
      }

      if (
        message.type === 'USER_JOINED' &&
        message.username
      ) {
        setUsers((prev) =>
          prev.includes(
            message.username!
          )
            ? prev
            : [
                ...prev,
                message.username!,
              ]
        );

        pushActivity(
          `${message.username} joined`
        );

        return;
      }

      if (
        message.type === 'USER_LEFT' &&
        message.username
      ) {
        setUsers((prev) =>
          prev.filter(
            (u) =>
              u !== message.username
          )
        );

        pushActivity(
          `${message.username} left`
        );
      }
    };

    socket.onclose = () => {
      setStatus('disconnected');
    };

    socket.onerror = () => {
      setStatus('error');

      setError(
        'WebSocket connection failed. Create the room first, then join.'
      );
    };

    return () => {
      socket.close();
      socketRef.current = null;
    };
  }, [
    roomId,
    username,
    pushActivity,
  ]);

  const send = (
    message: CodeMessage
  ) => {
    const socket =
      socketRef.current;

    if (
      socket &&
      socket.readyState ===
        WebSocket.OPEN
    ) {
      socket.send(
        JSON.stringify(message)
      );
    }
  };

  const sendCodeUpdate =
    useCallback(
      (
        path: string,
        nextContent: string
      ) => {
        setFiles((prev) => ({
          ...prev,
          [path]: nextContent,
        }));

        if (
          applyingRemoteRef.current
        ) {
          return;
        }

        send({
          type: 'CODE_UPDATE',
          path,
          content: nextContent,
        });
      },
      []
    );

  const sendLanguageChange =
    useCallback(
      (nextLanguage: string) => {
        setLanguageState(
          nextLanguage
        );

        send({
          type: 'LANGUAGE_UPDATE',
          language:
            nextLanguage,
        });
      },
      []
    );

  const createFile =
    useCallback(
      (path: string) => {
        send({
          type: 'FILE_CREATE',
          path,
        });
      },
      []
    );

  const deleteFile =
    useCallback(
      (path: string) => {
        send({
          type: 'FILE_DELETE',
          path,
        });
      },
      []
    );

  return {
    files,
    activeFile,
    setActiveFile,
    language,
    sendLanguageChange,
    status,
    error,
    users,
    activity,
    sendCodeUpdate,
    createFile,
    deleteFile,
    isConnected:
      status === 'connected',
  };
}