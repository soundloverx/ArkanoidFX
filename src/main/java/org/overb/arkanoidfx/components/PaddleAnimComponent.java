package org.overb.arkanoidfx.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.Texture;
import com.almasb.fxgl.time.TimerAction;
import javafx.geometry.Rectangle2D;
import javafx.util.Duration;

public class PaddleAnimComponent extends Component {

    private final Texture texture;
    private final int frames;
    private final int frameW;
    private final int frameH;
    private final double frameDurationSec;
    private int currentFrame = 0;
    private TimerAction action;

    public PaddleAnimComponent(Texture texture, int frames, int frameW, int frameH, double frameDurationSec) {
        this.texture = texture;
        this.frames = Math.max(1, frames);
        this.frameW = Math.max(1, frameW);
        this.frameH = Math.max(1, frameH);
        this.frameDurationSec = Math.max(0.0, frameDurationSec);
    }

    @Override
    public void onAdded() {
        applyFrame(0);
        if (frames > 1 && frameDurationSec > 0) {
            action = FXGL.getGameTimer().runAtInterval(() -> {
                currentFrame = (currentFrame + 1) % frames;
                applyFrame(currentFrame);
            }, Duration.seconds(frameDurationSec));
        }
    }

    @Override
    public void onRemoved() {
        if (action != null) {
            action.expire();
            action = null;
        }
    }

    private void applyFrame(int frameIndex) {
        double x = (double) frameIndex * frameW;
        texture.setViewport(new Rectangle2D(x, 0, frameW, frameH));
    }
}