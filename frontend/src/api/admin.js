const API_BASE = '/api/admin'
const TOKEN_KEY = 'alspd_admin_token'

function buildUrl(path, params = {}) {
  const url = new URL(`${API_BASE}${path}`, window.location.origin)
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, value)
    }
  })
  return url
}

async function adminRequest(path, options = {}) {
  const token = options.token || getStoredAdminToken()
  const isFormData = options.body instanceof FormData
  const response = await fetch(buildUrl(path, options.params), {
    method: options.method || 'GET',
    headers: {
      'X-Admin-Token': token,
      ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
      ...(options.headers || {})
    },
    body: options.body === undefined ? undefined : isFormData ? options.body : JSON.stringify(options.body)
  })

  let payload = null
  try {
    payload = await response.json()
  } catch (error) {
    payload = null
  }

  if (!response.ok) {
    throw new Error(payload?.message || `HTTP ${response.status}`)
  }
  if (!payload?.success) {
    throw new Error(payload?.message || '请求失败')
  }
  return payload.data
}

export function getStoredAdminToken() {
  return window.localStorage.getItem(TOKEN_KEY) || ''
}

export function setStoredAdminToken(token) {
  window.localStorage.setItem(TOKEN_KEY, token)
}

export function clearStoredAdminToken() {
  window.localStorage.removeItem(TOKEN_KEY)
}

export function getAdminPosts(params) {
  return adminRequest('/posts', { params })
}

export function getAdminPost(id) {
  return adminRequest(`/posts/${id}`)
}

export function createAdminPost(body) {
  return adminRequest('/posts', { method: 'POST', body })
}

export function updateAdminPost(id, body) {
  return adminRequest(`/posts/${id}`, { method: 'PUT', body })
}

export function deleteAdminPost(id) {
  return adminRequest(`/posts/${id}`, { method: 'DELETE' })
}

export function getAdminCategories() {
  return adminRequest('/categories')
}

export function createAdminCategory(body) {
  return adminRequest('/categories', { method: 'POST', body })
}

export function updateAdminCategory(id, body) {
  return adminRequest(`/categories/${id}`, { method: 'PUT', body })
}

export function deleteAdminCategory(id) {
  return adminRequest(`/categories/${id}`, { method: 'DELETE' })
}

export function getAdminTags() {
  return adminRequest('/tags')
}

export function createAdminTag(body) {
  return adminRequest('/tags', { method: 'POST', body })
}

export function updateAdminTag(id, body) {
  return adminRequest(`/tags/${id}`, { method: 'PUT', body })
}

export function deleteAdminTag(id) {
  return adminRequest(`/tags/${id}`, { method: 'DELETE' })
}

export function uploadAdminImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return adminRequest('/uploads/images', {
    method: 'POST',
    body: formData
  })
}
