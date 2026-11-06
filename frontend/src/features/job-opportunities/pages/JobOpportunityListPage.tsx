import { Link, useLocation } from 'react-router'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { es } from '@/i18n/es'
import { ApiError } from '@/lib/api-error'
import { JobOpportunityList } from '@/features/job-opportunities/components/JobOpportunityList'
import { useJobOpportunities } from '@/features/job-opportunities/api/job-opportunities.queries'

function readNotice(state: unknown): string | undefined {
  if (
    typeof state === 'object' &&
    state !== null &&
    'notice' in state &&
    typeof state.notice === 'string'
  ) {
    return state.notice
  }
  return undefined
}

export function JobOpportunityListPage() {
  const location = useLocation()
  const query = useJobOpportunities()
  const notice = readNotice(location.state)

  if (query.isPending) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center text-slate-600 shadow-sm" role="status">
        {es.list.loading}
      </div>
    )
  }

  if (query.isError) {
    const message =
      query.error instanceof ApiError
        ? query.error.message
        : es.list.loadErrorDescription
    return (
      <section className="mx-auto max-w-2xl space-y-4">
        <Alert variant="error">
          <strong className="block">{es.list.loadErrorTitle}</strong>
          <span className="mt-1 block">{message}</span>
        </Alert>
        <Button onClick={() => void query.refetch()}>{es.list.retry}</Button>
      </section>
    )
  }

  const opportunities = query.data.content

  return (
    <section className="space-y-7">
      {notice && <Alert variant="success">{notice}</Alert>}
      <div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-wide text-indigo-700">
            {es.list.eyebrow}
          </p>
          <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">
            {es.list.title}
          </h1>
          <p className="mt-2 text-slate-600">{es.list.description}</p>
        </div>
        <Link
          className="inline-flex min-h-11 items-center justify-center rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-700"
          to="/oportunidades/nueva"
        >
          {es.list.newOpportunity}
        </Link>
      </div>

      {opportunities.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-12 text-center">
          <h2 className="text-lg font-bold text-slate-900">{es.list.emptyTitle}</h2>
          <p className="mx-auto mt-2 max-w-md text-slate-600">{es.list.emptyDescription}</p>
          <Link
            className="mt-5 inline-flex min-h-11 items-center justify-center rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
            to="/oportunidades/nueva"
          >
            {es.list.emptyAction}
          </Link>
        </div>
      ) : (
        <>
          <p className="text-sm text-slate-500">{es.list.results(query.data.totalElements)}</p>
          <JobOpportunityList opportunities={opportunities} />
        </>
      )}
    </section>
  )
}
