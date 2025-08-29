package org.overb.arkanoidfx;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigIO {

    public static ConfigOptions loadOrDefault() {
        try {
            Path file = getConfigPath();
            Files.createDirectories(file.getParent());
            if (Files.exists(file)) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(file.toFile(), ConfigOptions.class);
            }
        } catch (Exception ignored) {
        }
        return new ConfigOptions();
    }

    public static void save(ConfigOptions cfg) {
        try {
            Path file = getConfigPath();
            Files.createDirectories(file.getParent());
            ObjectMapper mapper = new ObjectMapper();
            try (var w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(w, cfg);
            }
        } catch (Exception ignored) {
        }
    }

    private static Path getConfigPath() throws IOException {
        Path configDir = Paths.get(
                System.getProperty("os.name").toLowerCase().contains("win")
                        ? System.getenv("APPDATA")
                        : System.getProperty("user.home"),
                "org.overb.ArkanoidFX"
        );
        Files.createDirectories(configDir);
        return configDir.resolve("settings.json");
    }
}
