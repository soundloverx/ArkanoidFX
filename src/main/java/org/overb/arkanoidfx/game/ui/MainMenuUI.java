package org.overb.arkanoidfx.game.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.function.Consumer;

public class MainMenuUI extends StackPane {

    private final VBox menuBox = new VBox(16);

    public enum Item { PLAY, EDITOR, OPTIONS, EXIT }

    public MainMenuUI(Consumer<Item> onAction) {
        setStyle("-fx-background-color: black;");
        // Fill the entire visible window: bind to THIS node's scene size when available
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
        // If scene already set, apply now
        if (getScene() != null) {
            minWidthProperty().bind(getScene().widthProperty());
            minHeightProperty().bind(getScene().heightProperty());
            prefWidthProperty().bind(getScene().widthProperty());
            prefHeightProperty().bind(getScene().heightProperty());
            maxWidthProperty().bind(getScene().widthProperty());
            maxHeightProperty().bind(getScene().heightProperty());
        }
        setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        menuBox.setAlignment(Pos.CENTER);
        getChildren().add(menuBox);

        Text title = new Text("ArkanoidFX");
        title.setFill(Color.WHITE);
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 60));
        var titleWrapper = new StackPane(title);
        titleWrapper.setPadding(new Insets(20, 20, 40, 20));
        title.setEffect(new DropShadow(35, Color.DARKVIOLET));
        menuBox.getChildren().add(titleWrapper);

        addItem("Play", () -> onAction.accept(Item.PLAY));
        addItem("Level Editor", () -> onAction.accept(Item.EDITOR));
        addItem("Options", () -> onAction.accept(Item.OPTIONS));
        addItem("Exit", () -> onAction.accept(Item.EXIT));

        widthProperty().addListener((o, ov, nv) -> requestLayout());
        heightProperty().addListener((o, ov, nv) -> requestLayout());
        // focus first item for keyboard navigation
        if (!menuBox.getChildren().isEmpty()) {
            menuBox.getChildren().getFirst().requestFocus();
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        // Explicitly center menuBox using this pane's current size and menuBox preferred size
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
        text.setFont(Font.font("Cambria", 48));
        applyHoverEffects(text);

        text.setOnMouseClicked(e -> action.run());

        menuBox.getChildren().add(text);

        // Keyboard navigation support
        text.setFocusTraversable(true);
        text.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> action.run();
                case UP, W -> focusPrev(text);
                case DOWN, S -> focusNext(text);
            }
        });
    }

    private void focusPrev(Node node) {
        int idx = menuBox.getChildren().indexOf(node);
        int prev = (idx - 1 + menuBox.getChildren().size()) % menuBox.getChildren().size();
        menuBox.getChildren().get(prev).requestFocus();
    }

    private void focusNext(Node node) {
        int idx = menuBox.getChildren().indexOf(node);
        int next = (idx + 1) % menuBox.getChildren().size();
        menuBox.getChildren().get(next).requestFocus();
    }

    private void applyHoverEffects(Text text) {
        Glow glow = new Glow(0.0);
        DropShadow shadow = new DropShadow(24, Color.AQUA);
        shadow.setSpread(0.4);
        text.setEffect(glow);

        text.setOnMouseEntered(e -> {
            glow.setLevel(0.8);
            text.setScaleX(1.1);
            text.setScaleY(1.1);
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
