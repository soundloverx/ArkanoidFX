package org.overb.arkanoidfx.util;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.texture.Texture;
import javafx.geometry.Rectangle2D;

public final class TextureUtils {

    private TextureUtils() {
    }

    public static Texture loadTextureOrNull(String spritePath, double targetWidth, double targetHeight, int frameW, int frameH) {
        if (spritePath == null || spritePath.isBlank()) {
            return null;
        }
        try {
            Texture tex = FXGL.texture(spritePath);
            tex.setFitWidth(targetWidth);
            tex.setFitHeight(targetHeight);
            tex.setPreserveRatio(false);
            tex.setSmooth(false);
            int fw = Math.max(1, frameW);
            int fh = Math.max(1, frameH);
            tex.setViewport(new Rectangle2D(0, 0, fw, fh));
            tex.setTranslateX(0);
            tex.setTranslateY(0);
            return tex;
        } catch (Exception ignore) {
            return null;
        }
    }

    public static void setViewportFrame(Texture texture, int frameIndex, int frameW, int frameH) {
        if (texture == null) return;
        int fw = Math.max(1, frameW);
        int fh = Math.max(1, frameH);
        double x = (double) Math.max(0, frameIndex) * fw;
        texture.setViewport(new Rectangle2D(x, 0, fw, fh));
    }
}