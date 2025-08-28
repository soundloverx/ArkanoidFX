package org.overb.arkanoidfx.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

public class DebugHitboxViewComponent extends Component {

    private final List<Node> overlays = new ArrayList<>();

    @Override
    public void onAdded() {
        Rectangle outline = new Rectangle(entity.getWidth(), entity.getHeight());
        outline.setFill(Color.color(0, 0, 0, 0));
        outline.setStroke(Color.LIMEGREEN);
        outline.setStrokeWidth(1.0);
        entity.getViewComponent().addChild(outline);
        overlays.add(outline);
    }

    @Override
    public void onRemoved() {
        overlays.forEach(node -> entity.getViewComponent().removeChild(node));
        overlays.clear();
    }
}