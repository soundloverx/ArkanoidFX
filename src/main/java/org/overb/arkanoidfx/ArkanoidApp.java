package org.overb.arkanoidfx;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.java.Log;
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
import org.overb.arkanoidfx.game.world.BallFactory;
import org.overb.arkanoidfx.game.world.PaddleFactory;
import org.overb.arkanoidfx.game.world.WallsFactory;

import java.util.List;

@Log
public class ArkanoidApp extends GameApplication {

    private MainMenuUI mainMenu;
    private OptionsMenuUI optionsMenu;

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
        EventBus.subscribe(EventType.LAST_DESTRUCTIBLE_DESTROYED, e -> levelManager.onLevelCleared());
        EventBus.subscribe(EventType.GAME_OVER, e -> levelManager.onGameOver(() -> {
        }));

        // Initialize UI: show Main Menu instead of starting the level directly
        // Ensure scene background is black while in menus
        FXGL.getGameScene().setBackgroundColor(javafx.scene.paint.Color.BLACK);
        showMainMenu();

        FXGL.getGameTimer().runOnceAfter(() -> {
            Stage stage = FXGL.getPrimaryStage();
            ConfigOptions cfg = ConfigIO.loadOrDefault();
            // Apply volumes from config at startup
            org.overb.arkanoidfx.audio.AudioMixer.getInstance().setMasterVolume(cfg.audio.master);
            org.overb.arkanoidfx.audio.AudioMixer.getInstance().setMusicVolume(cfg.audio.music);
            org.overb.arkanoidfx.audio.AudioMixer.getInstance().setSfxVolume(cfg.audio.sfx);

            Resolution res = Resolution.getFromHeight(cfg.height);
            ResolutionManager.getInstance().applyWindowed(stage, FXGL.getGameScene().getRoot(), res);
            ResolutionManager.getInstance().hookResizeListeners(stage, FXGL.getGameScene().getRoot());

            // Start main menu music
            org.overb.arkanoidfx.audio.MusicBus.getInstance().loop("main_menu.mp3");
        }, javafx.util.Duration.millis(1));
    }

    private void showMainMenu() {
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
        ConfigOptions cfg = ConfigIO.loadOrDefault();
        if (optionsMenu == null) {
            optionsMenu = new OptionsMenuUI(cfg, (orig, updated) -> {
                // Apply and save configuration
                applyAndSaveConfig(updated);
                // Return to main menu after apply
                showMainMenu();
            }, v -> {
                // Cancel: restore original volumes already handled; return to main
                showMainMenu();
            });
        } else {
            // rebuild with latest cfg values for selections
            optionsMenu = new OptionsMenuUI(cfg, (orig, updated) -> {
                applyAndSaveConfig(updated);
                showMainMenu();
            }, v -> showMainMenu());
        }
        FXGL.getGameScene().clearUINodes();
        FXGL.getGameScene().addUINodes(optionsMenu);
    }

    private void applyAndSaveConfig(ConfigOptions updated) {
        // Volumes
        org.overb.arkanoidfx.audio.AudioMixer.getInstance().setMasterVolume(updated.audio.master);
        org.overb.arkanoidfx.audio.AudioMixer.getInstance().setMusicVolume(updated.audio.music);
        org.overb.arkanoidfx.audio.AudioMixer.getInstance().setSfxVolume(updated.audio.sfx);

        // Window mode and resolution
        Stage stage = FXGL.getPrimaryStage();
        if ("FULLSCREEN".equalsIgnoreCase(updated.fullscreenMode)) {
            stage.setFullScreen(true);
        } else {
            Resolution res = Resolution.getFromHeight(updated.height);
            ResolutionManager.getInstance().applyWindowed(stage, FXGL.getGameScene().getRoot(), res);
        }
        // Nudge UI to relayout/center after resolution change
        FXGL.runOnce(() -> {
            if (optionsMenu != null && optionsMenu.isVisible()) optionsMenu.requestLayout();
            if (mainMenu != null && mainMenu.isVisible()) mainMenu.requestLayout();
        }, javafx.util.Duration.millis(50));
        // Persist
        ConfigIO.save(updated);
    }

    private void startGameFromMenu() {
        // stop menu music, start game
        org.overb.arkanoidfx.audio.MusicService.getInstance().stopCurrentMusic();
        org.overb.arkanoidfx.audio.MusicBus.getInstance().stop();
        FXGL.getGameScene().clearUINodes();
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
        // release ball on click
        FXGL.onBtnDown(javafx.scene.input.MouseButton.PRIMARY, () -> {
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
        FXGL.onKeyDown(javafx.scene.input.KeyCode.F9, () -> {
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

    public static void main(String[] args) {
        launch(args);
    }
}