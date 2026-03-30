<template>
  <main class="page" v-if="report">
    <header class="hero">
      <div>
        <p class="kicker">AI Intelligence Daily</p>
        <h1>AI Daily News</h1>
        <p class="meta">
          {{ report.date }} · 更新于 {{ formatTime(report.generatedAt) }}
        </p>
        <div class="stats">
          <span>{{ totalCount }} 条情报</span>
          <span>{{ categoryNames.length }} 个分类</span>
          <span>Top {{ topSignals.length }}</span>
        </div>
      </div>

      <div class="hero-actions">
        <div class="theme-switch">
          <button :class="{ active: themeMode === 'light' }" @click="setTheme('light')">浅色</button>
          <button :class="{ active: themeMode === 'dark' }" @click="setTheme('dark')">深色</button>
          <button :class="{ active: themeMode === 'system' }" @click="setTheme('system')">跟随系统</button>
        </div>
        <a class="btn" :href="latestMdLink" target="_blank" rel="noreferrer">Markdown 日报</a>
      </div>
    </header>

    <section class="search-bar">
      <input
        v-model="keyword"
        type="text"
        placeholder="搜索标题 / 摘要 / 分类（中英文都支持）"
      />
      <select v-model="selectedCategory">
        <option value="ALL">全部分类</option>
        <option v-for="name in categoryNames" :key="name" :value="name">{{ name }}</option>
      </select>
      <span class="result-count">{{ searchMode ? '搜索模式' : '浏览模式' }} · {{ filteredItems.length }} 条</span>
    </section>

    <section class="card summary-card" v-if="!searchMode">
      <div class="section-title">
        <h2>Today Brief</h2>
        <span>当日全分类汇总</span>
      </div>
      <p class="summary">{{ report.summaryZh }}</p>
    </section>

    <section class="card" v-if="!searchMode">
      <div class="section-title">
        <h2>Top Signals</h2>
        <span>今日关键信号（Top 5）</span>
      </div>
      <div class="grid">
        <article class="news-card signal" v-for="(item, idx) in topSignals" :key="item.url + idx">
          <div class="badge">#{{ idx + 1 }} · {{ item.category }}</div>
          <h3>{{ item.titleZh || item.title }}</h3>
          <p>{{ item.contentZh || item.content }}</p>
          <a :href="item.url" target="_blank" rel="noreferrer">Source ↗</a>
        </article>
      </div>
    </section>

    <section class="card" v-if="!searchMode">
      <div class="section-title">
        <h2>Category Stream</h2>
        <span>按分类阅读</span>
      </div>

      <div class="category-block" v-for="name in categoryNames" :key="name">
        <div class="category-header">
          <h3>{{ name }}</h3>
          <button class="ghost" @click="toggleCategory(name)">
            {{ isExpanded(name) ? '收起' : '展开全部' }}
          </button>
        </div>

        <div class="grid">
          <article
            class="news-card"
            v-for="(item, idx) in visibleCategoryItems(name)"
            :key="item.url + name + idx"
          >
            <div class="badge">score {{ Number(item.score || 0).toFixed(2) }}</div>
            <h3>{{ item.titleZh || item.title }}</h3>
            <p>{{ item.contentZh || item.content }}</p>
            <a :href="item.url" target="_blank" rel="noreferrer">Source ↗</a>
          </article>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="section-title">
        <h2>Search Intelligence</h2>
        <span>搜索与筛选结果</span>
      </div>

      <div class="grid" v-if="filteredItems.length">
        <article class="news-card" v-for="(item, idx) in filteredItems" :key="item.url + '-f-' + idx">
          <div class="badge">{{ item.category }} · {{ Number(item.score || 0).toFixed(2) }}</div>
          <h3>{{ item.titleZh || item.title }}</h3>
          <p>{{ item.contentZh || item.content }}</p>
          <a :href="item.url" target="_blank" rel="noreferrer">Source ↗</a>
        </article>
      </div>
      <p v-else class="empty">没有匹配结果，换个关键词试试。</p>
    </section>
  </main>

  <main class="page" v-else>
    <section class="card">
      <h1>AI Daily News</h1>
      <p>暂无数据。请先运行后端 pipeline 生成 <code>public/data/daily-summary.json</code>。</p>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'

const report = ref(null)
const keyword = ref('')
const selectedCategory = ref('ALL')
const expanded = ref({})

const themeMode = ref(localStorage.getItem('theme-mode') || 'system') // light | dark | system
const prefersDark = ref(false)
let mediaQuery = null
let mediaListener = null

const categoryNames = computed(() => {
  if (!report.value?.categories) return []
  return Object.keys(report.value.categories)
})

const allItems = computed(() => {
  if (!report.value?.categories) return []
  const arr = []
  for (const [category, items] of Object.entries(report.value.categories)) {
    for (const item of items) arr.push({ ...item, category })
  }
  return arr
})

const totalCount = computed(() => allItems.value.length)

const topSignals = computed(() => {
  const src = report.value?.highlights || []
  return src.slice(0, 5)
})

const searchMode = computed(() => {
  return keyword.value.trim().length > 0 || selectedCategory.value !== 'ALL'
})

const filteredItems = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return allItems.value.filter((item) => {
    const matchCategory = selectedCategory.value === 'ALL' || item.category === selectedCategory.value
    if (!matchCategory) return false
    if (!kw) return true

    const text = [item.category, item.title, item.titleZh, item.content, item.contentZh]
      .join(' ')
      .toLowerCase()

    return text.includes(kw)
  })
})

const latestMdLink = computed(() => {
  if (!report.value?.date) return './'
  return `https://github.com/eric-ckz/ai-daily-news/blob/main/reports/daily-news-${report.value.date}.md`
})

