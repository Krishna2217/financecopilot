import { useState, type FormEvent } from 'react';

export function QueryForm({
  onSubmit,
  isSubmitting,
  initialValue = '',
}: {
  onSubmit: (question: string) => void;
  isSubmitting: boolean;
  initialValue?: string;
}) {
  const [question, setQuestion] = useState(initialValue);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (question.trim().length === 0) return;
    onSubmit(question.trim());
  }

  return (
    <form onSubmit={handleSubmit} className="flex gap-2">
      <input
        type="text"
        value={question}
        onChange={(e) => setQuestion(e.target.value)}
        placeholder="How much did I spend on groceries last month?"
        className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
      />
      <button
        type="submit"
        disabled={isSubmitting || question.trim().length === 0}
        className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {isSubmitting ? 'Asking…' : 'Ask'}
      </button>
    </form>
  );
}
