import { QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Layout } from '@/app/Layout'
import { NotFoundPage } from '@/app/NotFoundPage'
import { AuthProvider, useAuth } from '@/features/auth/auth-context'
import { AuthPage } from '@/features/auth/pages/AuthPage'
import { JobOpportunityListPage } from '@/features/job-opportunities/pages/JobOpportunityListPage'
import { NewJobOpportunityPage } from '@/features/job-opportunities/pages/NewJobOpportunityPage'
import { CrmPage } from '@/features/crm/CrmPage'
import { JobApplicationsPage } from '@/features/job-applications/JobApplicationsPage'
import { ClientServicesPage } from '@/features/client-services/ClientServicesPage'
import { ProposalsPage } from '@/features/proposals/ProposalsPage'
import { SubscriptionsPage } from '@/features/subscriptions/SubscriptionsPage'
import { TasksPage } from '@/features/tasks/TasksPage'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { es } from '@/i18n/es'

const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error, query) => {
      if (import.meta.env.DEV) {
        console.error('TanStack Query error', {
          queryKey: query.queryKey,
          error,
        })
      }
    },
  }),
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
      <BrowserRouter>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

function AppRoutes() {
  const { status, error, refresh } = useAuth()

  if (status === 'loading') {
    return <main className="p-8 text-center text-slate-600" role="status">{es.auth.checkingSession}</main>
  }
  if (status === 'error') {
    return (
      <main className="mx-auto max-w-lg space-y-4 p-8">
        <Alert variant="error">{error}</Alert>
        <Button onClick={() => void refresh()}>{es.auth.retrySession}</Button>
      </main>
    )
  }
  if (status === 'unauthenticated') {
    return <AuthPage />
  }

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="oportunidades" element={<JobOpportunityListPage />} />
        <Route path="oportunidades/nueva" element={<NewJobOpportunityPage />} />
        <Route path="crm" element={<CrmPage />} />
        <Route path="candidaturas" element={<JobApplicationsPage />} />
        <Route path="clientes-servicios" element={<ClientServicesPage />} />
        <Route path="propuestas" element={<ProposalsPage />} />
        <Route path="suscripciones" element={<SubscriptionsPage />} />
        <Route path="tareas" element={<TasksPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
