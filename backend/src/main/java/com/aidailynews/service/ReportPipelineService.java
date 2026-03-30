package com.aidailynews.service;

import com.aidailynews.config.AppProperties;
import com.aidailynews.model.CategoryConfig;
import com.aidailynews.model.DailyReport;
import com.aidailynews.model.NewsItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportPipelineService {

    private final AppProperties app;
    private final TavilyService tavilyService;
    private final TranslationService translationService;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public ReportPipelineService(AppProperties app, TavilyService tavilyService, TranslationService translationService) {
        this.app = app;
        this.tavilyService = tavilyService;
        this.translationService = translationService;
    }

    public void generateDailyReport() throws Exception {
        Map<String, List<NewsItem>> grouped = new LinkedHashMap<>();

        for (CategoryConfig c : app.getCategories()) {
            List<NewsItem> items = tavilyService.searchNews(c.getName(), c.getQuery(), app.getMaxPerCategory(), app.getDaysBack());
            List<NewsItem> normalized = items.stream()
                    .filter(i -> i.getUrl() != null && !i.getUrl().isBlank())
                    .collect(Collectors.toMap(NewsItem::getUrl, i -> i, (a, b) -> a))
                    .values().stream()
                    .sorted(Comparator.comparingDouble(NewsItem::getScore).reversed())
                    .toList();

            int translateLimit = Math.min(normalized.size(), 3);
            for (int i = 0; i < translateLimit; i++) {
                NewsItem item = normalized.get(i);
                item.setTitleZh(translationService.translateToChinese(item.getTitle(), 220));
                item.setContentZh(translationService.translateToChinese(item.getContent(), 450));
            }

            for (NewsItem item : normalized) {
                if (item.getTitleZh() == null || item.getTitleZh().isBlank()) {
                    item.setTitleZh(item.getTitle());
                }
                if (item.getContentZh() == null || item.getContentZh().isBlank()) {
                    item.setContentZh(item.getContent());
                }
            }

            grouped.put(c.getName(), normalized);
        }

        List<NewsItem> allItems = grouped.values().stream().flatMap(List::stream)
                .sorted(Comparator.comparingDouble(NewsItem::getScore).reversed())
                .toList();

        List<NewsItem> highlights = new ArrayList<>(allItems.stream().limit(12).toList());

        String summaryInput = highlights.stream()
                .map(i -> "[" + i.getCategory() + "] " + i.getTitleZh())
                .collect(Collectors.joining("\n"));

        String dailySummary = translationService.translateToChinese(
                "请基于以下新闻标题生成中文日报总览（200-300字，分点）:\n" + summaryInput,
                3500
        );

        String date = OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ISO_LOCAL_DATE);

        DailyReport report = new DailyReport();
        report.setDate(date);
        report.setGeneratedAt(OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
        report.setSummaryZh(dailySummary);
        report.setHighlights(highlights);
        report.setCategories(grouped);

        Path out = Path.of(app.getOutputPath());
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        mapper.writeValue(new File(out.toString()), report);

        writeMarkdownReport(report, date);

        System.out.println("Daily report generated: " + out.toAbsolutePath());
    }

    private void writeMarkdownReport(DailyReport report, String date) throws Exception {
        String markdownDir = app.getMarkdownDir();
        if (markdownDir == null || markdownDir.isBlank()) {
            markdownDir = "../reports";
        }

        Path dir = Path.of(markdownDir);
        Files.createDirectories(dir);
        Path mdPath = dir.resolve("daily-news-" + date + ".md");

        StringBuilder sb = new StringBuilder();
        sb.append("# AI Daily News - ").append(date).append("\n\n");
        sb.append("生成时间：").append(report.getGeneratedAt()).append("\n\n");

        sb.append("## 当日全分类汇总\n\n");
        sb.append(report.getSummaryZh() == null ? "" : report.getSummaryZh()).append("\n\n");

        sb.append("## 今日重点\n\n");
        int idx = 1;
        for (NewsItem item : report.getHighlights()) {
            sb.append(idx++).append(". **[").append(item.getCategory()).append("] ")
                    .append(safe(item.getTitleZh(), item.getTitle())).append("**\n");
            sb.append("   - 摘要：").append(safe(item.getContentZh(), item.getContent())).append("\n");
            sb.append("   - 链接：").append(item.getUrl()).append("\n\n");
        }

        for (Map.Entry<String, List<NewsItem>> entry : report.getCategories().entrySet()) {
            sb.append("## ").append(entry.getKey()).append("（").append(entry.getValue().size()).append("）\n\n");
            int cidx = 1;
            for (NewsItem item : entry.getValue()) {
                sb.append(cidx++).append(". **").append(safe(item.getTitleZh(), item.getTitle())).append("**\n");
                sb.append("   - 摘要：").append(safe(item.getContentZh(), item.getContent())).append("\n");
                sb.append("   - 原文：").append(item.getUrl()).append("\n");
                sb.append("   - Score：").append(String.format("%.2f", item.getScore())).append("\n\n");
            }
        }

        Files.writeString(mdPath, sb.toString(), StandardCharsets.UTF_8);
        System.out.println("Markdown report generated: " + mdPath.toAbsolutePath());
    }

    private String safe(String preferred, String fallback) {
        return (preferred != null && !preferred.isBlank()) ? preferred : (fallback == null ? "" : fallback);
    }
}
