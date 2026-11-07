import { Link } from 'react-router'
import { es } from '@/i18n/es'

export function NotFoundPage() {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
      <p className="text-sm font-semibold uppercase tracking-wide text-indigo-600">404</p>
      <h1 className="mt-2 text-2xl font-bold text-slate-950">{es.notFound.title}</h1>
      <p className="mt-2 text-slate-600">{es.notFound.description}</p>
      <Link
        className="mt-6 inline-flex rounded-lg bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-700"
        to="/oportunidades"
      >
        {es.notFound.link}
      </Link>
    </section>
  )
}
