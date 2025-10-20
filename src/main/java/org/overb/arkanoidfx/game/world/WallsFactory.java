package org.overb.arkanoidfx.game.world;

import com.almasb.fxgl.dsl.EntityBuilder;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.time.TimerAction;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.overb.arkanoidfx.enums.EntityType;
import org.overb.arkanoidfx.game.ResolutionManager;

public final class WallsFactory {

    private Entity safetyWall;
    private Rectangle safetyWallView;
    private TimerAction safetyWallTimer;
    private TimerAction flickerStartTimer;
    private TimerAction flickerSpeedUpTimer;
    private FadeTransition appearFade;
    private FadeTransition expireFlicker;

    public void spawnWalls() {
        new EntityBuilder()
                .type(EntityType.WALL_LEFT)
                .at(0, 0)
                .bbox(new HitBox(BoundingShape.box(2, ResolutionManager.DESIGN_RESOLUTION.getHeight())))
                .with(new CollidableComponent(true))
                .buildAndAttach();
        new EntityBuilder()
                .type(EntityType.WALL_RIGHT)
                .at(ResolutionManager.DESIGN_RESOLUTION.getWidth() - 2, 0)
                .bbox(new HitBox(BoundingShape.box(2, ResolutionManager.DESIGN_RESOLUTION.getHeight())))
                .with(new CollidableComponent(true))
                .buildAndAttach();
        new EntityBuilder()
                .type(EntityType.WALL_TOP)
                .at(0, -2)
                .bbox(new HitBox(BoundingShape.box(ResolutionManager.DESIGN_RESOLUTION.getWidth(), 2)))
                .with(new CollidableComponent(true))
                .buildAndAttach();
        new EntityBuilder()
                .type(EntityType.WALL_BOTTOM_SENSOR)
                .at(0, ResolutionManager.DESIGN_RESOLUTION.getHeight() - 2)
                .bbox(new HitBox(BoundingShape.box(ResolutionManager.DESIGN_RESOLUTION.getWidth(), 50)))
                .with(new CollidableComponent(true))
                .buildAndAttach();
    }

    public void enableSafetyWall(double durationSeconds) {
        if (safetyWall == null || !safetyWall.isActive()) {
            double sceneWidth = ResolutionManager.DESIGN_RESOLUTION.getWidth();
            double bboxHeight = 8.0;
            Image safetyImg = FXGL.image("safety_wall_single_small.png");
            double viewHeight = safetyImg.getHeight();
            double y = ResolutionManager.DESIGN_RESOLUTION.getHeight() - viewHeight;
            Rectangle viewRect = new Rectangle(sceneWidth, viewHeight);
            viewRect.setFill(new ImagePattern(safetyImg, 0, 0, safetyImg.getWidth(), safetyImg.getHeight(), false));
            safetyWall = new EntityBuilder()
                    .type(EntityType.WALL_SAFETY)
                    .at(0, y)
                    .view(viewRect)
                    .bbox(new HitBox(BoundingShape.box(sceneWidth, bboxHeight)))
                    .with(new CollidableComponent(true))
                    .buildAndAttach();

            safetyWallView = viewRect;
            if (appearFade != null) {
                appearFade.stop();
            }
            safetyWallView.setOpacity(0.0);
            appearFade = new FadeTransition(Duration.millis(300), safetyWallView);
            appearFade.setFromValue(0.0);
            appearFade.setToValue(1.0);
            appearFade.play();

            pushBallsAboveSafetyWall();
            FXGL.getGameTimer().runOnceAfter(this::pushBallsAboveSafetyWall, Duration.millis(1));
        } else {
            if (safetyWallView != null) {
                safetyWallView.setOpacity(1.0);
            }
        }
        if (safetyWallTimer != null) {
            safetyWallTimer.expire();
        }
        stopExpireWarning();
        safetyWallTimer = FXGL.getGameTimer().runOnceAfter(() -> {
            stopExpireWarning();
            if (safetyWall != null && safetyWall.isActive()) {
                safetyWall.removeFromWorld();
            }
            safetyWall = null;
            safetyWallView = null;
            safetyWallTimer = null;
        }, Duration.seconds(durationSeconds));
        double warnWindow = Math.min(3.0, Math.max(0.0, durationSeconds));
        double delayBeforeWarn = Math.max(0.0, durationSeconds - warnWindow);
        if (warnWindow > 0.0) {
            if (delayBeforeWarn == 0.0) {
                startExpireWarning(warnWindow);
            } else {
                flickerStartTimer = FXGL.getGameTimer().runOnceAfter(() -> startExpireWarning(warnWindow), Duration.seconds(delayBeforeWarn));
            }
        }
    }

    private void startExpireWarning(double warnWindowSeconds) {
        if (safetyWallView == null) {
            return;
        }
        if (expireFlicker != null) {
            expireFlicker.stop();
        }
        expireFlicker = new FadeTransition(Duration.millis(600), safetyWallView);
        expireFlicker.setFromValue(1.0);
        expireFlicker.setToValue(0.35);
        expireFlicker.setAutoReverse(true);
        expireFlicker.setCycleCount(Animation.INDEFINITE);
        expireFlicker.play();

        final int maxSteps = Math.max(1, (int) Math.floor(warnWindowSeconds / 0.25));
        final int[] step = new int[]{0};
        final TimerAction[] holder = new TimerAction[1];
        holder[0] = FXGL.getGameTimer().runAtInterval(() -> {
            if (safetyWall == null || !safetyWall.isActive() || safetyWallView == null) {
                if (holder[0] != null) holder[0].expire();
                return;
            }
            step[0]++;
            double progress = Math.min(1.0, step[0] / (double) maxSteps);
            // increase flicker speed
            double newMs = 600.0 - progress * (600.0 - 120.0);
            if (expireFlicker != null) {
                expireFlicker.stop();
                expireFlicker.setDuration(Duration.millis(newMs));
                expireFlicker.play();
            }
            if (step[0] >= maxSteps) {
                if (holder[0] != null) holder[0].expire();
            }
        }, Duration.seconds(0.25));
        flickerSpeedUpTimer = holder[0];
    }

    private void stopExpireWarning() {
        if (flickerStartTimer != null) {
            flickerStartTimer.expire();
            flickerStartTimer = null;
        }
        if (flickerSpeedUpTimer != null) {
            flickerSpeedUpTimer.expire();
            flickerSpeedUpTimer = null;
        }
        if (expireFlicker != null) {
            expireFlicker.stop();
            expireFlicker = null;
        }
        if (safetyWallView != null) {
            safetyWallView.setOpacity(1.0);
        }
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