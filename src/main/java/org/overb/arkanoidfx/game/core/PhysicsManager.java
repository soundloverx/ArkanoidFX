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

    private final Consumer<Entity> surprisePickupAction;

    public PhysicsManager(Consumer<Entity> surprisePickupAction) {
        this.surprisePickupAction = surprisePickupAction;
    }

    public void init() {
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PADDLE, EntityType.SURPRISE) {
            @Override
            protected void onCollisionBegin(Entity paddle, Entity surprise) {
                surprisePickupAction.accept(surprise);
                surprise.removeFromWorld();
            }
        });
    }
}