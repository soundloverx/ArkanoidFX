package org.overb.arkanoidfx.game.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import lombok.extern.java.Log;
import org.overb.arkanoidfx.audio.SfxBus;

@Log
public final class NotificationUI {

    public static void show(String message) {
        try {
            StackPane container = new StackPane();
            container.setPickOnBounds(false);

            StackPane box = new StackPane();
            box.setPadding(new Insets(12, 18, 12, 18));
            box.setBackground(new Background(new BackgroundFill(Color.color(0.08, 0.08, 0.1, 0.92), new CornerRadii(10), Insets.EMPTY)));
            box.setBorder(new Border(new BorderStroke(Color.color(0.5, 0.8, 1.0, 0.9), BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1.2))));

            Text text = new Text(message);
            text.setFill(Color.WHITE);
            text.setTextAlignment(TextAlignment.CENTER);
            text.setFont(Font.font("Verdana", FontWeight.BOLD, 20));

            box.getChildren().add(text);
            StackPane.setAlignment(text, Pos.CENTER);

            container.getChildren().add(box);
            StackPane.setAlignment(box, Pos.TOP_CENTER);
            box.setTranslateY(40);
            box.setOpacity(0);

            var root = FXGL.getGameScene().getRoot();
            container.setMouseTransparent(true);
            box.setMouseTransparent(true);
            root.getChildren().add(container);

            SfxBus.getInstance().play("lightning.wav");
            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), box);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            PauseTransition stay = new PauseTransition(Duration.seconds(2));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(220), box);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            SequentialTransition seq = new SequentialTransition(fadeIn, stay, fadeOut);
            seq.setOnFinished(e -> {
                try {
                    root.getChildren().remove(container);
                } catch (Exception ignored) {}
            });
            seq.play();
        } catch (Exception e) {
            log.warning("Unable to show notification: " + message + " | cause=" + e.getMessage());
        }
    }
}
