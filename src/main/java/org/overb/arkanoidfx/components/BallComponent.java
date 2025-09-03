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
import org.overb.arkanoidfx.enums.EventType;
import org.overb.arkanoidfx.game.core.EventBus;
import org.overb.arkanoidfx.game.core.GameEvent;

public class BallComponent extends Component {

    private static final double BASE_SPEED = 700.0; // pixels/sec at 1080p
    private static final double MIN_SPEED_MULTIPLIER = 0.5;
    private static final double MAX_SPEED_MULTIPLIER = 2.4;
    private static final double STEP_FRACTION_OF_BALL = 0.15;
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
        double speed = velocity.magnitude();
        if (speed <= 0) return;
        double ballSize = Math.max(entity.getWidth(), entity.getHeight());
        double maxStep = Math.max(1.0, ballSize * STEP_FRACTION_OF_BALL);
        int steps = (int) Math.ceil((speed * timePerFrame) / maxStep);
        steps = Math.min(steps, MAX_SUBSTEPS_PER_FRAME);
        double remainingFrameTime = timePerFrame;
        for (int i = 0; i < steps && remainingFrameTime > 0; i++) {
            // slice time per substep to not exceed the frame
            double sliceTime = Math.min(remainingFrameTime, maxStep / Math.max(1e-6, speed));
            remainingFrameTime -= sliceTime;
            // there may be multiple collisions within this slice if the ball is fast
            double timeLeft = sliceTime;
            for (int guard = 0; guard < 8 && timeLeft > 1e-6; guard++) {
                Point2D v = velocity;
                double vLen = v.magnitude();
                if (vLen < 1e-6) {
                    break;
                }
                Point2D dir = new Point2D(v.getX() / vLen, v.getY() / vLen);
                double maxTravel = vLen * timeLeft;

                // find the earliest time of impact
                TOI best = findEarliestTOI(dir, maxTravel);
                if (best == null || best.distance > maxTravel) {
                    // no hit this time, continue
                    entity.translate(dir.multiply(maxTravel));
                    break;
                }
                // advance to impact
                double travel = Math.max(0, best.distance - 1e-4); // tiny epsilon to avoid initial overlap
                if (travel > 0) {
                    entity.translate(dir.multiply(travel));
                }
                switch (best.target) {
                    case WALL_LEFT:
                    case WALL_RIGHT:
                        bounceHorizontal();
                        playWallHit();
                        break;
                    case WALL_TOP:
                    case WALL_SAFETY:
                        bounceVertical();
                        playWallHit();
                        break;
                    case WALL_BOTTOM_SENSOR:
                        entity.removeFromWorld();
                        if (FXGL.getGameWorld().getEntitiesByType(EntityType.BALL).isEmpty()) {
                            playLost();
                            EventBus.publish(GameEvent.of(EventType.BALL_LOST));
                        }
                        return;
                    case PADDLE:
                        reflectFromPaddle(paddle);
                        playPaddleHit();
                        break;
                    case BRICK:
                        best.brickHitCallbackIfAny();
                        Axis ax = chooseBounceAxisCircleRect(getBallCenter(entity), best.targetBox);
                        if (ax == Axis.HORIZONTAL) {
                            bounceHorizontal();
                        } else {
                            bounceVertical();
                        }
                        break;
                }
                enforceMinVerticalComponent();
                // nudge to avoid recollision with same brick next frame
                Point2D newDir = normalized(velocity);
                entity.translate(newDir.multiply(NUDGE));
                // consume the time associated with the distance at the previous ball velocity
                double usedTime = best.distance / vLen;
                timeLeft = Math.max(0, timeLeft - usedTime);
            }
        }
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

    private static Point2D getBallCenter(Entity e) {
        return new Point2D(e.getX() + e.getWidth() / 2.0, e.getY() + e.getHeight() / 2.0);
    }

    private static double getBallRadius(Entity e) {
        return Math.min(e.getWidth(), e.getHeight()) / 2.0;
    }

    private static Axis chooseBounceAxisCircleRect(Point2D c, Rectangle2D rect) {
        double closestX = Math.max(rect.getMinX(), Math.min(c.getX(), rect.getMaxX()));
        double closestY = Math.max(rect.getMinY(), Math.min(c.getY(), rect.getMaxY()));
        double dx = c.getX() - closestX;
        double dy = c.getY() - closestY;
        if (Math.abs(dx) > Math.abs(dy)) {
            return Axis.HORIZONTAL;
        } else {
            return Axis.VERTICAL;
        }
    }

    private static class TOI {
        double distance;
        EntityType target;
        Rectangle2D targetBox;
        Entity targetEntity;
        Runnable onHit;

        void brickHitCallbackIfAny() {
            if (onHit != null) onHit.run();
        }
    }

