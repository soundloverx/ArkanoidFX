package org.overb.arkanoidfx.game.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.time.TimerAction;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.robot.Robot;
import javafx.util.Duration;

import java.util.function.Consumer;

public final class MouseUI {

    private static EventHandler<MouseEvent> confineHandlerMoved;
    private static EventHandler<MouseEvent> confineHandlerDragged;
    private static TimerAction mouseConfinePollTask;
    private static Robot mouseRobot;
    private static boolean mouseWarping = false;

    private MouseUI() {}

    public static void setMouseVisible(boolean visible) {
        if (!visible) {
            FXGL.getGameScene().getRoot().setCursor(Cursor.NONE);
        } else {
            FXGL.getGameScene().getRoot().setCursor(Cursor.DEFAULT);
        }
        setMouseConstrained(!visible);
    }

    private static void setMouseConstrained(boolean constrained) {
        var root = FXGL.getGameScene().getRoot();
        var scene = root.getScene();
        Runnable apply = () -> {
            var sc = root.getScene();
            if (sc == null) return;
            if (confineHandlerMoved != null) {
                sc.removeEventFilter(MouseEvent.MOUSE_MOVED, confineHandlerMoved);
                confineHandlerMoved = null;
            }
            if (confineHandlerDragged != null) {
                sc.removeEventFilter(MouseEvent.MOUSE_DRAGGED, confineHandlerDragged);
                confineHandlerDragged = null;
            }
            if (mouseConfinePollTask != null) {
                mouseConfinePollTask.expire();
                mouseConfinePollTask = null;
            }
            sc.setOnMouseExited(null);
            if (!constrained) {
                return;
            }
            if (mouseRobot == null) {
                mouseRobot = new javafx.scene.robot.Robot();
            }
            Consumer<Point2D> confineByScreenPoint = screenPt -> {
                if (mouseWarping) return;
                var screenBounds = root.localToScreen(root.getBoundsInLocal());
                if (screenBounds == null) return;
                double sx = screenPt.getX();
                double sy = screenPt.getY();
                double minX = Math.ceil(screenBounds.getMinX()) + 1;
                double minY = Math.ceil(screenBounds.getMinY()) + 1;
                double maxX = Math.floor(screenBounds.getMaxX()) - 1;
                double maxY = Math.floor(screenBounds.getMaxY()) - 1;
                double clampedX = Math.min(Math.max(sx, minX), maxX);
                double clampedY = Math.min(Math.max(sy, minY), maxY);
                if (clampedX != sx || clampedY != sy) {
                    mouseWarping = true;
                    try {
                        mouseRobot.mouseMove(clampedX, clampedY);
                    } finally {
                        FXGL.runOnce(() -> mouseWarping = false, Duration.millis(0.5));
                    }
                }
            };
            Consumer<MouseEvent> confineOnEvent = evt -> {
                if (mouseWarping) return;
                confineByScreenPoint.accept(new Point2D(evt.getScreenX(), evt.getScreenY()));
                if (mouseWarping) evt.consume();
            };
            confineHandlerMoved = confineOnEvent::accept;
            confineHandlerDragged = confineOnEvent::accept;
            sc.addEventFilter(MouseEvent.MOUSE_MOVED, confineHandlerMoved);
            sc.addEventFilter(MouseEvent.MOUSE_DRAGGED, confineHandlerDragged);
            mouseConfinePollTask = FXGL.getGameTimer().runAtInterval(() -> {
                if (mouseWarping) return;
                var pos = new Point2D(mouseRobot.getMouseX(), mouseRobot.getMouseY());
                confineByScreenPoint.accept(pos);
            }, Duration.millis(16));
        };
        if (scene == null) {
            FXGL.runOnce(apply, Duration.millis(1));
        } else {
            apply.run();
        }
    }
}
