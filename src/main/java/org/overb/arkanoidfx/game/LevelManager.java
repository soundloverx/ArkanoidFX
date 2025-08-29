package org.overb.arkanoidfx.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.scene.paint.Color;
import lombok.extern.java.Log;
import org.overb.arkanoidfx.audio.MusicService;
import org.overb.arkanoidfx.entities.EntityRepository;
import org.overb.arkanoidfx.entities.LevelEntity;
import org.overb.arkanoidfx.enums.EntityType;
import org.overb.arkanoidfx.game.loaders.LevelLoader;
import org.overb.arkanoidfx.game.ui.HUDManager;
import org.overb.arkanoidfx.game.world.BallFactory;
import org.overb.arkanoidfx.game.world.PaddleFactory;
import org.overb.arkanoidfx.game.world.WallsFactory;

import java.util.List;

@Log
public final class LevelManager {

    private final EntityRepository repository;
    private final GameSession session;
    private final HUDManager hudManager;
    private final MusicService musicService;
    private final WallsFactory wallsFactory;
    private final PaddleFactory paddleFactory;
    private final BallFactory ballFactory;
    private final LevelLoader levelLoader;

    private List<String> levelOrder;
    private int currentLevelIndex = 0;
    private LevelEntity currentLevel;

    public LevelManager(EntityRepository repository, GameSession session, HUDManager hudManager, MusicService musicService,
                        WallsFactory wallsFactory, PaddleFactory paddleFactory, BallFactory ballFactory, LevelLoader levelLoader) {
        this.repository = repository;
        this.session = session;
        this.hudManager = hudManager;
        this.musicService = musicService;
        this.wallsFactory = wallsFactory;
        this.paddleFactory = paddleFactory;
        this.ballFactory = ballFactory;
        this.levelLoader = levelLoader;
    }

    public void setLevelOrder(List<String> levelOrder) {
        this.levelOrder = levelOrder;
        this.currentLevelIndex = 0;
    }

    public void startInitialLevel() {
        if (levelOrder == null || levelOrder.isEmpty()) {
            throw new IllegalStateException("Level order is not set or empty");
        }
        loadAndStart();
    }

    public void spawnPaddleAndBall() {
        Entity paddle = paddleFactory.spawnPaddle();
        ballFactory.spawnBallAttachedToPaddle(paddle);
        musicService.startLevelMusicIfNeeded(currentLevel);
        hudManager.refresh(session);
    }

    public void onLevelCleared() {
        musicService.stopCurrentMusic();
        FXGL.getGameWorld().getEntitiesByType(
                EntityType.BALL, EntityType.SURPRISE, EntityType.BRICK, EntityType.WALL_SAFETY, EntityType.PADDLE
        ).forEach(e -> {
            if (e.isActive()) {
                e.removeFromWorld();
            }
        });
        int nextIndex = currentLevelIndex + 1;
        if (levelOrder == null || levelOrder.isEmpty()) {
            FXGL.getDialogService().showMessageBox("Level Cleared!", this::loadAndStart);
            return;
        }
        if (nextIndex >= levelOrder.size()) {
            FXGL.getDialogService().showMessageBox("Victory! Score: " + session.getScoreRounded(), this::loadAndStart);
        } else {
            currentLevelIndex = nextIndex;
            FXGL.getDialogService().showMessageBox("Level Cleared!", this::loadAndStart);
        }
    }

    public void onGameOver(Runnable afterDialog) {
        musicService.stopCurrentMusic();
        FXGL.getGameWorld().getEntitiesByType(
                EntityType.BALL, EntityType.SURPRISE, EntityType.BRICK, EntityType.WALL_SAFETY, EntityType.PADDLE
        ).forEach(e -> {
            if (e.isActive()) {
                e.removeFromWorld();
            }
        });
        FXGL.getDialogService().showMessageBox("Game Over! Score: " + session.getScoreRounded(), () -> {
            session.resetForNewGame();
            currentLevelIndex = 0;
            loadAndStart();
            if (afterDialog != null) {
                afterDialog.run();
            }
        });
    }

    private void loadAndStart() {
        session.setCurrentLevel(currentLevelIndex + 1);
        String levelFileName = levelOrder.get(currentLevelIndex);
        FXGL.getGameScene().clearUINodes();
        FXGL.getGameScene().setBackgroundColor(Color.web("#000000"));
        session.resetLevel();

        LevelEntity level;
        try {
            level = levelLoader.loadLevel(levelFileName);
        } catch (Exception ex) {
            log.severe("Failed to load level: " + levelFileName + " : " + ex.getMessage());
            throw new RuntimeException(ex);
        }

        currentLevel = level;
        log.info("Loaded level: " + levelFileName + " (" + level.cols + "x" + level.rows + "), music=" + level.music);

        new LevelBuilder(repository, session).buildBricks(level);

        hudManager.initHUD();
        hudManager.refresh(session);

        wallsFactory.spawnWalls();
        spawnPaddleAndBall();
    }
}