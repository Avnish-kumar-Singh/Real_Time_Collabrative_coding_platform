// Central place that maps a file's extension to a Piston/Monaco language id,
// and builds the shell command that would run that file. Keeping this in one
// module means the editor's syntax highlighting, the Run button, and the
// terminal-style command box in RunPanel all agree on the same language.

export interface LanguageDef {
  id: string;
  label: string;
  monaco: string;
  extensions: string[];
  defaultFileName: string;
}

export const LANGUAGES: LanguageDef[] = [
  { id: 'javascript', label: 'JavaScript', monaco: 'javascript', extensions: ['js', 'mjs', 'cjs'], defaultFileName: 'index.js' },
  { id: 'typescript', label: 'TypeScript', monaco: 'typescript', extensions: ['ts', 'tsx'], defaultFileName: 'index.ts' },
  { id: 'python', label: 'Python', monaco: 'python', extensions: ['py', 'pyw'], defaultFileName: 'main.py' },
  { id: 'java', label: 'Java', monaco: 'java', extensions: ['java'], defaultFileName: 'Main.java' },
  { id: 'c', label: 'C', monaco: 'c', extensions: ['c', 'h'], defaultFileName: 'main.c' },
  { id: 'cpp', label: 'C++', monaco: 'cpp', extensions: ['cpp', 'cc', 'cxx', 'hpp'], defaultFileName: 'main.cpp' },
  { id: 'csharp', label: 'C#', monaco: 'csharp', extensions: ['cs'], defaultFileName: 'Program.cs' },
  { id: 'go', label: 'Go', monaco: 'go', extensions: ['go'], defaultFileName: 'main.go' },
  { id: 'ruby', label: 'Ruby', monaco: 'ruby', extensions: ['rb'], defaultFileName: 'main.rb' },
  { id: 'php', label: 'PHP', monaco: 'php', extensions: ['php'], defaultFileName: 'index.php' },
];

const EXTENSION_TO_LANGUAGE: Record<string, string> = {};
for (const lang of LANGUAGES) {
  for (const ext of lang.extensions) {
    EXTENSION_TO_LANGUAGE[ext] = lang.id;
  }
}

export function basename(path: string | null | undefined): string {
  if (!path) return '';
  const trimmed = path.trim();
  if (!trimmed) return '';
  const parts = trimmed.split(/[/\\]/);
  return parts[parts.length - 1] || trimmed;
}

function extensionOf(fileName: string): string {
  const dot = fileName.lastIndexOf('.');
  if (dot <= 0) return '';
  return fileName.slice(dot + 1).toLowerCase();
}

/** Detects the language id from a file path's extension. Returns null if unrecognized/no path. */
export function languageFromPath(path: string | null | undefined): string | null {
  const file = basename(path);
  if (!file) return null;
  const ext = extensionOf(file);
  return EXTENSION_TO_LANGUAGE[ext] ?? null;
}

export function monacoLanguageFor(languageId: string): string {
  return LANGUAGES.find((lang) => lang.id === languageId)?.monaco ?? 'plaintext';
}

export function defaultFileNameFor(languageId: string): string {
  return LANGUAGES.find((lang) => lang.id === languageId)?.defaultFileName ?? 'main.txt';
}

/**
 * Builds the exact command that will be run for a given file/language, mirroring
 * what the backend actually does (compile step + run step, or interpreter + file).
 * This is what's shown (and editable) in RunPanel's terminal box.
 */
export function buildCommand(languageId: string, path: string | null | undefined, args: string): string {
  const file = basename(path) || defaultFileNameFor(languageId);
  const fileBase = file.replace(/\.[^./\\]+$/, '');
  const argText = args.trim() ? ` ${args.trim()}` : '';

  switch (languageId) {
    case 'java':
      return `javac ${file} && java ${fileBase}${argText}`;
    case 'python':
      return `python ${file}${argText}`;
    case 'javascript':
      return `node ${file}${argText}`;
    case 'typescript':
      return `ts-node ${file}${argText}`;
    case 'c':
      return `gcc ${file} -o ${fileBase} && ./${fileBase}${argText}`;
    case 'cpp':
      return `g++ ${file} -o ${fileBase} && ./${fileBase}${argText}`;
    case 'csharp':
      return `csc ${file} && dotnet ${fileBase}.dll${argText}`;
    case 'go':
      return `go run ${file}${argText}`;
    case 'ruby':
      return `ruby ${file}${argText}`;
    case 'php':
      return `php ${file}${argText}`;
    default:
      return `run ${file}${argText}`;
  }
}

/**
 * Given a (possibly hand-edited) command line the user typed into the terminal box,
 * extracts the trailing program arguments so we can still send them to the backend
 * (which always runs code through Piston, not a literal shell — see CodeExecutionService).
 * Handles the "compile && run" languages by looking at the segment after the last `&&`.
 */
export function parseArgsFromCommand(languageId: string, path: string | null | undefined, command: string): string[] {
  const file = basename(path) || defaultFileNameFor(languageId);
  const fileBase = file.replace(/\.[^./\\]+$/, '');

  const segments = command.split('&&').map((s) => s.trim());
  const runSegment = segments[segments.length - 1] ?? '';
  const tokens = runSegment.split(/\s+/).filter(Boolean);
  if (tokens.length === 0) return [];

  // Drop the leading "interpreter/entry" tokens so only real program args remain.
  // e.g. "python main.py --flag x"      -> drop "python", "main.py"
  //      "./main --flag x"              -> drop "./main"
  //      "dotnet Program.dll --flag x"  -> drop "dotnet", "Program.dll"
  //      "java Main --flag x"           -> drop "java", "Main"
  const first = tokens[0].toLowerCase();
  let dropCount = 0;
  if (first.startsWith('./') || first === fileBase.toLowerCase()) {
    dropCount = 1;
  } else if (['java', 'python', 'python3', 'node', 'ts-node', 'ruby', 'php', 'dotnet', 'go'].includes(first)) {
    dropCount = tokens.length > 1 ? 2 : 1;
    // "go run main.go args" has an extra "run" token
    if (first === 'go' && tokens[1]?.toLowerCase() === 'run') {
      dropCount = 3;
    }
  } else {
    // Unrecognized leading token (user rewrote the command); best effort —
    // drop just the first token and treat the rest as args.
    dropCount = 1;
  }

  return tokens.slice(dropCount).filter(Boolean);
}
