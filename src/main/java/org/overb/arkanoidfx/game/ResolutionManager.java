package org.overb.arkanoidfx.game;

import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.overb.arkanoidfx.enums.Resolution;

public final class ResolutionManager {

    public static final ResolutionManager INSTANCE = new ResolutionManager();
    public static final Resolution DESIGN_RESOLUTION = Resolution.R1920x1080;

    public static ResolutionManager getInstance() {
        return INSTANCE;
    }

    public void applyWindowed(Stage stage, Parent gameRoot, Resolution resolution) {
        int targetW = resolution.getWidth();
        int targetH = resolution.getHeight();
        stage.setFullScreen(false);
        stage.setMaximized(false);
        ensureClientAreaSize(stage, targetW, targetH);
        applyScale(gameRoot);
    }

    private void ensureClientAreaSize(Stage stage, int clientW, int clientH) {
        Scene scene = FXGL.getGameScene().getRoot().getScene();
        if (scene == null) {
            stage.setWidth(clientW);
            stage.setHeight(clientH);
            return;
        }
        if (scene.getWidth() <= 0 || scene.getHeight() <= 0) {
            stage.sizeToScene();
        }

        double decoW = stage.getWidth() - scene.getWidth();
        double decoH = stage.getHeight() - scene.getHeight();

        if (Double.isNaN(decoW) || decoW < 0) {
            decoW = 16;
        }
        if (Double.isNaN(decoH) || decoH < 0){
            decoH = 39;
        }
        stage.setWidth(clientW + decoW);
        stage.setHeight(clientH + decoH);
    }

    public void applyScale(Parent gameRoot) {
        gameRoot.setScaleX(1.0);
        gameRoot.setScaleY(1.0);
        gameRoot.setTranslateX(0.0);
        gameRoot.setTranslateY(0.0);
    }

    public void hookResizeListeners(Stage stage, Parent gameRoot) {
        Scene scene = FXGL.getGameScene().getRoot().getScene();
        if (scene != null) {
            scene.widthProperty().addListener((obs, ov, nv) -> applyScale(gameRoot));
            scene.heightProperty().addListener((obs, ov, nv) -> applyScale(gameRoot));
        } else {
            stage.widthProperty().addListener((obs, ov, nv) -> applyScale(gameRoot));
            stage.heightProperty().addListener((obs, ov, nv) -> applyScale(gameRoot));
        }
    }
}