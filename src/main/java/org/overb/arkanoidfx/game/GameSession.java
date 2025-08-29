package org.overb.arkanoidfx.game;

import lombok.Getter;
import lombok.Setter;

@Getter
public final class GameSession {

    private double score = 0;
    private int combo = 0;
    private int lives = 3;
    private int destructibleBricksLeft = 0;
    private int lastGain = 0;
    @Setter
    private int currentLevel = 0;
    private double lastMultiplier = 1.0;

    private static double comboSpeedMultiplier(int comboCount, double sNorm) {
        double comboFactor = 0.15 * Math.sqrt(Math.max(0, comboCount));
        double speedFactor = 0.75 * Math.max(0.0, sNorm - 1.0);
        double total = 1.0 + comboFactor + speedFactor;
        return Math.max(1.0, Math.min(5.0, total));
    }

    public void onBrickDestroyed(int basePoints, double maxBallSpeed, double baseBallSpeed) {
        combo++;
        destructibleBricksLeft = Math.max(0, destructibleBricksLeft - 1);
        double sNorm = (baseBallSpeed > 1e-6) ? (maxBallSpeed / baseBallSpeed) : 1.0;
        lastMultiplier = comboSpeedMultiplier(combo, sNorm);
        double gain = basePoints * lastMultiplier;
        lastGain = (int) Math.round(gain);
        score += gain;
    }

    public void resetCombo() {
        combo = 0;
        lastMultiplier = 1.0;
        lastGain = 0;
    }

    public void loseLife() {
        lives = Math.max(0, lives - 1);
        resetCombo();
    }

    public void registerDestructibleBrick() {
        destructibleBricksLeft++;
    }

    public void resetForNewGame() {
        score = 0;
        lives = 3;
        resetLevel();
    }

    public void resetLevel() {
        resetCombo();
        destructibleBricksLeft = 0;
    }

    public boolean areSurprisesEnabled() {
        return destructibleBricksLeft >= 3;
    }

    public int getScoreRounded() {
        return (int) Math.round(score);
    }

    public void addLife() {
        lives++;
    }
}
