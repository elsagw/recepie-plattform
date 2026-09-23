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
    {
      path: '/recension/:id',
      name: 'review-detail',
      component: () => import('../views/ReviewDetailView.vue'),
    },
    {
      path: '/sparat',
      name: 'saved',
      component: () => import('../views/SavedView.vue'),
    },
    {
      path: '/kompisar',
      name: 'friends',
      component: () => import('../views/FriendsView.vue'),
    },
    {
      path: '/konto',
      name: 'account',
      component: () => import('../views/AccountView.vue'),
    },
    {
      path: '/hjalp',
      name: 'help',
      component: () => import('../views/HelpView.vue'),
    },
  ],
})

export default router
