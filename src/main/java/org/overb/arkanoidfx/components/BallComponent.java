package org.overb.arkanoidfx.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import lombok.Getter;
import lombok.Setter;
import org.overb.arkanoidfx.audio.SfxBus;
import org.overb.arkanoidfx.enums.Axis;
import org.overb.arkanoidfx.enums.EntityType;

public class BallComponent extends Component {

    private static final double BASE_DESIGN_SPEED = 700.0; // pixels/sec at 1080p
    private static double BASE_SPEED = BASE_DESIGN_SPEED;
    private static final double MIN_SPEED_MULTIPLIER = 0.5;
    private static final double MAX_SPEED_MULTIPLIER = 2.4;
    private static final double STEP_FRACTION_OF_BALL = 0.25;
    private static final int MAX_SUBSTEPS_PER_FRAME = 64;
    private static final double NUDGE = 0.25; // small nudge after bounce to avoid hitting the same brick twice
    private static final int CONTACT_SEARCH_ITERS = 3; // binary search iterations to find contact
    // anti-trap parameters
    private static final double MIN_ABS_VY = 60.0;      // minimum vertical speed component after any bounce
    private static final double TINY_JITTER_RAD = 0.02; // small jitter to avoid infinite vertical bouncing

    private final Entity paddle;
    @Getter
    private boolean launched = false;

    @Getter
    @Setter
    private Point2D velocity = Point2D.ZERO;
    private double speedMultiplier = 1.0;
    private String sndHitWall;
    private String sndHitPaddle;
    private String sndLost;

    public BallComponent(Entity paddle) {
        this.paddle = paddle;
    }

    public void setSounds(String hitWall, String hitPaddle, String lost) {
        this.sndHitWall = hitWall;
        this.sndHitPaddle = hitPaddle;
        this.sndLost = lost;
    }

    public void playWallHit() {
        SfxBus.getInstance().play(sndHitWall);
    }

    public void playPaddleHit() {
        SfxBus.getInstance().play(sndHitPaddle);
    }

    public void playLost() {
        SfxBus.getInstance().play(sndLost);
    }

    @Override
    public void onUpdate(double timePerFrame) {
        if (!launched) {
            double px = paddle.getX();
            double pw = paddle.getWidth();
            double py = paddle.getY();
            double ballW = entity.getWidth();
            entity.setX(px + pw / 2.0 - ballW / 2.0);
            entity.setY(py - entity.getHeight() - 4.0);
            return;
        }
        moveWithSubsteps(timePerFrame);
    }

    private void moveWithSubsteps(double timePerFrame) {
        double totalDistance = velocity.magnitude() * timePerFrame;
        if (totalDistance <= 0) return;

        double ballSize = Math.max(entity.getWidth(), entity.getHeight());
        double maxStep = Math.max(1.0, ballSize * STEP_FRACTION_OF_BALL);
        int steps = (int) Math.ceil(totalDistance / maxStep);
        steps = Math.min(steps, MAX_SUBSTEPS_PER_FRAME);

        Point2D direction = normalized(velocity);
        double stepDistance = totalDistance / steps;

        for (int i = 0; i < steps; i++) {
            entity.translate(direction.multiply(stepDistance));
            // paddle
            if (checkAndResolvePaddleOverlap(direction, stepDistance)) {
                direction = normalized(velocity);
                entity.translate(direction.multiply(NUDGE));
            }
            // walls
            if (checkAndResolveWallOverlap(direction, stepDistance)) {
                direction = normalized(velocity);
                entity.translate(direction.multiply(NUDGE));
            }
            // bricks
            if (checkAndResolveBrickOverlap(direction, stepDistance)) {
                direction = normalized(velocity);
                entity.translate(direction.multiply(NUDGE));
            }
        }
    }

    private void snapBackToContactAgainst(Rectangle2D otherAABB, Point2D dir, double maxBack) {
        Rectangle2D startAABB = getAABB(entity);
        if (!intersects(startAABB, otherAABB)) {
            return;
        }
        double low = 0.0;
        double high = maxBack;
        entity.translate(dir.multiply(-high));
        boolean separated = !intersects(getAABB(entity), otherAABB);
        if (!separated) {
            entity.translate(dir.multiply(high));
            return;
        }
        for (int k = 0; k < CONTACT_SEARCH_ITERS; k++) {
            double mid = (low + high) * 0.5;
            double forward = high - mid;
            entity.translate(dir.multiply(forward)); // move forward
            boolean nowOverlapping = intersects(getAABB(entity), otherAABB);
            if (nowOverlapping) {
                entity.translate(dir.multiply(-forward));
                low = mid;
            } else {
                high = mid;
            }
        }
    }

