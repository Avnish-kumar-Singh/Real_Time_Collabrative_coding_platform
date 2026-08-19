interface LogoProps {
  className?: string;
  size?: number;
}

/**
 * The brand mark: angle brackets (code) framing two vertical carets that blink
 * out of sync in two different accent colors (two collaborators' live cursors).
 * It's a literal, animated depiction of the product's core feature — real-time,
 * multiplayer text cursors in a shared file — rather than a generic logo shape.
 */
export default function Logo({ className, size = 40 }: LogoProps) {
  return (
    <svg
      className={`logo-mark ${className ?? ''}`}
      width={size}
      height={size}
      viewBox="0 0 40 40"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      aria-label="CodeSync logo"
    >
      <path
        d="M15 9 L6 20 L15 31"
        className="logo-bracket logo-bracket-a"
        strokeWidth="3"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
      />
      <path
        d="M25 9 L34 20 L25 31"
        className="logo-bracket logo-bracket-b"
        strokeWidth="3"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
      />
      <rect x="16.8" y="13" width="2.6" height="14" rx="1.3" className="logo-cursor logo-cursor-a" />
      <rect x="20.6" y="13" width="2.6" height="14" rx="1.3" className="logo-cursor logo-cursor-b" />
    </svg>
  );
}
