import { useState, type FormEvent } from 'react'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { useClients, useServices } from '@/features/client-services/client-services.queries'
import { useDeleteProposal, useProposals, useSaveProposal } from './proposals.queries'
import type { Proposal } from './proposals.types'

const labels: Record<string, string> = { DRAFT: 'Borrador', PENDING_REVIEW: 'Pendiente de revisión', SENT: 'Enviada', ACCEPTED: 'Aceptada', REJECTED: 'Rechazada', EXPIRED: 'Caducada' }

export function ProposalsPage() {
  const clients = useClients(); const services = useServices(); const proposals = useProposals(); const save = useSaveProposal(); const remove = useDeleteProposal()
  const [clientId, setClientId] = useState(''); const [serviceId, setServiceId] = useState(''); const [title, setTitle] = useState(''); const [quantity, setQuantity] = useState('1'); const [error, setError] = useState<string>()
  if (clients.isPending || services.isPending || proposals.isPending) return <div role="status" className="rounded-2xl bg-white p-8 text-center text-slate-600">Cargando propuestas…</div>
  if (clients.isError || services.isError || proposals.isError) return <Alert variant="error">No se pudieron cargar las propuestas.</Alert>
  const serviceList = services.data.content
  async function submit(e: FormEvent) { e.preventDefault(); setError(undefined); const service = serviceList.find(s => s.id === serviceId); try { if (!clientId || !service) throw new Error('Selecciona un cliente y un servicio'); await save.mutateAsync({ request: { clientId, title, currency: 'EUR', items: [{ serviceId, description: service.name, quantity: Number(quantity), unitPrice: service.rate }] } }); setTitle('') } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar la propuesta') } }
  return <section className="space-y-7">{error && <Alert variant="error">{error}</Alert>}<div><p className="text-sm font-semibold uppercase tracking-wide text-indigo-700">Ventas</p><h1 className="mt-2 text-3xl font-bold text-slate-950">Propuestas comerciales</h1><p className="mt-2 text-slate-600">Relaciona clientes con servicios y calcula importes con precisión decimal.</p></div><form onSubmit={submit} className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-5 md:grid-cols-5"><Input required placeholder="Título" aria-label="Título" value={title} onChange={e => setTitle(e.target.value)} /><Select aria-label="Cliente" required value={clientId} onChange={e => setClientId(e.target.value)}><option value="">Cliente</option>{clients.data.content.map(c => <option key={c.id} value={c.id}>{c.displayName}</option>)}</Select><Select aria-label="Servicio" required value={serviceId} onChange={e => setServiceId(e.target.value)}><option value="">Servicio</option>{serviceList.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</Select><Input required min="0.0001" step="0.0001" type="number" aria-label="Cantidad" value={quantity} onChange={e => setQuantity(e.target.value)} /><Button disabled={save.isPending} type="submit">{save.isPending ? 'Guardando…' : 'Crear propuesta'}</Button></form><div className="space-y-4">{proposals.data.content.map(p => <ProposalCard key={p.id} proposal={p} remove={() => remove.mutate(p.id)} />)}</div></section>
}

function ProposalCard({ proposal, remove }: { proposal: Proposal; remove: () => void }) { return <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><div className="flex items-start justify-between gap-4"><div><h2 className="text-lg font-bold text-slate-950">{proposal.title}</h2><p className="text-sm text-slate-600">{labels[proposal.status]} · {proposal.total.toFixed(2)} {proposal.currency}</p></div><Button variant="secondary" onClick={remove}>Eliminar</Button></div><ul className="mt-3 space-y-1 text-sm text-slate-600">{proposal.items.map(i => <li key={i.id}>{i.description} · {i.quantity} × {i.unitPrice.toFixed(2)} = {i.amount.toFixed(2)}</li>)}</ul></article> }
