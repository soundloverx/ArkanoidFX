package org.overb.arkanoidfx.game.loaders;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.overb.arkanoidfx.entities.LevelEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LevelLoader {

    private final String baseDir;
    private final ObjectMapper mapper;

    public LevelLoader() {
        this("assets/levels/", new ObjectMapper());
    }

    public LevelLoader(String baseDir, ObjectMapper mapper) {
        this.baseDir = baseDir.endsWith("/") ? baseDir : baseDir + "/";
        this.mapper = mapper;
    }

    public List<String> loadLevelOrder() throws Exception {
        var url = Thread.currentThread().getContextClassLoader()
                .getResource(baseDir + "levels.txt");
        if (url == null) {
            throw new IllegalStateException(baseDir + "levels.txt not found");
        }
        List<String> result = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                result.add(trimmed);
            }
        }
        return result;
    }

    public List<String> loadLevelOrderFromFile(String path) throws Exception {
        List<String> result = new ArrayList<>();
        File playlistFile = new File(path);
        File baseDir = playlistFile.getParentFile();
        try (var reader = new BufferedReader(new InputStreamReader(new FileInputStream(playlistFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                File f = new File(trimmed);
                if (!f.isAbsolute() && baseDir != null) {
                    f = new File(baseDir, trimmed);
                }
                result.add(f.getAbsolutePath());
            }
        }
        return result;
    }

    public LevelEntity loadLevel(String levelFileName) throws Exception {
        String resourcePath = baseDir + levelFileName;
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Level not found on classpath: " + resourcePath);
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return mapper.readValue(json, LevelEntity.class);
        }
    }

    public LevelEntity loadLevelFromFile(String path) throws Exception {
        try (InputStream is = new FileInputStream(new File(path))) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return mapper.readValue(json, LevelEntity.class);
        }
    }
}