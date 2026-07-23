import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { Card } from '@/shared/components/Card';
import { formatCurrency, formatMonth } from '@/shared/lib/formatters';
import type { CashflowPoint } from '@/shared/lib/types';

export function CashflowChart({ data }: { data: CashflowPoint[] }) {
  return (
    <Card title="Cashflow Trend">
      <div className="h-72">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="month" tickFormatter={formatMonth} />
            <YAxis tickFormatter={(v) => formatCurrency(v)} width={80} />
            <Tooltip
              labelFormatter={(label) => formatMonth(String(label))}
              formatter={(value) => formatCurrency(Number(value))}
            />
            <Legend />
            <Line type="monotone" dataKey="income" stroke="#16a34a" name="Income" />
            <Line type="monotone" dataKey="expenses" stroke="#dc2626" name="Expenses" />
            <Line type="monotone" dataKey="netCashflow" stroke="#0f172a" name="Net" />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </Card>
  );
}