    private boolean checkAndResolvePaddleOverlap(Point2D direction, double stepDistance) {
        if (paddle == null || !paddle.isActive()) {
            return false;
        }
        Rectangle2D ballAABB = getAABB(entity);
        Rectangle2D padAABB = getAABB(paddle);
        if (!intersects(ballAABB, padAABB)) {
            return false;
        }
        // place the ball at the contact point first
        snapBackToContactAgainst(padAABB, direction, Math.max(stepDistance, NUDGE));
        reflectFromPaddle(paddle);
        playPaddleHit();
        enforceMinVerticalComponent();
        return true;
    }

    private boolean checkAndResolveWallOverlap(Point2D direction, double stepDistance) {
        Rectangle2D ballAABB = getAABB(entity);
        var world = FXGL.getGameWorld();
        var walls = world.getEntitiesByType(EntityType.WALL_LEFT, EntityType.WALL_RIGHT, EntityType.WALL_TOP, EntityType.WALL_BOTTOM_SENSOR, EntityType.WALL_SAFETY);
        boolean bounced = false;
        for (Entity wall : walls) {
            Rectangle2D wallAABB = getAABB(wall);
            if (!intersects(ballAABB, wallAABB)) {
                continue;
            }
            EntityType type = (EntityType) wall.getTypeComponent().getValue();
            if (type == EntityType.WALL_BOTTOM_SENSOR) {
                return true;
            }
            snapBackToContactAgainst(wallAABB, direction, Math.max(stepDistance, NUDGE));
            if (type == EntityType.WALL_LEFT || type == EntityType.WALL_RIGHT) {
                bounceHorizontal();
                playWallHit();
            } else {
                bounceVertical();
                playWallHit();
            }
            enforceMinVerticalComponent();
            bounced = true;
            ballAABB = getAABB(entity);
        }
        return bounced;
    }


    private boolean checkAndResolveBrickOverlap(Point2D direction, double stepDistance) {
        Rectangle2D ballAABB = getAABB(entity);
        var bricks = FXGL.getGameWorld()
                .getEntitiesByType(EntityType.BRICK)
                .stream().filter(Entity::isActive).toList();
        boolean bounced = false;
        for (Entity brick : bricks) {
            Rectangle2D brickAABB = getAABB(brick);
            if (!intersects(ballAABB, brickAABB)) {
                continue;
            }
            snapBackToContactAgainst(brickAABB, direction, Math.max(stepDistance, NUDGE));
            Axis axis = chooseBounceAxis(getAABB(entity), brickAABB);
            brick.getComponentOptional(BrickComponent.class).ifPresent(comp -> comp.onBallHit(entity));
            if (axis == Axis.HORIZONTAL) {
                bounceHorizontal();
            } else {
                bounceVertical();
            }
            enforceMinVerticalComponent();
            bounced = true;
            ballAABB = getAABB(entity);
        }
        return bounced;
    }

    private Axis chooseBounceAxis(Rectangle2D ball, Rectangle2D box) {
        double overlapLeft = ball.getMaxX() - box.getMinX();
        double overlapRight = box.getMaxX() - ball.getMinX();
        double overlapTop = ball.getMaxY() - box.getMinY();
        double overlapBottom = box.getMaxY() - ball.getMinY();
        double minXOverlap = Math.min(overlapLeft, overlapRight);
        double minYOverlap = Math.min(overlapTop, overlapBottom);
        if (minXOverlap < minYOverlap) {
            return Axis.HORIZONTAL;
        }
        return Axis.VERTICAL;
    }

    private void enforceMinVerticalComponent() {
        double speed = velocity.magnitude();
        if (speed <= 1e-6) return;
        double vx = velocity.getX();
        double vy = velocity.getY();
        if (Math.abs(vy) < MIN_ABS_VY) {
            double signY = (vy >= 0) ? 1.0 : -1.0;
            vy = signY * MIN_ABS_VY;
            double vxSign = Math.signum(vx == 0 ? 1 : vx);
            double newVx = Math.sqrt(Math.max(0.0, speed * speed - vy * vy)) * vxSign;
            velocity = new Point2D(newVx, vy);
            if (Math.abs(velocity.getY()) < MIN_ABS_VY * 0.75) {
                velocity = rotate(velocity, (vy >= 0 ? 1 : -1) * TINY_JITTER_RAD);
            }
        }
    }

