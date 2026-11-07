import { ApiError } from '@/lib/api-error'
import { es } from '@/i18n/es'

const apiBaseUrl = (import.meta.env.VITE_API_URL || '/api').replace(/\/+$/, '')
const csrfPath = '/auth/csrf'
const unsafeMethods = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

interface CsrfTokenResponse {
  token: string
  headerName: string
}

let csrfToken: CsrfTokenResponse | undefined
let csrfRequest: Promise<CsrfTokenResponse> | undefined

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function readFieldErrors(value: unknown): Record<string, string> {
  if (!isRecord(value)) {
    return {}
  }

  return Object.fromEntries(
    Object.entries(value).filter(
      (entry): entry is [string, string] => typeof entry[1] === 'string',
    ),
  )
}

function problemMessage(payload: unknown, fallback: string): string {
  if (!isRecord(payload)) {
    return fallback
  }

  if (typeof payload.detail === 'string' && payload.detail.length > 0) {
    return payload.detail
  }
  if (typeof payload.title === 'string' && payload.title.length > 0) {
    return payload.title
  }
  return fallback
}

export async function apiRequest(path: string, init?: RequestInit): Promise<unknown> {
  const method = (init?.method ?? 'GET').toUpperCase()
  const headers = new Headers(init?.headers)
  headers.set('Accept', 'application/json')
  if (init?.body) {
    headers.set('Content-Type', 'application/json')
  }

  if (unsafeMethods.has(method) && path !== csrfPath) {
    const csrf = await getCsrfToken()
    headers.set(csrf.headerName, csrf.token)
  }

  let response: Response
  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...init,
      credentials: 'include',
      headers,
    })
  } catch {
    throw new ApiError(es.form.networkError, 0)
  }

  const payload = await parseResponse(response)

  if (!response.ok) {
    if (response.status === 401 && path !== '/auth/login' && path !== '/auth/register') {
      window.dispatchEvent(new Event('fluxis:unauthorized'))
    }
    throw new ApiError(
      problemMessage(payload, es.api.requestFailed(response.status)),
      response.status,
      isRecord(payload) ? readFieldErrors(payload.errors) : {},
    )
  }

  if (path === '/auth/login' || path === '/auth/logout') {
    clearCsrfToken()
  }

  return payload
}

async function getCsrfToken(): Promise<CsrfTokenResponse> {
  if (csrfToken) {
    return csrfToken
  }
  if (!csrfRequest) {
    csrfRequest = apiRequest(csrfPath).then((response) => {
      if (
        !isRecord(response)
        || typeof response.token !== 'string'
        || typeof response.headerName !== 'string'
      ) {
        throw new ApiError(es.api.invalidResponse, 200)
      }
      csrfToken = { token: response.token, headerName: response.headerName }
      return csrfToken
    }).finally(() => {
      csrfRequest = undefined
    })
  }
  return csrfRequest
}

function clearCsrfToken() {
  csrfToken = undefined
  csrfRequest = undefined
}

async function parseResponse(response: Response): Promise<unknown> {
  const responseText = await response.text()
  if (!responseText) {
    return null
  }
  try {
    return JSON.parse(responseText) as unknown
  } catch {
    if (response.ok) {
      throw new ApiError(es.api.invalidResponse, response.status)
    }
    return null
  }
}
