import type { ReactNode } from 'react';

export function Card({ title, children, className = '' }: { title?: string; children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-xl border border-slate-200 bg-white p-4 shadow-sm ${className}`}>
      {title && <h3 className="mb-3 text-sm font-medium text-slate-500">{title}</h3>}
      {children}
    </div>
  );
}
