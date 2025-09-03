package org.overb.arkanoidfx.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.texture.Texture;
import com.almasb.fxgl.time.TimerAction;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.util.Duration;
import lombok.Getter;
import org.overb.arkanoidfx.audio.SfxBus;
import org.overb.arkanoidfx.entities.BrickEntity;
import org.overb.arkanoidfx.entities.EntityRepository;
import org.overb.arkanoidfx.entities.SurpriseEntity;
import org.overb.arkanoidfx.enums.EventType;
import org.overb.arkanoidfx.game.GameSession;
import org.overb.arkanoidfx.game.core.EventBus;
import org.overb.arkanoidfx.game.core.GameEvent;
import org.overb.arkanoidfx.game.world.SurpriseFactory;
import org.overb.arkanoidfx.util.BallQueries;
import org.overb.arkanoidfx.util.TextureUtils;

import java.util.concurrent.ThreadLocalRandom;

public class BrickComponent extends Component {

    private final BrickEntity brickEntity;
    private final GameSession session;
    private final Texture texture;
    private final EntityRepository repository;
    private final SurpriseFactory surpriseFactory;
    private int hp;
    private int currentFrame = 0;
    @Getter
    private boolean destroyed = false;

    public BrickComponent(BrickEntity brickEntity, GameSession session, Texture texture,
                          EntityRepository repository, SurpriseFactory surpriseFactory) {
        this.brickEntity = brickEntity;
        this.session = session;
        this.hp = brickEntity.hp;
        this.texture = texture;
        this.repository = repository;
        this.surpriseFactory = surpriseFactory;
    }

    @Override
    public void onAdded() {
        if (texture != null) {
            TextureUtils.setViewportFrame(texture, 0, brickEntity.visual.frameW, brickEntity.visual.frameH);
        }
    }

    public void onBallHit(Entity ballEntity) {
        if (destroyed) {
            return;
        }
        SfxBus.getInstance().play(brickEntity.hitSound);
        if (brickEntity.speedEffect > 0) {
            ballEntity.getComponentOptional(BallComponent.class)
                    .ifPresent(bc -> bc.boostSpeedByFactor(1.0 + brickEntity.speedEffect));
        }
        ballEntity.getComponentOptional(BallComponent.class).ifPresent(bc -> {
            if (ThreadLocalRandom.current().nextDouble() < 0.20) {
                double sign = ThreadLocalRandom.current().nextBoolean() ? 1.0 : -1.0;
                double angRad = Math.toRadians(0.5) * sign;
                var v = bc.getVelocity();
                if (v != null && v.magnitude() > 1e-6) {
                    double cos = Math.cos(angRad);
                    double sin = Math.sin(angRad);
                    double nx = v.getX() * cos - v.getY() * sin;
                    double ny = v.getX() * sin + v.getY() * cos;
                    bc.setVelocity(new Point2D(nx, ny));
                }
            }
        });
        if (brickEntity.damageAdvancesFrame && brickEntity.visual.frames > 1) {
            advanceFrame();
        }
        if (hp == -1) {
            return;
        }
        hp = Math.max(0, hp - 1);
        if (hp != 0) {
            return;
        }
        destroyed = true;
        entity.getComponentOptional(CollidableComponent.class).ifPresent(cc -> cc.setValue(false));
        double maxSpeed = BallQueries.findMaxBallSpeed();
        double baseSpeed = BallComponent.getBaseSpeed();
        session.onBrickDestroyed(brickEntity.points, maxSpeed, baseSpeed);
        EventBus.publish(GameEvent.of(EventType.HUD_UPDATE));
        SfxBus.getInstance().play(brickEntity.destroySound);
        spawnSurpriseIfAny(entity.getX(), entity.getY());
        if (brickEntity.breakAnim != null) {
            final Entity e = entity;
            if (e == null) {
                return;
            }
            FXGL.getGameTimer().runOnceAfter(() -> {
                if (!e.isActive()) {
                    return;
                }
                playBreakAnimationOrRemove(e);
            }, Duration.millis(1));
        } else {
            if (entity != null && entity.isActive()) {
                entity.removeFromWorld();
            }
        }
        if (session.getDestructibleBricksLeft() == 0) {
            Platform.runLater(() -> EventBus.publish(GameEvent.of(EventType.LEVEL_FINISHED)));
        }
    }

    private void spawnSurpriseIfAny(double x, double y) {
        if (!session.areSurprisesEnabled()) {
            return;
        }
        var surprises = repository.getSurprises().values();
        if (surprises.isEmpty()) {
            return;
        }
        SurpriseEntity chosen = null;
        double bestRoll = 0;
        for (var s : surprises) {
            double chance = s.spawnChance > 0 ? s.spawnChance : 0.10;
            double roll = ThreadLocalRandom.current().nextDouble();
            if (roll < chance && (chosen == null || roll < bestRoll)) {
                chosen = s;
                bestRoll = roll;
            }
        }
        if (chosen == null) {
            return;
        }
        surpriseFactory.buildAt(x, y, chosen).ifPresent(e -> FXGL.getGameWorld().addEntity(e));
        SfxBus.getInstance().play("surprise.wav");
    }

    private void advanceFrame() {
        if (texture == null) return;
        int total = Math.max(1, brickEntity.visual.frames);
        currentFrame = (currentFrame + 1) % total;
        TextureUtils.setViewportFrame(texture, currentFrame, brickEntity.visual.frameW, brickEntity.visual.frameH);
    }

    private void playBreakAnimationOrRemove(Entity e) {
        var anim = brickEntity.breakAnim;
        if (anim == null) {
            if (e.isActive()) e.removeFromWorld();
            return;
        }
        Texture breakTex;
        try {
            breakTex = FXGL.texture(anim.sprite);
        } catch (Exception ex) {
            if (e.isActive()) e.removeFromWorld();
            return;
        }
        double targetW = e.getWidth();
        double targetH = e.getHeight();
        int fw = Math.max(1, anim.frameW);
        int fh = Math.max(1, anim.frameH);

        breakTex.setFitWidth(targetW);
        breakTex.setFitHeight(targetH);
        breakTex.setPreserveRatio(false);
        breakTex.setSmooth(false);
        breakTex.setTranslateX(0);
        breakTex.setTranslateY(0);
        TextureUtils.setViewportFrame(breakTex, 0, fw, fh);
        if (!e.isActive()) {
            return;
        }
        e.getViewComponent().clearChildren();
        e.getViewComponent().addChild(breakTex);
        int total = Math.max(1, anim.frames);
        double frameDurSec = anim.frameDuration > 0 ? anim.frameDuration : 0.05;
        final int[] frameIndex = {0};
        final TimerAction[] actionHolder = new TimerAction[1];
        actionHolder[0] = FXGL.getGameTimer().runAtInterval(() -> {
            if (!e.isActive()) {
                if (actionHolder[0] != null) actionHolder[0].expire();
                return;
            }
            frameIndex[0]++;
            if (frameIndex[0] >= total) {
                if (actionHolder[0] != null) actionHolder[0].expire();
                if (e.isActive()) e.removeFromWorld();
            } else {
                TextureUtils.setViewportFrame(breakTex, frameIndex[0], fw, fh);
            }
        }, Duration.seconds(frameDurSec));
    }
}