import { useState, type FormEvent } from 'react'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Textarea } from '@/components/ui/Textarea'
import { Select } from '@/components/ui/Select'
import { ApiError } from '@/lib/api-error'
import { useJobOpportunities } from '@/features/job-opportunities/api/job-opportunities.queries'
import {
  useApplications,
  useCreateApplication,
  useCreateFollowUp,
  useCreateInterview,
  useDeleteApplication,
  useInterviews,
  useFollowUps,
  useUpdateApplication,
} from './application.queries'
import { APPLICATION_STATUSES, type JobApplication } from './application.types'

const labels: Record<string, string> = {
  PENDING: 'Pendiente',
  INTERESTING: 'Interesante',
  PREPARING_APPLICATION: 'Preparando candidatura',
  APPLICATION_SENT: 'Candidatura enviada',
  INTERVIEW: 'Entrevista',
  OFFER_RECEIVED: 'Oferta recibida',
  REJECTED: 'Rechazada',
  DISCARDED: 'Descartada',
}

const optional = (value: string) => value.trim() || undefined
const failure = (error: unknown) =>
  error instanceof ApiError ? error.message : 'No se pudo completar la operación.'

export function JobApplicationsPage() {
  const opportunities = useJobOpportunities()
  const applications = useApplications()
  const create = useCreateApplication()
  const [error, setError] = useState<string>()
  const [selected, setSelected] = useState('')

  async function retry() {
    await Promise.all([opportunities.refetch(), applications.refetch()])
  }

  if (opportunities.isPending || applications.isPending) {
    return <div role="status" className="rounded-2xl bg-white p-8 text-center text-slate-600">Cargando candidaturas...</div>
  }

  if (opportunities.isError || applications.isError) {
    return (
      <div className="space-y-4">
        <Alert variant="error">No se pudieron cargar las candidaturas.</Alert>
        <Button onClick={() => void retry()}>Reintentar</Button>
      </div>
    )
  }

  const existing = new Set(applications.data.content.map((application) => application.jobOpportunityId))

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!selected) return
    setError(undefined)
    try {
      await create.mutateAsync({ jobOpportunityId: selected, status: 'PENDING' })
      setSelected('')
    } catch (requestError) {
      setError(failure(requestError))
    }
  }

  return (
    <section className="space-y-7">
      {error && <Alert variant="error">{error}</Alert>}
      <div>
        <p className="text-sm font-semibold uppercase tracking-wide text-indigo-700">Seguimiento profesional</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-950">Candidaturas</h1>
        <p className="mt-2 text-slate-600">Controla estados, entrevistas, notas y próximos seguimientos.</p>
      </div>
      <form onSubmit={submit} className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-end">
        <label className="flex-1 text-sm font-medium text-slate-700">
          Convertir oportunidad en candidatura
          <select
            id="application-opportunity"
            name="jobOpportunityId"
            aria-label="Oportunidad laboral"
            required
            value={selected}
            onChange={(event) => setSelected(event.target.value)}
            className="mt-2 min-h-11 w-full rounded-lg border border-slate-300 bg-white px-3"
          >
            <option value="">Selecciona una oportunidad</option>
            {opportunities.data.content
              .filter((opportunity) => !existing.has(opportunity.id))
              .map((opportunity) => (
                <option key={opportunity.id} value={opportunity.id}>
                  {opportunity.companyName} · {opportunity.title}
                </option>
              ))}
          </select>
        </label>
        <Button disabled={create.isPending} type="submit">{create.isPending ? 'Guardando...' : 'Crear candidatura'}</Button>
      </form>
      {applications.data.content.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-slate-600">Todavía no tienes candidaturas.</div>
      ) : (
        <div className="space-y-5">
          {applications.data.content.map((application) => <ApplicationCard key={application.id} application={application} setError={setError} />)}
        </div>
      )}
    </section>
  )
}

