<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import {
  ArrowLeft,
  Edit3,
  FilePlus2,
  ImagePlus,
  LogOut,
  RefreshCw,
  Save,
  ShieldCheck,
  Trash2,
  X
} from 'lucide-vue-next'
import {
  clearStoredAdminToken,
  createAdminCategory,
  createAdminPost,
  createAdminTag,
  deleteAdminCategory,
  deleteAdminPost,
  deleteAdminTag,
  getAdminCategories,
  getAdminPost,
  getAdminPosts,
  getAdminTags,
  getStoredAdminToken,
  setStoredAdminToken,
  updateAdminCategory,
  updateAdminPost,
  updateAdminTag,
  uploadAdminImage
} from '../api/admin'

const emit = defineEmits(['back'])

const emptyPostForm = () => ({
  id: null,
  title: '',
  slug: '',
  summary: '',
  content: '',
  coverImage: '',
  categoryId: '',
  status: 'DRAFT',
  featured: false,
  publishedAt: '',
  tagIds: []
})

const tokenInput = ref(getStoredAdminToken())
const hasToken = ref(Boolean(tokenInput.value))
const posts = ref([])
const categories = ref([])
const tags = ref([])
const pageInfo = ref({
  current: 1,
  pages: 0,
  total: 0
})
const filters = ref({
  keyword: '',
  status: '',
  categorySlug: ''
})
const postForm = ref(emptyPostForm())
const categoryForm = ref({ id: null, name: '', slug: '', description: '', sortOrder: 0 })
const tagForm = ref({ id: null, name: '', slug: '' })
const loading = ref(false)
const saving = ref(false)
const uploadingCover = ref(false)
const uploadingContentImage = ref(false)
const message = ref('')
const error = ref('')
const contentTextareaRef = ref(null)

const isEditingPost = computed(() => Boolean(postForm.value.id))
const selectedTagIds = computed(() => new Set(postForm.value.tagIds.map((id) => Number(id))))

function saveToken() {
  const token = tokenInput.value.trim()
  if (!token) {
    error.value = '管理令牌不能为空'
    return
  }
  setStoredAdminToken(token)
  hasToken.value = true
  loadAdminData(1)
}

function logout() {
  clearStoredAdminToken()
  tokenInput.value = ''
  hasToken.value = false
  posts.value = []
  resetPostForm()
}

