package org.overb.arkanoidfx.game.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.time.TimerAction;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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

            hudScore.setFont(Font.font(hudScore.getFont().getFamily(), FontWeight.BOLD, hudScore.getFont().getSize()));
            hudCombo.setFont(Font.font(hudCombo.getFont().getFamily(), FontWeight.BOLD, hudCombo.getFont().getSize()));
            hudLives.setFont(Font.font(hudLives.getFont().getFamily(), FontWeight.BOLD, hudLives.getFont().getSize()));
            hudMultiplier.setFont(Font.font(hudMultiplier.getFont().getFamily(), FontWeight.BOLD, hudMultiplier.getFont().getSize()));
            hudLevel.setFont(Font.font(hudLevel.getFont().getFamily(), FontWeight.BOLD, hudLevel.getFont().getSize()));
            hudFps.setFont(Font.font(hudFps.getFont().getFamily(), FontWeight.BOLD, hudFps.getFont().getSize()));

            DropShadow ds = new DropShadow();
            ds.setRadius(7);
            ds.setSpread(0.3);
            ds.setOffsetY(1.0);
            ds.setColor(Color.color(0, 0, 0, 1));

            hudScore.setEffect(ds);
            hudCombo.setEffect(ds);
            hudLives.setEffect(ds);
            hudMultiplier.setEffect(ds);
            hudLevel.setEffect(ds);
            hudFps.setEffect(ds);
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
        double bottom = ResolutionManager.DESIGN_RESOLUTION.getHeight() - 20;

        hudLives.applyCss();
        hudScore.applyCss();
        hudMultiplier.applyCss();
        hudCombo.applyCss();
        hudLevel.applyCss();
        hudFps.applyCss();

        double x = right;
        placeStatusTextElement(hudLives, x, bottom);
        x = hudLives.getX() - 16.0;
        placeStatusTextElement(hudScore, x, bottom);
        x = hudScore.getX() - 16.0;
        placeStatusTextElement(hudMultiplier, x, bottom);
        x = hudMultiplier.getX() - 16.0;
        placeStatusTextElement(hudCombo, x, bottom);
        x = hudCombo.getX() - 16.0;
        placeStatusTextElement(hudLevel, x, bottom);
        x = hudLevel.getX() - 16.0;
        placeStatusTextElement(hudFps, x, bottom);
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
