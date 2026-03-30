<template>
  <main class="container" v-if="report">
    <h1>AI Daily News</h1>
    <p class="meta">日期：{{ report.date }} ｜ 生成时间：{{ report.generatedAt }}</p>

    <section class="card">
      <h2>当日全分类汇总</h2>
      <pre class="summary">{{ report.summaryZh }}</pre>
    </section>

    <section class="card">
      <h2>今日重点（Top {{ report.highlights?.length || 0 }}）</h2>
      <ul>
        <li v-for="(item, idx) in report.highlights" :key="item.url">
          <strong>{{ idx + 1 }}. [{{ item.category }}] {{ item.titleZh || item.title }}</strong>
          <p>{{ item.contentZh || item.content }}</p>
          <a :href="item.url" target="_blank" rel="noreferrer">原文链接</a>
        </li>
      </ul>
    </section>

    <section v-for="(items, category) in report.categories" :key="category" class="card">
      <h2>{{ category }}（{{ items.length }}）</h2>
      <ul>
        <li v-for="item in items" :key="item.url">
          <strong>{{ item.titleZh || item.title }}</strong>
          <p>{{ item.contentZh || item.content }}</p>
          <small>score: {{ Number(item.score).toFixed(2) }}</small>
          <br/>
          <a :href="item.url" target="_blank" rel="noreferrer">查看原文</a>
        </li>
      </ul>
    </section>
  </main>
  <main class="container" v-else>
    <h1>AI Daily News</h1>
    <p>暂无数据。请先运行后端 pipeline 生成 <code>public/data/daily-summary.json</code>。</p>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const report = ref(null)

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
  font-family: Inter, system-ui, -apple-system, Segoe UI, Roboto, 'PingFang SC', sans-serif;
}
body { margin: 0; background: #f7f8fa; color: #222; }
.container { max-width: 980px; margin: 0 auto; padding: 24px; }
.card { background: #fff; border-radius: 12px; padding: 16px 20px; margin: 14px 0; box-shadow: 0 1px 4px rgba(0,0,0,.05); }
.meta { color: #666; }
ul { padding-left: 20px; }
li { margin-bottom: 12px; }
.summary { white-space: pre-wrap; line-height: 1.6; }
a { color: #2f6feb; text-decoration: none; }
a:hover { text-decoration: underline; }
</style>
