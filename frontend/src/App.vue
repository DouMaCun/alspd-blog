<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  ArrowLeft,
  BookOpen,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Eye,
  Hash,
  Layers3,
  Search,
  Sparkles
} from 'lucide-vue-next'
import {
  getCategories,
  getFeaturedPosts,
  getPost,
  getPosts,
  getStats,
  getTags
} from './api/blog'
import AdminPanel from './components/AdminPanel.vue'
import heroImage from './assets/knowledge-desk.png'

const pageSize = 6

const posts = ref([])
const featuredPosts = ref([])
const categories = ref([])
const tags = ref([])
const stats = ref({
  postCount: 0,
  categoryCount: 0,
  tagCount: 0,
  totalViews: 0
})
const pageInfo = ref({
  current: 1,
  pages: 0,
  total: 0
})

const keyword = ref('')
const searchDraft = ref('')
const selectedCategory = ref('')
const selectedTag = ref('')
const activePost = ref(null)
const loading = ref(false)
const detailLoading = ref(false)
const error = ref('')
const isAdminView = ref(window.location.pathname === '/admin')

const contentBlocks = computed(() => {
  if (!activePost.value?.content) {
    return []
  }
  return activePost.value.content
    .split(/\n+/)
    .map((item) => item.trim())
    .filter(Boolean)
})

const activeCategoryName = computed(() => {
  const category = categories.value.find((item) => item.slug === selectedCategory.value)
  return category?.name || '全部分类'
})

const canGoPrev = computed(() => pageInfo.value.current > 1)
const canGoNext = computed(() => pageInfo.value.current < pageInfo.value.pages)

async function loadInitialData() {
  error.value = ''
  try {
    const [categoryData, tagData, statsData, featuredData] = await Promise.all([
      getCategories(),
      getTags(),
      getStats(),
      getFeaturedPosts(4)
    ])
    categories.value = categoryData
    tags.value = tagData
    stats.value = statsData
    featuredPosts.value = featuredData
    await loadPosts(1)
  } catch (err) {
    error.value = err.message || '加载失败'
  }
}

