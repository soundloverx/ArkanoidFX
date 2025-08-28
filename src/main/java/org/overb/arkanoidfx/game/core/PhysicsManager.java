package org.overb.arkanoidfx.game.core;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.physics.CollisionHandler;
import org.overb.arkanoidfx.components.BallComponent;
import org.overb.arkanoidfx.enums.EntityType;
import org.overb.arkanoidfx.enums.EventType;
import org.overb.arkanoidfx.game.GameSession;
import org.overb.arkanoidfx.game.ui.HUDManager;

import java.util.List;
import java.util.function.Consumer;

public final class PhysicsManager {

    private final GameSession session;
    private final HUDManager hudManager;
    private final Consumer<Void> spawnPaddleAndBallAction;
    private final Consumer<Entity> surprisePickupAction;

    public PhysicsManager(GameSession session,
                          HUDManager hudManager,
                          Consumer<Void> spawnPaddleAndBallAction,
                          Consumer<Entity> surprisePickupAction) {
        this.session = session;
        this.hudManager = hudManager;
        this.spawnPaddleAndBallAction = spawnPaddleAndBallAction;
        this.surprisePickupAction = surprisePickupAction;
    }

    public void init() {
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.WALL_BOTTOM_SENSOR) {
            @Override
            protected void onCollisionBegin(Entity ballEntity, Entity sensor) {
                List<Entity> balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                if (balls.size() > 1) {
                    ballEntity.removeFromWorld();
                    return;
                }
                session.loseLife();
                ballEntity.getComponentOptional(BallComponent.class).ifPresent(BallComponent::playLost);
                ballEntity.removeFromWorld();
                EventBus.publish(GameEvent.of(EventType.HUD_UPDATE));
                if (session.getLives() > 0) {
                    spawnPaddleAndBallAction.accept(null);
                } else {
                    EventBus.publish(GameEvent.of(EventType.GAME_OVER));
                }
            }
        });

        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PADDLE, EntityType.SURPRISE) {
            @Override
            protected void onCollisionBegin(Entity paddle, Entity surprise) {
                surprisePickupAction.accept(surprise);
                surprise.removeFromWorld();
            }
        });
    }
}