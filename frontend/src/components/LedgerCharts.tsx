import {
  CartesianGrid,
  Line,
  LineChart,
  Pie,
  PieChart,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { CategorySpending, MonthlyEvolution } from "../types/api";
const money = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});
const tooltipStyle = {
  backgroundColor: "#18181a",
  border: "1px solid #2c2c2e",
  borderRadius: 8,
  color: "#fff",
  fontFamily: "JetBrains Mono, monospace",
};
const tooltipTextStyle = { color: "#ffffff" };
export function LedgerCharts({
  categories,
  evolution,
  showCategories = true,
  showEvolution = true,
}: {
  categories: CategorySpending[];
  evolution: MonthlyEvolution[];
  showCategories?: boolean;
  showEvolution?: boolean;
}) {
  const total = categories.reduce((sum, item) => sum + Math.abs(item.spent), 0),
    top = [...categories]
      .sort((a, b) => Math.abs(b.spent) - Math.abs(a.spent))
      .slice(0, 3);
  return (
    <div className="ledger-chart-grid">
      {showCategories && <section className="ledger-panel">
        <h2>Detalhamento de Despesas</h2>
        {categories.length === 0 ? (
          <p className="ledger-empty">
            Nenhum gasto categorizado neste período.
          </p>
        ) : (
          <>
            <div
              aria-label="Donut de despesas por categoria"
              className="ledger-chart monolith-donut"
            >
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie
                    data={categories.slice(0, 5)}
                    dataKey="spent"
                    nameKey="categoryName"
                    innerRadius={72}
                    outerRadius={100}
                    stroke="#2c2c2e"
                  >
                    {categories.slice(0, 5).map((item, index) => <Cell key={item.categoryId} fill={["#ffffff", "#a3a3a3", "#666666", "#353535", "#1f1f1f"][index]} />)}
                  </Pie>
                  <Tooltip contentStyle={tooltipStyle} itemStyle={tooltipTextStyle} labelStyle={tooltipTextStyle} />
                </PieChart>
              </ResponsiveContainer>
              <span>Despesa Total</span>
              <strong>{money.format(total)}</strong>
            </div>
            <ul
              className="monolith-category-list"
              aria-label="Três maiores despesas por categoria"
            >
              {top.map((item) => (
                <li key={item.categoryId}>
                  <span>{item.categoryName}</span>
                  <strong>
                    {money.format(Math.abs(item.spent))} ·{" "}
                    {total
                      ? Math.round((Math.abs(item.spent) / total) * 100)
                      : 0}
                    %
                  </strong>
                </li>
              ))}
            </ul>
          </>
        )}
      </section>}
      {showEvolution && <section className="ledger-panel">
        <h2>Evolução dos últimos seis meses</h2>
        {evolution.length === 0 ? (
          <p className="ledger-empty">Sem evolução mensal para exibir.</p>
        ) : (
          <>
            <div
              aria-label="Gráfico de evolução mensal"
              className="ledger-chart"
            >
              <ResponsiveContainer width="100%" height={260}>
                <LineChart data={evolution}>
                  <CartesianGrid stroke="var(--monolith-border, var(--ledger-border))" />
                  <XAxis dataKey="month" />
                  <YAxis />
                  <Tooltip contentStyle={tooltipStyle} itemStyle={tooltipTextStyle} labelStyle={tooltipTextStyle} />
                  <Line
                    dataKey="income"
                    name="Rendas"
                    stroke="var(--monolith-text, #fff)"
                    strokeWidth={2}
                    dot={false}
                  />
                  <Line
                    dataKey="expense"
                    name="Despesas"
                    stroke="var(--monolith-dim, #5a5a5c)"
                    strokeDasharray="6 4"
                    strokeWidth={2}
                    dot={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
            <ul
              aria-label="Legenda do fluxo financeiro"
              className="monolith-chart-legend"
            >
              <li>Rendas — linha sólida</li>
              <li>Despesas — linha tracejada</li>
            </ul>
            <table className="sr-only">
              <caption>Resumo textual do fluxo financeiro</caption>
              <tbody>
                {evolution.map((item) => (
                  <tr key={item.month}>
                    <th>{item.month}</th>
                    <td>{money.format(item.income)}</td>
                    <td>{money.format(Math.abs(item.expense))}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}
      </section>}
    </div>
  );
}
