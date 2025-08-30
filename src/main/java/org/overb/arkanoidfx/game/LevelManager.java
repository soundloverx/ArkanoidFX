package org.overb.arkanoidfx.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import lombok.Setter;
import lombok.extern.java.Log;
import org.overb.arkanoidfx.ArkanoidApp;
import org.overb.arkanoidfx.audio.MusicService;
import org.overb.arkanoidfx.entities.EntityRepository;
import org.overb.arkanoidfx.entities.LevelEntity;
import org.overb.arkanoidfx.enums.EntityType;
import org.overb.arkanoidfx.game.loaders.LevelLoader;
import org.overb.arkanoidfx.game.ui.InGameMenuUI;
import org.overb.arkanoidfx.game.ui.HUDManager;
import org.overb.arkanoidfx.game.ui.ShatteredOverlay;
import org.overb.arkanoidfx.game.world.BallFactory;
import org.overb.arkanoidfx.game.world.PaddleFactory;
import org.overb.arkanoidfx.game.world.WallsFactory;

import java.util.List;

@Log
public final class LevelManager {

    public interface MenuReturnHandler {
        void showMainMenu();
    }

    private final EntityRepository repository;
    private final GameSession session;
    private final HUDManager hudManager;
    private final WallsFactory wallsFactory;
    private final PaddleFactory paddleFactory;
    private final BallFactory ballFactory;
    private final LevelLoader levelLoader;
    @Setter
    private MenuReturnHandler menuReturnHandler;

    private List<String> levelOrder;
    private int currentLevelIndex = 0;
    private LevelEntity currentLevel;

    public LevelManager(EntityRepository repository, GameSession session, HUDManager hudManager, WallsFactory wallsFactory,
                        PaddleFactory paddleFactory, BallFactory ballFactory, LevelLoader levelLoader) {
        this.repository = repository;
        this.session = session;
        this.hudManager = hudManager;
        this.wallsFactory = wallsFactory;
        this.paddleFactory = paddleFactory;
        this.ballFactory = ballFactory;
        this.levelLoader = levelLoader;
    }

    public void quitToMainMenuNoDialog() {
        MusicService.getInstance().stopCurrentMusic();
        FXGL.getGameWorld().getEntitiesByType(
                EntityType.BALL, EntityType.SURPRISE, EntityType.BRICK, EntityType.WALL_SAFETY, EntityType.PADDLE
        ).forEach(e -> {
            if (e.isActive()) {
                e.removeFromWorld();
            }
        });
        session.resetForNewGame();
        currentLevelIndex = 0;
        returnToMainMenu();
    }

    private void returnToMainMenu() {
        FXGL.getGameScene().clearUINodes();
        FXGL.getGameScene().setBackgroundColor(Color.web("#000000"));
        MusicService.getInstance().stopCurrentMusic();
        MusicService.getInstance().play("main_menu.mp3");
        if (menuReturnHandler != null) {
            menuReturnHandler.showMainMenu();
        }
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
        MusicService.getInstance().play(currentLevel.music);
        hudManager.refresh(session);
    }

    public void onLevelCleared(Runnable afterDialog) {
        var ballsForImpact = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        javafx.geometry.Point2D impact = null;
        if (!ballsForImpact.isEmpty()) {
            Entity b = ballsForImpact.getFirst();
            impact = new javafx.geometry.Point2D(b.getX() + b.getWidth() / 2.0, b.getY() + b.getHeight() / 2.0);
        }
        ShatteredOverlay overlay = ShatteredOverlay.showBackground(impact);
        cleanupLevelEntities();
        MusicService.getInstance().stopCurrentMusic();
        FXGL.getGameController().pauseEngine();
        FXGL.getGameScene().getRoot().setCursor(Cursor.DEFAULT);
        ArkanoidApp.setEndStateMenuVisible(true);

        StackPane[] refMenu = new StackPane[1];
        refMenu[0] = InGameMenuUI.builder()
                .withTitle("Level cleared")
                .withMenuItem("Continue", () -> continueToNextLevel(afterDialog, refMenu, overlay))
                .withMenuItem("Quit to main menu", () -> quitToMainMenu(afterDialog, refMenu, overlay))
                .withMenuItem("Exit", () -> exitGame(refMenu, overlay))
                .build();
        FXGL.getGameScene().addUINode(refMenu[0]);
    }

    private static void exitGame(StackPane[] refMenu, ShatteredOverlay overlay) {
        FXGL.getGameScene().removeUINode(refMenu[0]);
        overlay.dismiss();
        ArkanoidApp.setEndStateMenuVisible(false);
        FXGL.getGameController().exit();
    }

    private void quitToMainMenu(Runnable afterDialog, StackPane[] refMenu, ShatteredOverlay overlay) {
        FXGL.getGameScene().removeUINode(refMenu[0]);
        overlay.dismiss();
        FXGL.getGameController().resumeEngine();
        ArkanoidApp.setEndStateMenuVisible(false);
        FXGL.getGameScene().getRoot().setCursor(Cursor.DEFAULT);
        quitToMainMenuNoDialog();
        if (afterDialog != null) afterDialog.run();
    }

    private void continueToNextLevel(Runnable afterDialog, StackPane[] refMenu, ShatteredOverlay overlay) {
        FXGL.getGameScene().removeUINode(refMenu[0]);
        overlay.dismiss();
        int nextIndex = currentLevelIndex + 1;
        if (levelOrder == null || levelOrder.isEmpty()) {
            loadAndStart();
        } else if (nextIndex >= levelOrder.size()) {
            session.resetForNewGame();
            currentLevelIndex = 0;
            ArkanoidApp.setEndStateMenuVisible(false);
            returnToMainMenu();
            if (afterDialog != null) afterDialog.run();
            return;
        } else {
            currentLevelIndex = nextIndex;
            loadAndStart();
        }
        FXGL.getGameController().resumeEngine();
        ArkanoidApp.setEndStateMenuVisible(false);
        FXGL.getGameScene().getRoot().setCursor(Cursor.NONE);
        if (afterDialog != null) afterDialog.run();
    }

    public void onGameOver(Runnable afterDialog) {
        var ballsForImpact = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        javafx.geometry.Point2D impact = null;
        if (!ballsForImpact.isEmpty()) {
            Entity b = ballsForImpact.getFirst();
            impact = new javafx.geometry.Point2D(b.getX() + b.getWidth() / 2.0, b.getY() + b.getHeight() / 2.0);
        }
        ShatteredOverlay overlay = ShatteredOverlay.showBackground(impact);
        cleanupLevelEntities();
        MusicService.getInstance().stopCurrentMusic();
        FXGL.getGameController().pauseEngine();
        FXGL.getGameScene().getRoot().setCursor(Cursor.DEFAULT);
        ArkanoidApp.setEndStateMenuVisible(true);

        StackPane[] refMenu = new StackPane[1];
        refMenu[0] = InGameMenuUI.builder()
                .withTitle("Game over")
                .withMenuItem("Main menu", () -> quitToMainMenu(afterDialog, refMenu, overlay))
                .withMenuItem("Exit", () -> exitGame(refMenu, overlay))
                .build();
        FXGL.getGameScene().addUINode(refMenu[0]);
    }

    private void cleanupLevelEntities() {
        FXGL.getGameWorld().getEntitiesByType(
                EntityType.BALL, EntityType.SURPRISE, EntityType.BRICK, EntityType.WALL_SAFETY, EntityType.PADDLE
        ).forEach(e -> {
            if (e.isActive()) e.removeFromWorld();
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