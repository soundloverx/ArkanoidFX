package org.overb.arkanoidfx.game.ui.base;

import javafx.scene.image.Image;
import javafx.scene.layout.*;

import java.util.Objects;

public class FullscreenMenuUI extends StackPane {

    protected void applyBackground(String resource) {
        try {
            Image img = new Image(Objects.requireNonNull(getClass().getResourceAsStream(resource)));
            BackgroundSize size = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true);
            BackgroundImage bg = new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, size);
            setBackground(new Background(bg));
        } catch (Exception e) {
            setStyle("-fx-background-color: black;");
        }
    }
}
