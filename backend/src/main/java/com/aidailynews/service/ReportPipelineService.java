package com.aidailynews.service;

import com.aidailynews.config.AppProperties;
import com.aidailynews.model.CategoryConfig;
import com.aidailynews.model.DailyReport;
import com.aidailynews.model.NewsItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.File;
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

            for (NewsItem item : normalized) {
                item.setTitleZh(translationService.translateToChinese(item.getTitle(), 300));
                item.setContentZh(translationService.translateToChinese(item.getContent(), 1200));
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

        DailyReport report = new DailyReport();
        report.setDate(OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ISO_LOCAL_DATE));
        report.setGeneratedAt(OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
        report.setSummaryZh(dailySummary);
        report.setHighlights(highlights);
        report.setCategories(grouped);

        Path out = Path.of(app.getOutputPath());
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        mapper.writeValue(new File(out.toString()), report);

        System.out.println("Daily report generated: " + out.toAbsolutePath());
    }
}
