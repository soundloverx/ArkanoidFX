package org.overb.arkanoidfx.game.world;

import com.almasb.fxgl.dsl.EntityBuilder;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.texture.Texture;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.overb.arkanoidfx.components.BallAnimComponent;
import org.overb.arkanoidfx.components.BallComponent;
import org.overb.arkanoidfx.entities.BallEntity;
import org.overb.arkanoidfx.entities.EntityRepository;
import org.overb.arkanoidfx.enums.EntityType;
import org.overb.arkanoidfx.game.ResolutionManager;
import org.overb.arkanoidfx.util.TextureUtils;

import java.util.Objects;

public final class BallFactory {

    private final BallEntity defaultBallDefinition;

    public BallFactory(EntityRepository repository) {
        this.defaultBallDefinition = repository.getBalls().values().stream().findFirst().orElse(null);
    }

    public void spawnBallAttachedToPaddle(Entity paddle) {
        double ballWidth = EntityType.BALL.getDesignWidth();
        double ballHeight = EntityType.BALL.getDesignHeight();
        double paddleY = ResolutionManager.DESIGN_RESOLUTION.getHeight() - 80;
        Texture ballTexture = createBallTexture(ballWidth, ballHeight);
        double startX = paddle.getX() + paddle.getWidth() / 2.0 - ballWidth / 2.0;
        double startY = paddleY - ballHeight - 4.0;
        Entity ball = buildBaseBall(startX, startY, ballWidth, ballHeight, ballTexture);
        attachBallComponents(ball, ballTexture, paddle);
    }

    public void spawnLaunchedBallAt(double x, double y, Point2D velocity, Entity paddle) {
        double ballWidth = EntityType.BALL.getDesignWidth();
        double ballHeight = EntityType.BALL.getDesignHeight();
        Texture ballTexture = createBallTexture(ballWidth, ballHeight);
        Entity ball = buildBaseBall(x, y, ballWidth, ballHeight, ballTexture);
        attachBallComponents(ball, ballTexture, paddle);
        ball.getComponent(BallComponent.class).setLaunchedWithVelocity(velocity);
    }

    private Texture createBallTexture(double ballWidth, double ballHeight) {
        if (defaultBallDefinition == null || defaultBallDefinition.visual == null || defaultBallDefinition.visual.sprite == null) {
            return null;
        }
        return TextureUtils.loadTextureOrNull(
                defaultBallDefinition.visual.sprite,
                ballWidth,
                ballHeight,
                Math.max(1, defaultBallDefinition.visual.frameW),
                Math.max(1, defaultBallDefinition.visual.frameH)
        );
    }

    private Entity buildBaseBall(double x, double y, double ballWidth, double ballHeight, Texture ballTexture) {
        return new EntityBuilder()
                .type(EntityType.BALL)
                .at(x, y)
                .view(Objects.requireNonNullElseGet(ballTexture, () -> new Rectangle(ballWidth, ballHeight, Color.ORANGE)))
                .bbox(new HitBox(BoundingShape.circle(Math.min(ballWidth, ballHeight) / 2.0)))
                .with(new CollidableComponent(true))
                .buildAndAttach();
    }

    private void attachBallComponents(Entity ball, Texture ballTexture, Entity paddle) {
        BallComponent ballComponent = new BallComponent(paddle);
        if (defaultBallDefinition != null && defaultBallDefinition.sounds != null) {
            ballComponent.setSounds(defaultBallDefinition.sounds.hitWall, defaultBallDefinition.sounds.hitPaddle, defaultBallDefinition.sounds.lost);
        }
        ball.addComponent(ballComponent);
        if (ballTexture != null && defaultBallDefinition != null && defaultBallDefinition.visual != null) {
            int frames = Math.max(1, defaultBallDefinition.visual.frames);
            double frameDuration = defaultBallDefinition.visual.frameDuration > 0 ? defaultBallDefinition.visual.frameDuration : 0.0;
            if (frames > 1 && frameDuration > 0) {
                ball.addComponent(new BallAnimComponent(
                        ballTexture,
                        frames,
                        Math.max(1, defaultBallDefinition.visual.frameW),
                        Math.max(1, defaultBallDefinition.visual.frameH),
                        frameDuration
                ));
            }
        }
    }
}