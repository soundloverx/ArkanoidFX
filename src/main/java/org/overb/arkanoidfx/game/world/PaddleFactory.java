package org.overb.arkanoidfx.game.world;

import com.almasb.fxgl.dsl.EntityBuilder;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.texture.Texture;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.overb.arkanoidfx.components.PaddleAnimComponent;
import org.overb.arkanoidfx.components.PaddleComponent;
import org.overb.arkanoidfx.entities.EntityRepository;
import org.overb.arkanoidfx.entities.PaddleEntity;
import org.overb.arkanoidfx.enums.EntityType;
import org.overb.arkanoidfx.game.ResolutionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PaddleFactory {

    private final EntityRepository repository;

    public PaddleFactory(EntityRepository repository) {
        this.repository = repository;
    }

    public Entity spawnPaddle(int levelIndex) {
        List<Integer> paddleIds = new ArrayList<>(repository.getPaddles().keySet());
        PaddleEntity paddleDefinition = repository.getPaddle(paddleIds.get(levelIndex % paddleIds.size()));

        double paddleWidth = EntityType.PADDLE.getDesignWidth();
        double paddleHeight = EntityType.PADDLE.getDesignHeight();
        double y = ResolutionManager.DESIGN_RESOLUTION.getHeight() - 80;
        double mouseX = com.almasb.fxgl.dsl.FXGL.getInput().getMousePositionWorld().getX();
        double x = mouseX - paddleWidth / 2.0;
        double minX = 0.0;
        double maxX = ResolutionManager.DESIGN_RESOLUTION.getWidth() - paddleWidth;
        if (x < minX) {
            x = minX;
        }
        if (x > maxX) {
            x = maxX;
        }

        Texture paddleTexture = null;
        if (paddleDefinition != null && paddleDefinition.visual != null && paddleDefinition.visual.sprite != null) {
            paddleTexture = org.overb.arkanoidfx.util.TextureUtils.loadTextureOrNull(
                    paddleDefinition.visual.sprite,
                    paddleWidth,
                    paddleHeight,
                    Math.max(1, paddleDefinition.visual.frameW),
                    Math.max(1, paddleDefinition.visual.frameH)
            );
        }
        EntityBuilder builder = new EntityBuilder()
                .type(EntityType.PADDLE)
                .at(x, y)
                .view(Objects.requireNonNullElseGet(paddleTexture, () -> new Rectangle(paddleWidth, paddleHeight, Color.DARKSLATEBLUE)))
                .bbox(new HitBox(BoundingShape.box(paddleWidth, paddleHeight)))
                .with(new CollidableComponent(true))
                .with(new PaddleComponent(paddleWidth, y));
        Entity paddle = builder.buildAndAttach();
        if (paddleTexture != null && paddleDefinition != null && paddleDefinition.visual != null) {
            int frames = Math.max(1, paddleDefinition.visual.frames);
            double frameDuration = paddleDefinition.visual.frameDuration > 0 ? paddleDefinition.visual.frameDuration : 0.0;
            if (frames > 1 && frameDuration > 0) {
                paddle.addComponent(new PaddleAnimComponent(
                        paddleTexture,
                        frames,
                        Math.max(1, paddleDefinition.visual.frameW),
                        Math.max(1, paddleDefinition.visual.frameH),
                        frameDuration
                ));
            }
        }
        return paddle;
    }
}