    private static Point2D rotate(Point2D v, double angleRad) {
        double c = Math.cos(angleRad);
        double s = Math.sin(angleRad);
        return new Point2D(v.getX() * c - v.getY() * s, v.getX() * s + v.getY() * c);
    }

    private static Rectangle2D getAABB(Entity e) {
        double x = e.getX();
        double y = e.getY();
        double w = e.getWidth();
        double h = e.getHeight();
        return new Rectangle2D(x, y, w, h);
    }

    private static boolean intersects(Rectangle2D a, Rectangle2D b) {
        return a.getMaxX() > b.getMinX() &&
                a.getMinX() < b.getMaxX() &&
                a.getMaxY() > b.getMinY() &&
                a.getMinY() < b.getMaxY();
    }

    public void launch() {
        if (launched) return;
        launched = true;
        velocity = new Point2D(0, -BASE_SPEED);
    }

    public double getSpeed() {
        return velocity.magnitude();
    }

    public static double getBaseSpeed() {
        return BASE_SPEED;
    }

    public void bounceHorizontal() {
        velocity = new Point2D(-velocity.getX(), velocity.getY());
    }

    public void bounceVertical() {
        velocity = new Point2D(velocity.getX(), -velocity.getY());
    }

    public void reflectFromPaddle(Entity paddle) {
        if (velocity.getY() <= 0) {
            return;
        }
        double paddleCenterX = paddle.getX() + paddle.getWidth() / 2.0;
        double ballCenterX = entity.getX() + entity.getWidth() / 2.0;
        double offset = (ballCenterX - paddleCenterX) / (paddle.getWidth() / 2.0);
        offset = Math.max(-1.0, Math.min(1.0, offset));
        double maxH = 0.85;
        double minH = 0.06;
        double dx = offset * maxH;
        if (Math.abs(dx) < minH) {
            double sign = (Math.abs(offset) >= 1e-6) ? Math.signum(offset) : ((Math.abs(velocity.getX()) >= 1e-6) ? Math.signum(velocity.getX()) : 1.0);
            dx = sign * minH;
        }
        double dy = -Math.sqrt(Math.max(0.0, 1.0 - dx * dx));
        double currentSpeed = velocity.magnitude();
        double baseline = Math.max(currentSpeed, BASE_SPEED * speedMultiplier);
        double angleFactor = Math.abs(offset);
        double gain = 1.0 + (1.0 - angleFactor) * 0.06;
        double unclamped = baseline * gain;
        double maxSpeed = BASE_SPEED * MAX_SPEED_MULTIPLIER;
        double newSpeed = Math.min(unclamped, maxSpeed);
        speedMultiplier = clamp(newSpeed / BASE_SPEED);
        setSpeedDir(new Point2D(dx, dy), newSpeed);
    }

    public void setLaunchedWithVelocity(Point2D initialVelocity) {
        if (initialVelocity == null) {
            return;
        }
        launched = true;
        velocity = initialVelocity;
    }

    public void boostSpeedByFactor(double factor) {
        if (factor <= 0) {
            return;
        }
        double currentSpeed = velocity.magnitude();
        double desiredMultiplier = clamp(speedMultiplier * factor);
        double desiredSpeed = BASE_SPEED * desiredMultiplier;
        double newSpeed;
        if (factor >= 1.0) {
            newSpeed = Math.max(currentSpeed, desiredSpeed);
            speedMultiplier = clamp(newSpeed / BASE_SPEED);
        } else {
            speedMultiplier = desiredMultiplier;
            newSpeed = desiredSpeed;
        }
        Point2D dir = normalized(velocity);
        setSpeedDir(dir, newSpeed);
    }

    private static double clamp(double velocity) {
        return Math.max(BallComponent.MIN_SPEED_MULTIPLIER, Math.min(BallComponent.MAX_SPEED_MULTIPLIER, velocity));
    }

    private void setSpeedDir(Point2D dirUnit, double speed) {
        double len = Math.hypot(dirUnit.getX(), dirUnit.getY());
        if (len < 1e-6) {
            velocity = new Point2D(0, -speed);
        } else {
            velocity = new Point2D(dirUnit.getX() / len * speed, dirUnit.getY() / len * speed);
        }
    }

    private static Point2D normalized(Point2D v) {
        double len = Math.hypot(v.getX(), v.getY());
        if (len < 1e-6) return new Point2D(0, -1);
        return new Point2D(v.getX() / len, v.getY() / len);
    }
}