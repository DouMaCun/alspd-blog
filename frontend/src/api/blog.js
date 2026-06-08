const API_BASE = '/api'

async function request(path, params = {}) {
  const url = new URL(`${API_BASE}${path}`, window.location.origin)
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, value)
    }
  })

  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }

  const payload = await response.json()
  if (!payload.success) {
    throw new Error(payload.message || '请求失败')
  }
  return payload.data
}

export function getPosts(params) {
  return request('/posts', params)
}

export function getFeaturedPosts(limit = 4) {
  return request('/posts/featured', { limit })
}

export function getPost(slug) {
  return request(`/posts/${encodeURIComponent(slug)}`)
}

export function getCategories() {
  return request('/categories')
}

export function getTags() {
  return request('/tags')
}

export function getStats() {
  return request('/stats')
}
