import { useEffect, useState } from 'react';
import { executeCode } from '../services/api';
import type { ExecuteResult } from '../services/api';
import { buildCommand, parseArgsFromCommand } from '../utils/language';

interface RunPanelProps {
  /** Language id auto-detected from the active file's extension (see utils/language.ts). */
  language: string;
  code: string;
  path?: string | null;
}

export default function RunPanel({ language, code, path }: RunPanelProps) {
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<ExecuteResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [stdin, setStdin] = useState('');
  const [showStdin, setShowStdin] = useState(false);

  const [showTerminal, setShowTerminal] = useState(false);
  const [command, setCommand] = useState(() => buildCommand(language, path, ''));
  const [commandDirty, setCommandDirty] = useState(false);

  // Keep the command line in sync with the active file/language, unless the
  // user has hand-edited it — an edit "pins" the command until they reset it.
  useEffect(() => {
    if (!commandDirty) {
      setCommand(buildCommand(language, path, ''));
    }
  }, [language, path, commandDirty]);

  const resetCommand = () => {
    setCommand(buildCommand(language, path, ''));
    setCommandDirty(false);
  };

  const handleRun = async () => {
    setRunning(true);
    setError(null);
    setResult(null);
    try {
      const args = showTerminal ? parseArgsFromCommand(language, path, command) : [];
      const response = await executeCode(language, code, stdin, path, args);
      setResult(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Execution failed');
    } finally {
      setRunning(false);
    }
  };

  return (
    <section className="run-panel">
      <div className="run-toolbar">
        <button type="button" onClick={handleRun} disabled={running || !code.trim()}>
          {running ? 'Running…' : '▶ Run'}
        </button>
        <button type="button" className="secondary" onClick={() => setShowTerminal((v) => !v)}>
          {showTerminal ? 'Hide terminal' : 'Run via command'}
        </button>
        <button type="button" className="secondary" onClick={() => setShowStdin((v) => !v)}>
          {showStdin ? 'Hide input' : 'Program input (stdin)'}
        </button>
        <span className="run-lang-pill">{language || 'auto'}</span>
      </div>

      {showTerminal && (
        <div className="run-command-bar">
          <label className="run-command-label">
            Command (editable — the file's extension picks the language automatically)
          </label>
          <input
            className="run-command-input"
            value={command}
            onChange={(event) => {
              setCommand(event.target.value);
              setCommandDirty(true);
            }}
            spellCheck={false}
          />
          {commandDirty && (
            <button type="button" className="link-button run-command-reset" onClick={resetCommand}>
              Reset to detected command
            </button>
          )}
        </div>
      )}

      {showStdin && (
        <textarea
          className="run-stdin"
          value={stdin}
          onChange={(event) => setStdin(event.target.value)}
          placeholder="Typed input your program reads via stdin, e.g. Scanner/input() calls — one value per line"
          rows={3}
        />
      )}

      {error && <div className="error-banner">{error}</div>}

      {result && (
        <div className="run-output">
          <div className={`run-exit-code ${result.exitCode === 0 ? 'ok' : 'fail'}`}>
            Exit code: {result.exitCode} · {result.language} {result.version}
          </div>
          {result.stdout && <pre className="run-stdout">{result.stdout}</pre>}
          {result.stderr && <pre className="run-stderr">{result.stderr}</pre>}
          {!result.stdout && !result.stderr && <p className="run-empty">(no output)</p>}
        </div>
      )}
    </section>
  );
}
