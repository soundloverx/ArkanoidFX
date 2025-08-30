package org.overb.arkanoidfx;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.time.TimerAction;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.java.Log;
import org.overb.arkanoidfx.audio.AudioMixer;
import org.overb.arkanoidfx.audio.MusicService;
import org.overb.arkanoidfx.audio.SfxBus;
import org.overb.arkanoidfx.components.BallComponent;
import org.overb.arkanoidfx.components.DebugHitboxViewComponent;
import org.overb.arkanoidfx.components.SurpriseComponent;
import org.overb.arkanoidfx.entities.EntityRepository;
import org.overb.arkanoidfx.enums.EntityType;
import org.overb.arkanoidfx.enums.EventType;
import org.overb.arkanoidfx.enums.Resolution;
import org.overb.arkanoidfx.game.GameSession;
import org.overb.arkanoidfx.game.LevelManager;
import org.overb.arkanoidfx.game.ResolutionManager;
import org.overb.arkanoidfx.game.SurpriseService;
import org.overb.arkanoidfx.game.core.EventBus;
import org.overb.arkanoidfx.game.core.GameEvent;
import org.overb.arkanoidfx.game.core.PhysicsManager;
import org.overb.arkanoidfx.game.loaders.DefinitionsLoader;
import org.overb.arkanoidfx.game.loaders.LevelLoader;
import org.overb.arkanoidfx.game.ui.HUDManager;
import org.overb.arkanoidfx.game.ui.MainMenuUI;
import org.overb.arkanoidfx.game.ui.OptionsMenuUI;
import org.overb.arkanoidfx.game.ui.InGameMenuUI;
import org.overb.arkanoidfx.game.world.BallFactory;
import org.overb.arkanoidfx.game.world.PaddleFactory;
import org.overb.arkanoidfx.game.world.WallsFactory;

import java.util.List;
import java.util.function.Consumer;

@Log
public class ArkanoidApp extends GameApplication {
    private static volatile boolean endStateMenuVisible = false;
    public static void setEndStateMenuVisible(boolean value) { endStateMenuVisible = value; }
    public static boolean isEndStateMenuVisible() { return endStateMenuVisible; }

    private MainMenuUI mainMenu;
    private OptionsMenuUI optionsMenu;
    private InGameMenuUI pauseMenu;
    private boolean paused = false;

    @Getter
    private EntityRepository entityRepository;
    private List<String> levelOrder;
    private final GameSession session = new GameSession();
    private boolean hitboxDebugShown = false;
    private HUDManager hudManager;
    private BallFactory ballFactory;
    private WallsFactory wallsFactory;
    private PaddleFactory paddleFactory;
    private LevelManager levelManager;
    private PhysicsManager physicsManager;
    private SurpriseService surpriseService;

    private Robot mouseRobot;
    private boolean mouseWarping;
    private ImageCursor transparentCursor;
    private boolean wantsCursorVisible = true;
    private EventHandler<MouseEvent> confineHandlerMoved;
    private EventHandler<MouseEvent> confineHandlerDragged;
    private TimerAction mouseConfinePollTask;


