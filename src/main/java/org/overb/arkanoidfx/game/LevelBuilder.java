package org.overb.arkanoidfx.game;

import com.almasb.fxgl.dsl.EntityBuilder;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.texture.Texture;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.overb.arkanoidfx.components.BrickComponent;
import org.overb.arkanoidfx.entities.BrickEntity;
import org.overb.arkanoidfx.entities.EntityRepository;
import org.overb.arkanoidfx.entities.LevelEntity;
import org.overb.arkanoidfx.enums.EntityType;
import org.overb.arkanoidfx.game.world.SurpriseFactory;
import org.overb.arkanoidfx.util.TextureUtils;

public class LevelBuilder {

    private final EntityRepository entityDefinitions;
    private final GameSession session;
    private final SurpriseFactory surpriseFactory;
    private double brickW;
    private double brickH;

    public LevelBuilder(EntityRepository entityDefinitions, GameSession session) {
        this.entityDefinitions = entityDefinitions;
        this.session = session;
        this.surpriseFactory = new SurpriseFactory();
    }

    public void buildBricks(LevelEntity level) {
        brickW = EntityType.BRICK.getDesignWidth();
        brickH = EntityType.BRICK.getDesignHeight();
        for (LevelEntity.Cell cell : level.cells) {
            BrickEntity def = entityDefinitions.getBrick(cell.brickId);
            if (def == null) {
                continue;
            }
            double x = cell.col * brickW;
            double y = cell.row * brickH;
            var viewPair = buildBrickView(def);
            Group viewRoot = viewPair.root;
            Texture texture = viewPair.texture;
            new EntityBuilder()
                    .type(EntityType.BRICK)
                    .at(x, y)
                    .view(viewRoot)
                    .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(brickW, brickH)))
                    .with(new CollidableComponent(true))
                    .with(new BrickComponent(def, session, texture, entityDefinitions, surpriseFactory))
                    .buildAndAttach()
                    .getTransformComponent().setScaleOrigin(new Point2D(0, 0));
            if (def.hp != -1) {
                session.registerDestructibleBrick();
            }
        }
    }

    private record ViewPair(Group root, Texture texture) {
    }

    private ViewPair buildBrickView(BrickEntity def) {
        Group root = new Group();
        root.setAutoSizeChildren(false);
        Texture tex = TextureUtils.loadTextureOrNull(def.visual.sprite, brickW, brickH, Math.max(1, def.visual.frameW), Math.max(1, def.visual.frameH));
        if (tex != null) {
            root.getChildren().add(tex);
            return new ViewPair(root, tex);
        } else {
            Rectangle fallback = new Rectangle(brickW, brickH, Color.HOTPINK);
            fallback.setStroke(Color.BLACK);
            fallback.setTranslateX(0);
            fallback.setTranslateY(0);
            root.getChildren().add(fallback);
            return new ViewPair(root, null);
        }
    }
}