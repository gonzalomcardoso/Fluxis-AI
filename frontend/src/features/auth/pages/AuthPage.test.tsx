import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AuthProvider } from '@/features/auth/auth-context'
import { AuthPage } from '@/features/auth/pages/AuthPage'
import { renderWithProviders } from '@/test/render-with-providers'
import { es } from '@/i18n/es'
import { ApiError } from '@/lib/api-error'

vi.mock('@/features/auth/api/auth.api', () => ({
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
}))

import {
  getCurrentUser,
  login,
  register,
} from '@/features/auth/api/auth.api'

const getCurrentUserMock = vi.mocked(getCurrentUser)
const loginMock = vi.mocked(login)
const registerMock = vi.mocked(register)
const authenticatedUser = {
  id: '18a29b6b-92ce-4260-8c3d-b8cc54d7c6de',
  email: 'user@example.test',
  displayName: 'Synthetic User',
}

function renderAuthPage() {
  return renderWithProviders(
    <AuthProvider>
      <AuthPage />
    </AuthProvider>,
  )
}

describe('AuthPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    getCurrentUserMock.mockRejectedValue(new ApiError('Unauthorized', 401))
    loginMock.mockResolvedValue(authenticatedUser)
    registerMock.mockResolvedValue()
  })

  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it('submits credentials and displays an authentication error', async () => {
    const user = userEvent.setup()
    loginMock.mockRejectedValueOnce(new ApiError('Correo o contraseña no válidos', 401))
    renderAuthPage()

    await user.type(await screen.findByLabelText(es.auth.email), 'user@example.test')
    await user.type(screen.getByLabelText(es.auth.password), 'wrong-password')
    await user.click(screen.getByRole('button', { name: es.auth.loginAction }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña no válidos')
    expect(loginMock).toHaveBeenCalledWith('user@example.test', 'wrong-password')
  }, 15_000)

  it('registers an account and signs it in', async () => {
    const user = userEvent.setup()
    renderAuthPage()

    await user.click(await screen.findByRole('button', { name: es.auth.registerAction }))
    await user.type(screen.getByLabelText(es.auth.displayName), 'Synthetic User')
    await user.type(screen.getByLabelText(es.auth.email), 'user@example.test')
    await user.type(screen.getByLabelText(es.auth.password), 'synthetic-password')
    await user.click(screen.getByRole('button', { name: es.auth.registerAction }))

    expect(registerMock).toHaveBeenCalledWith(
      'Synthetic User',
      'user@example.test',
      'synthetic-password',
    )
    expect(loginMock).toHaveBeenCalledWith('user@example.test', 'synthetic-password')
  })

  it('hides public registration in prod mode', async () => {
    vi.stubEnv('MODE', 'prod')
    renderAuthPage()

    await screen.findByLabelText(es.auth.email)
    expect(screen.queryByRole('button', { name: es.auth.registerAction })).not.toBeInTheDocument()
  })
})
