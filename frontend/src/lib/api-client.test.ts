import { beforeEach, describe, expect, it, vi } from 'vitest'

function jsonResponse(body: unknown, status = 200): Response {
  if (status === 204) {
    return new Response(null, { status })
  }
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('apiRequest session and CSRF handling', () => {
  beforeEach(() => {
    vi.resetModules()
  })

  it('sends cookies and fetches a CSRF token before each session change', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ token: 'csrf-one', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(jsonResponse({ id: 'user-id' }))
      .mockResolvedValueOnce(jsonResponse({ token: 'csrf-two', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(jsonResponse(null, 204))
    vi.stubGlobal('fetch', fetchMock)
    const { apiRequest } = await import('@/lib/api-client')

    await apiRequest('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: 'user@example.test', password: 'synthetic-password' }),
    })
    await apiRequest('/auth/logout', { method: 'POST' })

    const calls = fetchMock.mock.calls
    expect(calls).toHaveLength(4)
    expect(calls[0]?.[0]).toBe('/api/auth/csrf')
    expect(calls[0]?.[1]).toMatchObject({ credentials: 'include' })
    expect(new Headers(calls[1]?.[1]?.headers).get('X-CSRF-TOKEN')).toBe('csrf-one')
    expect(calls[1]?.[1]).toMatchObject({ credentials: 'include' })
    expect(calls[2]?.[0]).toBe('/api/auth/csrf')
    expect(new Headers(calls[3]?.[1]?.headers).get('X-CSRF-TOKEN')).toBe('csrf-two')
  })
})
