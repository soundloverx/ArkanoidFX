package org.overb.arkanoidfx.enums;

import lombok.Getter;

public enum Resolution {
    R1280x720(1280, 720),
    R1920x1080(1920, 1080),
    R2560x1440(2560, 1440);

    @Getter
    private final int width;
    @Getter
    private final int height;

    Resolution(int w, int h) {
        this.width = w;
        this.height = h;
    }

    public static Resolution getFromHeight(int h) {
        for (Resolution r : values()) {
            if (r.height == h) {
                return r;
            }
        }
        return R1280x720;
    }
}
