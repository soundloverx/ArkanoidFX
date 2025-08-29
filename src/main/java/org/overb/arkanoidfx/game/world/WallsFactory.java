package org.overb.arkanoidfx.game.world;

import com.almasb.fxgl.dsl.EntityBuilder;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.time.TimerAction;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.overb.arkanoidfx.enums.Axis;
import org.overb.arkanoidfx.enums.EntityType;
import org.overb.arkanoidfx.game.ResolutionManager;

public final class WallsFactory {

    private Entity safetyWall;
    private TimerAction safetyWallTimer;

    public void spawnWalls() {
        new EntityBuilder()
                .type(EntityType.WALL_LEFT)
                .at(0, 0)
                .bbox(new HitBox(BoundingShape.box(2, ResolutionManager.getScaledEntity(EntityType.WALL_LEFT).getY())))
                .with(new CollidableComponent(true))
                .buildAndAttach();

        new EntityBuilder()
                .type(EntityType.WALL_RIGHT)
                .at(ResolutionManager.getInstance().getCurrentResolution().getWidth() - 2, 0)
                .bbox(new HitBox(BoundingShape.box(2, ResolutionManager.getScaledEntity(EntityType.WALL_RIGHT).getY())))
                .with(new CollidableComponent(true))
                .buildAndAttach();

        new EntityBuilder()
                .type(EntityType.WALL_TOP)
                .at(0, -2)
                .bbox(new HitBox(BoundingShape.box(ResolutionManager.getScaledEntity(EntityType.WALL_TOP).getX(), 2)))
                .with(new CollidableComponent(true))
                .buildAndAttach();

        new EntityBuilder()
                .type(EntityType.WALL_BOTTOM_SENSOR)
                .at(0, ResolutionManager.getInstance().getCurrentResolution().getHeight() - 2)
                .bbox(new HitBox(BoundingShape.box(ResolutionManager.getScaledEntity(EntityType.WALL_BOTTOM_SENSOR).getX(), 50)))
                .with(new CollidableComponent(true))
                .buildAndAttach();
    }

    public void enableSafetyWall(double durationSeconds) {
        if (safetyWall == null || !safetyWall.isActive()) {
            double sceneWidth = ResolutionManager.getInstance().getCurrentResolution().getWidth();
            double viewHeight = 8.0;
            double bboxHeight = 8.0;
            double y = ResolutionManager.getScaledValue(ResolutionManager.DESIGN_RESOLUTION.getHeight() - 40.0, Axis.VERTICAL);

            safetyWall = new EntityBuilder()
                    .type(EntityType.WALL_SAFETY)
                    .at(0, y)
                    .view(new Rectangle(sceneWidth, viewHeight, Color.color(0, 1, 0, 0.3)))
                    .bbox(new HitBox(BoundingShape.box(sceneWidth, bboxHeight)))
                    .with(new CollidableComponent(true))
                    .buildAndAttach();
            pushBallsAboveSafetyWall();
            FXGL.getGameTimer().runOnceAfter(this::pushBallsAboveSafetyWall, javafx.util.Duration.millis(1));
        }
        if (safetyWallTimer != null) {
            safetyWallTimer.expire();
        }
        safetyWallTimer = FXGL.getGameTimer().runOnceAfter(() -> {
            if (safetyWall != null && safetyWall.isActive()) {
                safetyWall.removeFromWorld();
            }
            safetyWall = null;
            safetyWallTimer = null;
        }, javafx.util.Duration.seconds(durationSeconds));
    }

    private void pushBallsAboveSafetyWall() {
        if (safetyWall == null || !safetyWall.isActive()){
            return;
        }
        double wallTop = safetyWall.getY();
        double wallHeight = safetyWall.getHeight() > 0 ? safetyWall.getHeight() : 8.0;
        double wallBottom = wallTop + wallHeight;
        double epsilon = 1.0;

        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ball : balls) {
            double ballTop = ball.getY();
            double ballBottom = ball.getY() + ball.getHeight();
            boolean overlapsVertically = ballBottom > wallTop && ballTop < wallBottom;
            if (overlapsVertically) {
                ball.setY(wallTop - ball.getHeight() - epsilon);
            }
        }
    }
}