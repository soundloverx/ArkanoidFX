package org.overb.arkanoidfx.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import lombok.Setter;
import lombok.extern.java.Log;
import org.overb.arkanoidfx.ArkanoidApp;
import org.overb.arkanoidfx.audio.LevelMusicService;
import org.overb.arkanoidfx.audio.MusicBus;
import org.overb.arkanoidfx.entities.EntityRepository;
import org.overb.arkanoidfx.entities.LevelEntity;
import org.overb.arkanoidfx.enums.EntityType;
import org.overb.arkanoidfx.game.loaders.LevelLoader;
import org.overb.arkanoidfx.game.ui.*;
import org.overb.arkanoidfx.game.world.BallFactory;
import org.overb.arkanoidfx.game.world.PaddleFactory;
import org.overb.arkanoidfx.game.world.WallsFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private boolean customLevelsFromFileSystem = false;
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
        LevelMusicService.getInstance().stopCurrentMusic();
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
        LevelMusicService.getInstance().stopCurrentMusic();
        MusicBus.getInstance().loop("main_menu.mp3");
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
        var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
        Entity paddle;
        if (paddles.isEmpty()) {
            paddle = paddleFactory.spawnPaddle(currentLevelIndex);
        } else {
            paddle = paddles.getFirst();
            for (int i = 1; i < paddles.size(); i++) {
                var extra = paddles.get(i);
                if (extra.isActive()) extra.removeFromWorld();
            }
        }
        ballFactory.spawnBallAttachedToPaddle(paddle);
        LevelMusicService.getInstance().play(currentLevel.music);
        hudManager.refresh(session);
    }

    public void onLevelCleared(Runnable afterDialog) {
        ShatteredOverlay overlay = ShatteredOverlay.showBackground();
        cleanupLevelEntities();
        LevelMusicService.getInstance().stopCurrentMusic();
        FXGL.getGameController().pauseEngine();
        hudManager.hide();
        MouseUI.setMouseVisible(true);
        ArkanoidApp.setEndStateMenuVisible(true);

        StackPane[] refMenu = new StackPane[1];
        if(hasNextLevel()) {
            MusicBus.getInstance().loop("level_cleared.mp3");
            refMenu[0] = InGameMenuUI.builder()
                    .withTitle("Level cleared")
                    .withMenuItem("Continue", () -> continueToNextLevel(afterDialog, refMenu, overlay))
                    .withMenuItem("Main menu", () -> quitToMainMenu(afterDialog, refMenu, overlay))
                    .withMenuItem("Exit", () -> exitGame(refMenu, overlay))
                    .build();
        } else {
            MusicBus.getInstance().loop("scores.mp3");
            refMenu[0] = InGameMenuUI.builder()
                    .withTitle("All levels cleared")
                    .withMenuItem("Main menu", () -> quitToMainMenu(afterDialog, refMenu, overlay))
                    .withMenuItem("Exit", () -> exitGame(refMenu, overlay))
                    .build();
        }
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
        MouseUI.setMouseVisible(false);
        if (afterDialog != null) afterDialog.run();
    }

    public void onGameOver(Runnable afterDialog) {
        ShatteredOverlay overlay = ShatteredOverlay.showBackground();
        cleanupLevelEntities();
        LevelMusicService.getInstance().stopCurrentMusic();
        FXGL.getGameController().pauseEngine();
        hudManager.hide();
        MouseUI.setMouseVisible(true);
        ArkanoidApp.setEndStateMenuVisible(true);

        MusicBus.getInstance().loop("loser.mp3");
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
        MouseUI.setMouseVisible(false);
        String levelFileName = levelOrder.get(currentLevelIndex);
        FXGL.getGameScene().clearUINodes();
        try {
            FXGL.getGameScene().getRoot().setBackground(null);
            FXGL.getGameScene().setBackgroundColor(Color.web("#000000"));
        } catch (Exception ignored) {
        }
        session.resetLevel();
        LevelEntity level;
        try {
            Path p = Paths.get(levelFileName);
            if (customLevelsFromFileSystem) {
                if (Files.exists(p)) {
                    level = levelLoader.loadLevelFromFile(levelFileName);
                } else {
                    log.warning("Custom level file not found on disk: " + levelFileName);
                    showMissingLevelAndReturn(levelFileName);
                    return;
                }
            } else {
                // classpath levels and absolute paths
                if (Files.exists(p)) {
                    level = levelLoader.loadLevelFromFile(levelFileName);
                } else {
                    level = levelLoader.loadLevel(levelFileName);
                }
            }
        } catch (Exception ex) {
            log.severe("Failed to load level: " + levelFileName + " : " + ex.getMessage());
            NotificationUI.show("Failed to load level:\n" + levelFileName);
            quitToMainMenuNoDialog();
            return;
        }
        currentLevel = level;
        log.info("Loaded level: " + levelFileName + " (" + level.cols + "x" + level.rows + "), music=" + level.music + ", background=" + level.background);
        applyLevelBackground(level);
        new LevelBuilder(repository, session).buildBricks(level);
        hudManager.initHUD();
        hudManager.refresh(session);
        wallsFactory.spawnWalls();
        spawnPaddleAndBall();
    }

    private void applyLevelBackground(LevelEntity level) {
        var root = FXGL.getGameScene().getRoot();
        if (level == null || level.background == null || level.background.isBlank()) {
            try {
                root.setBackground(null);
                FXGL.getGameScene().setBackgroundColor(Color.web("#000000"));
            } catch (Exception ignored) {
            }
            return;
        }
        String name = level.background.trim();
        String resource = name.startsWith("/") ? name : "/assets/textures/bg/" + name;
        try {
            var is = getClass().getResourceAsStream(resource);
            if (is == null) {
                log.warning("Background not found: " + resource);
                try {
                    root.setBackground(null);
                    FXGL.getGameScene().setBackgroundColor(Color.web("#000000"));
                } catch (Exception ignored) {
                }
                return;
            }
            Image img = new Image(is);
            BackgroundSize size = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true);
            BackgroundImage bg = new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, size);
            root.setBackground(new Background(bg));
        } catch (Exception e) {
            log.warning("Failed to apply background '" + resource + "': " + e.getMessage());
            try {
                root.setBackground(null);
                FXGL.getGameScene().setBackgroundColor(Color.web("#000000"));
            } catch (Exception ignored) {
            }
        }
    }

    private boolean hasNextLevel() {
        return levelOrder != null && currentLevelIndex < levelOrder.size() - 1;
    }

    private void showMissingLevelAndReturn(String levelPath) {
        NotificationUI.show("Level file not found:\n" + levelPath);
        quitToMainMenuNoDialog();
    }
}