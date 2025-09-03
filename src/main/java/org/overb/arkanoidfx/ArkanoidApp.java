package org.overb.arkanoidfx;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.physics.CollisionHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
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
import org.overb.arkanoidfx.game.loaders.DefinitionsLoader;
import org.overb.arkanoidfx.game.loaders.LevelLoader;
import org.overb.arkanoidfx.game.ui.*;
import org.overb.arkanoidfx.game.world.BallFactory;
import org.overb.arkanoidfx.game.world.PaddleFactory;
import org.overb.arkanoidfx.game.world.WallsFactory;

import java.util.List;

@Log
public class ArkanoidApp extends GameApplication {
    @Getter
    @Setter
    private static volatile boolean endStateMenuVisible = false;

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
    private SurpriseService surpriseService;


    @Override
    protected void initSettings(GameSettings settings) {
        Resolution defaultResolution = Resolution.R1920x1080;
        settings.setTitle("Arkanoid FX");
        settings.setVersion("0.4");
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
            FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PADDLE, EntityType.SURPRISE) {
                @Override
                protected void onCollisionBegin(Entity paddle, Entity surprise) {
                    applySurprise(surprise);
                    surprise.removeFromWorld();
                }
            });
        } catch (Exception e) {
            log.severe("Failed to load levels.txt" + e.getMessage());
            throw new RuntimeException(e);
        }
        EventBus.subscribe(EventType.HUD_UPDATE, e -> hudManager.refresh(session));
        EventBus.subscribe(EventType.LEVEL_FINISHED, e -> {
            levelManager.onLevelCleared(() -> {
            });
        });
        EventBus.subscribe(EventType.GAME_OVER, e -> {
            levelManager.onGameOver(() -> {
            });
        });
        EventBus.subscribe(EventType.BALL_LOST, e -> processBallLost());

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

    private void processBallLost() {
        if (!FXGL.getGameWorld().getEntitiesByType(EntityType.BALL).isEmpty()) {
            return;
        }
        session.loseLife();
        EventBus.publish(GameEvent.of(EventType.HUD_UPDATE));
        if (session.getLives() > 0) {
            levelManager.spawnPaddleAndBall();
        } else {
            EventBus.publish(GameEvent.of(EventType.GAME_OVER));
        }
    }

    private void showMainMenu() {
        MouseUI.setMouseVisible(true);
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
        MouseUI.setMouseVisible(true);
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
        MouseUI.setMouseVisible(false);
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
            if (paused) {
                return;
            }
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
        // cheats
        FXGL.onKeyDown(KeyCode.F8, () -> surpriseService.applyMultiball());
        FXGL.onKeyDown(KeyCode.F7, () -> surpriseService.applySafetyWall(10.0));
}

private void applySurprise(Entity surprise) {
    var sc = surprise.getComponentOptional(SurpriseComponent.class).orElse(null);
    String effect = sc != null ? sc.getEffect() : null;
    String sound = sc != null ? sc.getSound() : null;
    SfxBus.getInstance().play(sound);
    if ("multiball".equalsIgnoreCase(effect)) {
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        if (!balls.isEmpty()) {
            surpriseService.applyMultiball();
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
    MouseUI.setMouseVisible(true);
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
    MouseUI.setMouseVisible(false);
}

private void quitToMainFromPause() {
    paused = false;
    FXGL.getGameController().resumeEngine();
    if (pauseMenu != null) {
        FXGL.getGameScene().removeUINode(pauseMenu);
    }
    MouseUI.setMouseVisible(true);
    levelManager.quitToMainMenuNoDialog();
}

public static void main(String[] args) {
    launch(args);
}
}