<template>
  <main class="page" v-if="report">
    <header class="hero">
      <div>
        <h1>AI Daily News</h1>
        <p class="meta">{{ report.date }} · 生成于 {{ formatTime(report.generatedAt) }}</p>
      </div>
      <a class="btn" :href="latestMdLink" target="_blank" rel="noreferrer">查看 Markdown 日报</a>
    </header>

    <section class="search-bar">
      <input
        v-model="keyword"
        type="text"
        placeholder="搜索标题/摘要/分类（支持中英文）"
      />
      <select v-model="selectedCategory">
        <option value="ALL">全部分类</option>
        <option v-for="name in categoryNames" :key="name" :value="name">{{ name }}</option>
      </select>
      <span class="result-count">结果：{{ filteredItems.length }}</span>
    </section>

    <section class="card summary-card">
      <h2>当日全分类汇总</h2>
      <p class="summary">{{ report.summaryZh }}</p>
    </section>

    <section class="card">
      <h2>今日重点</h2>
      <div class="grid">
        <article class="news-card" v-for="(item, idx) in report.highlights" :key="item.url + idx">
          <div class="badge">#{{ idx + 1 }} · {{ item.category }}</div>
          <h3>{{ item.titleZh || item.title }}</h3>
          <p>{{ item.contentZh || item.content }}</p>
          <a :href="item.url" target="_blank" rel="noreferrer">原文链接 ↗</a>
        </article>
      </div>
    </section>

    <section class="card">
      <h2>搜索结果</h2>
      <div class="grid" v-if="filteredItems.length">
        <article class="news-card" v-for="(item, idx) in filteredItems" :key="item.url + '-f-' + idx">
          <div class="badge">{{ item.category }} · score {{ Number(item.score || 0).toFixed(2) }}</div>
          <h3>{{ item.titleZh || item.title }}</h3>
          <p>{{ item.contentZh || item.content }}</p>
          <a :href="item.url" target="_blank" rel="noreferrer">查看原文 ↗</a>
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
import { computed, onMounted, ref } from 'vue'

const report = ref(null)
const keyword = ref('')
const selectedCategory = ref('ALL')

const categoryNames = computed(() => {
  if (!report.value?.categories) return []
  return Object.keys(report.value.categories)
})

const allItems = computed(() => {
  if (!report.value?.categories) return []
  const arr = []
  for (const [category, items] of Object.entries(report.value.categories)) {
    for (const item of items) {
      arr.push({ ...item, category })
    }
  }
  return arr
})

const filteredItems = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return allItems.value.filter(item => {
    const matchCategory = selectedCategory.value === 'ALL' || item.category === selectedCategory.value
    if (!matchCategory) return false
    if (!kw) return true

    const text = [
      item.category,
      item.title,
      item.titleZh,
      item.content,
      item.contentZh
    ].join(' ').toLowerCase()

    return text.includes(kw)
  })
})

const latestMdLink = computed(() => {
  if (!report.value?.date) return './'
  return `https://github.com/eric-ckz/ai-daily-news/blob/main/reports/daily-news-${report.value.date}.md`
})

function formatTime(v) {
  if (!v) return '-'
  try {
    return new Date(v).toLocaleString('zh-CN', { hour12: false })
  } catch {
    return v
  }
}

onMounted(async () => {
  try {
    const res = await fetch('./data/daily-summary.json')
    if (res.ok) report.value = await res.json()
  } catch (e) {
    console.error(e)
  }
})
</script>

<style>
:root {
  font-family: Inter, "PingFang SC", system-ui, -apple-system, Segoe UI, Roboto, sans-serif;
}
body {
  margin: 0;
  background: radial-gradient(circle at top, #ecf4ff 0%, #f6f8fc 40%, #f7f8fa 100%);
  color: #1f2937;
}
.page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 28px 18px 48px;
}
.hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.hero h1 {
  margin: 0;
  font-size: 36px;
  line-height: 1.1;
}
.meta {
  margin: 8px 0 0;
  color: #64748b;
}
.btn {
  background: #2563eb;
  color: #fff;
  text-decoration: none;
  padding: 10px 14px;
  border-radius: 10px;
  font-weight: 600;
}
.btn:hover { background: #1d4ed8; }

.search-bar {
  display: grid;
  grid-template-columns: 1fr 180px auto;
  gap: 10px;
  margin: 14px 0;
}
.search-bar input,
.search-bar select {
  border: 1px solid #d0d7e2;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  background: #fff;
}
.result-count {
  align-self: center;
  color: #475569;
  font-size: 14px;
}

.card {
  background: rgba(255,255,255,0.92);
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 18px;
  margin: 14px 0;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.06);
}
.card h2 {
  margin-top: 0;
}
.summary-card {
  background: linear-gradient(140deg, #f8fbff, #f5f9ff);
}
.summary {
  white-space: pre-wrap;
  line-height: 1.7;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}
.news-card {
  background: #fff;
  border: 1px solid #e6ebf4;
  border-radius: 12px;
  padding: 12px;
}
.news-card h3 {
  margin: 8px 0;
  font-size: 16px;
}
.news-card p {
  margin: 0 0 10px;
  color: #334155;
  line-height: 1.55;
  font-size: 14px;
}
.news-card a {
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
}
.badge {
  display: inline-block;
  font-size: 12px;
  color: #1d4ed8;
  background: #e8f0ff;
  border-radius: 999px;
  padding: 4px 8px;
}
.empty {
  color: #64748b;
}

@media (max-width: 860px) {
  .hero { flex-direction: column; align-items: flex-start; }
  .search-bar { grid-template-columns: 1fr; }
}
</style>
