package org.overb.arkanoidfx.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.util.Duration;
import org.overb.arkanoidfx.game.ResolutionManager;

public class PaddleComponent extends Component {

    private enum Phase {NONE, DOWN, UP}

    private final double halfWidth;
    private final double yFixed;

    // vertical push
    private final double pushDownPixels = 7.0;
    private final Duration pushAnimTotal = Duration.seconds(0.2);
    private Phase phase = Phase.NONE;
    private double startY;
    private double targetY;
    private double pushElapsed;
    private double pushLegDuration;

    // tilt
    private final double maxTiltDeg = 4.0;
    private final Duration tiltAnimTotal = Duration.seconds(0.2);
    private final double tiltDeadZone = 0.1;
    private Phase tiltPhase = Phase.NONE;
    private double startAngle;
    private double targetAngle;
    private double tiltElapsed;
    private double tiltLegDuration;

    public PaddleComponent(double width, double y) {
        this.halfWidth = width / 2.0;
        this.yFixed = y;
    }

    @Override
    public void onAdded() {
        entity.setY(yFixed);
        entity.setRotation(0);
    }

    @Override
    public void onUpdate(double tpf) {
        // follow mouse, no vertical change
        Point2D mouse = FXGL.getInput().getMousePositionWorld();
        double x = mouse.getX() - halfWidth;
        double minX = 0;
        double maxX = ResolutionManager.DESIGN_RESOLUTION.getWidth() - halfWidth * 2.0;
        if (x < minX) x = minX;
        if (x > maxX) x = maxX;
        entity.setX(x);

        // vertical animation
        if (phase != Phase.NONE) {
            pushElapsed += tpf;
            double t = Math.min(1.0, (pushLegDuration <= 0 ? 1.0 : pushElapsed / pushLegDuration));
            // simple ease-out on down, ease-in on up
            double easedT = (phase == Phase.DOWN) ? (1 - Math.pow(1 - t, 2)) : (t * t);
            double newY = startY + (targetY - startY) * easedT;
            entity.setY(newY);
            if (t >= 1.0 - 1e-6) {
                entity.setY(targetY);
                if (phase == Phase.DOWN) {
                    beginPushLeg(Phase.UP, targetY, yFixed, pushAnimTotal.toSeconds() / 2.0);
                } else {
                    phase = Phase.NONE;
                    pushElapsed = 0;
                    startY = targetY = yFixed;
                    entity.setY(yFixed);
                }
            }
        }

        // tilt animation
        if (tiltPhase != Phase.NONE) {
            tiltElapsed += tpf;
            double t2 = Math.min(1.0, (tiltLegDuration <= 0 ? 1.0 : tiltElapsed / tiltLegDuration));
            double easedT2 = (tiltPhase == Phase.DOWN) ? (1 - Math.pow(1 - t2, 2)) : (t2 * t2);
            double angle = startAngle + (targetAngle - startAngle) * easedT2;
            angle = Math.max(-maxTiltDeg, Math.min(maxTiltDeg, angle));
            entity.setRotation(angle);
            if (t2 >= 1.0 - 1e-6) {
                entity.setRotation(targetAngle);
                if (tiltPhase == Phase.DOWN) {
                    beginTiltLeg(Phase.UP, targetAngle, 0.0, tiltAnimTotal.toSeconds() / 2.0);
                } else {
                    tiltPhase = Phase.NONE;
                    tiltElapsed = 0.0;
                    startAngle = targetAngle = 0.0;
                    entity.setRotation(0.0);
                }
            }
        }
    }

    public void onBallHit() {
        double downTarget = yFixed + pushDownPixels;
        double currentY = entity.getY();
        if (currentY > downTarget) {
            currentY = downTarget;
            entity.setY(downTarget);
        }
        double half = pushAnimTotal.toSeconds() / 2.0;
        if (currentY < downTarget - 0.001) {
            beginPushLeg(Phase.DOWN, currentY, downTarget, half);
        } else {
            beginPushLeg(Phase.UP, currentY, yFixed, half);
        }
    }

    public void onBallHit(double hitOffsetNorm) {
        onBallHit();
        double hit = Math.max(-1.0, Math.min(1.0, hitOffsetNorm));
        double abs = Math.abs(hit);
        double effective;
        if (abs <= tiltDeadZone) {
            effective = 0.0;
        } else {
            effective = (abs - tiltDeadZone) / (1.0 - tiltDeadZone);
        }
        double sign = (hit == 0.0) ? 0.0 : Math.signum(hit);
        double desiredAngle = sign * maxTiltDeg * effective;
        double currentAngle = entity.getRotation();
        if (currentAngle > maxTiltDeg) {
            currentAngle = maxTiltDeg;
            entity.setRotation(currentAngle);
        } else if (currentAngle < -maxTiltDeg) {
            currentAngle = -maxTiltDeg;
            entity.setRotation(currentAngle);
        }
        double half = tiltAnimTotal.toSeconds() / 2.0;
        if (Math.abs(currentAngle - desiredAngle) > 0.05) {
            beginTiltLeg(Phase.DOWN, currentAngle, desiredAngle, half);
        } else {
            beginTiltLeg(Phase.UP, currentAngle, 0.0, half);
        }
    }

    private void beginPushLeg(Phase newPhase, double fromY, double toY, double durationSec) {
        this.phase = newPhase;
        this.startY = fromY;
        this.targetY = toY;
        this.pushElapsed = 0.0;
        this.pushLegDuration = Math.max(0.0001, durationSec);
    }

    private void beginTiltLeg(Phase newPhase, double fromAngle, double toAngle, double durationSec) {
        this.tiltPhase = newPhase;
        this.startAngle = fromAngle;
        this.targetAngle = toAngle;
        this.tiltElapsed = 0.0;
        this.tiltLegDuration = Math.max(0.0001, durationSec);
    }
}