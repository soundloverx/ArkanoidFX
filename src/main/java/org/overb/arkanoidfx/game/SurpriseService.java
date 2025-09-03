package org.overb.arkanoidfx.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.geometry.Point2D;
import org.overb.arkanoidfx.components.BallComponent;
import org.overb.arkanoidfx.enums.EntityType;
import org.overb.arkanoidfx.game.world.BallFactory;
import org.overb.arkanoidfx.game.world.WallsFactory;

import java.util.ArrayList;
import java.util.List;

public final class SurpriseService {

    private final BallFactory ballFactory;
    private final WallsFactory wallsFactory;

    public SurpriseService(BallFactory ballFactory, WallsFactory wallsFactory) {
        this.ballFactory = ballFactory;
        this.wallsFactory = wallsFactory;
    }

    public void applyMultiball() {
        // Snapshot current balls to avoid chain-spawning from newly created balls within the same frame
        List<Entity> balls = new ArrayList<>(FXGL.getGameWorld().getEntitiesByType(EntityType.BALL));
        if (balls.isEmpty() || balls.size() >= 30) {
            return;
        }

        // Resolve current paddle for new balls to attach logic/state
        List<Entity> paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
        Entity paddle = paddles.isEmpty() ? null : paddles.getFirst();
        if (paddle == null) {
            return;
        }

        double delta = Math.toRadians(10.0);

        for (Entity ball : balls) {
            var bc = ball.getComponentOptional(BallComponent.class).orElse(null);
            if (bc == null) {
                continue;
            }
            Point2D v = bc.getVelocity();
            double speed = v.magnitude();
            if (speed < 1e-3) {
                continue;
            }
            Point2D dir = v.normalize();

            spawnBallWithAngleOffset(ball, dir, speed, +delta, paddle);
            spawnBallWithAngleOffset(ball, dir, speed, -delta, paddle);
        }
    }

    private void spawnBallWithAngleOffset(Entity sourceBall, Point2D dir, double speed, double ang, Entity paddle) {
        Point2D rotated = rotate(dir, ang).normalize().multiply(speed);
        double x = sourceBall.getCenter().getX() - 8.0;
        double y = sourceBall.getCenter().getY() - 8.0;
        ballFactory.spawnLaunchedBallAt(x, y, rotated, paddle);
    }

    private Point2D rotate(Point2D v, double ang) {
        double c = Math.cos(ang), s = Math.sin(ang);
        return new Point2D(v.getX() * c - v.getY() * s, v.getX() * s + v.getY() * c);
    }

    public void applySafetyWall(double durationSec) {
        wallsFactory.enableSafetyWall(durationSec);
    }
}