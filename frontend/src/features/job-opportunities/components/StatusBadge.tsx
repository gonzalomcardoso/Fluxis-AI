import { es } from '@/i18n/es'
import type { JobOpportunity } from '@/features/job-opportunities/schemas/job-opportunity.schema'

interface StatusBadgeProps {
  status: JobOpportunity['status']
}

const statusColors: Record<JobOpportunity['status'], string> = {
  PENDING: 'bg-slate-100 text-slate-700',
  INTERESTING: 'bg-indigo-100 text-indigo-800',
  PREPARING_APPLICATION: 'bg-amber-100 text-amber-900',
  APPLICATION_SENT: 'bg-sky-100 text-sky-900',
  INTERVIEW: 'bg-violet-100 text-violet-900',
  OFFER_RECEIVED: 'bg-emerald-100 text-emerald-900',
  REJECTED: 'bg-rose-100 text-rose-900',
  DISCARDED: 'bg-slate-200 text-slate-700',
}

export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${statusColors[status]}`}
    >
      {es.statuses[status]}
    </span>
  )
}
