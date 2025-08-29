package org.overb.arkanoidfx.game.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.function.Consumer;

public class PauseMenuUI extends StackPane {

    private final VBox menuBox = new VBox(16);

    public enum Item { RESUME, QUIT_TO_MAIN, EXIT }

    public PauseMenuUI(Consumer<Item> onAction) {
        setStyle("-fx-background-color: black;");
        sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {
                minWidthProperty().bind(scene.widthProperty());
                minHeightProperty().bind(scene.heightProperty());
                prefWidthProperty().bind(scene.widthProperty());
                prefHeightProperty().bind(scene.heightProperty());
                maxWidthProperty().bind(scene.widthProperty());
                maxHeightProperty().bind(scene.heightProperty());
            }
        });
        setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        menuBox.setAlignment(Pos.CENTER);
        getChildren().add(menuBox);
        Text title = new Text("Paused");
        title.setFill(Color.WHITE);
        title.setFont(Font.font("Verdana", FontWeight.BOLD , 50));
        title.setEffect(new DropShadow(24, Color.DARKGRAY));
        var titleWrapper = new StackPane(title);
        titleWrapper.setPadding(new Insets(10, 10, 30, 10));
        menuBox.getChildren().add(titleWrapper);

        addItem("Resume", () -> onAction.accept(Item.RESUME));
        addItem("Quit to main menu", () -> onAction.accept(Item.QUIT_TO_MAIN));
        addItem("Exit", () -> onAction.accept(Item.EXIT));
        widthProperty().addListener((o, ov, nv) -> requestLayout());
        heightProperty().addListener((o, ov, nv) -> requestLayout());
        if (!menuBox.getChildren().isEmpty()) {
            menuBox.getChildren().get(1).requestFocus();
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double w = getWidth();
        double h = getHeight();
        menuBox.applyCss();
        menuBox.autosize();
        double mw = Math.max(menuBox.prefWidth(-1), menuBox.getWidth());
        double mh = Math.max(menuBox.prefHeight(-1), menuBox.getHeight());
        double x = (w - mw) / 2.0;
        double y = (h - mh) / 2.0;
        if (Double.isNaN(x)) x = 0;
        if (Double.isNaN(y)) y = 0;
        layoutInArea(menuBox, x, y, mw, mh, -1, Pos.CENTER.getHpos(), Pos.CENTER.getVpos());
    }

    private void addItem(String label, Runnable action) {
        Text text = new Text(label);
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Cambria", 42));
        applyHoverEffects(text);
        text.setOnMouseClicked(e -> action.run());
        menuBox.getChildren().add(text);
        text.setFocusTraversable(true);
    }

    private void applyHoverEffects(Text text) {
        Glow glow = new Glow(0.0);
        DropShadow shadow = new DropShadow(20, Color.AQUA);
        shadow.setSpread(0.35);
        text.setEffect(glow);
        text.setOnMouseEntered(e -> {
            glow.setLevel(0.8);
            text.setScaleX(1.08);
            text.setScaleY(1.08);
            text.setEffect(shadow);
        });
        text.setOnMouseExited(e -> {
            glow.setLevel(0.0);
            text.setScaleX(1.0);
            text.setScaleY(1.0);
            text.setEffect(null);
        });
    }
}
