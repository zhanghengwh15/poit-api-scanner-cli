package com.poit.doc.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 自动收集 Smart-doc 源码根（{@code .../src/main/java}）：当前 Reactor 内各模块 + 从外层 POM 目录起的目录树扫描。
 */
public final class SourceRootDiscovery {

    private static final int MAX_WALK_DEPTH = 10;

    private SourceRootDiscovery() {
    }

    /**
     * @param session 可为 null（仅用于测试）；正常插件执行时由 Maven 注入
     */
    public static List<String> discover(MavenSession session, MavenProject project, Log log) {
        LinkedHashSet<String> roots = new LinkedHashSet<>();
        if (session != null && session.getProjects() != null) {
            for (MavenProject p : session.getProjects()) {
                if (p != null && p.getBasedir() != null) {
                    addJavaMainRoot(p.getBasedir(), roots);
                }
            }
        }
        File treeRoot = outermostPomDirectory(project);
        if (treeRoot != null && treeRoot.isDirectory()) {
            walkModuleTree(treeRoot, roots, MAX_WALK_DEPTH, log);
        }
        return new ArrayList<>(roots);
    }

    /**
     * 沿 {@link MavenProject#getParent()} 找到最外层仍带 {@code pom.xml} 的目录，作为多模块树扫描起点。
     */
    static File outermostPomDirectory(MavenProject project) {
        if (project == null || project.getBasedir() == null) {
            return null;
        }
        MavenProject top = project;
        while (top.getParent() != null && top.getParent().getBasedir() != null) {
            top = top.getParent();
        }
        return top.getBasedir();
    }

    static void addJavaMainRoot(File moduleBase, Set<String> out) {
        if (moduleBase == null) {
            return;
        }
        File javaRoot = new File(moduleBase, "src" + File.separator + "main" + File.separator + "java");
        if (javaRoot.isDirectory()) {
            out.add(javaRoot.getAbsolutePath());
        }
    }

    /**
     * 若当前目录存在标准 {@code src/main/java}，视为一个模块并收录，且不再向下钻（避免扫进源码树内部）。
     * 否则继续扫描一级子目录。
     */
    static void walkModuleTree(File dir, Set<String> out, int depthRemaining, Log log) {
        if (depthRemaining < 0 || dir == null || !dir.isDirectory()) {
            return;
        }
        File javaRoot = new File(dir, "src" + File.separator + "main" + File.separator + "java");
        if (javaRoot.isDirectory()) {
            String abs = javaRoot.getAbsolutePath();
            if (out.add(abs) && log != null && log.isDebugEnabled()) {
                log.debug("自动发现源码根: " + abs);
            }
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (!child.isDirectory() || shouldSkipDirectoryName(child.getName())) {
                continue;
            }
            walkModuleTree(child, out, depthRemaining - 1, log);
        }
    }

    static boolean shouldSkipDirectoryName(String name) {
        if (name == null || name.isEmpty()) {
            return true;
        }
        switch (name) {
            case "target":
            case ".git":
            case ".svn":
            case ".idea":
            case "node_modules":
            case "build":
                return true;
            default:
                return name.startsWith(".");
        }
    }
}
