package org.overb.arkanoidfx.game.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.overb.arkanoidfx.ConfigOptions;
import org.overb.arkanoidfx.audio.AudioMixer;
import org.overb.arkanoidfx.game.ResolutionManager;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class OptionsMenuUI extends StackPane {

    private final GridPane grid = new GridPane();
    private final ComboBox<String> resolutionBox = new ComboBox<>();
    private final ToggleGroup windowModeGroup = new ToggleGroup();
    private final RadioButton rbWindowed = new RadioButton("Windowed");
    private final RadioButton rbFullscreen = new RadioButton("Fullscreen");
    private final Slider masterSlider = mkSlider();
    private final Slider musicSlider = mkSlider();
    private final Slider sfxSlider = mkSlider();
    private ConfigOptions original;

    public OptionsMenuUI(ConfigOptions cfg, BiConsumer<ConfigOptions, ConfigOptions> onApply, Consumer<Void> onCancel) {
        applyBackground("/assets/textures/bg/black-red-square-rectangle-rq-2560x1440.jpg");
        setPrefSize(ResolutionManager.DESIGN_RESOLUTION.getWidth(), ResolutionManager.DESIGN_RESOLUTION.getHeight());
        setMaxSize(ResolutionManager.DESIGN_RESOLUTION.getWidth(), ResolutionManager.DESIGN_RESOLUTION.getHeight());
        setMinSize(ResolutionManager.DESIGN_RESOLUTION.getWidth(), ResolutionManager.DESIGN_RESOLUTION.getHeight());
        this.original = copyOf(cfg);

        grid.setAlignment(Pos.CENTER);
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(32, 5, 5, 5));

        int row = 0;
        Text title = new Text("Options");
        title.setFill(Color.WHITE);
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 50));
        title.setEffect(new DropShadow(24, Color.LIGHTGRAY));
        grid.add(title, 0, row++, 2, 1);

        // Resolution
        grid.add(label("Resolution"), 0, row);
        List<String> resItems = Arrays.asList("1280x720", "1920x1080", "2560x1440");
        resolutionBox.setItems(FXCollections.observableArrayList(resItems));
        String currentRes = cfg.width + "x" + cfg.height;
        if (!resItems.contains(currentRes)) currentRes = "1920x1080";
        resolutionBox.getSelectionModel().select(currentRes);
        grid.add(resolutionBox, 1, row++);

        // Window mode
        grid.add(label("Window mode"), 0, row);
        rbWindowed.setToggleGroup(windowModeGroup);
        rbFullscreen.setToggleGroup(windowModeGroup);
        rbWindowed.setTextFill(Color.WHITE);
        rbFullscreen.setTextFill(Color.WHITE);
        rbWindowed.setSelected(!"FULLSCREEN".equalsIgnoreCase(cfg.fullscreenMode));
        rbFullscreen.setSelected("FULLSCREEN".equalsIgnoreCase(cfg.fullscreenMode));
        grid.add(mkHBox(12.0, rbWindowed, rbFullscreen), 1, row++);

        // Master
        grid.add(label("Master volume"), 0, row);
        masterSlider.setValue(clamp01(cfg.audio.master) * 100);
        masterSlider.setPadding(new Insets(16, 0, 0, 0));
        grid.add(masterSlider, 1, row++);

        // Music
        grid.add(label("Music volume"), 0, row);
        musicSlider.setValue(clamp01(cfg.audio.music) * 100);
        musicSlider.setPadding(new Insets(16, 0, 0, 0));
        grid.add(musicSlider, 1, row++);

        // SFX
        grid.add(label("SFX volume"), 0, row);
        sfxSlider.setValue(clamp01(cfg.audio.sfx) * 100);
        sfxSlider.setPadding(new Insets(16, 0, 0, 0));
        grid.add(sfxSlider, 1, row++);

        // Buttons
        Button btnApply = new Button("Apply");
        Button btnCancel = new Button("Cancel");
        btnApply.setOnAction(e -> {
            ConfigOptions updated = computeUpdated(cfg);
            onApply.accept(original, updated);
            original = copyOf(updated);
        });
        btnCancel.setOnAction(e -> {
            applyVolumes(original);
            resolutionBox.getSelectionModel().select(original.width + "x" + original.height);
            rbWindowed.setSelected(!"FULLSCREEN".equalsIgnoreCase(original.fullscreenMode));
            rbFullscreen.setSelected("FULLSCREEN".equalsIgnoreCase(original.fullscreenMode));
            onCancel.accept(null);
        });
        grid.add(mkHBox(16.0, btnApply, btnCancel), 0, row, 2, 1);

        getChildren().add(grid);
        StackPane.setAlignment(grid, Pos.CENTER);

        masterSlider.valueProperty().addListener((o, ov, nv) -> applyVolumes(computePreview()));
        musicSlider.valueProperty().addListener((o, ov, nv) -> applyVolumes(computePreview()));
        sfxSlider.valueProperty().addListener((o, ov, nv) -> applyVolumes(computePreview()));
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double w = ResolutionManager.DESIGN_RESOLUTION.getWidth();
        double h = ResolutionManager.DESIGN_RESOLUTION.getHeight();
        grid.applyCss();
        double mw = Math.max(grid.prefWidth(-1), grid.getWidth());
        double mh = Math.max(grid.prefHeight(-1), grid.getHeight());
        double x = (w - mw) / 2.0;
        double y = (h - mh) / 2.0;
        if (Double.isNaN(x)) x = 0;
        if (Double.isNaN(y)) y = 0;
        layoutInArea(grid, x, y, mw, mh, -1, Pos.CENTER.getHpos(), Pos.CENTER.getVpos());
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.WHITE);
        l.setFont(Font.font("Cambria", 22));
        return l;
    }

    private javafx.scene.layout.HBox mkHBox(double spacing, Node... nodes) {
        javafx.scene.layout.HBox hb = new javafx.scene.layout.HBox(spacing, nodes);
        hb.setAlignment(Pos.CENTER_LEFT);
        return hb;
    }

    private static Slider mkSlider() {
        Slider s = new Slider(0, 100, 100);
        s.setShowTickLabels(true);
        s.setShowTickMarks(true);
        s.setMajorTickUnit(25);
        s.setMinorTickCount(4);
        s.setBlockIncrement(1);
        s.setPrefWidth(280);
        return s;
    }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    private ConfigOptions computeUpdated(ConfigOptions base) {
        ConfigOptions updated = copyOf(base);
        String sel = resolutionBox.getSelectionModel().getSelectedItem();
        if (sel != null) {
            String[] parts = sel.split("x");
            updated.width = Integer.parseInt(parts[0]);
            updated.height = Integer.parseInt(parts[1]);
        }
        updated.fullscreenMode = rbFullscreen.isSelected() ? "FULLSCREEN" : "WINDOWED";
        ConfigOptions.AudioCfg a = new ConfigOptions.AudioCfg();
        a.master = clamp01(masterSlider.getValue() / 100.0);
        a.music = clamp01(musicSlider.getValue() / 100.0);
        a.sfx = clamp01(sfxSlider.getValue() / 100.0);
        updated.audio = a;
        return updated;
    }

    private ConfigOptions computePreview() {
        ConfigOptions tmp = copyOf(original);
        tmp.audio.master = clamp01(masterSlider.getValue() / 100.0);
        tmp.audio.music = clamp01(musicSlider.getValue() / 100.0);
        tmp.audio.sfx = clamp01(sfxSlider.getValue() / 100.0);
        return tmp;
    }

    private static ConfigOptions copyOf(ConfigOptions c) {
        ConfigOptions d = new ConfigOptions();
        d.width = c.width;
        d.height = c.height;
        d.fullscreenMode = c.fullscreenMode;
        d.nativeW = c.nativeW;
        d.nativeH = c.nativeH;
        d.audio.master = c.audio.master;
        d.audio.music = c.audio.music;
        d.audio.sfx = c.audio.sfx;
        return d;
    }

    public void applyVolumes(ConfigOptions cfg) {
        AudioMixer.getInstance().setMasterVolume(cfg.audio.master);
        AudioMixer.getInstance().setMusicVolume(cfg.audio.music);
        AudioMixer.getInstance().setSfxVolume(cfg.audio.sfx);
    }

    private void applyBackground(String resource) {
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
