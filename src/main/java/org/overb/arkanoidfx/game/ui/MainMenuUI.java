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
import org.overb.arkanoidfx.game.ResolutionManager;

import java.util.function.Consumer;

public class MainMenuUI extends StackPane {

    private final VBox menuBox = new VBox(16);

    public enum Item { PLAY, EDITOR, OPTIONS, EXIT }

    public MainMenuUI(Consumer<Item> onAction) {
        setStyle("-fx-background-color: black;");
        setPrefSize(ResolutionManager.DESIGN_RESOLUTION.getWidth(), ResolutionManager.DESIGN_RESOLUTION.getHeight());
        setMaxSize(ResolutionManager.DESIGN_RESOLUTION.getWidth(), ResolutionManager.DESIGN_RESOLUTION.getHeight());
        setMinSize(ResolutionManager.DESIGN_RESOLUTION.getWidth(), ResolutionManager.DESIGN_RESOLUTION.getHeight());
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
        if (!menuBox.getChildren().isEmpty()) {
            menuBox.getChildren().getFirst().requestFocus();
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double w = ResolutionManager.DESIGN_RESOLUTION.getWidth();
        double h = ResolutionManager.DESIGN_RESOLUTION.getHeight();
        menuBox.applyCss();
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
        text.setFocusTraversable(true);
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
