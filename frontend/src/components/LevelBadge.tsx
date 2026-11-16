import type { LevelInfo } from "@/lib/types";

/** ΥΠ / ΕΠ / Ε / ΕΛ as a coloured tag, using the colour the guide assigns to each level. */
export function LevelBadge({ level }: { level: LevelInfo | undefined }) {
  if (!level) return null;
  const colour = level.color === "blue" || level.color === "orange" || level.color === "green" ? level.color : "gray";
  return (
    <span className={`badge ${colour}`} title={level.name.el}>
      {level.short}
    </span>
  );
}
