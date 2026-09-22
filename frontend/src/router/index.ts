import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/feed',
    },
    {
      path: '/feed',
      name: 'feed',
      component: () => import('../views/FeedView.vue'),
    },
    {
      path: '/add-review',
      name: 'add-review',
      component: () => import('../views/AddReviewView.vue'),
    },
  ],
})

export default router