    @Override
    protected void initSettings(GameSettings settings) {
        Resolution defaultResolution = Resolution.R1920x1080;
        settings.setTitle("Arkanoid FX");
        settings.setVersion("0.3");
        settings.setWidth(defaultResolution.getWidth());
        settings.setHeight(defaultResolution.getHeight());
        settings.setFullScreenAllowed(true);
        settings.setFullScreenFromStart(false);
        settings.setProfilingEnabled(false);
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);
    }

    @Override
    protected void initGame() {
        try {
            entityRepository = new DefinitionsLoader().loadAll();
            log.info("Loaded defs: bricks=" + entityRepository.getBricks().size() +
                    ", surprises=" + entityRepository.getSurprises().size());
        } catch (Exception e) {
            log.severe("Failed to load definitions" + e.getMessage());
            throw new RuntimeException(e);
        }
        session.resetForNewGame();
        try {
            LevelLoader levelLoader = new LevelLoader();
            levelOrder = levelLoader.loadLevelOrder();
            if (levelOrder.isEmpty()) {
                throw new IllegalStateException("levels.txt contains no levels");
            }
            log.info("Level order: " + levelOrder);
            hudManager = new HUDManager();
            ballFactory = new BallFactory(entityRepository);
            wallsFactory = new WallsFactory();
            paddleFactory = new PaddleFactory(entityRepository);
            levelManager = new LevelManager(entityRepository, session, hudManager, wallsFactory, paddleFactory, ballFactory, levelLoader);
            levelManager.setLevelOrder(levelOrder);
            levelManager.setMenuReturnHandler(() -> {
                showMainMenu();
                MusicService.getInstance().play("main_menu.mp3");
            });
            surpriseService = new SurpriseService(ballFactory, wallsFactory);
            physicsManager = new PhysicsManager(session, hudManager, v -> {
                levelManager.spawnPaddleAndBall();
            }, this::applySurprise);
            physicsManager.init();
        } catch (Exception e) {
            log.severe("Failed to load levels.txt" + e.getMessage());
            throw new RuntimeException(e);
        }
        EventBus.subscribe(EventType.HUD_UPDATE, e -> hudManager.refresh(session));
        EventBus.subscribe(EventType.LAST_DESTRUCTIBLE_DESTROYED, e -> {
            setMouseVisible(true);
            setMouseConstrained(false);
            levelManager.onLevelCleared(() -> {
                setMouseVisible(false);
                setMouseConstrained(true);
            });
        });
        EventBus.subscribe(EventType.GAME_OVER, e -> {
            setMouseVisible(true);
            setMouseConstrained(false);
            levelManager.onGameOver(() -> {
            });
        });
        
        FXGL.getGameScene().setBackgroundColor(Color.BLACK);
        showMainMenu();
        MusicService.getInstance().play("main_menu.mp3");
        FXGL.getGameTimer().runOnceAfter(() -> {
            Stage stage = FXGL.getPrimaryStage();
            ConfigOptions cfg = ConfigIO.loadOrDefault();
            AudioMixer.getInstance().setMasterVolume(cfg.audio.master);
            AudioMixer.getInstance().setMusicVolume(cfg.audio.music);
            AudioMixer.getInstance().setSfxVolume(cfg.audio.sfx);
            Resolution res = Resolution.getFromHeight(cfg.height);
            if ("FULLSCREEN".equalsIgnoreCase(cfg.fullscreenMode)) {
                stage.setFullScreen(true);
            } else {
                ResolutionManager.getInstance().applyWindowed(stage, FXGL.getGameScene().getRoot(), res);
            }
            ResolutionManager.getInstance().hookResizeListeners(stage, FXGL.getGameScene().getRoot());
            FXGL.runOnce(() -> {
                if (optionsMenu != null && optionsMenu.isVisible()) optionsMenu.requestLayout();
                if (mainMenu != null && mainMenu.isVisible()) mainMenu.requestLayout();
            }, javafx.util.Duration.millis(50));
        }, javafx.util.Duration.millis(1));
    }

    private void showMainMenu() {
        setMouseVisible(true);
        setMouseConstrained(false);
        if (mainMenu == null) {
            mainMenu = new MainMenuUI(item -> {
                switch (item) {
                    case PLAY -> startGameFromMenu();
                    case EDITOR -> {
                        // TODO later
                    }
                    case OPTIONS -> showOptionsMenu();
                    case EXIT -> FXGL.getGameController().exit();
                }
            });
        }
        FXGL.getGameScene().clearUINodes();
        FXGL.getGameScene().addUINodes(mainMenu);
    }

    private void showOptionsMenu() {
        setMouseVisible(true);
        setMouseConstrained(false);
        ConfigOptions cfg = ConfigIO.loadOrDefault();
        if (optionsMenu == null) {
            optionsMenu = new OptionsMenuUI(cfg, (orig, updated) -> {
                applyAndSaveConfig(updated);
                showMainMenu();
            }, v -> {
                showMainMenu();
            });
        } else {
            optionsMenu = new OptionsMenuUI(cfg, (orig, updated) -> {
                applyAndSaveConfig(updated);
                showMainMenu();
            }, v -> showMainMenu());
        }
        FXGL.getGameScene().clearUINodes();
        FXGL.getGameScene().addUINodes(optionsMenu);
    }

    private void applyAndSaveConfig(ConfigOptions updated) {
        AudioMixer.getInstance().setMasterVolume(updated.audio.master);
        AudioMixer.getInstance().setMusicVolume(updated.audio.music);
        AudioMixer.getInstance().setSfxVolume(updated.audio.sfx);
        Stage stage = FXGL.getPrimaryStage();
        if ("FULLSCREEN".equalsIgnoreCase(updated.fullscreenMode)) {
            stage.setFullScreen(true);
        } else {
            Resolution res = Resolution.getFromHeight(updated.height);
            ResolutionManager.getInstance().applyWindowed(stage, FXGL.getGameScene().getRoot(), res);
        }
        FXGL.runOnce(() -> {
            if (optionsMenu != null && optionsMenu.isVisible()) optionsMenu.requestLayout();
            if (mainMenu != null && mainMenu.isVisible()) mainMenu.requestLayout();
        }, javafx.util.Duration.millis(50));
        ConfigIO.save(updated);
    }

    private void startGameFromMenu() {
        MusicService.getInstance().stopCurrentMusic();
        FXGL.getGameScene().clearUINodes();
        setMouseVisible(false);
        setMouseConstrained(true);
        levelManager.startInitialLevel();
    }

    @Override
    protected void onUpdate(double tpf) {
        if (hudManager != null) {
            hudManager.onFrame();
        }
    }

    @Override
    protected void initInput() {
        // pause
        FXGL.onKeyDown(KeyCode.ESCAPE, this::onPauseToggle);
        // release ball on click
        FXGL.onBtnDown(MouseButton.PRIMARY, () -> {
            var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
            for (var entity : balls) {
                var bc = entity.getComponentOptional(BallComponent.class).orElse(null);
                if (bc != null && !bc.isLaunched()) {
                    bc.launch();
                    break;
                }
            }
        });
        // show debug hitboxes
        FXGL.onKeyDown(KeyCode.F9, () -> {
            hitboxDebugShown = !hitboxDebugShown;
            if (hitboxDebugShown) {
                FXGL.getGameWorld().getEntities().forEach(e -> {
                    if (e.getComponentOptional(DebugHitboxViewComponent.class).isEmpty()) {
                        e.addComponent(new DebugHitboxViewComponent());
                    }
                });
            } else {
                FXGL.getGameWorld().getEntities().forEach(e ->
                        e.getComponentOptional(DebugHitboxViewComponent.class).ifPresent(DebugHitboxViewComponent::onRemoved)
                );
            }
        });
    }

    private void applySurprise(Entity surprise) {
        var sc = surprise.getComponentOptional(SurpriseComponent.class).orElse(null);
        String effect = sc != null ? sc.getEffect() : null;
        String sound = sc != null ? sc.getSound() : null;
        SfxBus.getInstance().play(sound);
        if ("multiball".equalsIgnoreCase(effect)) {
            var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
            if (!balls.isEmpty()) {
                surpriseService.applyMultiball(balls.getFirst());
            }
        } else if ("safety_wall".equalsIgnoreCase(effect)) {
            surpriseService.applySafetyWall(10.0);
        } else if ("bonus_life".equalsIgnoreCase(effect)) {
            session.addLife();
            EventBus.publish(GameEvent.of(EventType.HUD_UPDATE));
        }
    }

    private void onPauseToggle() {
            if (ArkanoidApp.isEndStateMenuVisible()) {
                return;
            }
        if (isAnyMenuVisible()) {
            return;
        }
        if (!paused) {
            paused = true;
            FXGL.getGameController().pauseEngine();
            showPauseMenu();
        } else {
            resumeFromPause();
        }
    }

    private boolean isAnyMenuVisible() {
        if (mainMenu != null && mainMenu.getParent() != null && mainMenu.isVisible()) {
            return true;
        }
        return optionsMenu != null && optionsMenu.getParent() != null && optionsMenu.isVisible();
    }

    private void showPauseMenu() {
        setMouseVisible(true);
        setMouseConstrained(false);
        if (pauseMenu == null) {
            pauseMenu = InGameMenuUI.builder()
                    .withTitle("Paused")
                    .withMenuItem("Resume", this::resumeFromPause)
                    .withMenuItem("Quit to main menu", this::quitToMainFromPause)
                    .withMenuItem("Exit", () -> FXGL.getGameController().exit())
                    .build();
        }
        FXGL.getGameScene().addUINodes(pauseMenu);
    }

    private void resumeFromPause() {
        if (!paused) {
            return;
        }
        paused = false;
        FXGL.getGameController().resumeEngine();
        if (pauseMenu != null) {
            FXGL.getGameScene().removeUINode(pauseMenu);
        }
        setMouseVisible(false);
        setMouseConstrained(true);
    }

    private void quitToMainFromPause() {
        paused = false;
        FXGL.getGameController().resumeEngine();
        if (pauseMenu != null) {
            FXGL.getGameScene().removeUINode(pauseMenu);
        }
        setMouseVisible(true);
        setMouseConstrained(false);
        levelManager.quitToMainMenuNoDialog();
    }

    private void setMouseVisible(boolean visible) {
        if (!visible) {
            FXGL.getGameScene().getRoot().setCursor(Cursor.NONE);
        } else {
            FXGL.getGameScene().getRoot().setCursor(Cursor.DEFAULT);
        }
    }

    private void setMouseConstrained(boolean constrained) {
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
            sc.addEventFilter(MouseEvent.MOUSE_MOVED, confineOnEvent::accept);
            sc.addEventFilter(MouseEvent.MOUSE_DRAGGED, confineOnEvent::accept);
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

    public static void main(String[] args) {
        launch(args);
    }
}