import { ApiError } from '@/shared/api/client';

export function PageError({ error }: { error: unknown }) {
  const message = error instanceof ApiError ? error.message : 'Something went wrong.';
  return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
      {message}
    </div>
  );
}