function ApplicationCard({ application, setError }: { application: JobApplication; setError: (message: string | undefined) => void }) {
  const update = useUpdateApplication()
  const remove = useDeleteApplication()
  const interviews = useInterviews(application.id)
  const followUps = useFollowUps(application.id)
  const createInterview = useCreateInterview(application.id)
  const createFollowUp = useCreateFollowUp(application.id)
  const [status, setStatus] = useState(application.status)
  const [notes, setNotes] = useState(application.notes ?? '')
  const [scheduledAt, setScheduledAt] = useState('')
  const [interviewType, setInterviewType] = useState('Videollamada')
  const [interviewNotes, setInterviewNotes] = useState('')
  const [dueOn, setDueOn] = useState('')
  const [description, setDescription] = useState('')

  async function save() {
    try {
      await update.mutateAsync({ id: application.id, request: { status, appliedOn: application.appliedOn ?? undefined, notes: optional(notes) } })
    } catch (requestError) {
      setError(failure(requestError))
    }
  }

  async function addInterview(event: FormEvent) {
    event.preventDefault()
    try {
      await createInterview.mutateAsync({ scheduledAt: new Date(scheduledAt).toISOString(), interviewType, notes: optional(interviewNotes) })
      setScheduledAt('')
      setInterviewNotes('')
    } catch (requestError) {
      setError(failure(requestError))
    }
  }

  async function addFollowUp(event: FormEvent) {
    event.preventDefault()
    try {
      await createFollowUp.mutateAsync({ dueOn, description, completed: false })
      setDueOn('')
      setDescription('')
    } catch (requestError) {
      setError(failure(requestError))
    }
  }

  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-950">{application.companyName}</h2>
          <p className="text-slate-600">{application.title}</p>
          {application.appliedOn && <p className="mt-1 text-xs text-slate-500">Aplicada el {application.appliedOn}</p>}
        </div>
        <Button variant="secondary" onClick={() => remove.mutate(application.id)}>Eliminar</Button>
      </div>
      <div className="mt-5 grid gap-4 lg:grid-cols-2">
        <div className="space-y-3">
          <Select id={`application-status-${application.id}`} name="status" aria-label="Estado de candidatura" value={status} onChange={(event) => setStatus(event.target.value as typeof status)}>
            {APPLICATION_STATUSES.map((value) => <option key={value} value={value}>{labels[value]}</option>)}
          </Select>
          <Textarea id={`application-notes-${application.id}`} name="notes" aria-label="Notas de candidatura" placeholder="Notas de la candidatura" value={notes} onChange={(event) => setNotes(event.target.value)} />
          <Button disabled={update.isPending} onClick={() => void save()}>{update.isPending ? 'Guardando...' : 'Guardar estado y notas'}</Button>
        </div>
        <div className="space-y-4 rounded-xl bg-slate-50 p-4">
          <h3 className="font-semibold text-slate-900">Entrevistas</h3>
          {interviews.data?.map((interview) => <p key={interview.id} className="text-sm text-slate-600">{new Date(interview.scheduledAt).toLocaleString('es-ES')} · {interview.interviewType}</p>)}
          <form onSubmit={addInterview} className="space-y-2">
            <Input id={`interview-scheduled-at-${application.id}`} name="scheduledAt" required type="datetime-local" aria-label="Fecha de entrevista" value={scheduledAt} onChange={(event) => setScheduledAt(event.target.value)} />
            <Input id={`interview-type-${application.id}`} name="interviewType" required placeholder="Tipo de entrevista" aria-label="Tipo de entrevista" value={interviewType} onChange={(event) => setInterviewType(event.target.value)} />
            <Input id={`interview-notes-${application.id}`} name="interviewNotes" placeholder="Notas de entrevista" aria-label="Notas de entrevista" value={interviewNotes} onChange={(event) => setInterviewNotes(event.target.value)} />
            <Button disabled={createInterview.isPending} type="submit" variant="secondary">Añadir entrevista</Button>
          </form>
          <h3 className="pt-2 font-semibold text-slate-900">Seguimientos</h3>
          {followUps.data?.map((followUp) => <p key={followUp.id} className="text-sm text-slate-600">{followUp.dueOn} · {followUp.description}{followUp.completed ? ' · Completado' : ''}</p>)}
          <form onSubmit={addFollowUp} className="space-y-2">
            <Input id={`follow-up-due-on-${application.id}`} name="dueOn" required type="date" aria-label="Fecha de seguimiento" value={dueOn} onChange={(event) => setDueOn(event.target.value)} />
            <Input id={`follow-up-description-${application.id}`} name="description" required placeholder="Descripción del seguimiento" aria-label="Descripción del seguimiento" value={description} onChange={(event) => setDescription(event.target.value)} />
            <Button disabled={createFollowUp.isPending} type="submit" variant="secondary">Añadir seguimiento</Button>
          </form>
        </div>
      </div>
    </article>
  )
}
