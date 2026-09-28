import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { api, ApiError } from './client'

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

describe('api client', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/'
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('GET requests do not attach a CSRF header', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { ok: true }))

    await api.get('/api/feed')

    const [, init] = vi.mocked(fetch).mock.calls[0]!
    const headers = init!.headers as Record<string, string>
    expect(headers['X-XSRF-TOKEN']).toBeUndefined()
  })

  it('POST requests read the XSRF-TOKEN cookie and echo it back as a header', async () => {
    document.cookie = 'XSRF-TOKEN=abc123'
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(201, { id: 1 }))

    await api.post('/api/reviews', { recipeId: 1, rating: 5, comment: 'bra' })

    const [url, init] = vi.mocked(fetch).mock.calls[0]!
    const headers = init!.headers as Record<string, string>
    expect(url).toBe('http://localhost:8080/api/reviews')
    expect(init!.credentials).toBe('include')
    expect(headers['X-XSRF-TOKEN']).toBe('abc123')
    expect(init!.body).toBe(JSON.stringify({ recipeId: 1, rating: 5, comment: 'bra' }))
  })

  it('a 204 response resolves to undefined without parsing a body', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 204 }))

    const result = await api.delete('/api/recipes/1/save')

    expect(result).toBeUndefined()
  })

  it('a non-ok response throws an ApiError carrying the shared error shape', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse(422, {
        status: 422,
        code: 'INVALID_RATING',
        message: 'Betyg måste vara ett heltal mellan 1 och 5.',
        path: '/api/reviews',
        timestamp: '2026-01-01T00:00:00Z',
      }),
    )

    await expect(api.post('/api/reviews', { recipeId: 1, rating: 9 })).rejects.toMatchObject({
      status: 422,
      code: 'INVALID_RATING',
      message: 'Betyg måste vara ett heltal mellan 1 och 5.',
    })
  })

  it('falls back to a generic message when the error body is unparseable', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response('', { status: 500 }))

    let caught: unknown
    try {
      await api.get('/api/feed')
    } catch (error) {
      caught = error
    }

    expect(caught).toBeInstanceOf(ApiError)
    expect((caught as ApiError).code).toBe('UNKNOWN_ERROR')
  })

  it('postForm sends multipart data with the CSRF header but no explicit Content-Type', async () => {
    document.cookie = 'XSRF-TOKEN=abc123'
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { reviewId: 1 }))
    const formData = new FormData()
    formData.append('image', new Blob(['fake'], { type: 'image/jpeg' }), 'photo.jpg')

    await api.postForm('/api/reviews/1/image', formData)

    const [, init] = vi.mocked(fetch).mock.calls[0]!
    const headers = init?.headers as Record<string, string>
    expect(headers['X-XSRF-TOKEN']).toBe('abc123')
    expect(headers['Content-Type']).toBeUndefined()
    expect(init?.body).toBe(formData)
  })
})