async function loadAdminData(page = pageInfo.value.current || 1) {
  if (!hasToken.value) {
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [postPage, categoryData, tagData] = await Promise.all([
      getAdminPosts({
        page,
        size: 10,
        keyword: filters.value.keyword,
        status: filters.value.status,
        categorySlug: filters.value.categorySlug
      }),
      getAdminCategories(),
      getAdminTags()
    ])
    posts.value = postPage.records || []
    pageInfo.value = {
      current: postPage.current || page,
      pages: postPage.pages || 0,
      total: postPage.total || 0
    }
    categories.value = categoryData || []
    tags.value = tagData || []
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  loadAdminData(1)
}

function resetPostForm() {
  postForm.value = emptyPostForm()
}

async function editPost(post) {
  error.value = ''
  try {
    const detail = await getAdminPost(post.id)
    postForm.value = {
      id: detail.id,
      title: detail.title || '',
      slug: detail.slug || '',
      summary: detail.summary || '',
      content: detail.content || '',
      coverImage: detail.coverImage || '',
      categoryId: detail.categoryId || '',
      status: detail.status || 'DRAFT',
      featured: Boolean(detail.featured),
      publishedAt: toDateTimeLocal(detail.publishedAt),
      tagIds: detail.tagIds || []
    }
  } catch (err) {
    error.value = err.message || '文章加载失败'
  }
}

async function savePost() {
  saving.value = true
  error.value = ''
  message.value = ''
  try {
    const payload = normalizePostPayload(postForm.value)
    const saved = postForm.value.id
      ? await updateAdminPost(postForm.value.id, payload)
      : await createAdminPost(payload)
    message.value = postForm.value.id ? '文章已更新' : '文章已创建'
    postForm.value.id = saved.id
    await loadAdminData(pageInfo.value.current || 1)
  } catch (err) {
    error.value = err.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleCoverUpload(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) {
    return
  }
  uploadingCover.value = true
  error.value = ''
  try {
    const upload = await uploadImageFile(file)
    postForm.value.coverImage = upload.url
    message.value = '封面图已上传'
  } catch (err) {
    error.value = err.message || '封面图上传失败'
  } finally {
    uploadingCover.value = false
  }
}

async function handleContentImageUpload(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) {
    return
  }
  uploadingContentImage.value = true
  error.value = ''
  try {
    const upload = await uploadImageFile(file)
    await insertContentImage(upload.url, upload.filename)
    message.value = '正文图片已插入'
  } catch (err) {
    error.value = err.message || '正文图片上传失败'
  } finally {
    uploadingContentImage.value = false
  }
}

async function uploadImageFile(file) {
  if (!file.type || !file.type.startsWith('image/')) {
    throw new Error('只能上传图片文件')
  }
  return uploadAdminImage(file)
}

async function insertContentImage(url, filename) {
  const textarea = contentTextareaRef.value
  const text = postForm.value.content || ''
  const altText = filename.replace(/\.[^.]+$/, '')
  const insertText = `\n![${altText}](${url})\n`

  if (!textarea || typeof textarea.selectionStart !== 'number') {
    postForm.value.content = `${text}${insertText}`
    return
  }

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  postForm.value.content = `${text.slice(0, start)}${insertText}${text.slice(end)}`
  await nextTick()
  textarea.focus()
  const cursor = start + insertText.length
  textarea.setSelectionRange(cursor, cursor)
}

async function removePost(post) {
  if (!window.confirm(`删除文章「${post.title}」？`)) {
    return
  }
  error.value = ''
  try {
    await deleteAdminPost(post.id)
    message.value = '文章已删除'
    if (postForm.value.id === post.id) {
      resetPostForm()
    }
    await loadAdminData(pageInfo.value.current || 1)
  } catch (err) {
    error.value = err.message || '删除失败'
  }
}

function toggleTag(tagId) {
  const ids = new Set(postForm.value.tagIds.map((id) => Number(id)))
  if (ids.has(tagId)) {
    ids.delete(tagId)
  } else {
    ids.add(tagId)
  }
  postForm.value.tagIds = Array.from(ids)
}

async function saveCategory() {
  error.value = ''
  try {
    const payload = {
      name: categoryForm.value.name,
      slug: categoryForm.value.slug,
      description: categoryForm.value.description,
      sortOrder: Number(categoryForm.value.sortOrder || 0)
    }
    if (categoryForm.value.id) {
      await updateAdminCategory(categoryForm.value.id, payload)
      message.value = '分类已更新'
    } else {
      await createAdminCategory(payload)
      message.value = '分类已创建'
    }
    categoryForm.value = { id: null, name: '', slug: '', description: '', sortOrder: 0 }
    await loadAdminData(pageInfo.value.current || 1)
  } catch (err) {
    error.value = err.message || '分类保存失败'
  }
}

function editCategory(category) {
  categoryForm.value = {
    id: category.id,
    name: category.name || '',
    slug: category.slug || '',
    description: category.description || '',
    sortOrder: category.sortOrder || 0
  }
}

async function removeCategory(category) {
  if (!window.confirm(`删除分类「${category.name}」？`)) {
    return
  }
  try {
    await deleteAdminCategory(category.id)
    message.value = '分类已删除'
    await loadAdminData(pageInfo.value.current || 1)
  } catch (err) {
    error.value = err.message || '分类删除失败'
  }
}

async function saveTag() {
  error.value = ''
  try {
    const payload = {
      name: tagForm.value.name,
      slug: tagForm.value.slug
    }
    if (tagForm.value.id) {
      await updateAdminTag(tagForm.value.id, payload)
      message.value = '标签已更新'
    } else {
      await createAdminTag(payload)
      message.value = '标签已创建'
    }
    tagForm.value = { id: null, name: '', slug: '' }
    await loadAdminData(pageInfo.value.current || 1)
  } catch (err) {
    error.value = err.message || '标签保存失败'
  }
}

function editTag(tag) {
  tagForm.value = {
    id: tag.id,
    name: tag.name || '',
    slug: tag.slug || ''
  }
}

async function removeTag(tag) {
  if (!window.confirm(`删除标签「${tag.name}」？`)) {
    return
  }
  try {
    await deleteAdminTag(tag.id)
    message.value = '标签已删除'
    await loadAdminData(pageInfo.value.current || 1)
  } catch (err) {
    error.value = err.message || '标签删除失败'
  }
}

function goPage(offset) {
  const nextPage = pageInfo.value.current + offset
  if (nextPage < 1 || nextPage > pageInfo.value.pages) {
    return
  }
  loadAdminData(nextPage)
}

function normalizePostPayload(form) {
  return {
    title: form.title,
    slug: form.slug,
    summary: form.summary,
    content: form.content,
    coverImage: form.coverImage || null,
    categoryId: form.categoryId ? Number(form.categoryId) : null,
    status: form.status,
    featured: Boolean(form.featured),
    publishedAt: form.publishedAt ? `${form.publishedAt}:00` : null,
    tagIds: form.tagIds.map((id) => Number(id))
  }
}

function toDateTimeLocal(value) {
  if (!value) {
    return ''
  }
  return String(value).slice(0, 16)
}

function formatDate(value) {
  if (!value) {
    return '未发布'
  }
  return String(value).slice(0, 16).replace('T', ' ')
}

onMounted(() => {
  if (hasToken.value) {
    loadAdminData(1)
  }
})
</script>

<template>
  <div class="app-shell admin-shell">
    <header class="admin-header">
      <button class="plain-nav-button" type="button" @click="emit('back')">
        <ArrowLeft :size="17" aria-hidden="true" />
        博客
      </button>
      <div>
        <p class="eyebrow">Version 2</p>
        <h1>文章管理</h1>
      </div>
      <button v-if="hasToken" class="plain-nav-button" type="button" @click="logout">
        <LogOut :size="17" aria-hidden="true" />
        退出
      </button>
    </header>

    <section v-if="!hasToken" class="admin-login">
      <ShieldCheck :size="24" aria-hidden="true" />
      <form class="admin-token-form" @submit.prevent="saveToken">
        <label>
          <span>管理令牌</span>
          <input v-model="tokenInput" type="password" autocomplete="current-password" />
        </label>
        <button type="submit">
          <ShieldCheck :size="17" aria-hidden="true" />
          进入
        </button>
      </form>
    </section>

    <template v-else>
      <p v-if="error" class="error-line">{{ error }}</p>
      <p v-if="message" class="success-line">{{ message }}</p>

      <section class="admin-toolbar">
        <input v-model="filters.keyword" type="search" placeholder="标题、摘要、标识" />
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option value="DRAFT">草稿</option>
          <option value="PUBLISHED">已发布</option>
        </select>
        <select v-model="filters.categorySlug">
          <option value="">全部分类</option>
          <option v-for="category in categories" :key="category.slug" :value="category.slug">
            {{ category.name }}
          </option>
        </select>
        <button type="button" @click="applyFilters">
          <RefreshCw :size="16" aria-hidden="true" />
          刷新
        </button>
        <button type="button" @click="resetPostForm">
          <FilePlus2 :size="16" aria-hidden="true" />
          新建
        </button>
      </section>

      <main class="admin-grid">
        <section class="admin-list">
          <div class="section-title">
            <h2>文章</h2>
            <span>{{ pageInfo.total }} 篇</span>
          </div>

          <div v-if="loading" class="state-line">正在加载...</div>
          <div v-else class="admin-post-list">
            <div v-if="!posts.length" class="state-line">暂无文章</div>
            <button
              v-for="post in posts"
              :key="post.id"
              class="admin-post-row"
              :class="{ active: postForm.id === post.id }"
              type="button"
              @click="editPost(post)"
            >
              <span class="status-pill" :class="post.status === 'PUBLISHED' ? 'published' : 'draft'">
                {{ post.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </span>
              <strong>{{ post.title }}</strong>
              <small>{{ post.category?.name || '未分类' }} · {{ formatDate(post.updatedAt || post.createdAt) }}</small>
              <span class="row-actions">
                <Edit3 :size="15" aria-hidden="true" />
                <Trash2 :size="15" aria-hidden="true" @click.stop="removePost(post)" />
              </span>
            </button>
          </div>

          <div class="pager">
            <button type="button" :disabled="pageInfo.current <= 1 || loading" @click="goPage(-1)">上一页</button>
            <span>{{ pageInfo.current }} / {{ pageInfo.pages || 1 }}</span>
            <button
              type="button"
              :disabled="pageInfo.current >= pageInfo.pages || loading"
              @click="goPage(1)"
            >
              下一页
            </button>
          </div>
        </section>

        <section class="admin-editor">
          <div class="section-title">
            <h2>{{ isEditingPost ? '编辑文章' : '新建文章' }}</h2>
            <span>{{ postForm.status === 'PUBLISHED' ? '发布' : '草稿' }}</span>
          </div>

          <form class="post-form" @submit.prevent="savePost">
            <label>
              <span>标题</span>
              <input v-model="postForm.title" required maxlength="180" />
            </label>
            <label>
              <span>标识</span>
              <input v-model="postForm.slug" required maxlength="200" placeholder="my-post-slug" />
            </label>
            <label>
              <span>摘要</span>
              <textarea v-model="postForm.summary" required rows="3" maxlength="500" />
            </label>
            <div class="field-block">
              <span>正文</span>
              <div class="editor-upload-row">
                <label class="file-upload-button">
                  <ImagePlus :size="16" aria-hidden="true" />
                  {{ uploadingContentImage ? '上传中' : '插入图片' }}
                  <input type="file" accept="image/*" :disabled="uploadingContentImage" @change="handleContentImageUpload" />
                </label>
              </div>
              <textarea ref="contentTextareaRef" v-model="postForm.content" required rows="12" />
            </div>

            <div class="form-row">
              <label>
                <span>分类</span>
                <select v-model="postForm.categoryId">
                  <option value="">未分类</option>
                  <option v-for="category in categories" :key="category.id" :value="category.id">
                    {{ category.name }}
                  </option>
                </select>
              </label>
              <label>
                <span>状态</span>
                <select v-model="postForm.status">
                  <option value="DRAFT">草稿</option>
                  <option value="PUBLISHED">已发布</option>
                </select>
              </label>
            </div>

            <div class="form-row">
              <label>
                <span>发布时间</span>
                <input v-model="postForm.publishedAt" type="datetime-local" />
              </label>
              <div class="field-block">
                <span>封面图</span>
                <div class="cover-input-row">
                  <input v-model="postForm.coverImage" placeholder="/uploads/images/..." />
                  <label class="file-upload-button">
                    <ImagePlus :size="16" aria-hidden="true" />
                    {{ uploadingCover ? '上传中' : '上传' }}
                    <input type="file" accept="image/*" :disabled="uploadingCover" @change="handleCoverUpload" />
                  </label>
                </div>
              </div>
            </div>

            <img v-if="postForm.coverImage" :src="postForm.coverImage" alt="" class="cover-preview" />

            <label class="check-line">
              <input v-model="postForm.featured" type="checkbox" />
              <span>精选文章</span>
            </label>

            <div class="admin-tags">
              <button
                v-for="tag in tags"
                :key="tag.id"
                type="button"
                :class="{ active: selectedTagIds.has(tag.id) }"
                @click="toggleTag(tag.id)"
              >
                #{{ tag.name }}
              </button>
            </div>

            <div class="form-actions">
              <button type="submit" :disabled="saving">
                <Save :size="17" aria-hidden="true" />
                保存
              </button>
              <button type="button" class="secondary-button" @click="resetPostForm">
                <X :size="17" aria-hidden="true" />
                清空
              </button>
            </div>
          </form>
        </section>
      </main>

      <section class="taxonomy-grid">
        <div class="taxonomy-panel">
          <div class="section-title">
            <h2>分类</h2>
          </div>
          <form class="mini-form" @submit.prevent="saveCategory">
            <input v-model="categoryForm.name" placeholder="分类名称" required />
            <input v-model="categoryForm.slug" placeholder="category-slug" required />
            <input v-model.number="categoryForm.sortOrder" type="number" placeholder="排序" />
            <button type="submit">
              <Save :size="15" aria-hidden="true" />
            </button>
          </form>
          <div class="taxonomy-list">
            <span v-for="category in categories" :key="category.id">
              {{ category.name }}
              <button type="button" @click="editCategory(category)"><Edit3 :size="13" /></button>
              <button type="button" @click="removeCategory(category)"><Trash2 :size="13" /></button>
            </span>
          </div>
        </div>

        <div class="taxonomy-panel">
          <div class="section-title">
            <h2>标签</h2>
          </div>
          <form class="mini-form" @submit.prevent="saveTag">
            <input v-model="tagForm.name" placeholder="标签名称" required />
            <input v-model="tagForm.slug" placeholder="tag-slug" required />
            <button type="submit">
              <Save :size="15" aria-hidden="true" />
            </button>
          </form>
          <div class="taxonomy-list">
            <span v-for="tag in tags" :key="tag.id">
              #{{ tag.name }}
              <button type="button" @click="editTag(tag)"><Edit3 :size="13" /></button>
              <button type="button" @click="removeTag(tag)"><Trash2 :size="13" /></button>
            </span>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>
