import { useState } from 'react';

interface TreeNode {
  name: string;
  fullPath: string;
  isFile: boolean;
  children: Map<string, TreeNode>;
}

function buildTree(paths: string[]): TreeNode {
  const root: TreeNode = { name: '', fullPath: '', isFile: false, children: new Map() };

  for (const path of paths) {
    const segments = path.split('/');
    let node = root;
    let accumulated = '';

    segments.forEach((segment, index) => {
      accumulated = accumulated ? `${accumulated}/${segment}` : segment;
      const isFile = index === segments.length - 1;

      if (!node.children.has(segment)) {
        node.children.set(segment, { name: segment, fullPath: accumulated, isFile, children: new Map() });
      }
      node = node.children.get(segment)!;
    });
  }

  return root;
}

interface FileExplorerProps {
  paths: string[];
  activeFile: string | null;
  onSelect: (path: string) => void;
  onCreate: (path: string) => void;
  onDelete: (path: string) => void;
}

function TreeView({
  node,
  depth,
  activeFile,
  onSelect,
  onDelete,
}: {
  node: TreeNode;
  depth: number;
  activeFile: string | null;
  onSelect: (path: string) => void;
  onDelete: (path: string) => void;
}) {
  const entries = [...node.children.values()].sort((a, b) => {
    if (a.isFile !== b.isFile) {
      return a.isFile ? 1 : -1;
    }
    return a.name.localeCompare(b.name);
  });

  return (
    <>
      {entries.map((child) => (
        <div key={child.fullPath}>
          <div
            className={`file-tree-row${child.isFile && child.fullPath === activeFile ? ' active' : ''}`}
            style={{ paddingLeft: `${depth * 14 + 8}px` }}
          >
            {child.isFile ? (
              <button type="button" className="file-tree-name" onClick={() => onSelect(child.fullPath)}>
                📄 {child.name}
              </button>
            ) : (
              <span className="file-tree-name folder">📁 {child.name}</span>
            )}
            <button
              type="button"
              className="file-tree-delete"
              title={child.isFile ? 'Delete file' : 'Delete folder and contents'}
              onClick={() => onDelete(child.fullPath)}
            >
              ✕
            </button>
          </div>
          {!child.isFile && (
            <TreeView node={child} depth={depth + 1} activeFile={activeFile} onSelect={onSelect} onDelete={onDelete} />
          )}
        </div>
      ))}
    </>
  );
}

export default function FileExplorer({ paths, activeFile, onSelect, onCreate, onDelete }: FileExplorerProps) {
  const [newPath, setNewPath] = useState('');
  const tree = buildTree(paths);

  const handleCreate = () => {
    const trimmed = newPath.trim();
    if (trimmed) {
      onCreate(trimmed);
      setNewPath('');
    }
  };

  return (
    <aside className="file-explorer">
      <h3>Files</h3>
      <div className="file-tree">
        <TreeView node={tree} depth={0} activeFile={activeFile} onSelect={onSelect} onDelete={onDelete} />
        {paths.length === 0 && <p className="file-tree-empty">No files yet.</p>}
      </div>
      <div className="file-tree-new">
        <input
          value={newPath}
          onChange={(event) => setNewPath(event.target.value)}
          onKeyDown={(event) => event.key === 'Enter' && handleCreate()}
          placeholder="path/to/file.js"
        />
        <button type="button" onClick={handleCreate}>
          + New
        </button>
      </div>
    </aside>
  );
}
