package org.overb.arkanoidfx;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.java.Log;
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
import org.overb.arkanoidfx.game.world.BallFactory;
import org.overb.arkanoidfx.game.world.PaddleFactory;
import org.overb.arkanoidfx.game.world.WallsFactory;

import java.util.List;

@Log
public class ArkanoidApp extends GameApplication {

    @Getter
    private EntityRepository entityRepository;
    private List<String> levelOrder;
    private final GameSession session = new GameSession();
    private boolean hitboxDebugShown = false;
    private HUDManager hudManager;
    private MusicService musicService;
    private BallFactory ballFactory;
    private WallsFactory wallsFactory;
    private PaddleFactory paddleFactory;
    private LevelManager levelManager;
    private PhysicsManager physicsManager;
    private SurpriseService surpriseService;

    @Override
    protected void initSettings(GameSettings settings) {
        Resolution res = Resolution.R1920x1080;
        settings.setTitle("Arkanoid FX");
        settings.setVersion("0.2");
        settings.setWidth(res.getWidth());
        settings.setHeight(res.getHeight());
        ResolutionManager.getInstance().setCurrentResolution(res);
        BallComponent.refreshBaseSpeedAtCurrentResolution();
        settings.setFullScreenAllowed(true);
        settings.setFullScreenFromStart(false);
        settings.setProfilingEnabled(false);
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);
//        settings.setStageStyle(StageStyle.UNDECORATED);
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
            musicService = new MusicService();
            ballFactory = new BallFactory(entityRepository);
            wallsFactory = new WallsFactory();
            paddleFactory = new PaddleFactory(entityRepository);
            levelManager = new LevelManager(entityRepository, session, hudManager, musicService, wallsFactory, paddleFactory, ballFactory, levelLoader);
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
        levelManager.startInitialLevel();

        Stage stage = FXGL.getPrimaryStage();
        ResolutionManager.getInstance().applyScale(stage, FXGL.getGameScene().getRoot());
        ResolutionManager.getInstance().hookResizeListeners(stage, FXGL.getGameScene().getRoot());
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