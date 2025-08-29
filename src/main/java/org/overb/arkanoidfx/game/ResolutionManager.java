package org.overb.arkanoidfx.game;

import com.almasb.fxgl.dsl.FXGL;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import org.overb.arkanoidfx.enums.Axis;
import org.overb.arkanoidfx.enums.EntityType;
import org.overb.arkanoidfx.enums.Resolution;

public final class ResolutionManager {

    public static final ResolutionManager INSTANCE = new ResolutionManager();
    public static final Resolution DESIGN_RESOLUTION = Resolution.R1920x1080;
    @Getter
    @Setter
    private Resolution currentResolution = Resolution.R1280x720;

    public static ResolutionManager getInstance() {
        return INSTANCE;
    }

    public void applyWindowed(Stage stage, Parent gameRoot, Resolution resolution) {
        int targetW = resolution.getWidth();
        int targetH = resolution.getHeight();

        if (resolution == Resolution.NATIVE) {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            targetW = (int) bounds.getWidth();
            targetH = (int) bounds.getHeight();
        }
        currentResolution = resolution;
        stage.setFullScreen(false);
        stage.setMaximized(false);
        stage.setWidth(targetW);
        stage.setHeight(targetH);
        applyScale(stage, gameRoot);
    }

    public void applyScale(Stage stage, Parent gameRoot) {
        Scene scene = FXGL.getGameScene().getRoot().getScene();
        double contentW;
        double contentH;
        if (scene != null) {
            contentW = scene.getWidth();
            contentH = scene.getHeight();
        } else {
            contentW = DESIGN_RESOLUTION.getWidth();
            contentH = DESIGN_RESOLUTION.getHeight();
        }
        double scale = Math.min(contentW / DESIGN_RESOLUTION.getWidth(), contentH / DESIGN_RESOLUTION.getHeight());
        if (Double.isNaN(scale) || scale <= 0) {
            scale = 1.0;
        }
        gameRoot.setScaleX(scale);
        gameRoot.setScaleY(scale);
        double scaledW = DESIGN_RESOLUTION.getWidth() * scale;
        double scaledH = DESIGN_RESOLUTION.getHeight() * scale;
        double offsetX = (contentW - scaledW) / 2.0;
        double offsetY = (contentH - scaledH) / 2.0;
        if (Math.abs(offsetX) < 0.75) offsetX = 0.0;
        if (Math.abs(offsetY) < 0.75) offsetY = 0.0;
        gameRoot.setTranslateX(offsetX);
        gameRoot.setTranslateY(offsetY);
    }

    public void hookResizeListeners(Stage stage, Parent gameRoot) {
        Scene scene = FXGL.getGameScene().getRoot().getScene();
        if (scene != null) {
            scene.widthProperty().addListener((obs, ov, nv) -> applyScale(stage, gameRoot));
            scene.heightProperty().addListener((obs, ov, nv) -> applyScale(stage, gameRoot));
        } else {
            stage.widthProperty().addListener((obs, ov, nv) -> applyScale(stage, gameRoot));
            stage.heightProperty().addListener((obs, ov, nv) -> applyScale(stage, gameRoot));
        }
    }

    public static Point2D getScaledEntity(EntityType entityType) {
        return new Point2D(entityType.getDesignWidth() * ((double) ResolutionManager.getInstance().currentResolution.getWidth() / DESIGN_RESOLUTION.getWidth()),
                entityType.getDesignHeight() * ((double) ResolutionManager.getInstance().currentResolution.getHeight() / DESIGN_RESOLUTION.getHeight()));
    }

    public static double getScaledValue(double value, Axis axis) {
        if (axis == Axis.HORIZONTAL) {
            return value * ((double) ResolutionManager.getInstance().currentResolution.getWidth() / DESIGN_RESOLUTION.getWidth());
        }
        return value * ((double) ResolutionManager.getInstance().currentResolution.getHeight() / DESIGN_RESOLUTION.getHeight());
    }
}