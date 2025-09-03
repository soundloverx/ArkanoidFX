package org.overb.arkanoidfx.game.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.time.TimerAction;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.overb.arkanoidfx.game.GameSession;
import org.overb.arkanoidfx.game.ResolutionManager;

public final class HUDManager {

    private Text hudScore;
    private Text hudCombo;
    private Text hudLives;
    private Text hudMultiplier;
    private Text hudLevel;
    private Text hudFps;
    private long fpsFrames = 0;
    private TimerAction fpsTimer;

    public void initHUD() {
        if (hudScore == null) {
            hudScore = new Text();
            hudCombo = new Text();
            hudLives = new Text();
            hudMultiplier = new Text();
            hudLevel = new Text();
            hudFps = new Text("FPS: --");

            hudScore.setFill(Color.WHITE);
            hudCombo.setFill(Color.LIGHTGRAY);
            hudLives.setFill(Color.WHITE);
            hudMultiplier.setFill(Color.LIGHTGREEN);
            hudLevel.setFill(Color.WHITE);
            hudFps.setFill(Color.YELLOW);
        }

        this.show();
        FXGL.getGameScene().addUINodes(hudScore, hudCombo, hudMultiplier, hudLives, hudLevel, hudFps);
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
        hudMultiplier.setText(String.format("Multiplier: %.2fx", session.getLastMultiplier()));
        hudLevel.setText("Level: " + session.getCurrentLevel());
        hudLives.setText("Lives: " + session.getLives());
        layoutBottomRightHorizontal();
    }

    public void onFrame() {
        fpsFrames++;
    }

    private void layoutBottomRightHorizontal() {
        double right = ResolutionManager.DESIGN_RESOLUTION.getWidth() - 16;
        double bottom = ResolutionManager.DESIGN_RESOLUTION.getHeight() - 8;

        hudLives.applyCss();
        hudScore.applyCss();
        hudMultiplier.applyCss();
        hudCombo.applyCss();
        hudLevel.applyCss();
        hudFps.applyCss();

        double x = right;
        x = placeStatusTextElement(hudLives, x, bottom);
        x = placeStatusTextElement(hudScore, x, bottom);
        x = placeStatusTextElement(hudMultiplier, x, bottom);
        x = placeStatusTextElement(hudCombo, x, bottom);
        x = placeStatusTextElement(hudLevel, x, bottom);
        x = placeStatusTextElement(hudFps, x, bottom);
    }

    private double placeStatusTextElement(Text element, double x, double bottom) {
        x -= element.getLayoutBounds().getWidth();
        element.setX(x);
        element.setY(bottom);
        return x - 16.0;
    }

    public void hide() {
        hudLevel.setVisible(false);
        hudScore.setVisible(false);
        hudCombo.setVisible(false);
        hudMultiplier.setVisible(false);
        hudLives.setVisible(false);
        hudFps.setVisible(false);
    }

    public void show() {
        hudLevel.setVisible(true);
        hudScore.setVisible(true);
        hudCombo.setVisible(true);
        hudMultiplier.setVisible(true);
        hudLives.setVisible(true);
        hudFps.setVisible(true);
    }
}
