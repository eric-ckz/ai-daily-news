package com.aidailynews.config;

import com.aidailynews.model.CategoryConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String outputPath;
    private int maxPerCategory = 8;
    private int daysBack = 1;
    private List<CategoryConfig> categories = new ArrayList<>();

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }

    public int getMaxPerCategory() { return maxPerCategory; }
    public void setMaxPerCategory(int maxPerCategory) { this.maxPerCategory = maxPerCategory; }

    public int getDaysBack() { return daysBack; }
    public void setDaysBack(int daysBack) { this.daysBack = daysBack; }

    public List<CategoryConfig> getCategories() { return categories; }
    public void setCategories(List<CategoryConfig> categories) { this.categories = categories; }
}
