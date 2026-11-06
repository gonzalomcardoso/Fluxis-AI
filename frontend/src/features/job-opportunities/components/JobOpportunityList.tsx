import { es } from '@/i18n/es'
import type { JobOpportunity } from '@/features/job-opportunities/schemas/job-opportunity.schema'
import { StatusBadge } from '@/features/job-opportunities/components/StatusBadge'

interface JobOpportunityListProps {
  opportunities: JobOpportunity[]
}

function formatDate(date: string): string {
  return new Intl.DateTimeFormat('es-ES', {
    dateStyle: 'medium',
    timeZone: 'UTC',
  }).format(new Date(date))
}

export function JobOpportunityList({ opportunities }: JobOpportunityListProps) {
  return (
    <div className="space-y-4">
      {opportunities.map((opportunity) => {
        const date = opportunity.postedOn ?? opportunity.createdAt
        const dateLabel = opportunity.postedOn ? es.list.postedOn : es.list.addedOn

        return (
          <article
            className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center sm:justify-between sm:p-6"
            key={opportunity.id}
          >
            <div className="min-w-0 space-y-2">
              <h2 className="break-words text-lg font-bold text-slate-950">{opportunity.title}</h2>
              <p className="text-sm text-slate-600">
                <span className="font-medium text-slate-800">{es.list.company}: </span>
                {opportunity.companyName}
              </p>
              <p className="text-sm text-slate-600">
                <span className="font-medium text-slate-800">{dateLabel}: </span>
                <time dateTime={date}>{formatDate(date)}</time>
              </p>
            </div>
            <div className="flex shrink-0 flex-wrap items-center gap-3">
              <StatusBadge status={opportunity.status} />
              {opportunity.url && (
                <a
                  className="rounded-lg px-3 py-2 text-sm font-semibold text-indigo-700 hover:bg-indigo-50"
                  href={opportunity.url}
                  rel="noopener noreferrer"
                  target="_blank"
                  aria-label={`${es.list.openJob}: ${opportunity.title}`}
                >
                  {es.list.openJob}
                </a>
              )}
            </div>
          </article>
        )
      })}
      <p className="sr-only" aria-live="polite">
        {es.list.results(opportunities.length)}
      </p>
    </div>
  )
}