async function loadPosts(nextPage = pageInfo.value.current || 1) {
  loading.value = true
  error.value = ''
  try {
    const data = await getPosts({
      page: nextPage,
      size: pageSize,
      keyword: keyword.value,
      categorySlug: selectedCategory.value,
      tagSlug: selectedTag.value
    })
    posts.value = data.records || []
    pageInfo.value = {
      current: data.current || nextPage,
      pages: data.pages || 0,
      total: data.total || 0
    }
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function applySearch() {
  keyword.value = searchDraft.value.trim()
  activePost.value = null
  loadPosts(1)
}

function selectCategory(slug) {
  selectedCategory.value = selectedCategory.value === slug ? '' : slug
  activePost.value = null
  loadPosts(1)
}

function selectTag(slug) {
  selectedTag.value = selectedTag.value === slug ? '' : slug
  activePost.value = null
  loadPosts(1)
}

async function openPost(post) {
  detailLoading.value = true
  error.value = ''
  try {
    activePost.value = await getPost(post.slug)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (err) {
    error.value = err.message || '文章加载失败'
  } finally {
    detailLoading.value = false
  }
}

function closePost() {
  activePost.value = null
}

function syncRoute() {
  isAdminView.value = window.location.pathname === '/admin'
  if (!isAdminView.value && !posts.value.length) {
    loadInitialData()
  }
}

function showAdmin() {
  window.history.pushState({}, '', '/admin')
  isAdminView.value = true
}

function showBlog() {
  window.history.pushState({}, '', '/')
  isAdminView.value = false
  if (!posts.value.length) {
    loadInitialData()
  }
}

function goPage(offset) {
  const nextPage = pageInfo.value.current + offset
  if (nextPage < 1 || nextPage > pageInfo.value.pages) {
    return
  }
  loadPosts(nextPage)
}

function formatDate(value) {
  if (!value) {
    return '未发布'
  }
  return String(value).slice(0, 10).replaceAll('-', '.')
}

onMounted(() => {
  window.addEventListener('popstate', syncRoute)
  if (!isAdminView.value) {
    loadInitialData()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('popstate', syncRoute)
})
</script>

<template>
  <AdminPanel v-if="isAdminView" @back="showBlog" />
  <div v-else class="app-shell">
    <header class="site-header">
      <a class="brand" href="#" @click.prevent="closePost">
        <span class="brand-mark">A</span>
        <span>ALSPD.LOG</span>
      </a>
      <nav class="top-stats" aria-label="站点统计">
        <span>{{ stats.postCount }} 篇</span>
        <span>{{ stats.categoryCount }} 类</span>
        <span>{{ stats.totalViews }} 次阅读</span>
        <button class="header-admin-link" type="button" @click="showAdmin">管理</button>
      </nav>
    </header>

    <section class="intro-band">
      <div class="intro-copy">
        <p class="eyebrow">Java · 微服务 · 算法 · 嵌入式 · AI</p>
        <h1>把工程实践写成可复用的技术笔记</h1>
        <p class="intro-text">
          记录系统设计、识别算法、嵌入式调试、AI 工作流和开发复盘。
        </p>
      </div>
      <img :src="heroImage" alt="" class="intro-image" />
    </section>

    <section class="toolbar" aria-label="文章筛选">
      <form class="search-box" @submit.prevent="applySearch">
        <Search :size="18" aria-hidden="true" />
        <input v-model="searchDraft" type="search" placeholder="搜索文章、算法、框架" />
        <button type="submit">搜索</button>
      </form>
      <div class="current-filter">
        <Layers3 :size="17" aria-hidden="true" />
        <span>{{ activeCategoryName }}</span>
      </div>
    </section>

    <p v-if="error" class="error-line">{{ error }}</p>

    <main class="content-grid">
      <aside class="side-rail" aria-label="博客导航">
        <section class="rail-section">
          <h2>分类</h2>
          <div class="filter-list">
            <button
              class="filter-button"
              :class="{ active: selectedCategory === '' }"
              type="button"
              @click="selectCategory('')"
            >
              全部
            </button>
            <button
              v-for="category in categories"
              :key="category.slug"
              class="filter-button"
              :class="{ active: selectedCategory === category.slug }"
              type="button"
              @click="selectCategory(category.slug)"
            >
              {{ category.name }}
            </button>
          </div>
        </section>

        <section class="rail-section">
          <h2>标签</h2>
          <div class="tag-cloud">
            <button
              v-for="tag in tags"
              :key="tag.slug"
              class="tag-button"
              :class="{ active: selectedTag === tag.slug }"
              type="button"
              @click="selectTag(tag.slug)"
            >
              <Hash :size="14" aria-hidden="true" />
              {{ tag.name }}
            </button>
          </div>
        </section>
      </aside>

      <section class="main-panel" aria-live="polite">
        <article v-if="activePost" class="post-detail">
          <button class="back-button" type="button" @click="closePost">
            <ArrowLeft :size="17" aria-hidden="true" />
            返回列表
          </button>

          <div class="detail-heading">
            <p class="post-meta">
              <CalendarDays :size="16" aria-hidden="true" />
              {{ formatDate(activePost.publishedAt) }}
              <span v-if="activePost.category">{{ activePost.category.name }}</span>
              <span>
                <Eye :size="15" aria-hidden="true" />
                {{ activePost.viewCount || 0 }}
              </span>
            </p>
            <h2>{{ activePost.title }}</h2>
            <p>{{ activePost.summary }}</p>
          </div>

          <div class="detail-tags">
            <span v-for="tag in activePost.tags" :key="tag.slug">#{{ tag.name }}</span>
          </div>

          <div class="post-content">
            <p v-for="(block, index) in contentBlocks" :key="index">{{ block }}</p>
          </div>
        </article>

        <template v-else>
          <section v-if="featuredPosts.length" class="featured-strip">
            <div class="section-title">
              <Sparkles :size="18" aria-hidden="true" />
              <h2>精选</h2>
            </div>
            <div class="featured-grid">
              <button
                v-for="post in featuredPosts"
                :key="post.slug"
                class="featured-item"
                type="button"
                @click="openPost(post)"
              >
                <span>{{ post.category?.name || '未分类' }}</span>
                <strong>{{ post.title }}</strong>
              </button>
            </div>
          </section>

          <section class="post-list">
            <div class="section-title">
              <BookOpen :size="18" aria-hidden="true" />
              <h2>最新文章</h2>
              <span>{{ pageInfo.total }} 篇</span>
            </div>

            <div v-if="loading" class="state-line">正在加载...</div>
            <div v-else-if="!posts.length" class="state-line">暂无文章</div>
            <div v-else class="post-grid">
              <article v-for="post in posts" :key="post.slug" class="post-card">
                <button type="button" class="post-card-button" @click="openPost(post)">
                  <span class="post-category">{{ post.category?.name || '未分类' }}</span>
                  <h3>{{ post.title }}</h3>
                  <p>{{ post.summary }}</p>
                  <span class="post-footer">
                    <span>
                      <CalendarDays :size="15" aria-hidden="true" />
                      {{ formatDate(post.publishedAt) }}
                    </span>
                    <span>
                      <Eye :size="15" aria-hidden="true" />
                      {{ post.viewCount || 0 }}
                    </span>
                  </span>
                </button>
              </article>
            </div>

            <div class="pager" aria-label="分页">
              <button type="button" :disabled="!canGoPrev || loading" @click="goPage(-1)">
                <ChevronLeft :size="17" aria-hidden="true" />
                上一页
              </button>
              <span>{{ pageInfo.current }} / {{ pageInfo.pages || 1 }}</span>
              <button type="button" :disabled="!canGoNext || loading" @click="goPage(1)">
                下一页
                <ChevronRight :size="17" aria-hidden="true" />
              </button>
            </div>
          </section>
        </template>

        <div v-if="detailLoading" class="detail-loading">正在打开文章...</div>
      </section>
    </main>
  </div>
</template>
