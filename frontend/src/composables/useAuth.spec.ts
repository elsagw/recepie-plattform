import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { flushPromises } from '@vue/test-utils'

const originalLocation = window.location

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

describe('useAuth', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.stubGlobal('fetch', vi.fn())
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { ...originalLocation, href: '' },
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    Object.defineProperty(window, 'location', { configurable: true, value: originalLocation })
  })

  it('fetches the current user on first use and exposes it once resolved', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse(200, { id: 1, email: 'elsa@example.com', displayName: 'Elsa', avatarUrl: null }),
    )

    const { useAuth } = await import('./useAuth')
    const { currentUser, isLoading } = useAuth()

    expect(isLoading.value).toBe(true)
    await flushPromises()

    expect(isLoading.value).toBe(false)
    expect(currentUser.value).toEqual({ id: 1, email: 'elsa@example.com', displayName: 'Elsa', avatarUrl: null })
  })

  it('treats a 401 as "not logged in" rather than an error', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse(401, { status: 401, code: 'UNAUTHORIZED', message: 'Du måste vara inloggad.', path: '/api/auth/me', timestamp: '2026-01-01T00:00:00Z' }),
    )
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})

    const { useAuth } = await import('./useAuth')
    const { currentUser, isLoading } = useAuth()
    await flushPromises()

    expect(isLoading.value).toBe(false)
    expect(currentUser.value).toBeNull()
    expect(consoleError).not.toHaveBeenCalled()
  })

  it('login() navigates to the Google OAuth2 authorization endpoint', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(401, {}))
    const { useAuth } = await import('./useAuth')
    const { login } = useAuth()

    login()

    expect(window.location.href).toBe('http://localhost:8080/oauth2/authorization/google')
  })

  it('logout() calls the logout endpoint and clears the current user', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse(200, { id: 1, email: 'elsa@example.com', displayName: 'Elsa', avatarUrl: null }),
    )
    const { useAuth } = await import('./useAuth')
    const { currentUser, logout } = useAuth()
    await flushPromises()
    expect(currentUser.value).not.toBeNull()

    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 204 }))
    await logout()

    expect(currentUser.value).toBeNull()
    expect(fetch).toHaveBeenLastCalledWith(
      'http://localhost:8080/api/auth/logout',
      expect.objectContaining({ method: 'POST', credentials: 'include' }),
    )
  })
})
