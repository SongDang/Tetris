package com.se330.tetris.controller;

import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.Constants;
import com.se330.tetris.util.SoundType;
import com.se330.tetris.game.*;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameController {

    // --- FXML fields ---
    @FXML private Pane gamePane;
    @FXML private Canvas gameCanvas;
    @FXML private Canvas vfxCanvas;
    @FXML private Canvas nextBlockCanvas;
    @FXML private Canvas nextBlockCanvas1;
    @FXML private Canvas nextBlockCanvas2;
    @FXML private Canvas nextBlockCanvas3;
    @FXML private Label scoreLabel;
    @FXML private Label levelLabel;
    @FXML private Label linesLabel;
    @FXML private Label timeLabel;
    @FXML private VBox timePanel;
    @FXML private Label freezeLabel;
    @FXML private Canvas holdBlockCanvas;
    @FXML private javafx.scene.image.ImageView lightBulbView;
    @FXML private javafx.scene.image.ImageView mainFrameView;
    @FXML private javafx.scene.control.Label flavourLabel;
    @FXML private javafx.scene.control.Label comboLabel;
    @FXML private Label bombsLabel;
    @FXML private VBox bombInventoryBox;

    private javafx.scene.image.Image lightOnImage;
    private javafx.scene.image.Image lightOffImage;
    private javafx.scene.image.Image hardMainImage;
    private javafx.scene.image.Image darkMainImage;
    private javafx.scene.image.Image standardMainImage;
    private javafx.scene.image.Image timeAttackMainImage;

    private SceneManager sceneManager;
    private GameContext gameContext;

    // --- Components ---
    private BoardEngine boardEngine;
    private GameRenderer renderer;
    private HardModeHandler hardMode;
    private TimeAttackHandler timeAttack;

    // --- Piece state ---
    private Piece currentPiece;
    private TetrominoType holdType;
    private boolean canHold = true;
    private final ArrayDeque<Piece> nextQueue = new ArrayDeque<>();
    private final List<Piece> suspendedPieces = new ArrayList<>();

    // --- Bag randomizer ---
    private List<TetrominoType> bag = new ArrayList<>();
    private final Random rng = new Random();
    private static final long RANDOM_BLOCK_INTERVAL_MS = 2000L;
    private static final double RANDOM_BLOCK_CHANCE = 0.30;
    private final boolean randomBlockEnabled = true;

    // --- Loop state ---
    private AnimationTimer gameLoop;
    private boolean gamePaused = false;
    private boolean isGameOver = false;
    private long lastFallTime = 0;
    private long lastFrameTime = 0;
    private long fallIntervalNs = 500_000_000L;

    // --- Tetris freeze (visual row-flash before clear) ---
    private long freezeUntil = 0;
    private long gameOverFreezeUntil = 0;
    private int[] pendingTetrisClear = null;
    private int[] frozenRowFlash = null;
    private boolean pendingTetrisClearHasTimeBlock = false;

    // --- Input ---
    private boolean softDropping = false;
    private boolean fuseLoopPlaying = false;
    private int mouseTargetColumn = -1;
    private int dropStartRow = -1;

    // --- Scoring / combo ---
    private int comboCount = 0;

    // --- Bomb ---
    private int bombsRemaining = 3;
    private String bombInventoryBaseStyle = "";

    // --- Images ---
    private javafx.scene.image.Image lightOnImage;
    private javafx.scene.image.Image lightOffImage;
    private javafx.scene.image.Image hardMainImage;
    private javafx.scene.image.Image darkMainImage;
    private javafx.scene.image.Image bombSprite;

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext = GameContext.getInstance();
        gameContext.reset();

        gameGc = gameCanvas.getGraphicsContext2D();
        nextGc1 = nextBlockCanvas1 != null ? nextBlockCanvas1.getGraphicsContext2D() : null;
        nextGc2 = nextBlockCanvas2 != null ? nextBlockCanvas2.getGraphicsContext2D() : null;
        nextGc3 = nextBlockCanvas3 != null ? nextBlockCanvas3.getGraphicsContext2D() : null;

        vfxGc = vfxCanvas.getGraphicsContext2D();
        holdGc = holdBlockCanvas.getGraphicsContext2D();
        // Sync vfxCanvas.layoutY to gameCanvas's actual post-layout position so
        // their y=0 coordinates map to the same screen row.
        Platform.runLater(() -> vfxCanvas.setLayoutY(gameCanvas.getBoundsInParent().getMinY()));
        bombSprite = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/bomb.png"));

        lightOnImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/lightson.png"));
        lightOffImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/lightsoff.png"));
        hardMainImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/hardmain.png"));
        darkMainImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/darkmain.png"));
        standardMainImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/standardmain.png"));
        timeAttackMainImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/timeatkmain.png"));

        applyGameModeTheme();

        // HARD MODE
        if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
            fallIntervalNs = 300_000_000L;
            if (lightBulbView != null) { lightBulbView.setImage(lightOnImage); lightBulbView.setVisible(true); }
            Platform.runLater(() -> hardMode.buildFlavourChars(flavourLabel));
        }

        holdType = null;
        canHold = true;
        nextQueue.clear();
        for (int i = 0; i < 4; i++) nextQueue.addLast(randomPiece());
        spawnPiece();
        refreshLabels();
        timeAttack.setup();

        gamePane.setOnKeyPressed(this::handleKeyPressed);
        gamePane.setOnKeyReleased(e -> { if (e.getCode() == KeyCode.S) softDropping = false; });
        gameCanvas.setOnMouseMoved(this::handleMouseMoved);
        gameCanvas.setOnMouseExited(e -> mouseTargetColumn = -1);
        gameCanvas.setOnMouseClicked(this::handleMouseClicked);

        Platform.runLater(() -> {
            gamePane.requestFocus();
            vfxCanvas.setLayoutY(gameCanvas.getBoundsInParent().getMinY());
            Canvas startupCanvas = new Canvas(gamePane.getWidth(), gamePane.getHeight());
            startupCanvas.setManaged(false);
            startupCanvas.setMouseTransparent(true);
            gamePane.getChildren().add(startupCanvas);
            renderer.setStartupCanvas(startupCanvas);
            gamePane.layoutBoundsProperty().addListener((obs, o, n) -> {
                if (renderer.startupGlitchElapsed < 0.75) {
                    startupCanvas.setWidth(n.getWidth());
                    startupCanvas.setHeight(n.getHeight());
                }
            });
        });

        startGameLoop();
    }

    private void loadImages() {
        bombSprite    = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/bomb.png"));
        lightOnImage  = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/lightson.png"));
        lightOffImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/lightsoff.png"));
        hardMainImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/hardmain.png"));
        darkMainImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/darkmain.png"));
    }

    // --- Game loop ---

    private void startGameLoop() {
        lastFallTime = System.nanoTime();
        lastFrameTime = System.nanoTime();
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = (now - lastFrameTime) / 1_000_000_000.0;
                lastFrameTime = now;

                handleTetrisFreezeExpiry(now);
                handleGameOverFreeze(now);
                renderer.update(dt, gamePaused, isGameOver);

                if (!gamePaused && !isGameOver && freezeUntil == 0) {
                    applyMouseTarget();
                    if (!timeAttack.isFreezeActive) {
                        if (hardMode.tickBlackoutDrop(dt)) {
                            hardDrop();
                        } else {
                            long effectiveInterval = softDropping ? 50_000_000L : fallIntervalNs;
                            if (now - lastFallTime >= effectiveInterval) {
                                updateFall();
                                lastFallTime = now;
                            }
                        }
                    }
                    updateFuseEffects();
                }

                if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE && !isGameOver && !gamePaused)
                    hardMode.update(dt);

                timeAttack.setGameState(gamePaused, isGameOver);

                renderer.render(currentPiece, nextQueue, holdType, suspendedPieces,
                        gamePaused, isGameOver, freezeUntil, frozenRowFlash);
            }
        };
        gameLoop.start();
    }

    private void handleTetrisFreezeExpiry(long now) {
        if (freezeUntil > 0 && now >= freezeUntil) {
            lastFallTime = now;
            freezeUntil = 0;
            frozenRowFlash = null;
            if (pendingTetrisClear != null) {
                int[] rows = pendingTetrisClear;
                pendingTetrisClear = null;
                boolean hasTimeBlock = pendingTetrisClearHasTimeBlock;
                pendingTetrisClearHasTimeBlock = false;
                int avgRow = Arrays.stream(rows).sum() / rows.length;
                List<Integer> rowList = new ArrayList<>();
                for (int r : rows) rowList.add(r);
                Collections.sort(rowList, Collections.reverseOrder());
                boardEngine.clearRows(rowList);
                addScore(4);
                addLines(4);
                updateLevel();
                renderer.emitScorePopup(4, avgRow);
                timeAttack.triggerFreezeIfNeeded(hasTimeBlock);
                canHold = true;
                spawnAndCheckGameOver();
            }
        }
    }

    private void handleGameOverFreeze(long now) {
        if (isGameOver && gameOverFreezeUntil > 0 && now >= gameOverFreezeUntil) {
            gameOverFreezeUntil = 0;
            boolean hardModeOn = gameContext.getGameMode() == GameContext.GameMode.HARD_MODE;
            Color tearBg = Color.web(hardModeOn ? "#280C00" : "#0f0d1a");
            renderer.triggerGameOverTear(boardEngine.getBoard(), Constants.BLOCK_SIZE, tearBg, () -> {
                gameLoop.stop();
                sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
            });
        }
    }

    private void updateFuseEffects() {
        int cs = Constants.BLOCK_SIZE;
        if (currentPiece.getType() == TetrominoType.BOMB) {
            if (!fuseLoopPlaying) { SoundManager.getInstance().playLooping(SoundType.FUSE); fuseLoopPlaying = true; }
            renderer.onFuseSparks((currentPiece.getX() + 0.5) * cs, (currentPiece.getY() + 0.5) * cs,
                    cs, currentPiece.getRotation());
        } else if (fuseLoopPlaying) {
            SoundManager.getInstance().stopLooping();
            fuseLoopPlaying = false;
        }
    }

    // --- Piece logic ---

    private void updateFall() {
        if (boardEngine.canMove(currentPiece, 0, 1, currentPiece.getRotation(), suspendedPieces))
            currentPiece.setY(currentPiece.getY() + 1);
        else
            lockAndSpawn();
        updateSuspendedFall();
    }

    private void lockAndSpawn() {
        stopRandomBlockIfNeeded(currentPiece);
        int cs = Constants.BLOCK_SIZE;

        if (currentPiece.getType() == TetrominoType.BOMB) {
            detonateBomb(currentPiece.getX(), currentPiece.getY());
        } else {
            BoardEngine.LockResult lock = boardEngine.lockPiece(currentPiece);
            SoundManager.getInstance().playSE(SoundType.BLOCK_DROP);
            if (dropStartRow >= 0 && lock.minCol <= lock.maxCol) {
                for (int[] cell : lock.cells)
                    renderer.onLockParticles(cell[0], cell[1], currentPiece.getType().getColor(), cs);
                renderer.onLightColumn(
                        lock.minCol * cs, dropStartRow * cs, (lock.maxRow + 1) * cs,
                        (lock.maxCol - lock.minCol + 1) * cs, currentPiece.getType().getColor());
                dropStartRow = -1;
            }
        }

        List<Integer> fullRows = boardEngine.findFullRows();
        if (!fullRows.isEmpty()) {
            boolean hasTimeBlock = fullRows.stream().anyMatch(boardEngine::rowHasTimeBlock);
            int cleared = fullRows.size();
            int avgRow = fullRows.stream().mapToInt(Integer::intValue).sum() / cleared;

            renderer.onLineClear(cleared, fullRows, currentPiece.getType().getColor(), cs);

            comboCount++;
            if (comboLabel != null) comboLabel.setText("x" + comboCount);
            if (comboCount >= 2) renderer.onCombo(comboCount, avgRow, cs);

            if (cleared == 4) {
                fullRows.sort(Collections.reverseOrder());
                frozenRowFlash = fullRows.stream().mapToInt(Integer::intValue).toArray();
                pendingTetrisClear = frozenRowFlash;
                pendingTetrisClearHasTimeBlock = hasTimeBlock;
                freezeUntil = System.nanoTime() + 300_000_000L;
                SoundManager.getInstance().playSE(SoundType.TETRIS);
                PauseTransition delay = new PauseTransition(Duration.millis(300));
                delay.setOnFinished(e -> SoundManager.getInstance().playSE(SoundType.TETRIS2));
                delay.play();
                if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE)
                    hardMode.trySetFlavourTetris();
                return;
            }

            fullRows.sort(Collections.reverseOrder());
            boardEngine.clearRows(fullRows);
            addScore(cleared);
            addLines(cleared);
            updateLevel();
            renderer.emitScorePopup(cleared, avgRow);
            timeAttack.triggerFreezeIfNeeded(hasTimeBlock);

            if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
                boolean flavSet = comboCount >= 2 && hardMode.trySetFlavourCombo();
                if (!flavSet) hardMode.trySetFlavourClear();
            }
        } else {
            comboCount = 0;
            if (comboLabel != null) comboLabel.setText("x0");
            if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
                hardMode.placesSinceFlavour++;
                if (hardMode.placesSinceFlavour >= 7) {
                    hardMode.placesSinceFlavour = 0;
                    hardMode.trySetFlavourPlace(boardEngine.getStackTopRow());
                }
            }
        }

        canHold = true;
        lastFallTime = System.nanoTime();
        spawnAndCheckGameOver();
    }

    private void spawnAndCheckGameOver() {
        spawnPiece();
        if (!boardEngine.canMove(currentPiece, 0, 0, currentPiece.getRotation(), suspendedPieces)) {
            isGameOver = true;
            stopRandomBlockIfNeeded(currentPiece);
            timeAttack.stopAll();
            SoundManager.getInstance().playSE(SoundType.GAME_OVER);
            SoundManager.getInstance().stopMusic();
            gameLoop.stop();
            sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
            gameOverFreezeUntil = System.nanoTime() + 500_000_000L;
            renderer.onGameOver();
        }
    }

    private void spawnPiece() {
        currentPiece = nextQueue.removeFirst();
        if (hardMode.blackoutState == HardModeHandler.BlackoutState.BLACKOUT
                && currentPiece.getType() == TetrominoType.BOMB) {
            nextQueue.addLast(currentPiece);
            int qSize = nextQueue.size();
            for (int i = 0; i < qSize; i++) {
                Piece candidate = nextQueue.removeFirst();
                if (candidate.getType() != TetrominoType.BOMB) { currentPiece = candidate; break; }
                nextQueue.addLast(candidate);
            }
        }
        currentPiece.setX(Constants.BOARD_WIDTH / 2 - 2);
        currentPiece.setY(0);
        nextQueue.addLast(randomPiece());
        startRandomBlockIfNeeded(currentPiece);
        if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE)
            hardMode.resetBlackoutOnSpawn();
    }

    private Piece randomPiece() {
        if (bag.isEmpty()) {
            bag.addAll(Arrays.asList(TetrominoType.values()));
            Collections.shuffle(bag);
        }
        TetrominoType type = bag.remove(0);
        if (randomBlockEnabled && type != TetrominoType.BOMB && rng.nextDouble() < RANDOM_BLOCK_CHANCE) {
            RandomBlock block = new RandomBlock(type, 0, 0, RANDOM_BLOCK_INTERVAL_MS);
            block.setTypeValidator((p, t) -> boardEngine.canPlaceType(p, t));
            return block;
        }
        return new Piece(type, 0, 0);
    }

    private void startRandomBlockIfNeeded(Piece piece) {
        if (piece instanceof RandomBlock rb) rb.startTimer();
    }

    private void stopRandomBlockIfNeeded(Piece piece) {
        if (piece instanceof RandomBlock rb) rb.lockBlock();
    }

    private void detonateBomb(int centerX, int centerY) {
        int cs = Constants.BLOCK_SIZE;
        List<BoardEngine.ClearedCell> cells = boardEngine.clearBombRadius(centerX, centerY);
        SoundManager.getInstance().stopLooping();
        fuseLoopPlaying = false;
        renderer.onBombExplode((centerX + 0.5) * cs, (centerY + 0.5) * cs, cs, cells);
        SoundManager.getInstance().playSE(SoundType.BOMB_EXPLODE);
    }

    private void suspendCurrentPiece() {
        if (currentPiece == null) return;
        stopRandomBlockIfNeeded(currentPiece);
        Piece frozen = new Piece(currentPiece.getType(), currentPiece.getX(), currentPiece.getY());
        frozen.setRotationSilent(currentPiece.getRotation());
        suspendedPieces.add(frozen);
        spawnAndCheckGameOver();
    }

    private void updateSuspendedFall() {
        if (suspendedPieces.isEmpty()) return;
        List<Piece> locked = new ArrayList<>();
        for (Piece piece : suspendedPieces) {
            if (boardEngine.canMoveSuspended(piece, 0, 1, currentPiece, suspendedPieces))
                piece.setY(piece.getY() + 1);
            else
                locked.add(piece);
        }
        for (Piece piece : locked) {
            suspendedPieces.remove(piece);
            if (piece.getType() == TetrominoType.BOMB) {
                detonateBomb(piece.getX(), piece.getY());
            } else {
                boardEngine.mergePiece(piece);
                processSuspendedLineClears();
            }
        }
    }

    private void processSuspendedLineClears() {
        List<Integer> fullRows = boardEngine.findFullRows();
        if (fullRows.isEmpty()) return;
        boolean hasTimeBlock = fullRows.stream().anyMatch(boardEngine::rowHasTimeBlock);
        fullRows.sort(Collections.reverseOrder());
        boardEngine.clearRows(fullRows);
        comboCount = 0;
        if (comboLabel != null) comboLabel.setText("x0");
        addScore(fullRows.size());
        addLines(fullRows.size());
        updateLevel();
        timeAttack.triggerFreezeIfNeeded(hasTimeBlock);
    }

    // --- Score & level ---

    private void addScore(int linesCleared) {
        int bonus = switch (linesCleared) {
            case 1 -> 100; case 2 -> 300; case 3 -> 500; case 4 -> 800; default -> 0;
        };
        int newScore = gameContext.getScore() + bonus;
        gameContext.setScore(newScore);
        scoreLabel.setText(String.valueOf(newScore));
        SoundManager.getInstance().playSE(SoundType.SCORE);
    }

    private void addLines(int count) {
        int newLines = gameContext.getLines() + count;
        gameContext.setLines(newLines);
        linesLabel.setText(String.valueOf(newLines));
        timeAttack.applyBonus(count);
    }

    private void updateLevel() {
        int newLevel = Math.min(gameContext.getLines() / 10 + 1, 10);
        if (newLevel != gameContext.getLevel()) {
            gameContext.setLevel(newLevel);
            levelLabel.setText(String.valueOf(newLevel));
            long base = (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) ? 300_000_000L : 500_000_000L;
            fallIntervalNs = Math.max(100_000_000L, base - (newLevel - 1) * 40_000_000L);
            renderer.onLevelUp(newLevel);
        }
    }

    private void refreshLabels() {
        scoreLabel.setText(String.valueOf(gameContext.getScore()));
        levelLabel.setText(String.valueOf(gameContext.getLevel()));
        linesLabel.setText(String.valueOf(gameContext.getLines()));
    }

    // --- Input ---

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (isGameOver) return;
        KeyCode code = event.getCode();
        if (code == KeyCode.P) { handlePause(); event.consume(); return; }
        if (freezeUntil > 0) { event.consume(); return; }
        switch (code) {
            case LEFT, A -> {
                mouseTargetColumn = -1;
                if (boardEngine.canMove(currentPiece, -1, 0, currentPiece.getRotation(), suspendedPieces))
                    currentPiece.setX(currentPiece.getX() - 1);
            }
            case RIGHT, D -> {
                mouseTargetColumn = -1;
                if (boardEngine.canMove(currentPiece, 1, 0, currentPiece.getRotation(), suspendedPieces))
                    currentPiece.setX(currentPiece.getX() + 1);
            }
            case DOWN -> {
                if (!timeAttack.isFreezeActive && hardMode.blackoutState != HardModeHandler.BlackoutState.BLACKOUT)
                    hardDrop();
            }
            case S -> {
                if (!timeAttack.isFreezeActive && hardMode.blackoutState != HardModeHandler.BlackoutState.BLACKOUT)
                    softDropping = true;
            }
            case UP, W -> {
                if (tryRotateWithWallKick()) {
                    renderer.onRotate();
                    renderer.onRotationArc(currentPiece, Constants.BLOCK_SIZE);
                }
            }
            case SPACE -> {
                if (timeAttack.isFreezeActive) { suspendCurrentPiece(); return; }
                if (hardMode.blackoutState != HardModeHandler.BlackoutState.BLACKOUT) hardDrop();
            }
            case P -> {} // handled above
            case B -> useBombSkill();
            case C, SHIFT -> holdCurrentPiece();
            case ESCAPE -> handleExit();
            default -> { return; }
        }
        event.consume();
    }

    private void handleMouseMoved(MouseEvent event) {
        if (gamePaused || isGameOver) return;
        mouseTargetColumn = (int) (event.getX() / Constants.BLOCK_SIZE);
        event.consume();
    }

    private void applyMouseTarget() {
        if (mouseTargetColumn < 0) return;
        int targetX = getTargetPieceX(mouseTargetColumn);
        int currentX = currentPiece.getX();
        if (targetX == currentX) return;
        int step = Integer.compare(targetX, currentX);
        if (boardEngine.canMove(currentPiece, step, 0, currentPiece.getRotation(), suspendedPieces))
            currentPiece.setX(currentX + step);
    }

    private void handleMouseClicked(MouseEvent event) {
        if (gamePaused || isGameOver) return;
        if (freezeUntil > 0) { event.consume(); return; }
        gamePane.requestFocus();
        if (event.getButton() == MouseButton.PRIMARY) {
            if (timeAttack.isFreezeActive) { suspendCurrentPiece(); }
            else if (hardMode.blackoutState != HardModeHandler.BlackoutState.BLACKOUT) hardDrop();
            event.consume();
        } else if (event.getButton() == MouseButton.SECONDARY) {
            if (tryRotateWithWallKick()) {
                renderer.onRotate();
                renderer.onRotationArc(currentPiece, Constants.BLOCK_SIZE);
            }
            event.consume();
        }
    }

    private boolean tryRotateWithWallKick() {
        int nextRotation = (currentPiece.getRotation() + 1) % 4;
        for (int dx : new int[]{0, -1, 1, -2, 2}) {
            if (boardEngine.canMove(currentPiece, dx, 0, nextRotation, suspendedPieces)) {
                currentPiece.setX(currentPiece.getX() + dx);
                currentPiece.setRotation(nextRotation);
                return true;
            }
        }
        return false;
    }

    private int getTargetPieceX(int targetColumn) {
        int clampedTarget = Math.max(0, Math.min(Constants.BOARD_WIDTH - 1, targetColumn));
        int rotation = currentPiece.getRotation();
        int[][] shape = currentPiece.getType().getShape(rotation);

        int minCol = 4;
        int maxCol = -1;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);
                }
            }
        }

        if (maxCol < 0)
            return currentPiece.getX();

        int centerCol = (minCol + maxCol) / 2;
        int desiredX = clampedTarget - centerCol;

        int minX = -minCol;
        int maxX = Constants.BOARD_WIDTH - 1 - maxCol;
        return Math.max(minX, Math.min(maxX, desiredX));
    }

    private void handlePause() {
        gamePaused = !gamePaused;
        if (timeAttackTimeline != null) {
            if (gamePaused) {
                timeAttackTimeline.pause();
            } else {
                timeAttackTimeline.play();
            }
        }
    }

    private void handleExit() {
        System.out.println("Exiting to main menu");
        stopRandomBlockIfNeeded(currentPiece);
        stopTimeAttackTimer();
        stopFreezeTimer();

        SoundManager.getInstance().stopLooping();

        if (gameLoop != null) {
            gameLoop.stop();
        }
        gameContext.reset();
        sceneManager.clearSceneCache();
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }

    // ── Score popups ──────────────────────────────────────────────────────────

    private static class ScorePopup {
        String text;
        double x, y, vx, vy, life;
        Color color;
        int fontSize;
    }

    private final java.util.List<ScorePopup> scorePopups = new java.util.ArrayList<>();

    private void renderTopLayer() {
        GraphicsContext gc = gameGc;
        if (comboFloatAlpha > 0) {
            double cx = comboFloatX + (rng.nextDouble() - 0.5) * 2 * comboShakeAmount;
            double cy = comboFloatY + (rng.nextDouble() - 0.5) * 2 * comboShakeAmount * 0.4;
            double flash = (Math.sin(comboFloatPhase * 3.0) + 1.0) / 2.0;
            Color drawColor = comboColor.interpolate(Color.WHITE, flash);
            int fontSize = (int) Math.min(75, 32 + comboDisplay * 7);
            gc.setGlobalAlpha(comboFloatAlpha);
            gc.setFont(Font.font("VT323", FontWeight.BOLD, fontSize));
            gc.setFill(drawColor);
            gc.fillText("COMBO x" + comboDisplay, cx, cy);
            gc.setGlobalAlpha(1.0);
        }
        renderScorePopups();
    }

    private void renderScorePopups() {
        if (scorePopups.isEmpty())
            return;
        GraphicsContext gc = gameGc;
        for (ScorePopup p : scorePopups) {
            gc.save();
            gc.setGlobalAlpha(Math.max(0, p.life));
            gc.setFont(Font.font("VT323", FontWeight.BOLD, p.fontSize));
            gc.setFill(p.color);
            gc.fillText(p.text, p.x, p.y);
            gc.restore();
        }
    }

    private void emitScorePopup(int cleared, int avgRow) {
        int bonus = switch (cleared) {
            case 1 -> 100;
            case 2 -> 300;
            case 3 -> 500;
            case 4 -> 800;
            default -> 0;
        };
        if (bonus == 0)
            return;
        double speed = 90 + rng.nextDouble() * 40;
        double rad = Math.toRadians(30 + rng.nextDouble() * 120);
        ScorePopup p = new ScorePopup();
        p.text = "+" + bonus;
        p.x = 8;
        p.y = avgRow * Constants.BLOCK_SIZE - 4;
        p.vx = Math.cos(rad) * speed;
        p.vy = -Math.sin(rad) * speed;
        p.life = 1.0;
        p.fontSize = 20 + cleared * 3;
        p.color = switch (cleared) {
            case 4 -> Color.web("#00ffff");
            case 3 -> Color.web("#ffaa00");
            default -> Color.web("#ffffff");
        };
        scorePopups.add(p);
    }

    // Base background color components for gamePane (#0f0d1a)
    private static final double BG_R = 0x0f / 255.0;
    private static final double BG_G = 0x0d / 255.0;
    private static final double BG_B = 0x1a / 255.0;
    private static final String PANE_BASE_STYLE = "-fx-padding: 20; -fx-spacing: 20; -fx-alignment: center;";

    private void render() {
        // Swap hard-mode UI panels to dark assets during blackout
        if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
            boolean inBlackout = blackoutState == BlackoutState.BLACKOUT;
            var classes = gamePane.getStyleClass();
            if (inBlackout && !classes.contains("blackout")) {
                classes.add("blackout");
                if (mainFrameView != null)
                    mainFrameView.setImage(darkMainImage);
            } else if (!inBlackout && classes.contains("blackout")) {
                classes.remove("blackout");
                if (mainFrameView != null)
                    mainFrameView.setImage(hardMainImage);
            }
        }

        if (gameContext.getGameMode() != GameContext.GameMode.HARD_MODE) {
            if (flashIntensity > 0) {
                double r = BG_R + flashIntensity * (1.0 - BG_R);
                double g = BG_G + flashIntensity * (1.0 - BG_G);
                double b = BG_B + flashIntensity * (1.0 - BG_B);
                String hex = String.format("#%02x%02x%02x", (int) (r * 255), (int) (g * 255), (int) (b * 255));
                gamePane.setStyle("-fx-background-color: " + hex + "; " + PANE_BASE_STYLE);
            } else {
                gamePane.setStyle("-fx-background-color: #0f0d1a; " + PANE_BASE_STYLE);
            }
        }

        drawGameBoard();

        if (glitchExplosionEffect != null)
            glitchExplosionEffect.render(gameGc);

        // --- XỬ LÝ RENDER THEO THỨ TỰ LỚP (Layering) ---
        if (glitchTearEffect != null) {
            vfxGc.clearRect(0, 0, vfxCanvas.getWidth(), vfxCanvas.getHeight());
            glitchTearEffect.render(gameGc, gameCanvas.getWidth(), gameCanvas.getHeight());
            double wa = glitchTearEffect.getWhiteoutAlpha();
            if (wa > 0) {
                flashIntensity = wa;
                vfxGc.setFill(Color.color(1, 1, 1, wa));
                vfxGc.fillRect(0, 0, vfxCanvas.getWidth(), vfxCanvas.getHeight());
            }
        } else {
            if (levelUpEffect != null)
                levelUpEffect.render(gameGc, gameCanvas.getWidth(), gameCanvas.getHeight());

            particleSystem.render(gameGc);
            particleSystem.renderBeams(vfxGc);

            if (borderPulseEffect != null)
                borderPulseEffect.render(gameGc, vfxGc, gameCanvas.getWidth(), gameCanvas.getHeight(), VFX_MARGIN);
        }
        renderTopLayer();
        drawHoldBlock();
        drawNextBlocks();
        updateLightBulb();

        // Flavour text render
        if (lightsOutMode) {
            // Vibrate the whole line
            if (flavourHBox != null) {
                flavourHBox.setTranslateX((rng.nextDouble() - 0.5) * 8);
                flavourHBox.setTranslateY((rng.nextDouble() - 0.5) * 5);
            }
            // Per-char glitch: random opacity drops and color flashes
            for (javafx.scene.control.Label l : flavourChars) {
                if (rng.nextDouble() < 0.12) {
                    l.setOpacity(rng.nextDouble() < 0.4 ? 0.0 : 1.0);
                } else {
                    l.setOpacity(1.0);
                }
                String col = rng.nextDouble() < 0.06
                        ? (rng.nextBoolean() ? "#ffffff" : "#880000")
                        : "#FF0000";
                l.setStyle("-fx-font-family: 'VT323'; -fx-font-size: 40; -fx-text-fill: " + col + ";");
            }
        } else if (darknessLoomsMode) {
            if (flavourHBox != null) {
                flavourHBox.setTranslateX(0);
                flavourHBox.setTranslateY(0);
            }
            int nVisible = Math.min(flavourChars.size(), (int) (flavourTypeElapsed / (FLAVOUR_CHAR_DELAY * 1.8)));
            for (int i = 0; i < flavourChars.size(); i++) {
                javafx.scene.control.Label l = flavourChars.get(i);
                boolean visible = i < nVisible;
                // Subtle flicker on revealed chars
                if (visible && rng.nextDouble() < 0.04)
                    l.setOpacity(0.4 + rng.nextDouble() * 0.3);
                else
                    l.setOpacity(visible ? 1.0 : 0.0);
                if (visible)
                    l.setTranslateY(Math.sin(flavourWaveTime * 1.2 + i * 0.45) * 5.0);
            }
        } else {
            if (flavourHBox != null) {
                flavourHBox.setTranslateX(0);
                flavourHBox.setTranslateY(0);
            }
            int nVisible = Math.min(flavourChars.size(), (int) (flavourTypeElapsed / FLAVOUR_CHAR_DELAY));
            for (int i = 0; i < flavourChars.size(); i++) {
                javafx.scene.control.Label l = flavourChars.get(i);
                boolean visible = i < nVisible;
                l.setOpacity(visible ? 1.0 : 0.0);
                if (visible)
                    l.setTranslateY(Math.sin(flavourWaveTime * 2.5 + i * 0.45) * 2.5);
            }
        }

        renderStartupGlitch();
    }

    private void renderStartupGlitch() {
        if (startupGlitchElapsed >= STARTUP_GLITCH_DUR || startupGc == null)
            return;
        double t = startupGlitchElapsed / STARTUP_GLITCH_DUR;
        double alpha = (1.0 - t * t) * 0.55;
        double w = startupCanvas.getWidth();
        double h = startupCanvas.getHeight();

        startupGc.clearRect(0, 0, w, h);

        // clean scanline bands — each drifts sideways at its own phase
        int bands = 6 + rng.nextInt(5);
        for (int i = 0; i < bands; i++) {
            double by = rng.nextDouble() * h;
            double bh = rng.nextDouble() * (h * 0.05) + 1;
            int pick = rng.nextInt(3);
            double cr = pick == 0 ? 0 : 1;
            double cg = pick == 0 ? 1 : 1;
            double cb = pick == 0 ? 1 : 0;
            double baseA = pick == 0 ? 0.5 : pick == 1 ? 0.4 : 0.3;
            double xOffset = Math.sin(t * Math.PI * 5 + i * 1.9) * w * 0.18;
            double pad = Math.abs(xOffset);
            startupGc.setFill(Color.color(cr, cg, cb, Math.min(1, alpha * baseA)));
            startupGc.fillRect(xOffset - pad, by, w + pad * 2, bh);
            // a couple of bright accent pixels on the band
            for (int j = 0; j < 3; j++) {
                double hx = rng.nextDouble() * w + xOffset;
                startupGc.setFill(Color.color(1, 1, 1, alpha * (0.6 + rng.nextDouble() * 0.4)));
                startupGc.fillRect(hx, by, rng.nextDouble() * 6 + 2, bh);
            }
        }
        // light pixel scatter
        int pixels = (int) (250 * (1.0 - t));
        for (int i = 0; i < pixels; i++) {
            double px = rng.nextDouble() * w;
            double py = rng.nextDouble() * h;
            startupGc.setFill(Color.color(1, 1, 1, alpha * (rng.nextDouble() * 0.6 + 0.2)));
            startupGc.fillRect(px, py, 2, 2);
        }
        // white flashes at t=0.0, 0.22, 0.48 — sharp spike then fast decay
        double[] flashTimes = { 0.0, 0.14, 0.28 };
        double[] flashPeaks = { 0.90, 0.42, 0.42 };
        double flashHalf = 0.07;
        double totalFlash = 0;
        for (int fi = 0; fi < flashTimes.length; fi++) {
            double d = Math.abs(t - flashTimes[fi]);
            if (d < flashHalf)
                totalFlash = Math.max(totalFlash, (1.0 - d / flashHalf) * flashPeaks[fi]);
        }
        if (totalFlash > 0) {
            startupGc.setFill(Color.color(1, 1, 1, totalFlash));
            startupGc.fillRect(0, 0, w, h);
        }
    }

    public void drawGameBoard() {
        GraphicsContext gc = gameGc;
        double w = gameCanvas.getWidth();
        double h = gameCanvas.getHeight();
        int cs = Constants.BLOCK_SIZE;

        // Background
        boolean hardMode = gameContext.getGameMode() == GameContext.GameMode.HARD_MODE;
        gc.setFill(Color.web(hardMode ? "#280C00" : "#0f0d1a"));
        gc.fillRect(0, 0, w, h);

        // Grid lines
        gc.setStroke(Color.web(hardMode ? "#3d1500" : "#2b2740"));
        gc.setLineWidth(0.5);
        for (int x = 0; x <= Constants.BOARD_WIDTH; x++)
            gc.strokeLine(x * cs, 0, x * cs, h);
        for (int y = 0; y <= Constants.BOARD_HEIGHT; y++)
            gc.strokeLine(0, y * cs, w, y * cs);

        // Border — hidden in hard mode (wooden frame provides it)
        if (gameContext.getGameMode() != GameContext.GameMode.HARD_MODE) {
            gc.setStroke(Color.web("#00ff00"));
            gc.setLineWidth(2);
            gc.strokeRect(0, 0, w, h);
        }

        // HARD MODE
        boolean isDark = false;
        if (blackoutState == BlackoutState.FLICKER) {
            // chớp nhanh: 10 lần / giây
            isDark = ((int) (blackoutFlickerTimer * 10)) % 2 == 0;
        }
        if (blackoutState == BlackoutState.BLACKOUT) {
            isDark = true;
        }
        if (!isDark) {
            // Locked cells
            for (int y = 0; y < Constants.BOARD_HEIGHT; y++) {
                for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                    if (board[y][x] != 0) {
                        if (board[y][x] == TIME_BLOCK_ID) {
                            drawTimeBlockCell(gc, x, y, cs);
                            continue;
                        }
                        TetrominoType t = idToType(board[y][x]);
                        if (t != null)
                            drawCell(gc, x, y, t.getColor(), 1.0);
                    }
                }
            }
        }

        // Locked cells
        for (int y = 0; y < Constants.BOARD_HEIGHT; y++) {
            for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                if (board[y][x] != 0) {
                    if (board[y][x] == TIME_BLOCK_ID) {
                        drawTimeBlockCell(gc, x, y, cs);
                        continue;
                    }
                    TetrominoType t = idToType(board[y][x]);
                    if (t == null)
                        continue;
                    if (t == TetrominoType.BOMB)
                        gc.drawImage(bombSprite, x * cs, y * cs, cs, cs);
                    else
                        drawCell(gc, x, y, t.getColor(), 1.0);
                }
            }
        }

        // Flash cleared rows white during Tetris freeze
        if (frozenRowFlash != null && freezeUntil > 0) {
            gc.setFill(Color.WHITE);
            for (int row : frozenRowFlash) {
                gc.fillRect(0, row * cs, w, cs);
            }
        }

        // White flash on board at moment of game over
        if (gameOverFlashAlpha > 0) {
            gc.setFill(Color.color(1, 1, 1, gameOverFlashAlpha));
            gc.fillRect(0, 0, w, h);
        }

        // Ghost + current piece hidden during game over (glitch effect takes over)
        if (isGameOver)
            return;

        // Ghost
        if (freezeUntil == 0) {
            int drop = getDropDistance();
            int[][] ghostShape = currentPiece.getType().getShape(currentPiece.getRotation());
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    if (ghostShape[row][col] == 1) {
                        drawCell(gc,
                                currentPiece.getX() + col,
                                currentPiece.getY() + row + drop,
                                currentPiece.getType().getColor().deriveColor(0, 1, 1, 0.25),
                                1.0);
                    }
                }
            }
        }

        renderSuspendedPieces(gc, cs);

        // Current piece - apply rotation pulse brightness
        // Current piece - always drawn on top, visible even during blackout
        int[][] shape = currentPiece.getType().getShape(currentPiece.getRotation());
        int minCol = 4, maxCol = -1;
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++)
                if (shape[row][col] == 1) { minCol = Math.min(minCol, col); maxCol = Math.max(maxCol, col); }
        if (maxCol < 0) return currentPiece.getX();
        int centerCol = (minCol + maxCol) / 2;
        int desired = clamped - centerCol;
        return Math.max(-minCol, Math.min(Constants.BOARD_WIDTH - 1 - maxCol, desired));
    }

    // --- Actions ---

    private void hardDrop() {
        dropStartRow = currentPiece.getY();
        renderer.onHardDrop();
        int drop = boardEngine.getDropDistance(currentPiece, suspendedPieces);
        currentPiece.setY(currentPiece.getY() + drop);
        lockAndSpawn();
    }

    private void holdCurrentPiece() {
        if (!canHold || currentPiece == null || isGameOver || currentPiece.getType() == TetrominoType.BOMB) return;
        canHold = false;
        stopRandomBlockIfNeeded(currentPiece);
        TetrominoType currentType = currentPiece.getType();
        if (holdType == null) {
            holdType = currentType;
            spawnPiece();
        } else {
            drawPreview(holdGc, holdBlockCanvas, null);
        }
    }

    public void drawNextBlocks() {
        java.util.Iterator<Piece> it = nextQueue.iterator();
        if (nextGc1 != null)
            drawPreview(nextGc1, nextBlockCanvas1, it.hasNext() ? it.next() : null);
        else if (it.hasNext())
            it.next();
        if (nextGc2 != null)
            drawPreview(nextGc2, nextBlockCanvas2, it.hasNext() ? it.next() : null);
        else if (it.hasNext())
            it.next();
        if (nextGc3 != null)
            drawPreview(nextGc3, nextBlockCanvas3, it.hasNext() ? it.next() : null);
        else if (it.hasNext())
            it.next();
    }

    private void drawPreview(GraphicsContext gc, Canvas canvas, Piece piece) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.web(
                blackoutState == BlackoutState.BLACKOUT ? "#000000"
                        : gameContext.getGameMode() == GameContext.GameMode.HARD_MODE ? "#280C00" : "#0f0d1a"));
        gc.fillRect(0, 0, w, h);
        // HARD MODE
        if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
            drawQuestionMark(gc, canvas);
            return;
        }
        // Xóa nền và vẽ viền

        if (piece == null)
            return;

        boolean isRandomPreview = piece instanceof RandomBlock;
        TetrominoType type = piece.getType();
        int[][] shape = type.getShape(0); // Luôn vẽ ở góc xoay mặc định

        // 1. Tính toán vùng bao (Bounding Box) để căn giữa khối gạch
        int minRow = 4, maxRow = -1, minCol = 4, maxCol = -1;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);
                }
            }
        }

        if (maxRow < 0 || maxCol < 0)
            return;

        // 2. Tính toán vị trí vẽ để khối luôn nằm chính giữa Canvas
        int previewCellSize = 24; // Kích thước ô nhỏ hơn cho vùng Preview
        int pieceWidth = (maxCol - minCol + 1) * previewCellSize;
        int pieceHeight = (maxRow - minRow + 1) * previewCellSize;
        double offsetX = (w - pieceWidth) / 2.0;
        double offsetY = (h - pieceHeight) / 2.0;

        // 3. Vẽ từng ô của khối
        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                if (shape[row][col] == 1) {
                    double px = offsetX + (col - minCol) * previewCellSize;
                    double py = offsetY + (row - minRow) * previewCellSize;
                    if (type == TetrominoType.BOMB)
                        gc.drawImage(bombSprite, px, py, previewCellSize, previewCellSize);
                    else
                        drawCellAtPixel(gc, px, py, previewCellSize, type.getColor(), 1.0);
                }
            }
        }

        if (isRandomPreview) {
            gc.setStroke(Color.web("#00e5ff"));
            gc.setLineWidth(2.0);
            for (int row = minRow; row <= maxRow; row++) {
                for (int col = minCol; col <= maxCol; col++) {
                    if (shape[row][col] == 1) {
                        double px = offsetX + (col - minCol) * previewCellSize;
                        double py = offsetY + (row - minRow) * previewCellSize;
                        gc.strokeRect(px + 1, py + 1, previewCellSize - 2, previewCellSize - 2);
                    }
                }
            }
        }
    }

    // Helper: vẽ 1 ô trên gameGc
    private void drawCell(GraphicsContext gc, int x, int y, Color color, double opacity) {
        drawCell(gc, x, y, color, opacity, gc, Constants.BLOCK_SIZE);
    }

    private void drawCell(GraphicsContext gc, int x, int y, Color color,
            double opacity, GraphicsContext target, int cs) {
        double px = x * cs;
        double py = y * cs;
        target.setFill(color.deriveColor(0, 1, 1, opacity));
        target.fillRect(px, py, cs, cs);
        target.setStroke(Color.web("#111111"));
        target.setLineWidth(1);
        target.strokeRect(px, py, cs, cs);
    }

    private void drawCellAtPixel(GraphicsContext target, double px, double py,
            int size, Color color, double opacity) {
        target.setFill(color.deriveColor(0, 1, 1, opacity));
        target.fillRect(px, py, size, size);
        target.setStroke(Color.web("#111111"));
        target.setLineWidth(1);
        target.strokeRect(px, py, size, size);
    }

    private void drawTimeBlockCell(GraphicsContext gc, int x, int y, int cs) {
        drawCell(gc, x, y, TIME_BLOCK_COLOR, 1.0);
        gc.setStroke(Color.web("#e6ffff"));
        gc.setLineWidth(1.0);
        double px = x * cs + 4;
        double py = y * cs + 4;
        gc.strokeOval(px, py, cs - 8, cs - 8);
        gc.strokeLine(x * cs + cs / 2.0, y * cs + 6, x * cs + cs / 2.0, y * cs + cs / 2.0);
        gc.strokeLine(x * cs + cs / 2.0, y * cs + cs / 2.0, x * cs + cs - 8, y * cs + cs / 2.0);
    }

    private void drawRandomBlockIndicator(Piece piece, GraphicsContext gc, int cs) {
        int[][] shape = piece.getType().getShape(piece.getRotation());
        gc.setStroke(Color.web("#00e5ff"));
        gc.setLineWidth(2.2);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    double px = (piece.getX() + col) * cs;
                    double py = (piece.getY() + row) * cs;
                    gc.strokeRect(px + 1, py + 1, cs - 2, cs - 2);
                }
            }
        }
    }

    private void renderSuspendedPieces(GraphicsContext gc, int cs) {
        if (suspendedPieces.isEmpty()) {
            return;
        }
        for (Piece piece : suspendedPieces) {
            int[][] shape = piece.getType().getShape(piece.getRotation());
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    if (shape[row][col] == 1) {
                        int px = piece.getX() + col;
                        int py = piece.getY() + row;
                        if (piece.getType() == TetrominoType.BOMB) {
                            gc.drawImage(bombSprite, px * cs, py * cs, cs, cs);
                        } else {
                            drawCell(gc, px, py, piece.getType().getColor(), 1.0);
                        }
                    }
                }
            }
        }
    }

    private int typeId(TetrominoType type) {
        return switch (type) {
            case I -> 1;
            case O -> 2;
            case T -> 3;
            case S -> 4;
            case Z -> 5;
            case J -> 6;
            case L -> 7;
            case BOMB -> 8;
            case TRANSPARENT -> 9;
        };
    }

    private TetrominoType idToType(int id) {
        return switch (id) {
            case 1 -> TetrominoType.I;
            case 2 -> TetrominoType.O;
            case 3 -> TetrominoType.T;
            case 4 -> TetrominoType.S;
            case 5 -> TetrominoType.Z;
            case 6 -> TetrominoType.J;
            case 7 -> TetrominoType.L;
            case 8 -> TetrominoType.BOMB;
            case 9 -> TetrominoType.TRANSPARENT;
            default -> null;
        };
    }

    public void gameOver() {
        stopRandomBlockIfNeeded(currentPiece);
        stopTimeAttackTimer();
        stopFreezeTimer();
        if (gameLoop != null)
            gameLoop.stop();
            sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
        }
    }

    private void useBombSkill() {
        if (isGameOver || gamePaused || bombsRemaining <= 0 || currentPiece == null
                || currentPiece.getType() == TetrominoType.BOMB) return;
        bombsRemaining--;
        stopRandomBlockIfNeeded(currentPiece);
        currentPiece = new Piece(TetrominoType.BOMB, currentPiece.getX(), currentPiece.getY());
        if (bombsLabel != null) bombsLabel.setText("💣 x" + bombsRemaining);
        if (bombInventoryBox != null) {
            bombInventoryBox.setStyle(bombInventoryBaseStyle
                    + "; -fx-border-color: #ffcc00; -fx-border-width: 2;"
                    + " -fx-effect: dropshadow(gaussian, #ffcc00, 10, 0.6, 0, 0);");
            PauseTransition reset = new PauseTransition(Duration.millis(160));
            reset.setOnFinished(e -> bombInventoryBox.setStyle(bombInventoryBaseStyle));
            reset.play();
        }
    }

    private void handlePause() {
        gamePaused = !gamePaused;
        if (gamePaused) timeAttack.pause(); else timeAttack.resume();
    }

    private void handleExit() {
        stopRandomBlockIfNeeded(currentPiece);
        timeAttack.stopAll();
        SoundManager.getInstance().stopLooping();
        if (gameLoop != null) gameLoop.stop();
        gameContext.reset();
        sceneManager.clearSceneCache();
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }

    private void handleGameOver() {
        isGameOver = true;
        stopRandomBlockIfNeeded(currentPiece);
        timeAttack.stopAll();
        if (gameLoop != null) gameLoop.stop();
        sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
    }

    private void stopTimeAttackTimer() {
        if (timeAttackTimeline != null) {
            timeAttackTimeline.stop();
        }
    }

    private void stopFreezeTimer() {
        isFreezeActive = false;
        if (freezeTimeline != null) {
            freezeTimeline.stop();
        }
        if (freezeLabel != null) {
            freezeLabel.setVisible(false);
            freezeLabel.setManaged(false);
        }
    }

    private void drawQuestionMark(GraphicsContext gc, Canvas canvas) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.web("#00ff00")); // cùng màu viền cho đồng bộ
        gc.setFont(Font.font("VT323", FontWeight.BOLD, 40));

        String text = "?";

        // canh giữa tương đối
        gc.fillText(text, w / 2 - 8, h / 2 + 12);
        gc.setEffect(new DropShadow(10, Color.web("#00ff00")));
    }

    private void updateLightBulb() {
        if (!lightBulbView.isVisible())
            return;
        boolean off = switch (blackoutState) {
            case BLACKOUT -> true;
            case FLICKER, POST_FLICKER -> ((int) blackoutFlickerTimer) % 2 == 0;
            default -> false;
        };
        lightBulbView.setImage(off ? lightOffImage : lightOnImage);
    }

    private void buildFlavourChars() {
        if (flavourLabel == null)
            return;
        flavourLabel.setVisible(false);
        flavourPane = (javafx.scene.layout.StackPane) flavourLabel.getParent();
        if (flavourPane == null)
            return;
        flavourHBox = new javafx.scene.layout.HBox(0);
        flavourHBox.setAlignment(javafx.geometry.Pos.CENTER);
        flavourPane.getChildren().add(flavourHBox);
        rebuildFlavourChars(flavourLabel.getText());
    }

    private void rebuildFlavourChars(String text) {
        darknessLoomsMode = false;
        rebuildFlavourChars(text, null);
    }

    private void rebuildFlavourChars(String text, String hexColor) {
        if (flavourHBox == null)
            return;
        flavourChars.clear();
        flavourHBox.getChildren().clear();
        for (char c : text.toCharArray()) {
            javafx.scene.control.Label l = new javafx.scene.control.Label(String.valueOf(c));
            l.getStyleClass().add("hard-flavour-text");
            if (hexColor != null)
                l.setStyle("-fx-text-fill: " + hexColor + ";");
            l.setOpacity(0);
            flavourChars.add(l);
            flavourHBox.getChildren().add(l);
        }
        flavourTypeElapsed = 0;
    }

    private boolean trySetFlavour(String[] pool, double threshold) {
        if (lightsOutMode || flavourCooldown > threshold || flavourHBox == null)
            return false;
        rebuildFlavourChars(pool[rng.nextInt(pool.length)]);
        flavourCooldown = FLAVOUR_BASE_CD;
        placesSinceFlavour = 0;
        return true;
    }

    private int getStackTopRow() {
        for (int r = 0; r < Constants.BOARD_HEIGHT; r++)
            for (int c = 0; c < Constants.BOARD_WIDTH; c++)
                if (board[r][c] != 0)
                    return r;
        return Constants.BOARD_HEIGHT;
    }

    private void setLightsOut() {
        if (flavourHBox == null)
            return;
        lightsOutMode = true;
        darknessLoomsMode = false;
        flavourCooldown = Double.MAX_VALUE;
        flavourChars.clear();
        flavourHBox.getChildren().clear();
        for (char c : "LIGHTS OUT".toCharArray()) {
            javafx.scene.control.Label l = new javafx.scene.control.Label(String.valueOf(c));
            l.setStyle("-fx-font-family: 'VT323'; -fx-font-size: 40; -fx-text-fill: #FF0000;");
            l.setOpacity(1.0);
            flavourChars.add(l);
            flavourHBox.getChildren().add(l);
        }
    }

    private void clearLightsOut() {
        lightsOutMode = false;
        if (flavourHBox != null) {
            flavourHBox.setTranslateX(0);
            flavourHBox.setTranslateY(0);
        }
        rebuildFlavourChars(FLAVOUR_POST_BLACKOUT[rng.nextInt(FLAVOUR_POST_BLACKOUT.length)]);
        flavourCooldown = FLAVOUR_BASE_CD;
    }

    private void updateBlackout(double dt) {
        blackoutTimer += dt;

        switch (blackoutState) {

            case NORMAL -> {
                if (blackoutTimer >= 20.0) {
                    blackoutTimer = 0;
                    blackoutState = BlackoutState.FLICKER;
                    blackoutDuration = 0.3;
                    blackoutFlickerTimer = 0;
                    darknessLoomsMode = true;
                    rebuildFlavourChars("Darkness looms.", "#40A3FF");
                    flavourCooldown = FLAVOUR_BASE_CD;
                }
            }

            case FLICKER -> {
                blackoutDuration -= dt;
                blackoutFlickerTimer += dt * 10.0;
                if (blackoutDuration <= 0) {
                    blackoutState = BlackoutState.PRE_BLACKOUT;
                    blackoutDuration = 5.0;
                }
            }

            case PRE_BLACKOUT -> {
                blackoutDuration -= dt;
                if (blackoutDuration <= 0) {
                    blackoutState = BlackoutState.BLACKOUT;
                    blackoutDuration = 5.0;
                    blackoutDropTimer = 0;
                    SoundManager.getInstance().pauseMusic();
                    setLightsOut();
                }
            }

            case BLACKOUT -> {
                blackoutDuration -= dt;
                if (blackoutDuration <= 0) {
                    blackoutState = BlackoutState.POST_FLICKER;
                    blackoutDuration = 0.3;
                    blackoutFlickerTimer = 0;
                    SoundManager.getInstance().resumeMusic();
                    clearLightsOut();
                }
            }

            case POST_FLICKER -> {
                blackoutDuration -= dt;
                blackoutFlickerTimer += dt * 10.0;
                if (blackoutDuration <= 0) {
                    blackoutState = BlackoutState.NORMAL;
                    blackoutTimer = 0;
                }
            }
        }
    }
}