package org.overb.arkanoidfx.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import org.overb.arkanoidfx.game.ResolutionManager;

public class PaddleComponent extends Component {

    private final double halfWidth;
    private final double yFixed;

    public PaddleComponent(double width, double y) {
        this.halfWidth = width / 2.0;
        this.yFixed = y;
    }

    @Override
    public void onAdded() {
        entity.setY(yFixed);
    }

    @Override
    public void onUpdate(double tpf) {
        Point2D mouse = FXGL.getInput().getMousePositionWorld();
        double x = mouse.getX() - halfWidth;
        double minX = 0;
        double maxX = ResolutionManager.DESIGN_RESOLUTION.getWidth() - halfWidth * 2.0;
        if (x < minX) x = minX;
        if (x > maxX) x = maxX;
        entity.setX(x);
    }
}