import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Card } from '@/shared/components/Card';
import { formatCurrency } from '@/shared/lib/formatters';
import type { CategorySpend } from '@/shared/lib/types';

export function SpendByCategoryChart({ data }: { data: CategorySpend[] }) {
  return (
    <Card title="Spend by Category">
      <div className="h-72">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} layout="vertical" margin={{ left: 16 }}>
            <CartesianGrid strokeDasharray="3 3" horizontal={false} />
            <XAxis type="number" tickFormatter={(v) => formatCurrency(v)} />
            <YAxis type="category" dataKey="category" width={100} tick={{ fontSize: 12 }} />
            <Tooltip formatter={(value) => formatCurrency(Number(value))} />
            <Bar dataKey="totalAmount" fill="#0f172a" radius={[0, 4, 4, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </Card>
  );
}
