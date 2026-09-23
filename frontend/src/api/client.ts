import type { ApiErrorBody } from '@/types/api'

// Configurable so the same build can point at a different backend in prod (see .env.example).
export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080'

export class ApiError extends Error {
  status: number
  code: string
  body: ApiErrorBody | null

  constructor(status: number, code: string, message: string, body: ApiErrorBody | null) {
    super(message)
    this.status = status
    this.code = code
    this.body = body
  }
}

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'))
  return match ? decodeURIComponent(match[1] ?? '') : null
}

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (!SAFE_METHODS.has(method)) {
    const csrfToken = readCookie('XSRF-TOKEN')
    if (csrfToken) {
      headers['X-XSRF-TOKEN'] = csrfToken
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    credentials: 'include',
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  const json = text ? (JSON.parse(text) as unknown) : null

  if (!response.ok) {
    const errorBody = json as ApiErrorBody | null
    throw new ApiError(
      response.status,
      errorBody?.code ?? 'UNKNOWN_ERROR',
      errorBody?.message ?? 'Något gick fel. Försök igen.',
      errorBody,
    )
  }

  return json as T
}

async function requestForm<T>(path: string, formData: FormData): Promise<T> {
  const headers: Record<string, string> = {}
  const csrfToken = readCookie('XSRF-TOKEN')
  if (csrfToken) {
    headers['X-XSRF-TOKEN'] = csrfToken
  }
  // No Content-Type header here - the browser must set it itself (with the multipart
  // boundary) when the body is a FormData, or the upload won't parse on the server.

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers,
    credentials: 'include',
    body: formData,
  })

  const text = await response.text()
  const json = text ? (JSON.parse(text) as unknown) : null

  if (!response.ok) {
    const errorBody = json as ApiErrorBody | null
    throw new ApiError(
      response.status,
      errorBody?.code ?? 'UNKNOWN_ERROR',
      errorBody?.message ?? 'Något gick fel. Försök igen.',
      errorBody,
    )
  }

  return json as T
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  delete: <T>(path: string) => request<T>('DELETE', path),
  postForm: <T>(path: string, formData: FormData) => requestForm<T>(path, formData),
}

/** Resolves a possibly-relative image path (e.g. "/uploads/xyz.jpg") against the API host. */
export function resolveImageUrl(url: string | null | undefined): string | null {
  if (!url) return null
  return url.startsWith('/') ? `${API_BASE_URL}${url}` : url
}
