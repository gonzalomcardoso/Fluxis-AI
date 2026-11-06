import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { App } from '@/app/App'
import { getCurrentUser, login } from '@/features/auth/api/auth.api'
import { getJobOpportunities } from '@/features/job-opportunities/api/job-opportunities.api'
import { es } from '@/i18n/es'
import { ApiError } from '@/lib/api-error'
import { pageOf } from '@/test/job-opportunity.fixtures'

vi.mock('@/features/auth/api/auth.api', () => ({
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
}))

vi.mock('@/features/job-opportunities/api/job-opportunities.api', () => ({
  createJobOpportunity: vi.fn(),
  getJobOpportunities: vi.fn(),
}))

const getCurrentUserMock = vi.mocked(getCurrentUser)
const loginMock = vi.mocked(login)
const getOpportunitiesMock = vi.mocked(getJobOpportunities)
const authenticatedUser = {
  id: '18a29b6b-92ce-4260-8c3d-b8cc54d7c6de',
  email: 'user@example.test',
  displayName: 'Synthetic User',
}

describe('App authentication flow', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    getCurrentUserMock.mockRejectedValue(new ApiError('Unauthorized', 401))
    loginMock.mockResolvedValue(authenticatedUser)
    getOpportunitiesMock.mockResolvedValue(pageOf([]))
  })

  it("requires login before showing the user's opportunities", async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.type(await screen.findByLabelText(es.auth.email), 'user@example.test')
    await user.type(screen.getByLabelText(es.auth.password), 'synthetic-password')
    await user.click(screen.getByRole('button', { name: es.auth.loginAction }))

    expect(await screen.findByRole('heading', { name: es.list.title })).toBeInTheDocument()
    expect(loginMock).toHaveBeenCalledWith('user@example.test', 'synthetic-password')
    expect(getOpportunitiesMock).toHaveBeenCalled()
  }, 15_000)
})
