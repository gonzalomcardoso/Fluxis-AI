import { ApiError, ResponseValidationError } from '@/lib/api-error'
import { es } from '@/i18n/es'

const apiBaseUrl = (import.meta.env.VITE_API_URL || '/api').replace(/\/+$/, '')
const csrfPath = '/auth/csrf'
const unsafeMethods = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

export interface ResponseSchema<T> {
  safeParse(value: unknown):
    | { success: true; data: T }
    | { success: false; error: { issues: readonly unknown[] } }
}

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

export async function apiRequest<T = unknown>(
  path: string,
  init?: RequestInit,
  schema?: ResponseSchema<T>,
  allowCsrfRetry = true,
): Promise<T> {
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

  const { payload, rawBody } = await parseResponse(response)

  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      clearCsrfToken()
    }
    if (
      response.status === 403
      && allowCsrfRetry
      && unsafeMethods.has(method)
      && path !== csrfPath
    ) {
      return apiRequest(path, init, schema, false)
    }
    if (response.status === 401 && path !== '/auth/login' && path !== '/auth/register') {
      window.dispatchEvent(new Event('fluxis:unauthorized'))
    }
    throw new ApiError(
      problemMessage(payload, es.api.requestFailed(response.status)),
      response.status,
      isRecord(payload) ? readFieldErrors(payload.errors) : {},
      response.url,
      method,
      rawBody,
    )
  }

  if (schema) {
    const parsed = schema.safeParse(payload)
    if (!parsed.success) {
      if (import.meta.env.DEV) {
        console.error('API response validation failed', {
          url: response.url,
          method,
          issues: parsed.error.issues,
        })
      }
      throw new ResponseValidationError(
        import.meta.env.DEV ? 'API response validation failed' : es.api.invalidResponse,
        response.url,
        method,
        rawBody,
      )
    }
    return parsed.data
  }

  if (path === '/auth/login' || path === '/auth/logout') {
    clearCsrfToken()
  }

  return payload as T
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

async function parseResponse(response: Response): Promise<{ payload: unknown; rawBody: string }> {
  const responseText = await response.text()
  if (!responseText) {
    return { payload: null, rawBody: '' }
  }
  try {
    return { payload: JSON.parse(responseText) as unknown, rawBody: responseText }
  } catch {
    if (response.ok) {
      throw new ApiError(es.api.invalidResponse, response.status)
    }
    return { payload: null, rawBody: responseText }
  }
}
