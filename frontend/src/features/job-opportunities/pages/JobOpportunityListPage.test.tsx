import { act, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { JobOpportunityListPage } from '@/features/job-opportunities/pages/JobOpportunityListPage'
import { getJobOpportunities } from '@/features/job-opportunities/api/job-opportunities.api'
import { jobOpportunityFixture, pageOf } from '@/test/job-opportunity.fixtures'
import { renderWithProviders } from '@/test/render-with-providers'
import { ApiError } from '@/lib/api-error'
import { es } from '@/i18n/es'

vi.mock('@/features/job-opportunities/api/job-opportunities.api', () => ({
  createJobOpportunity: vi.fn(),
  getJobOpportunities: vi.fn(),
}))

const getMock = vi.mocked(getJobOpportunities)

describe('JobOpportunityListPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows a loading state while the list request is pending', async () => {
    let resolveList: ((value: ReturnType<typeof pageOf>) => void) | undefined
    getMock.mockReturnValue(new Promise((resolve) => {
      resolveList = resolve
    }))
    renderWithProviders(<JobOpportunityListPage />)

    expect(screen.getByRole('status')).toHaveTextContent(es.list.loading)

    await act(async () => {
      resolveList?.(pageOf([]))
    })
    expect(await screen.findByRole('heading', { name: es.list.title })).toBeInTheDocument()
  })

  it('shows the empty state when there are no opportunities', async () => {
    getMock.mockResolvedValue(pageOf([]))
    renderWithProviders(<JobOpportunityListPage />)

    expect(await screen.findByRole('heading', { name: es.list.emptyTitle })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: es.list.emptyAction })).toHaveAttribute(
      'href',
      '/oportunidades/nueva',
    )
  })

  it('shows opportunity details and a safe external link', async () => {
    getMock.mockResolvedValue(pageOf([jobOpportunityFixture]))
    renderWithProviders(<JobOpportunityListPage />)

    expect(await screen.findByRole('heading', { name: 'Backend Developer' })).toBeInTheDocument()
    expect(screen.getByText('Acme')).toBeInTheDocument()
    expect(screen.getByText(es.statuses.INTERESTING)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: `${es.list.openJob}: Backend Developer` }))
      .toHaveAttribute('rel', 'noopener noreferrer')
  })

  it('shows a retry action when the API request fails', async () => {
    const user = userEvent.setup()
    getMock.mockRejectedValueOnce(new ApiError('API no disponible.', 503))
    getMock.mockResolvedValueOnce(pageOf([]))
    renderWithProviders(<JobOpportunityListPage />)

    expect(await screen.findByRole('alert')).toHaveTextContent('API no disponible.')
    await user.click(screen.getByRole('button', { name: es.list.retry }))

    expect(await screen.findByRole('heading', { name: es.list.emptyTitle })).toBeInTheDocument()
  })
})
