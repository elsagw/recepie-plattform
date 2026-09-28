import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import type { FeedItem, FeedPage } from '@/types/api'

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/feed', name: 'feed', component: { template: '<div />' } },
      { path: '/add-review', name: 'add-review', component: { template: '<div />' } },
      { path: '/recension/:id', name: 'review-detail', component: { template: '<div />' } },
    ],
  })
}

const feedItem: FeedItem = {
  reviewId: 44,
  username: 'elsa',
  userAvatarUrl: null,
  recipe: { id: 12, title: 'Exempelrecept', imageUrl: null, domain: 'example.com' },
  rating: 4,
  comment: 'Jag bytte grädde mot kokosmjölk.',
  imageUrl: null,
  createdAt: '2026-09-20T12:05:00Z',
  updatedAt: '2026-09-20T12:05:00Z',
  savedByCurrentUser: false,
}

describe('FeedView', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows the login landing page when logged out, not the feed', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(401, {}))

    const { default: FeedView } = await import('./FeedView.vue')
    const router = makeRouter()
    await router.push('/feed')
    await router.isReady()
    const wrapper = mount(FeedView, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Logga in med Google')
    expect(wrapper.text()).not.toContain('Jag bytte grädde mot kokosmjölk.')
    // Only /api/auth/me was called - the feed itself must never be fetched while logged out.
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('loads and renders the feed once the user is logged in', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(
        jsonResponse(200, { id: 1, email: 'elsa@example.com', displayName: 'Elsa', avatarUrl: null }),
      )
      .mockResolvedValueOnce(
        jsonResponse(200, { content: [feedItem], page: 0, size: 21, totalElements: 1 } satisfies FeedPage),
      )

    const { default: FeedView } = await import('./FeedView.vue')
    const router = makeRouter()
    await router.push('/feed')
    await router.isReady()
    const wrapper = mount(FeedView, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Exempelrecept')
    expect(wrapper.text()).toContain('elsa')
    expect(wrapper.text()).toContain('Jag bytte grädde mot kokosmjölk.')
    expect(wrapper.text()).not.toContain('Logga in med Google')
  })

  it('toggling save calls the save endpoint and flips the saved state', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(
        jsonResponse(200, { id: 1, email: 'elsa@example.com', displayName: 'Elsa', avatarUrl: null }),
      )
      .mockResolvedValueOnce(
        jsonResponse(200, { content: [feedItem], page: 0, size: 21, totalElements: 1 } satisfies FeedPage),
      )

    const { default: FeedView } = await import('./FeedView.vue')
    const router = makeRouter()
    await router.push('/feed')
    await router.isReady()
    const wrapper = mount(FeedView, { global: { plugins: [router] } })
    await flushPromises()

    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(201, {}))
    const likeButton = wrapper.find('.like-button')
    expect(likeButton.classes()).not.toContain('saved')

    await likeButton.trigger('click')
    await flushPromises()

    expect(fetch).toHaveBeenLastCalledWith(
      'http://localhost:8080/api/recipes/12/save',
      expect.objectContaining({ method: 'POST' }),
    )
    expect(wrapper.find('.like-button').classes()).toContain('saved')
  })
})
