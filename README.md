# ai-daily-news

自动运行的 AI Daily Report 项目（Java + Vue + GitHub Pages）。

## 目标
- 每天北京时间 **08:00** 自动抓取全球 AI 相关新闻（Tavily）
- 抓取多语种内容并翻译成中文（标题 + 核心内容）
- 按分类生成日报，并输出当日全分类汇总
- 自动发布到 GitHub Pages

## 技术栈
- 后端：Java 21 + Spring Boot（数据抓取、清洗、翻译、聚合）
- 前端：Vue 3 + Vite（日报展示）
- 自动化：GitHub Actions + GitHub Pages

## 默认分类（可改）
- AI Agents
- LLMs
- AI Product
- Open Source AI
- Infra & Chips

## 环境变量
在 GitHub 仓库 Secrets 中配置：

- `TAVILY_API_KEY`（必需）

### 自定义模型网关（推荐）
- `LLM_API_KEY`（必需，用于翻译与汇总）
- `LLM_BASE_URL`（必需，例如 `https://your-gateway.example.com/v1`）
- `LLM_MODEL`（必需，例如 `custom-wududu-edu-kg/gpt-5.3-codex`）
- `LLM_CHAT_PATH`（可选，默认 `/chat/completions`）
- `LLM_AUTH_HEADER`（可选，默认 `Authorization`）
- `LLM_AUTH_SCHEME`（可选，默认 `Bearer`）

### OpenAI 兼容回退（可选）
- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`（可选，默认 `https://api.openai.com/v1`）
- `OPENAI_MODEL`（可选，默认 `gpt-4o-mini`）

> 优先级：`LLM_*` > `OPENAI_*`。

## 本地运行
```bash
cd backend
mvn spring-boot:run

cd ../frontend
npm install
npm run dev
```

后端默认会把数据输出到 `frontend/public/data/daily-summary.json`。

## 产物
- 网站数据：`frontend/public/data/daily-summary.json`
- 每日 Markdown 报告：`reports/daily-news-YYYY-MM-DD.md`

## GitHub Pages
工作流会在每天 `00:00 UTC`（北京时间 08:00）自动执行并发布。
并自动将当日 Markdown 日报提交到仓库。
