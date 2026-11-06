import { Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { NewJobOpportunityPage } from '@/features/job-opportunities/pages/NewJobOpportunityPage'
import { JobOpportunityListPage } from '@/features/job-opportunities/pages/JobOpportunityListPage'
import {
  createJobOpportunity,
  getJobOpportunities,
} from '@/features/job-opportunities/api/job-opportunities.api'
import type { JobOpportunity } from '@/features/job-opportunities/schemas/job-opportunity.schema'
import { jobOpportunityFixture, pageOf } from '@/test/job-opportunity.fixtures'
import { renderWithProviders } from '@/test/render-with-providers'
import { es } from '@/i18n/es'

vi.mock('@/features/job-opportunities/api/job-opportunities.api', () => ({
  createJobOpportunity: vi.fn(),
  getJobOpportunities: vi.fn(),
}))

const createMock = vi.mocked(createJobOpportunity)
const getMock = vi.mocked(getJobOpportunities)

describe('job opportunity create-to-list flow', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows a newly created opportunity in the list after navigation', async () => {
    const user = userEvent.setup()
    let savedOpportunities: JobOpportunity[] = []
    getMock.mockImplementation(async () => pageOf(savedOpportunities))
    createMock.mockImplementation(async (request) => {
      const created = {
        ...jobOpportunityFixture,
        companyName: request.companyName,
        title: request.title,
      }
      savedOpportunities = [created]
      return created
    })

    renderWithProviders(
      <Routes>
        <Route path="/oportunidades/nueva" element={<NewJobOpportunityPage />} />
        <Route path="/oportunidades" element={<JobOpportunityListPage />} />
      </Routes>,
      { initialEntries: ['/oportunidades/nueva'] },
    )

    await user.type(screen.getByLabelText('Empresa *'), 'Acme')
    await user.type(screen.getByLabelText('Puesto *'), 'Platform Engineer')
    await user.click(screen.getByRole('button', { name: es.form.submit }))

    expect(await screen.findByRole('heading', { name: 'Platform Engineer' })).toBeInTheDocument()
    expect(screen.getByText(es.list.createdNotice)).toBeInTheDocument()
    expect(getMock).toHaveBeenCalled()
  }, 15_000)
})
