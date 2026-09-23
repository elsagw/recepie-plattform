// Mirrors the backend's response DTOs (see backend/src/main/java/.../*Response.java).

export interface CurrentUser {
  id: number
  email: string
  displayName: string
}

export interface ExternalRecipe {
  id: number
  sourceUrl: string
  title: string | null
  imageUrl: string | null
  domain: string
  createdAt: string
}

export interface MyReview {
  reviewId: number
  rating: number
  comment: string | null
  createdAt: string
  updatedAt: string
}

export interface Review {
  reviewId: number
  recipeId: number
  rating: number
  comment: string | null
  imageUrl: string | null
  createdAt: string
  updatedAt: string
}

export interface FeedItem {
  reviewId: number
  username: string
  recipe: {
    id: number
    title: string | null
    imageUrl: string | null
    domain: string
  }
  rating: number
  comment: string | null
  imageUrl: string | null
  createdAt: string
  updatedAt: string
  savedByCurrentUser: boolean
}

export interface FeedPage {
  content: FeedItem[]
  page: number
  size: number
  totalElements: number
}

export interface SavedRecipe {
  recipeId: number
  title: string | null
  imageUrl: string | null
  domain: string
  savedAt: string
}

export interface Friend {
  userId: number
  displayName: string
  addedAt: string
}

export interface ApiErrorBody {
  status: number
  code: string
  message: string
  path: string
  timestamp: string
}
