package com.aidailynews.model;

import java.util.List;
import java.util.Map;

public class DailyReport {
    private String date;
    private String generatedAt;
    private String summaryZh;
    private List<NewsItem> highlights;
    private Map<String, List<NewsItem>> categories;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }

    public String getSummaryZh() { return summaryZh; }
    public void setSummaryZh(String summaryZh) { this.summaryZh = summaryZh; }

    public List<NewsItem> getHighlights() { return highlights; }
    public void setHighlights(List<NewsItem> highlights) { this.highlights = highlights; }

    public Map<String, List<NewsItem>> getCategories() { return categories; }
    public void setCategories(Map<String, List<NewsItem>> categories) { this.categories = categories; }
}
