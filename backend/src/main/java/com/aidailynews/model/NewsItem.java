package com.aidailynews.model;

public class NewsItem {
    private String category;
    private String title;
    private String titleZh;
    private String content;
    private String contentZh;
    private String url;
    private String source;
    private String publishedDate;
    private double score;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTitleZh() { return titleZh; }
    public void setTitleZh(String titleZh) { this.titleZh = titleZh; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContentZh() { return contentZh; }
    public void setContentZh(String contentZh) { this.contentZh = contentZh; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getPublishedDate() { return publishedDate; }
    public void setPublishedDate(String publishedDate) { this.publishedDate = publishedDate; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
