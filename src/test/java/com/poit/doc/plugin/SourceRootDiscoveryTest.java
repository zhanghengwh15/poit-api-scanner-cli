package com.poit.doc.plugin;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceRootDiscoveryTest {

    @Test
    void outermost_pom_directory_follows_parent_chain() {
        MavenProject root = Mockito.mock(MavenProject.class);
        MavenProject mid = Mockito.mock(MavenProject.class);
        MavenProject leaf = Mockito.mock(MavenProject.class);
        File rootDir = new File("/repo");
        File leafDir = new File("/repo/services/app");
        Mockito.when(leaf.getBasedir()).thenReturn(leafDir);
        Mockito.when(leaf.getParent()).thenReturn(mid);
        Mockito.when(mid.getBasedir()).thenReturn(new File("/repo/services"));
        Mockito.when(mid.getParent()).thenReturn(root);
        Mockito.when(root.getBasedir()).thenReturn(rootDir);
        Mockito.when(root.getParent()).thenReturn(null);

        assertEquals(rootDir, SourceRootDiscovery.outermostPomDirectory(leaf));
    }

    @Test
    void walk_finds_nested_modules_and_skips_target(@TempDir Path temp) throws Exception {
        File repo = temp.toFile();
        assertTrue(new File(repo, "api/src/main/java").mkdirs());
        assertTrue(new File(repo, "app/src/main/java").mkdirs());
        assertTrue(new File(repo, "app/target/src/main/java").mkdirs());

        Set<String> out = new LinkedHashSet<>();
        SourceRootDiscovery.walkModuleTree(repo, out, 10, null);

        assertEquals(2, out.size());
        assertTrue(out.stream().anyMatch(p -> p.endsWith("api" + File.separator + "src" + File.separator + "main"
                + File.separator + "java")));
        assertTrue(out.stream().anyMatch(p -> p.endsWith("app" + File.separator + "src" + File.separator + "main"
                + File.separator + "java")));
        assertTrue(out.stream().noneMatch(p -> p.contains("target")));
    }

    @Test
    void discover_merges_walk_when_session_null(@TempDir Path temp) throws Exception {
        File repo = temp.toFile();
        assertTrue(new File(repo, "mod-a/src/main/java").mkdirs());

        MavenProject leaf = Mockito.mock(MavenProject.class);
        Mockito.when(leaf.getBasedir()).thenReturn(new File(repo, "mod-a"));
        Mockito.when(leaf.getParent()).thenReturn(null);

        List<String> roots = SourceRootDiscovery.discover(null, leaf, null);
        assertEquals(1, roots.size());
        assertTrue(roots.get(0).endsWith("src" + File.separator + "main" + File.separator + "java"));
    }
}
