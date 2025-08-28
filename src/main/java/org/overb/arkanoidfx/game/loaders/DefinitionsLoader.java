package org.overb.arkanoidfx.game.loaders;

import com.almasb.fxgl.dsl.FXGL;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.overb.arkanoidfx.entities.*;

import java.util.List;

public class DefinitionsLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public EntityRepository loadAll() throws Exception {
        var repo = new EntityRepository();
        loadBricks(repo);
        loadSurprises(repo);
        loadBalls(repo);
        loadPaddles(repo);
        validate(repo);
        return repo;
    }

    private void loadBricks(EntityRepository repo) throws Exception {
        String json = tryLoadTextViaFXGL("bricks.json");
        JsonNode root = mapper.readTree(json);
        JsonNode bricks = root.path("bricks");
        if (!bricks.isArray()) {
            throw new IllegalStateException("defs/bricks.json: 'bricks' must be an array");
        }
        for (JsonNode node : bricks) {
            BrickEntity def = mapper.treeToValue(node, BrickEntity.class);
            repo.addBrick(def);
        }
    }

    private void loadSurprises(EntityRepository repo) throws Exception {
        String json = tryLoadTextViaFXGL("surprises.json");
        JsonNode root = mapper.readTree(json);
        JsonNode surprises = root.path("surprises");
        if (!surprises.isArray()) {
            throw new IllegalStateException("defs/surprises.json: 'surprises' must be an array");
        }
        for (JsonNode node : surprises) {
            SurpriseEntity def = mapper.treeToValue(node, SurpriseEntity.class);
            repo.addSurprise(def);
        }
    }

    private void loadBalls(EntityRepository repo) throws Exception {
        String json = tryLoadTextViaFXGL("balls.json");
        var root = mapper.readTree(json);
        var balls = root.path("balls");
        if (!balls.isArray()) {
            throw new IllegalStateException("defs/balls.json: 'balls' must be an array");
        }
        for (var node : balls) {
            BallEntity def = mapper.treeToValue(node, BallEntity.class);
            repo.addBall(def);
        }
    }

    private void loadPaddles(EntityRepository repo) throws Exception {
        String json = tryLoadTextViaFXGL("paddles.json");
        JsonNode root = mapper.readTree(json);
        JsonNode paddles = root.path("paddles");
        if (!paddles.isArray()) throw new IllegalStateException("defs/paddles.json: 'paddles' must be an array");
        for (JsonNode node : paddles) {
            PaddleEntity def = mapper.treeToValue(node, PaddleEntity.class);
            repo.addPaddle(def);
        }
    }

    private String tryLoadTextViaFXGL(String pathInAssets) {
        try {
            List<String> lines = FXGL.getAssetLoader().loadText(pathInAssets);
            if (lines.isEmpty()) {
                throw new IllegalStateException("Empty or missing text asset: " + pathInAssets + " (expected under assets/text)");
            }
            return String.join("\n", lines);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load text asset: " + pathInAssets + " (expected under assets/text)", e);
        }
    }

    private void validate(EntityRepository repo) {
        repo.getBricks().forEach((id, definition) -> {
            if (id <= 0) {
                throw new IllegalStateException("Brick id must be positive: " + id);
            }
            if (definition.visual.frames <= 0) {
                throw new IllegalStateException("Brick frames must be > 0 for id=" + id);
            }
            if (definition.visual.frameW <= 0 || definition.visual.frameH <= 0) {
                throw new IllegalStateException("Brick frame size invalid for id=" + id);
            }
        });
        repo.getSurprises().forEach((id, definition) -> {
            if (id <= 0) {
                throw new IllegalStateException("Surprise id must be positive: " + id);
            }
            if (definition.visual.frames <= 0) {
                throw new IllegalStateException("Surprise frames must be > 0 for id=" + id);
            }
            if (definition.visual.frameW <= 0 || definition.visual.frameH <= 0) {
                throw new IllegalStateException("Surprise frame size invalid for id=" + id);
            }
            if (definition.spawnChance < 0.0 || definition.spawnChance > 1.0) {
                throw new IllegalStateException("Surprise spawnChance must be in [0,1] for id=" + id);
            }
        });
        repo.getBalls().forEach((id, def) -> {
            if (id <= 0) {
                throw new IllegalStateException("Ball id must be positive: " + id);
            }
            if (def.sizeW <= 0 || def.sizeH <= 0) {
                throw new IllegalStateException("Ball size invalid for id=" + id);
            }
            if (def.visual != null) {
                if (def.visual.frameW <= 0 || def.visual.frameH <= 0) {
                    throw new IllegalStateException("Ball frame size invalid for id=" + id);
                }
            }
        });
        repo.getPaddles().forEach((id, definition) -> {
            if (id <= 0) {
                throw new IllegalStateException("Paddle id must be positive: " + id);
            }
            if (definition.sizeW <= 0 || definition.sizeH <= 0) {
                throw new IllegalStateException("Paddle size invalid for id=" + id);
            }
            if (definition.visual != null) {
                if (definition.visual.frameW <= 0 || definition.visual.frameH <= 0)
                    throw new IllegalStateException("Paddle frame size invalid for id=" + id);
            }
        });
    }
}