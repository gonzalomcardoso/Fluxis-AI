import type { ReactElement } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, type RenderOptions } from '@testing-library/react'
import { MemoryRouter } from 'react-router'

interface ProviderOptions extends Omit<RenderOptions, 'wrapper'> {
  initialEntries?: string[]
}

export function renderWithProviders(
  ui: ReactElement,
  { initialEntries = ['/'], ...renderOptions }: ProviderOptions = {},
) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  const result = render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>
    </QueryClientProvider>,
    renderOptions,
  )

  return { queryClient, ...result }
}
