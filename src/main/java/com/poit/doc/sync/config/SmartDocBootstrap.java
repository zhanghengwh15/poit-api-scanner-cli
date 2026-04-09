package com.poit.doc.sync.config;

import com.ly.doc.builder.ApiDataBuilder;
import com.ly.doc.model.ApiAllData;
import com.ly.doc.model.ApiConfig;
import com.ly.doc.model.ApiDoc;
import com.ly.doc.model.SourceCodePath;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * 构建 {@link ApiConfig} 并调用 Smart-doc 引擎拉取 {@link ApiDoc} 列表。
 */
public final class SmartDocBootstrap {

    private SmartDocBootstrap() {
    }

    public static List<ApiDoc> loadApiDocs(SmartDocRunConfig cfg) {
        ApiConfig config = new ApiConfig();
        config.setProjectName(cfg.getProjectName());
        config.setFramework(cfg.getFramework());
        if (cfg.getPackageFilters() != null && !cfg.getPackageFilters().isEmpty()) {
            config.setPackageFilters(cfg.getPackageFilters());
        }
        if (cfg.getPackageExcludeFilters() != null && !cfg.getPackageExcludeFilters().isEmpty()) {
            config.setPackageExcludeFilters(cfg.getPackageExcludeFilters());
        }
        config.setBaseDir(cfg.getBaseDir());
        if (cfg.getProjectClassLoader() != null) {
            config.setClassLoader(cfg.getProjectClassLoader());
        }

        List<String> effective = new ArrayList<>();
        List<String> configured = cfg.getSourcePaths();
        if (configured != null) {
            for (String p : configured) {
                if (p != null && !p.trim().isEmpty()) {
                    effective.add(new File(p.trim()).getAbsolutePath());
                }
            }
        }
        String defaultPath = cfg.getBaseDir() + File.separator + "src" + File.separator + "main" + File.separator + "java";
        if (effective.isEmpty()) {
            effective.add(defaultPath);
        }

        List<SourceCodePath> paths = new ArrayList<>();
        for (String p : effective) {
            paths.add(SourceCodePath.builder().setPath(p));
        }
        config.setSourceCodePaths(paths);
        config.setCodePath(effective.get(0));

        ApiAllData data = ApiDataBuilder.getApiData(config);
        List<ApiDoc> list = data.getApiDocList();
        return list != null ? list : new ArrayList<>();
    }

    public static ClassLoader compileClasspathLoader(List<String> classpathElements, ClassLoader parent)
            throws MalformedURLException {
        if (classpathElements == null || classpathElements.isEmpty()) {
            return parent;
        }
        List<URL> urls = new ArrayList<>();
        for (String el : classpathElements) {
            if (el == null || el.isEmpty()) {
                continue;
            }
            urls.add(new File(el).toURI().toURL());
        }
        return new URLClassLoader(urls.toArray(new URL[0]), parent);
    }
}
