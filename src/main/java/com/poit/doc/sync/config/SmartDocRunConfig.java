package com.poit.doc.sync.config;

import java.util.List;

/**
 * 运行期配置（Mojo 与独立入口共用）。
 */
public class SmartDocRunConfig {

    private String projectName = "API";
    private String baseDir;
    private List<String> sourcePaths;
    private String framework = "spring";
    private String packageFilters;
    private String packageExcludeFilters;
    private ClassLoader projectClassLoader;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public List<String> getSourcePaths() {
        return sourcePaths;
    }

    public void setSourcePaths(List<String> sourcePaths) {
        this.sourcePaths = sourcePaths;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getPackageFilters() {
        return packageFilters;
    }

    public void setPackageFilters(String packageFilters) {
        this.packageFilters = packageFilters;
    }

    public String getPackageExcludeFilters() {
        return packageExcludeFilters;
    }

    public void setPackageExcludeFilters(String packageExcludeFilters) {
        this.packageExcludeFilters = packageExcludeFilters;
    }

    public ClassLoader getProjectClassLoader() {
        return projectClassLoader;
    }

    public void setProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
    }
}
