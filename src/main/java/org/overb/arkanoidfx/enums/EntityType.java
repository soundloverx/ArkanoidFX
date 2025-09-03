package org.overb.arkanoidfx.enums;

import lombok.Getter;

public enum EntityType {
    PADDLE(200, 30),
    BALL(25, 25),
    BRICK(80, 30),
    SURPRISE(30, 30),
    WALL_LEFT(10, 1080),
    WALL_RIGHT(10, 1080),
    WALL_TOP(1920, 10),
    WALL_BOTTOM_SENSOR(1920, 10),
    WALL_SAFETY(1920, 10);

    @Getter
    private final double designWidth;
    @Getter
    private final double designHeight;

    EntityType(double designWidth, double designHeight) {
        this.designWidth = designWidth;
        this.designHeight = designHeight;
    }
}