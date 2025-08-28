package org.overb.arkanoidfx.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.time.TimerAction;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.overb.arkanoidfx.enums.Axis;
import org.overb.arkanoidfx.game.GameSession;
import org.overb.arkanoidfx.game.ResolutionManager;

public final class HUDManager {

    private Text hudScore;
    private Text hudCombo;
    private Text hudLives;
    private Text hudMultiplier;
    private Text hudFps;
    private long fpsFrames = 0;
    private TimerAction fpsTimer;

    public void initHUD() {
        if (hudScore == null) {
            hudScore = new Text();
            hudCombo = new Text();
            hudLives = new Text();
            hudMultiplier = new Text();
            hudFps = new Text("FPS: --");

            hudScore.setFill(Color.WHITE);
            hudCombo.setFill(Color.LIGHTGRAY);
            hudLives.setFill(Color.WHITE);
            hudMultiplier.setFill(Color.LIGHTGREEN);
            hudFps.setFill(Color.YELLOW);
        }

        FXGL.getGameScene().addUINodes(hudScore, hudCombo, hudMultiplier, hudLives, hudFps);
        if (fpsTimer != null) {
            fpsTimer.expire();
            fpsTimer = null;
        }
        fpsFrames = 0;
        fpsTimer = FXGL.getGameTimer().runAtInterval(() -> {
            hudFps.setText("FPS: " + fpsFrames);
            fpsFrames = 0;
            layoutBottomRightHorizontal();
        }, javafx.util.Duration.seconds(1));

        refresh(new GameSession());
    }

    public void refresh(GameSession session) {
        if (hudScore == null) {
            return;
        }
        hudScore.setText("Score: " + session.getScoreRounded());
        hudCombo.setText("Combo: " + session.getCombo());
        hudMultiplier.setText(String.format("Modifier: %.2f%%", session.getLastMultiplier() * 100.0));
        hudLives.setText("Lives: " + session.getLives());
        layoutBottomRightHorizontal();
    }

    public void onFrame() {
        fpsFrames++;
    }

    private void layoutBottomRightHorizontal() {
        double padding = 16.0;
        double right = ResolutionManager.getScaledValue(ResolutionManager.DESIGN_RESOLUTION.getWidth() - padding, Axis.HORIZONTAL);
        double bottom = ResolutionManager.getScaledValue(ResolutionManager.DESIGN_RESOLUTION.getHeight() - padding, Axis.VERTICAL);
        double gap = 16.0;

        hudLives.applyCss();
        hudScore.applyCss();
        hudMultiplier.applyCss();
        hudCombo.applyCss();
        hudFps.applyCss();

        double x = right;

        double widthLives = hudLives.getLayoutBounds().getWidth();
        x -= widthLives;
        hudLives.setX(x);
        hudLives.setY(bottom);
        x -= gap;

        double widthScore = hudScore.getLayoutBounds().getWidth();
        x -= widthScore;
        hudScore.setX(x);
        hudScore.setY(bottom);
        x -= gap;

        double widthMultiplier = hudMultiplier.getLayoutBounds().getWidth();
        x -= widthMultiplier;
        hudMultiplier.setX(x);
        hudMultiplier.setY(bottom);
        x -= gap;

        double widthCombo = hudCombo.getLayoutBounds().getWidth();
        x -= widthCombo;
        hudCombo.setX(x);
        hudCombo.setY(bottom);
        x -= gap;

        double widthFps = hudFps.getLayoutBounds().getWidth();
        x -= widthFps;
        hudFps.setX(x);
        hudFps.setY(bottom);
    }
}
