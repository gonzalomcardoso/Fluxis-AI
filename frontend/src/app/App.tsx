import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { Layout } from '@/app/Layout'
import { NotFoundPage } from '@/app/NotFoundPage'
import { JobOpportunityListPage } from '@/features/job-opportunities/pages/JobOpportunityListPage'
import { NewJobOpportunityPage } from '@/features/job-opportunities/pages/NewJobOpportunityPage'

const queryClient = new QueryClient({
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
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<Navigate to="/oportunidades" replace />} />
            <Route path="oportunidades" element={<JobOpportunityListPage />} />
            <Route path="oportunidades/nueva" element={<NewJobOpportunityPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
