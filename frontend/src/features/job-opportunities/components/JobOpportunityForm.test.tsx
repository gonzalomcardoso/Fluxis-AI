import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { JobOpportunityForm } from '@/features/job-opportunities/components/JobOpportunityForm'
import { createJobOpportunity } from '@/features/job-opportunities/api/job-opportunities.api'
import { jobOpportunityFixture } from '@/test/job-opportunity.fixtures'
import { renderWithProviders } from '@/test/render-with-providers'
import { ApiError } from '@/lib/api-error'
import { es } from '@/i18n/es'

vi.mock('@/features/job-opportunities/api/job-opportunities.api', () => ({
  createJobOpportunity: vi.fn(),
  getJobOpportunities: vi.fn(),
}))

const createMock = vi.mocked(createJobOpportunity)

describe('JobOpportunityForm', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows Zod validation errors when submitted empty', async () => {
    const user = userEvent.setup()
    renderWithProviders(<JobOpportunityForm />)

    await user.click(screen.getByRole('button', { name: es.form.submit }))

    expect(await screen.findByText(es.form.companyRequired)).toBeInTheDocument()
    expect(screen.getByText(es.form.titleRequired)).toBeInTheDocument()
    expect(createMock).not.toHaveBeenCalled()
  })

  it('sends normalized values when the form is valid', async () => {
    const user = userEvent.setup()
    createMock.mockResolvedValue(jobOpportunityFixture)
    renderWithProviders(<JobOpportunityForm />)

    await user.type(screen.getByLabelText('Empresa *'), '  Acme  ')
    await user.type(screen.getByLabelText('Puesto *'), '  Backend Developer  ')
    await user.type(
      screen.getByLabelText('URL de la oferta'),
      'https://jobs.example.test/acme',
    )
    await user.click(screen.getByRole('button', { name: es.form.submit }))

    await waitFor(() => {
      expect(createMock).toHaveBeenCalledWith({
        companyName: 'Acme',
        title: 'Backend Developer',
        url: 'https://jobs.example.test/acme',
        source: undefined,
        postedOn: undefined,
        description: undefined,
        requirements: undefined,
        technologies: [],
        status: 'PENDING',
      })
    })
  })

  it('shows a general error when the API request fails', async () => {
    const user = userEvent.setup()
    createMock.mockRejectedValue(new ApiError('El backend no está disponible.', 503))
    renderWithProviders(<JobOpportunityForm />)

    await user.type(screen.getByLabelText('Empresa *'), 'Acme')
    await user.type(screen.getByLabelText('Puesto *'), 'Backend Developer')
    await user.click(screen.getByRole('button', { name: es.form.submit }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El backend no está disponible.',
    )
  })
})
