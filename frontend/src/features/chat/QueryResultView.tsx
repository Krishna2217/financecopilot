import { Card } from '@/shared/components/Card';
import type { QueryResponse } from '@/shared/lib/types';

export function QueryResultView({ result }: { result: QueryResponse }) {
  return (
    <div className="space-y-4">
      <Card title="Summary">
        <p className="text-slate-800">{result.summary}</p>
      </Card>

      <Card title="Generated SQL">
        <pre className="overflow-x-auto rounded-lg bg-slate-900 p-3 text-xs text-slate-100">
          {result.generatedSql}
        </pre>
        <p className="mt-2 text-sm text-slate-500">{result.rationale}</p>
      </Card>

      {result.rows.length > 0 && (
        <Card title={`Results (${result.rows.length} rows)`}>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200">
                  {result.columns.map((col) => (
                    <th key={col} className="whitespace-nowrap py-2 pr-4 font-medium text-slate-500">
                      {col}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {result.rows.map((row, i) => (
                  <tr key={i} className="border-b border-slate-100">
                    {result.columns.map((col) => (
                      <td key={col} className="whitespace-nowrap py-2 pr-4 text-slate-700">
                        {String(row[col] ?? '')}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
}
