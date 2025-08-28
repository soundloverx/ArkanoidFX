package org.overb.arkanoidfx.util;

import com.almasb.fxgl.dsl.FXGL;
import org.overb.arkanoidfx.components.BallComponent;
import org.overb.arkanoidfx.enums.EntityType;

public final class BallQueries {

    private BallQueries() {}

    public static double findMaxBallSpeed() {
        return FXGL.getGameWorld()
                .getEntitiesByType(EntityType.BALL)
                .stream()
                .map(e -> e.getComponentOptional(BallComponent.class).map(BallComponent::getSpeed).orElse(0.0))
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
    }
}