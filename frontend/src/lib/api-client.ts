import { ApiError } from '@/lib/api-error'
import { es } from '@/i18n/es'

const apiBaseUrl = (import.meta.env.VITE_API_URL || '/api').replace(/\/+$/, '')

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
  let response: Response
  const headers = new Headers(init?.headers)
  headers.set('Accept', 'application/json')
  if (init?.body) {
    headers.set('Content-Type', 'application/json')
  }

  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...init,
      headers,
    })
  } catch {
    throw new ApiError(es.form.networkError, 0)
  }

  const responseText = await response.text()
  let payload: unknown = null
  if (responseText) {
    try {
      payload = JSON.parse(responseText) as unknown
    } catch {
      if (response.ok) {
        throw new ApiError(es.api.invalidResponse, response.status)
      }
    }
  }

  if (!response.ok) {
    throw new ApiError(
      problemMessage(payload, es.api.requestFailed(response.status)),
      response.status,
      isRecord(payload) ? readFieldErrors(payload.errors) : {},
    )
  }

  return payload
}