function visibleCategoryItems(name) {
  const items = report.value?.categories?.[name] || []
  return isExpanded(name) ? items : items.slice(0, 3)
}

function isExpanded(name) {
  return !!expanded.value[name]
}

function toggleCategory(name) {
  expanded.value = { ...expanded.value, [name]: !expanded.value[name] }
}

function formatTime(v) {
  if (!v) return '-'
  try {
    return new Date(v).toLocaleString('zh-CN', { hour12: false })
  } catch {
    return v
  }
}

function setTheme(mode) {
  themeMode.value = mode
  localStorage.setItem('theme-mode', mode)
  applyTheme()
}

function applyTheme() {
  const effective = themeMode.value === 'system'
    ? (prefersDark.value ? 'dark' : 'light')
    : themeMode.value
  document.documentElement.setAttribute('data-theme', effective)
}

onMounted(async () => {
  mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  prefersDark.value = mediaQuery.matches
  mediaListener = (e) => {
    prefersDark.value = e.matches
    applyTheme()
  }

  if (mediaQuery.addEventListener) mediaQuery.addEventListener('change', mediaListener)
  else mediaQuery.addListener(mediaListener)

  applyTheme()

  try {
    const res = await fetch('./data/daily-summary.json')
    if (res.ok) report.value = await res.json()
  } catch (e) {
    console.error(e)
  }
})

onBeforeUnmount(() => {
  if (!mediaQuery || !mediaListener) return
  if (mediaQuery.removeEventListener) mediaQuery.removeEventListener('change', mediaListener)
  else mediaQuery.removeListener(mediaListener)
})

watch(themeMode, applyTheme)
</script>

<style>
:root {
  --bg: #f3f6fb;
  --bg-soft: #eef3fb;
  --card: #ffffff;
  --card-soft: #f8fbff;
  --text: #1f2937;
  --text-soft: #64748b;
  --border: #dbe3ef;
  --primary: #2563eb;
  --primary-soft: #dbe9ff;
  --shadow: 0 10px 30px rgba(37, 99, 235, 0.10);
}

:root[data-theme='dark'] {
  --bg: #0b1220;
  --bg-soft: #0f172a;
  --card: #111b2f;
  --card-soft: #13203a;
  --text: #e5edf8;
  --text-soft: #96a8c5;
  --border: #233557;
  --primary: #60a5fa;
  --primary-soft: rgba(96, 165, 250, 0.15);
  --shadow: 0 16px 40px rgba(2, 8, 23, 0.45);
}

body {
  margin: 0;
  background: radial-gradient(circle at top left, var(--bg-soft), var(--bg));
  color: var(--text);
  font-family: Inter, "PingFang SC", system-ui, -apple-system, Segoe UI, Roboto, sans-serif;
}

.page {
  max-width: 1160px;
  margin: 0 auto;
  padding: 28px 18px 48px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.kicker {
  margin: 0;
  color: var(--primary);
  letter-spacing: 0.08em;
  font-size: 12px;
  text-transform: uppercase;
}

.hero h1 {
  margin: 4px 0;
  font-size: 38px;
  line-height: 1.1;
}

.meta {
  margin: 0;
  color: var(--text-soft);
}

.stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.stats span {
  background: var(--primary-soft);
  color: var(--primary);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
}

.hero-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.theme-switch {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 3px;
  display: inline-flex;
}

.theme-switch button {
  border: 0;
  background: transparent;
  color: var(--text-soft);
  padding: 6px 10px;
  border-radius: 8px;
  cursor: pointer;
}

.theme-switch button.active {
  background: var(--primary-soft);
  color: var(--primary);
}

.btn {
  background: var(--primary);
  color: white;
  text-decoration: none;
  padding: 10px 14px;
  border-radius: 10px;
  font-weight: 600;
}

.search-bar {
  display: grid;
  grid-template-columns: 1fr 190px auto;
  gap: 10px;
  margin: 14px 0;
}

.search-bar input,
.search-bar select {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  background: var(--card);
  color: var(--text);
}

.result-count {
  align-self: center;
  color: var(--text-soft);
  font-size: 14px;
}

.card {
  background: color-mix(in srgb, var(--card) 92%, transparent);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 18px;
  margin: 14px 0;
  box-shadow: var(--shadow);
}

.summary-card {
  background: color-mix(in srgb, var(--card-soft) 88%, transparent);
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;
}

.section-title h2 {
  margin: 0;
}

.section-title span {
  color: var(--text-soft);
  font-size: 13px;
}

.summary {
  white-space: pre-wrap;
  line-height: 1.75;
  color: var(--text);
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.news-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
  transition: transform .2s ease, border-color .2s ease, box-shadow .2s ease;
}

.news-card:hover {
  transform: translateY(-2px);
  border-color: var(--primary);
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.18);
}

.news-card.signal {
  border-left: 3px solid var(--primary);
}

.news-card h3 {
  margin: 8px 0;
  font-size: 16px;
  line-height: 1.45;
}

.news-card p {
  margin: 0 0 10px;
  color: var(--text-soft);
  line-height: 1.55;
  font-size: 14px;
}

.news-card a {
  color: var(--primary);
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
}

.badge {
  display: inline-block;
  font-size: 12px;
  color: var(--primary);
  background: var(--primary-soft);
  border-radius: 999px;
  padding: 4px 8px;
}

.category-block + .category-block {
  margin-top: 18px;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.category-header h3 {
  margin: 0;
}

.ghost {
  border: 1px solid var(--border);
  background: var(--card);
  color: var(--text-soft);
  border-radius: 10px;
  padding: 6px 10px;
  cursor: pointer;
}

.empty {
  color: var(--text-soft);
}

@media (max-width: 900px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    align-items: flex-start;
  }

  .search-bar {
    grid-template-columns: 1fr;
  }
}
</style>
