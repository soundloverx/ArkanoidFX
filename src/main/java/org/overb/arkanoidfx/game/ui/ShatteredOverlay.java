package org.overb.arkanoidfx.game.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.*;
import javafx.geometry.Point2D;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import lombok.extern.java.Log;
import org.overb.arkanoidfx.game.ResolutionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Log
public class ShatteredOverlay extends StackPane {

    private final Group fragmentsLayer = new Group();

    private ShatteredOverlay(Image snapshot, List<Poly> polys) {
        setPickOnBounds(true);
        setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.0), null, null)));
        var frags = new ArrayList<Node>();
        for (Poly p : polys) {
            frags.add(createFragment(snapshot, p));
        }
        fragmentsLayer.getChildren().addAll(frags);
        fragmentsLayer.setMouseTransparent(true);
        fragmentsLayer.setCache(true);
        fragmentsLayer.setCacheHint(CacheHint.SPEED);

        Region dim = new Region();
        dim.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.25), null, null)));
        dim.setMouseTransparent(true);
        dim.prefWidthProperty().bind(widthProperty());
        dim.prefHeightProperty().bind(heightProperty());

        getChildren().addAll(fragmentsLayer, dim);
        startGentleMotion(frags);
        setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(250), this);
        ft.setToValue(1);
        ft.play();
    }

    private Node createFragment(Image snapshot, Poly poly) {
        ImageView iv = new ImageView(snapshot);
        Polygon clip = new Polygon();
        for (int i = 0; i < poly.size(); i++) {
            clip.getPoints().addAll(poly.xs[i], poly.ys[i]);
        }
        iv.setClip(clip);

        Polygon border = new Polygon();
        border.getPoints().setAll(clip.getPoints());
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.color(1, 1, 1, 0.15));
        border.setStrokeWidth(1.0);

        Polygon gradShape = new Polygon();
        gradShape.getPoints().setAll(clip.getPoints());

        LinearGradient lg = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(1, 1, 1, 0.08)),
                new Stop(0.35, Color.color(1, 1, 1, 0.02)),
                new Stop(0.65, Color.color(0, 0, 0, 0.03)),
                new Stop(1.0, Color.color(0, 0, 0, 0.10))
        );
        gradShape.setFill(lg);
        gradShape.setMouseTransparent(true);
        gradShape.setBlendMode(BlendMode.SOFT_LIGHT);

        Group g = new Group(iv, gradShape, border);
        DropShadow ds = new DropShadow(6, Color.color(1, 1, 1, 0.25));
        ds.setSpread(0.06);
        g.setEffect(ds);
        g.setCache(true);
        g.setCacheHint(CacheHint.SPEED);
        g.setMouseTransparent(true);
        return g;
    }

    private void startGentleMotion(List<Node> frags) {
        Random rnd = new Random();
        for (Node n : frags) {
            double tx = 2 + rnd.nextDouble() * 6;
            double ty = 2 + rnd.nextDouble() * 6;
            double sx = 0.985 + rnd.nextDouble() * 0.03;
            double sy = 0.985 + rnd.nextDouble() * 0.03;
            double rot = (rnd.nextDouble() * 2 - 1) * 2.0;
            double period = 2.0 + rnd.nextDouble() * 1.5;

            TranslateTransition tt = new TranslateTransition(Duration.seconds(period), n);
            tt.setFromX(-tx);
            tt.setToX(tx);
            tt.setFromY(-ty);
            tt.setToY(ty);
            tt.setAutoReverse(true);
            tt.setCycleCount(Animation.INDEFINITE);

            ScaleTransition st = new ScaleTransition(Duration.seconds(period * 0.9), n);
            st.setFromX(sx);
            st.setToX(1.0 / sx);
            st.setFromY(sy);
            st.setToY(1.0 / sy);
            st.setAutoReverse(true);
            st.setCycleCount(Animation.INDEFINITE);

            RotateTransition rt = new RotateTransition(Duration.seconds(period * 1.3), n);
            rt.setFromAngle(-rot);
            rt.setToAngle(rot);
            rt.setAutoReverse(true);
            rt.setCycleCount(Animation.INDEFINITE);

            Duration delay = Duration.seconds(rnd.nextDouble() * 1.2);
            tt.setDelay(delay);
            st.setDelay(delay);
            rt.setDelay(delay);

            tt.play();
            st.play();
            rt.play();
        }
    }

    public static ShatteredOverlay showBackground() {
        WritableImage snap = takeSnapshot();
        double w = snap.getWidth();
        double h = snap.getHeight();
        log.info(String.format("Snapshot size: %d x %d", (int) w, (int) h));

        ImageView iv = new ImageView(snap);
        iv.setEffect(new GaussianBlur(6));
        double overscan = 1.02;
        iv.setScaleX(overscan);
        iv.setScaleY(overscan);
        iv.setPreserveRatio(false);
        Group g = new Group(iv);
        g.setClip(new Rectangle(w, h));
        var params = new javafx.scene.SnapshotParameters();
        params.setFill(Color.BLACK);
        WritableImage blurred = g.snapshot(params, new WritableImage((int) w, (int) h));
        BackgroundSize size = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true);
        BackgroundImage bg = new BackgroundImage(blurred, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, size);
        FXGL.getGameScene().getRoot().setBackground(new Background(bg));

        Point2D impact = new Point2D(w * 0.5, h * 0.5);
        List<Poly> polys = generateRadialSlices(w, h, impact);
        if (polys.isEmpty()) {
            polys = generateImpactBiasedFragments(w, h, impact);
        }
        ShatteredOverlay overlay = new ShatteredOverlay(snap, polys);
        FXGL.getGameScene().addUINode(overlay);
        overlay.setMinSize(w, h);
        overlay.setPrefSize(w, h);
        overlay.setMaxSize(w, h);
        return overlay;
    }

    public void dismiss() {
        FadeTransition ft = new FadeTransition(Duration.millis(200), this);
        ft.setToValue(0);
        ft.setOnFinished(e -> FXGL.getGameScene().removeUINode(this));
        ft.play();
    }

    private static WritableImage takeSnapshot() {
        var root = FXGL.getGameScene().getRoot();
        var params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return root.snapshot(params, null);
    }

    private record Poly(double[] xs, double[] ys) {
        int size() {
            return xs.length;
        }
    }

    private static List<Poly> generateImpactBiasedFragments(double w, double h, Point2D impact) {
        int baseCols = 8, baseRows = 6;
        double maxJitter = 24;
        Random rnd = new Random();
        List<Poly> out = new ArrayList<>();
        double cx = impact.getX();
        double cy = impact.getY();
        double maxDist = Math.hypot(Math.max(cx, w - cx), Math.max(cy, h - cy));
        for (int r = 0; r < baseRows; r++) {
            for (int c = 0; c < baseCols; c++) {
                double x0 = (c * w) / baseCols;
                double y0 = (r * h) / baseRows;
                double x1 = ((c + 1) * w) / baseCols;
                double y1 = ((r + 1) * h) / baseRows;
                double cellCx = (x0 + x1) * 0.5;
                double cellCy = (y0 + y1) * 0.5;
                double d = Math.hypot(cellCx - cx, cellCy - cy);
                double t = Math.min(1.0, d / (maxDist * 0.9));
                int sub = (t < 0.25) ? 3 : (t < 0.6 ? 2 : 1);
                double jitter = maxJitter * (0.4 + 0.6 * t);
                Point2D[][] pts = new Point2D[sub + 1][sub + 1];
                for (int sr = 0; sr <= sub; sr++) {
                    for (int sc = 0; sc <= sub; sc++) {
                        double x = x0 + (x1 - x0) * (sc / (double) sub);
                        double y = y0 + (y1 - y0) * (sr / (double) sub);
                        boolean border = (sr == 0 || sr == sub || sc == 0 || sc == sub);
                        double jx = border ? 0 : (rnd.nextDouble() * 2 - 1) * jitter;
                        double jy = border ? 0 : (rnd.nextDouble() * 2 - 1) * jitter;
                        pts[sr][sc] = new Point2D(x + jx, y + jy);
                    }
                }
                for (int sr = 0; sr < sub; sr++) {
                    for (int sc = 0; sc < sub; sc++) {
                        Point2D p00 = pts[sr][sc], p10 = pts[sr][sc + 1], p01 = pts[sr + 1][sc], p11 = pts[sr + 1][sc + 1];
                        boolean diag = ((sr + sc) % 2 == 0);
                        if (diag) {
                            out.add(polyOf(p00, p10, p11));
                            out.add(polyOf(p00, p11, p01));
                        } else {
                            out.add(polyOf(p00, p10, p01));
                            out.add(polyOf(p10, p11, p01));
                        }
                    }
                }
            }
        }
        int maxFrags = 200;
        if (out.size() > maxFrags) {
            List<Poly> trimmed = new ArrayList<>(maxFrags);
            for (int i = 0; i < maxFrags; i++) {
                trimmed.add(out.remove(rnd.nextInt(out.size())));
            }
            return trimmed;
        }
        return out;
    }

    private static List<Poly> generateRadialSlices(double w, double h, Point2D center) {
        List<Poly> out = new ArrayList<>();
        Random rnd = new Random();
        double cx = center.getX();
        double cy = center.getY();
        double maxR = Math.hypot(Math.max(cx, w - cx), Math.max(cy, h - cy)) + 20.0;

        double minDeg = 6.0;
        double maxDeg = 28.0;
        int maxSlices = 200;

        double angle = 0.0;
        while (angle < 360.0 && out.size() < maxSlices) {
            double remainingDeg = 360.0 - angle;
            double span = Math.min(remainingDeg, minDeg + rnd.nextDouble() * (maxDeg - minDeg));
            if (remainingDeg < minDeg) {
                span = remainingDeg;
            }
            double a0 = Math.toRadians(angle);
            double a1 = Math.toRadians(angle + span);
            int bands = 1 + rnd.nextInt(4);
            double rPrev = 0.0;
            for (int b = 0; b < bands; b++) {
                double remaining = maxR - rPrev;
                if (remaining <= 4) break;
                double thickness = remaining * (0.12 + rnd.nextDouble() * 0.23);
                double r1 = Math.min(maxR, rPrev + thickness);

                Point2D p0 = new Point2D(cx + rPrev * Math.cos(a0), cy + rPrev * Math.sin(a0));
                Point2D p1 = new Point2D(cx + r1 * Math.cos(a0), cy + r1 * Math.sin(a0));
                Point2D p2 = new Point2D(cx + r1 * Math.cos(a1), cy + r1 * Math.sin(a1));
                Point2D p3 = new Point2D(cx + rPrev * Math.cos(a1), cy + rPrev * Math.sin(a1));

                double j = 4.0;
                p1 = p1.add((rnd.nextDouble() - 0.5) * j, (rnd.nextDouble() - 0.5) * j);
                p2 = p2.add((rnd.nextDouble() - 0.5) * j, (rnd.nextDouble() - 0.5) * j);
                if (!within(p1, w, h) && !within(p2, w, h) && !within(p0, w, h) && !within(p3, w, h)) {
                    rPrev = r1;
                    continue;
                }
                Poly poly = polyOf(p0, p1, p2, p3);
                out.add(poly);
                if (out.size() >= maxSlices) break;
                rPrev = r1;
            }
            angle += span;
        }
        if (out.size() < maxSlices) {
            double outerA0 = 0.0;
            double ringInner = Math.max(0, maxR - 40.0);
            double ang = outerA0;
            while (ang < 360.0 && out.size() < maxSlices) {
                double piece = 20.0 + rnd.nextDouble() * 35.0;
                double a0r = Math.toRadians(ang);
                double a1r = Math.toRadians(Math.min(360.0, ang + piece));
                Point2D q0 = new Point2D(cx + ringInner * Math.cos(a0r), cy + ringInner * Math.sin(a0r));
                Point2D q1 = new Point2D(cx + maxR * Math.cos(a0r), cy + maxR * Math.sin(a0r));
                Point2D q2 = new Point2D(cx + maxR * Math.cos(a1r), cy + maxR * Math.sin(a1r));
                Point2D q3 = new Point2D(cx + ringInner * Math.cos(a1r), cy + ringInner * Math.sin(a1r));
                if (within(q0, w, h) || within(q1, w, h) || within(q2, w, h) || within(q3, w, h)) {
                    out.add(polyOf(q0, q1, q2, q3));
                }
                ang += piece;
            }
        }
        return out;
    }

    private static boolean within(Point2D p, double w, double h) {
        double x = p.getX();
        double y = p.getY();
        return x >= -2 && x <= w + 2 && y >= -2 && y <= h + 2;
    }

    private static Poly polyOf(Point2D... pts) {
        double[] xs = new double[pts.length];
        double[] ys = new double[pts.length];
        for (int i = 0; i < pts.length; i++) {
            xs[i] = pts[i].getX();
            ys[i] = pts[i].getY();
        }
        return new Poly(xs, ys);
    }
}