    private TOI findEarliestTOI(Point2D dirUnit, double maxDistance) {
        TOI best = null;
        Point2D c = getBallCenter(entity);
        double r = getBallRadius(entity);

        // Walls
        var world = FXGL.getGameWorld();
        var walls = world.getEntitiesByType(EntityType.WALL_LEFT, EntityType.WALL_RIGHT, EntityType.WALL_TOP, EntityType.WALL_BOTTOM_SENSOR, EntityType.WALL_SAFETY);
        for (Entity wall : walls) {
            EntityType type = (EntityType) wall.getTypeComponent().getValue();
            if (type == EntityType.WALL_SAFETY && velocity.getY() < 0) {
                continue;
            }
            Rectangle2D ra = getAABB(wall);
            TOI hit = sweepCircleAgainstAABB(c, dirUnit, r, ra);
            if (hit != null && hit.distance <= maxDistance) {
                if (type == EntityType.WALL_LEFT) hit.target = EntityType.WALL_LEFT;
                else if (type == EntityType.WALL_RIGHT) hit.target = EntityType.WALL_RIGHT;
                else if (type == EntityType.WALL_TOP) hit.target = EntityType.WALL_TOP;
                else if (type == EntityType.WALL_BOTTOM_SENSOR) hit.target = EntityType.WALL_BOTTOM_SENSOR;
                else if (type == EntityType.WALL_SAFETY) hit.target = EntityType.WALL_SAFETY;
                hit.targetEntity = wall;
                best = pickBetter(best, hit);
            }
        }

        // paddle (only if moving down)
        if (paddle != null && paddle.isActive() && velocity.getY() > 0) {
            Rectangle2D pr = getAABB(paddle);
            TOI hit = sweepCircleAgainstAABB(c, dirUnit, r, pr);
            if (hit != null && hit.distance <= maxDistance) {
                hit.target = EntityType.PADDLE;
                hit.targetEntity = paddle;
                best = pickBetter(best, hit);
            }
        }

        // bricks
        var bricks = FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK).stream().filter(Entity::isActive).toList();
        for (Entity brick : bricks) {
            var bc = brick.getComponentOptional(BrickComponent.class);
            if (bc.isEmpty() || bc.get().isDestroyed()) continue;
            Rectangle2D br = getAABB(brick);
            TOI hit = sweepCircleAgainstAABB(c, dirUnit, r, br);
            if (hit != null && hit.distance <= maxDistance) {
                hit.target = EntityType.BRICK;
                hit.targetEntity = brick;
                hit.onHit = () -> bc.ifPresent(comp -> comp.onBallHit(entity));
                best = pickBetter(best, hit);
            }
        }

        return best;
    }

    private static TOI pickBetter(TOI current, TOI candidate) {
        if (candidate == null) return current;
        if (current == null) return candidate;
        return candidate.distance < current.distance ? candidate : current;
    }

    // Swept circle vs AABB using Minkowski sum (expand rect by radius and cast point)
    private static TOI sweepCircleAgainstAABB(Point2D c, Point2D dirUnit, double r, Rectangle2D rect) {
        // Expand rectangle by radius
        Rectangle2D expanded = new Rectangle2D(rect.getMinX() - r, rect.getMinY() - r,
                rect.getWidth() + 2 * r, rect.getHeight() + 2 * r);
        // Ray from c in dirUnit. Compute t to each slab and take entry time
        double tx1, tx2, ty1, ty2;
        double dx = dirUnit.getX();
        double dy = dirUnit.getY();
        double tminX, tmaxX, tminY, tmaxY;
        double eps = 1e-8;
        if (Math.abs(dx) < eps) {
            if (c.getX() < expanded.getMinX() || c.getX() > expanded.getMaxX()) return null;
            tminX = Double.NEGATIVE_INFINITY;
            tmaxX = Double.POSITIVE_INFINITY;
        } else {
            tx1 = (expanded.getMinX() - c.getX()) / dx;
            tx2 = (expanded.getMaxX() - c.getX()) / dx;
            tminX = Math.min(tx1, tx2);
            tmaxX = Math.max(tx1, tx2);
        }
        if (Math.abs(dy) < eps) {
            if (c.getY() < expanded.getMinY() || c.getY() > expanded.getMaxY()) return null;
            tminY = Double.NEGATIVE_INFINITY;
            tmaxY = Double.POSITIVE_INFINITY;
        } else {
            ty1 = (expanded.getMinY() - c.getY()) / dy;
            ty2 = (expanded.getMaxY() - c.getY()) / dy;
            tminY = Math.min(ty1, ty2);
            tmaxY = Math.max(ty1, ty2);
        }
        double tEnter = Math.max(tminX, tminY);
        double tExit = Math.min(tmaxX, tmaxY);
        if (tExit < 0) return null; // box is behind
        if (tEnter > tExit) return null; // miss
        if (tEnter < 0) tEnter = 0; // already inside expanded box, treat as immediate hit
        TOI res = new TOI();
        res.distance = tEnter * Math.hypot(dirUnit.getX(), dirUnit.getY()); // since dirUnit length=1, distance = tEnter
        res.targetBox = rect;
        return res;
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