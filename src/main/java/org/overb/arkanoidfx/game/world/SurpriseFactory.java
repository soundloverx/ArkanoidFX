package org.overb.arkanoidfx.game.world;

import com.almasb.fxgl.dsl.EntityBuilder;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.texture.Texture;
import javafx.scene.Group;
import org.overb.arkanoidfx.components.BallAnimComponent;
import org.overb.arkanoidfx.components.SurpriseComponent;
import org.overb.arkanoidfx.entities.SurpriseEntity;
import org.overb.arkanoidfx.enums.EntityType;
import org.overb.arkanoidfx.util.TextureUtils;

import java.util.Optional;

public final class SurpriseFactory {

    public Optional<Entity> buildAt(double x, double y, SurpriseEntity def) {
        Group viewRoot = new Group();
        viewRoot.setAutoSizeChildren(false);
        int frameW = Math.max(1, def.visual.frameW);
        int frameH = Math.max(1, def.visual.frameH);
        Texture tex = TextureUtils.loadTextureOrNull(def.visual.sprite, frameW, frameH, frameW, frameH);
        viewRoot.getChildren().add(tex);
        Entity entity = new EntityBuilder()
                .type(EntityType.SURPRISE)
                .at(x, y)
                .view(viewRoot)
                .bbox(new HitBox(BoundingShape.box(frameW, frameH)))
                .with(new CollidableComponent(true))
                .with(new SurpriseComponent(def))
                .build();
        int frames = Math.max(1, def.visual.frames);
        double frameDuration = def.visual.frameDuration > 0 ? def.visual.frameDuration : 0.0;
        if (frames > 1 && frameDuration > 0.0) {
            entity.addComponent(new BallAnimComponent(tex, frames, frameW, frameH, frameDuration));
        }
        return Optional.of(entity);
    }
}