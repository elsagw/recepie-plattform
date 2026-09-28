import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import type { ExternalRecipe, Review } from '@/types/api'

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/feed', name: 'feed', component: { template: '<div />' } },
      { path: '/add-review', name: 'add-review', component: { template: '<div />' } },
    ],
  })
}

const recipe: ExternalRecipe = {
  id: 12,
  sourceUrl: 'https://example.com/recept',
  title: 'Exempelrecept',
  imageUrl: null,
  domain: 'example.com',
  createdAt: '2026-09-20T12:00:00Z',
}

describe('AddReviewView', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('gates the whole view behind login, not just publishing', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(401, {}))

    const { default: AddReviewView } = await import('./AddReviewView.vue')
    const router = makeRouter()
    await router.push('/add-review')
    await router.isReady()
    const wrapper = mount(AddReviewView, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Du måste vara inloggad')
    expect(wrapper.find('#url').exists()).toBe(false)
  })

  it('scraping a URL shows the recipe preview and past reviews', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(
        jsonResponse(200, { id: 1, email: 'elsa@example.com', displayName: 'Elsa', avatarUrl: null }),
      )
      .mockResolvedValueOnce(jsonResponse(200, recipe))
      .mockResolvedValueOnce(
        jsonResponse(200, [
          { reviewId: 40, rating: 4, comment: 'Bra men lite för salt förra gången.', createdAt: '2026-06-01T18:00:00Z', updatedAt: '2026-06-01T18:00:00Z' },
        ]),
      )

    const { default: AddReviewView } = await import('./AddReviewView.vue')
    const router = makeRouter()
    await router.push('/add-review')
    await router.isReady()
    const wrapper = mount(AddReviewView, { global: { plugins: [router] } })
    await flushPromises()

    await wrapper.find('#url').setValue('https://example.com/recept')
    await wrapper.find('.url-form').trigger('submit.prevent')
    await flushPromises()

    expect(fetch).toHaveBeenNthCalledWith(
      2,
      'http://localhost:8080/api/recipes/scrape?url=https%3A%2F%2Fexample.com%2Frecept',
      expect.objectContaining({ method: 'POST' }),
    )
    expect(wrapper.text()).toContain('Exempelrecept')
    expect(wrapper.text()).toContain('Bra men lite för salt förra gången.')
  })

  it('publishing posts the review and navigates to the feed', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(
        jsonResponse(200, { id: 1, email: 'elsa@example.com', displayName: 'Elsa', avatarUrl: null }),
      )
      .mockResolvedValueOnce(jsonResponse(200, recipe))
      .mockResolvedValueOnce(jsonResponse(200, []))

    const { default: AddReviewView } = await import('./AddReviewView.vue')
    const router = makeRouter()
    const pushSpy = vi.spyOn(router, 'push')
    await router.push('/add-review')
    await router.isReady()
    const wrapper = mount(AddReviewView, { global: { plugins: [router] } })
    await flushPromises()

    await wrapper.find('#url').setValue('https://example.com/recept')
    await wrapper.find('.url-form').trigger('submit.prevent')
    await flushPromises()

    // Publish button starts disabled until a rating is picked (rating < 1 guards publish()).
    expect(wrapper.find('.review-form button[type="submit"]').attributes('disabled')).toBeDefined()

    await wrapper.find('[aria-label="5 stjärnor"]').trigger('click')

    const created: Review = {
      reviewId: 99,
      recipeId: 12,
      rating: 5,
      comment: null,
      imageUrl: null,
      createdAt: '2026-09-28T10:00:00Z',
      updatedAt: '2026-09-28T10:00:00Z',
    }
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(201, created))

    await wrapper.find('.review-form').trigger('submit.prevent')
    await flushPromises()

    expect(fetch).toHaveBeenLastCalledWith(
      'http://localhost:8080/api/reviews',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ recipeId: 12, rating: 5, comment: null }),
      }),
    )
    expect(pushSpy).toHaveBeenCalledWith('/feed')
  })
})
