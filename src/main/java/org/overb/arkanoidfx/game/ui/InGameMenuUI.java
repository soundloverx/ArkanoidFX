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

import java.util.ArrayList;
import java.util.List;

public class InGameMenuUI extends StackPane {

    private final VBox menuBox = new VBox(16);
    private final StackPane menuContainer = new StackPane();

    private InGameMenuUI(String title, List<MenuItem> items) {
        setStyle("-fx-background-color: transparent;");
        setPrefSize(ResolutionManager.DESIGN_RESOLUTION.getWidth(), ResolutionManager.DESIGN_RESOLUTION.getHeight());
        setMaxSize(ResolutionManager.DESIGN_RESOLUTION.getWidth(), ResolutionManager.DESIGN_RESOLUTION.getHeight());
        setMinSize(ResolutionManager.DESIGN_RESOLUTION.getWidth(), ResolutionManager.DESIGN_RESOLUTION.getHeight());
        menuBox.setAlignment(Pos.CENTER);
        menuContainer.getChildren().add(menuBox);
        menuContainer.setStyle("-fx-border-color: rgba(0,0,0,0.1); -fx-border-width: 1; -fx-border-radius: 6; -fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 6;");
        getChildren().add(menuContainer);
        if (title != null && !title.isEmpty()) {
            Text titleNode = new Text(title);
            titleNode.setFill(Color.WHITE);
            titleNode.setFont(Font.font("Verdana", FontWeight.BOLD, 50));
            titleNode.setEffect(new DropShadow(24, Color.DARKGRAY));
            var titleWrapper = new StackPane(titleNode);
            titleWrapper.setPadding(new Insets(10, 10, 30, 10));
            menuBox.getChildren().add(titleWrapper);
        }
        for (MenuItem mi : items) {
            addItem(mi.label, mi.action);
        }
        widthProperty().addListener((o, ov, nv) -> requestLayout());
        heightProperty().addListener((o, ov, nv) -> requestLayout());
        if (!menuBox.getChildren().isEmpty()) {
            int index = (title != null && !title.isEmpty()) ? 1 : 0;
            if (index < menuBox.getChildren().size()) {
                menuBox.getChildren().get(index).requestFocus();
            }
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
        double pad = 16;
        double cw = mw + pad * 2;
        double ch = mh + pad * 2;
        menuContainer.setPadding(new Insets(pad));
        double x = (w - cw) / 2.0;
        double y = (h - ch) / 2.0;
        if (Double.isNaN(x)) x = 0;
        if (Double.isNaN(y)) y = 0;
        layoutInArea(menuContainer, x, y, cw, ch, -1, Pos.CENTER.getHpos(), Pos.CENTER.getVpos());
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

    public static Builder builder() {
        return new Builder();
    }

    private record MenuItem(String label, Runnable action) {}

    public static class Builder {
        private String title = "";
        private final List<MenuItem> items = new ArrayList<>();

        public Builder withTitle(String title) {
            this.title = title == null ? "" : title;
            return this;
        }

        public Builder withMenuItem(String text, Runnable action) {
            if (text == null || text.isBlank()) return this;
            if (action == null) action = () -> {};
            items.add(new MenuItem(text, action));
            return this;
        }

        public InGameMenuUI build() {
            return new InGameMenuUI(title, items);
        }
    }
}
