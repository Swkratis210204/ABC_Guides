"use client";

import { useCallback, useEffect, useState } from "react";

interface Stored {
  v: 2;
  codes: string[];
  typed: Record<string, string>;
  other: string;
}

/**
 * Everything the user has entered in the calculator, remembered per department in localStorage so a page reload
 * does not lose their work: the ticked courses, the ECTS they typed for free electives (the guide gives none), and
 * the "other ECTS" amount. Storage can be blocked or full, so every access is guarded and the calculator works without it.
 * Typed numbers are kept as strings so a half-typed value like "6." is not rewritten while the user is typing.
 */
export function useCalculatorSelection(storageKey: string) {
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [typed, setTypedState] = useState<Record<string, string>>({});
  const [other, setOtherState] = useState("");
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(storageKey);
      if (raw) {
        const parsed: unknown = JSON.parse(raw);
        if (Array.isArray(parsed)) {
          // The first version of this page stored only the list of codes.
          setSelected(new Set(parsed.filter((x): x is string => typeof x === "string")));
        } else if (parsed && typeof parsed === "object") {
          const s = parsed as Partial<Stored>;
          if (Array.isArray(s.codes)) setSelected(new Set(s.codes.filter((x): x is string => typeof x === "string")));
          if (s.typed && typeof s.typed === "object") {
            setTypedState(Object.fromEntries(Object.entries(s.typed).filter(([, v]) => typeof v === "string")));
          }
          if (typeof s.other === "string") setOtherState(s.other);
        }
      }
    } catch {
      /* storage unavailable or corrupt: start empty */
    }
    setLoaded(true);
  }, [storageKey]);

  useEffect(() => {
    if (!loaded) return;
    try {
      const value: Stored = { v: 2, codes: [...selected], typed, other };
      window.localStorage.setItem(storageKey, JSON.stringify(value));
    } catch {
      /* ignore */
    }
  }, [selected, typed, other, loaded, storageKey]);

  const toggle = useCallback((code: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(code)) next.delete(code);
      else next.add(code);
      return next;
    });
  }, []);

  const setMany = useCallback((codes: string[], on: boolean) => {
    setSelected((prev) => {
      const next = new Set(prev);
      codes.forEach((c) => (on ? next.add(c) : next.delete(c)));
      return next;
    });
  }, []);

  const setTyped = useCallback((code: string, value: string) => setTypedState((prev) => ({ ...prev, [code]: value })), []);

  const clear = useCallback(() => {
    setSelected(new Set());
    setTypedState({});
    setOtherState("");
  }, []);

  return { selected, toggle, setMany, typed, setTyped, other, setOther: setOtherState, clear, loaded };
}
