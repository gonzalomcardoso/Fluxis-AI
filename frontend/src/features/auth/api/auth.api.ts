import { z } from 'zod'
import { apiRequest } from '@/lib/api-client'

const authenticatedUserSchema = z.object({
  id: z.string().uuid(),
  email: z.email(),
  displayName: z.string(),
})

export type AuthenticatedUser = z.infer<typeof authenticatedUserSchema>

export async function getCurrentUser(): Promise<AuthenticatedUser> {
  return authenticatedUserSchema.parse(await apiRequest('/auth/me'))
}

export async function login(email: string, password: string): Promise<AuthenticatedUser> {
  return authenticatedUserSchema.parse(await apiRequest('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  }))
}

export async function register(
  displayName: string,
  email: string,
  password: string,
): Promise<void> {
  await apiRequest('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ displayName, email, password }),
  })
}

export async function logout(): Promise<void> {
  await apiRequest('/auth/logout', { method: 'POST' })
}
