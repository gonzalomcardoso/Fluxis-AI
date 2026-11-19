import { Alert } from '@/components/ui/Alert'
import { useDashboard } from './dashboard.queries'
import type { Dashboard } from './dashboard.types'

const statusLabels: Record<string, string> = { DRAFT: 'Borrador', PENDING_REVIEW: 'En revisión', SENT: 'Enviada', ACCEPTED: 'Aceptada', REJECTED: 'Rechazada', EXPIRED: 'Caducada' }
const amount = (value: number, currency: string) => `${value.toFixed(2)} ${currency}`

export function DashboardPage() {
  const dashboard = useDashboard()
  if (dashboard.isPending) return <div role="status">Cargando dashboard...</div>
  if (dashboard.isError) return <Alert variant="error">No se pudieron cargar las métricas.</Alert>
  const data = dashboard.data
  return <section className="space-y-7">
    <div><p className="text-sm font-semibold uppercase tracking-wide text-indigo-700">Visión general</p><h1 className="mt-2 text-3xl font-bold text-slate-950">Dashboard</h1><p className="mt-2 text-slate-600">Métricas reales de tus módulos de datos, actualizadas al abrir esta pantalla.</p></div>
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"><Metric label="Clientes" value={data.clients.total} detail={`${data.clients.active} activos · ${data.clients.potential} potenciales`} /><Metric label="Servicios" value={data.services.total} detail={`${data.services.active} activos`} /><Metric label="Propuestas" value={data.proposals.total} detail={`${data.proposals.acceptedByCurrency.length} monedas con aceptadas`} /><Metric label="Tareas pendientes" value={data.tasks.pending + data.tasks.inProgress} detail={`${data.tasks.overdue} vencidas`} /></div>
    <div className="grid gap-5 lg:grid-cols-2"><Panel title="Ingresos aceptados y gasto mensual"><p className="mb-3 text-sm text-slate-500">Ingresos de propuestas aceptadas menos gasto mensual de suscripciones.</p>{data.margins.length === 0 ? <Empty /> : data.margins.map(m => <div className="flex items-center justify-between border-b border-slate-100 py-3 last:border-0" key={m.currency}><span className="font-semibold">{m.currency}</span><span className={m.margin >= 0 ? 'font-semibold text-emerald-700' : 'font-semibold text-rose-700'}>{amount(m.margin, m.currency)}</span></div>)}</Panel><Panel title="Gasto recurrente mensual"><MoneyRows values={data.subscriptions.monthlyByCurrency} empty="No hay suscripciones activas." /></Panel></div>
    <div className="grid gap-5 lg:grid-cols-3"><Panel title="Propuestas por estado"><div className="space-y-2">{Object.entries(data.proposals.byStatus).map(([status, count]) => <div className="flex justify-between text-sm" key={status}><span>{statusLabels[status] ?? status}</span><strong>{count}</strong></div>)}</div></Panel><Panel title="Tareas"><div className="space-y-2 text-sm"><div className="flex justify-between"><span>Pendientes</span><strong>{data.tasks.pending}</strong></div><div className="flex justify-between"><span>En curso</span><strong>{data.tasks.inProgress}</strong></div><div className="flex justify-between"><span>Completadas</span><strong>{data.tasks.done}</strong></div><div className="flex justify-between text-rose-700"><span>Vencidas</span><strong>{data.tasks.overdue}</strong></div></div></Panel><Panel title="Renovaciones próximas"><p className="text-3xl font-bold text-slate-950">{data.subscriptions.upcomingRenewals}</p><p className="mt-1 text-sm text-slate-500">en los próximos 30 días</p></Panel></div>
  </section>
}

function Metric({ label, value, detail }: { label: string; value: number; detail: string }) { return <article className="rounded-2xl bg-white p-5 shadow-sm"><p className="text-sm text-slate-500">{label}</p><p className="mt-2 text-3xl font-bold text-slate-950">{value}</p><p className="mt-1 text-xs text-slate-500">{detail}</p></article> }
function Panel({ title, children }: { title: string; children: React.ReactNode }) { return <article className="rounded-2xl border border-slate-200 bg-white p-5"><h2 className="font-semibold text-slate-950">{title}</h2><div className="mt-3">{children}</div></article> }
function MoneyRows({ values, empty }: { values: Dashboard['subscriptions']['monthlyByCurrency']; empty: string }) { return values.length === 0 ? <p className="text-sm text-slate-500">{empty}</p> : <div className="space-y-2">{values.map(v => <div className="flex justify-between text-sm" key={v.currency}><span>{v.currency}</span><strong>{amount(v.amount, v.currency)}</strong></div>)}</div> }
function Empty() { return <p className="text-sm text-slate-500">No hay datos todavía.</p> }
