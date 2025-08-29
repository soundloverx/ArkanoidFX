package org.overb.arkanoidfx.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import org.overb.arkanoidfx.entities.SurpriseEntity;

public class SurpriseComponent extends Component {

    private final SurpriseEntity surpriseDefinition;

    public SurpriseComponent(SurpriseEntity surpriseDefinition) {
        this.surpriseDefinition = surpriseDefinition;
    }

    @Override
    public void onUpdate(double tpf) {
        double fallSpeed = surpriseDefinition.fallSpeed > 0 ? surpriseDefinition.fallSpeed : 220.0;
        entity.translate(new Point2D(0, fallSpeed * tpf));
        if (entity.getY() > 2000) {
            entity.removeFromWorld();
        }
    }

    public String getEffect() {
        return surpriseDefinition.effect;
    }

    public String getSound() {
        return surpriseDefinition.sound;
    }
}