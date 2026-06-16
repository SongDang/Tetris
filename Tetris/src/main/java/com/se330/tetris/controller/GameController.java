package com.se330.tetris.controller;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.se330.tetris.game.BoardEngine;
import com.se330.tetris.game.BulbSwingEffect;
import com.se330.tetris.game.Piece;
import com.se330.tetris.game.RandomBlock;
import com.se330.tetris.game.TetrominoType;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;
import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.Constants;
import com.se330.tetris.util.SoundType;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

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
    @FXML private javafx.scene.layout.AnchorPane settingsOverlay;
    @FXML private javafx.scene.layout.AnchorPane settingsPopupShell;
    @FXML private javafx.scene.layout.AnchorPane tutorialOverlay;
    @FXML private javafx.scene.layout.AnchorPane tutorialPopupShell;
    @FXML private Label tutorialTitleLabel;
    @FXML private Label tutorialBodyLabel;
    @FXML private Canvas bgEffectCanvas;
    @FXML private SettingsController settingsPopupController;

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
    private BulbSwingEffect bulbSwing;

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
    private boolean settingsPausedGame = false;
    private boolean tutorialPausedGame = false;
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
    private List<int[]> pendingTimeBlockPositions = List.of();
    private boolean timeStopSoundPlayed = false;

    // --- Input ---
    private boolean softDropping = false;
    private boolean fuseLoopPlaying = false;
    private int mouseTargetColumn = -1;
    private int dropStartRow = -1;

    // --- Flavour text ---
    private static final String[] STANDARD_TIPS = {
            "soft drop [ S ]   •   hard drop [ SPACE ]",
            "hold a piece with [ C ] or [ SHIFT ]",
            "rotate with [ W ] or [ UP ]",
            "right-click to rotate   •   left-click to hard drop"
    };
    private static final String[] TIME_ATTACK_TIPS = {
            "hey, time's running out",
            "clear rows with time blocks to freeze the clock",
            "tetris gives the most time back",
            "use bomb skill [ B ] to clear the field"
    };
    private int flavourTipIndex = 0;
    private javafx.animation.Timeline flavourTimeline;

    // --- Scoring / combo ---
    private int comboCount = 0;

    // --- Bomb ---
    private int bombsRemaining = 3;
    private String bombInventoryBaseStyle = "";

    private javafx.scene.image.Image bombSprite;
    private javafx.scene.image.Image ghostSprite;

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext = GameContext.getInstance();
        gameContext.reset();
        Platform.runLater(() -> vfxCanvas.setLayoutY(gameCanvas.getBoundsInParent().getMinY()));

        loadImages();

        boardEngine = new BoardEngine(gameContext, rng);
        hardMode = new HardModeHandler(
                rng,
                () -> SoundManager.getInstance().pauseMusic(),
                () -> SoundManager.getInstance().resumeMusic());

        timeAttack = new TimeAttackHandler(
                gameContext, timeLabel, freezeLabel, bombsLabel, timePanel, bombInventoryBox,
                this::handleGameOver);

        timeAttack.setOnFreezeEnd(() -> timeStopSoundPlayed = false);

        renderer = new GameRenderer(
                gameCanvas,
                vfxCanvas,
                holdBlockCanvas,
                nextBlockCanvas1,
                nextBlockCanvas2,
                nextBlockCanvas3,
                gamePane,
                lightBulbView,
                mainFrameView,
                bombSprite,
                ghostSprite,
                lightOnImage,
                lightOffImage,
                hardMainImage,
                darkMainImage,
                gameContext,
                boardEngine,
                hardMode,
                rng);

        if (bgEffectCanvas != null) {
            renderer.setBgEffectCanvas(bgEffectCanvas);
            gamePane.widthProperty().addListener((obs, old, nw) -> bgEffectCanvas.setWidth(nw.doubleValue()));
            gamePane.heightProperty().addListener((obs, old, nw) -> bgEffectCanvas.setHeight(nw.doubleValue()));
            Platform.runLater(() -> {
                bgEffectCanvas.setWidth(gamePane.getWidth());
                bgEffectCanvas.setHeight(gamePane.getHeight());
            });
        }

        applyGameModeTheme();

        if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
            fallIntervalNs = 300_000_000L;
            if (lightBulbView != null) {
                lightBulbView.setImage(lightOnImage);
                lightBulbView.setVisible(true);
                bulbSwing = new BulbSwingEffect(lightBulbView);
            }
            Platform.runLater(() -> hardMode.buildFlavourChars(flavourLabel));
        }

        holdType = null;
        canHold = true;
        nextQueue.clear();
        for (int i = 0; i < 4; i++)
            nextQueue.addLast(randomPiece());
        spawnPiece();
        refreshLabels();

        if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
            hardMode.setupBombUI(bombsLabel, bombInventoryBox);
        }

        timeAttack.setup();

        if (settingsPopupController != null) {
            settingsPopupController.setScoreVisible(true);
            settingsPopupController.setCloseHandler(this::closePauseSettings);
            settingsPopupController.setQuitHandler(this::quitToMainMenuFromSettings);
        }
        if (settingsOverlay != null) {
            settingsOverlay.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    closePauseSettings();
                }
                event.consume();
            });
        }
        if (tutorialOverlay != null) {
            tutorialOverlay.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    closeTutorial();
                }
                event.consume();
            });
        }

        gamePane.setOnKeyPressed(this::handleKeyPressed);
        gamePane.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.S || e.getCode() == KeyCode.DOWN)
                softDropping = false;
        });
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
                startupCanvas.setWidth(n.getWidth());
                startupCanvas.setHeight(n.getHeight());
            });
        });

        startGameLoop();
    }

    private void loadImages() {
        bombSprite = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/bomb.png"));
        ghostSprite = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/GhostBlock.png"));
        lightOnImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/lightson.png"));
        lightOffImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/lightsoff.png"));
        hardMainImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/hardmain.png"));
        darkMainImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/darkmain.png"));
        standardMainImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/standardmain.png"));
        timeAttackMainImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/timeatkmain.png"));
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
                renderer.update(dt, gamePaused, isGameOver, currentPiece, freezeUntil, timeAttack.isFreezeActive);

                if (!gamePaused && !isGameOver && freezeUntil == 0) {
                    applyMouseTarget();
                    if (!timeAttack.isFreezeActive) {
                        if (hardMode.blackoutState == HardModeHandler.BlackoutState.BLACKOUT)
                            softDropping = false;
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
                        gamePaused, isGameOver, freezeUntil, frozenRowFlash, timeAttack.isFreezeActive);
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
                for (int r : rows)
                    rowList.add(r);
                Collections.sort(rowList, Collections.reverseOrder());
                renderer.triggerPostClearFlash(rows);
                boardEngine.clearRows(rowList);
                addScore(4);
                addLines(4);
                updateLevel();
                renderer.emitScorePopup(4, avgRow);
                if (hasTimeBlock && !pendingTimeBlockPositions.isEmpty() && !timeAttack.isFreezeActive) {
                    List<int[]> cells = pendingTimeBlockPositions;
                    pendingTimeBlockPositions = List.of();
                    if (!timeStopSoundPlayed) {
                        timeStopSoundPlayed = true;
                        SoundManager.getInstance().playSE(SoundType.TIME_STOP, 0.70f);
                    }
                    renderer.triggerPreFreezeCinematic(cells, () -> timeAttack.triggerFreezeIfNeeded(true));
                } else {
                    timeAttack.triggerFreezeIfNeeded(hasTimeBlock);
                }
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
            if (!fuseLoopPlaying) {
                SoundManager.getInstance().playLooping(SoundType.FUSE);
                fuseLoopPlaying = true;
            }
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
            if (comboLabel != null)
                comboLabel.setText("x" + comboCount);
            if (comboCount >= 2)
                renderer.onCombo(comboCount, avgRow, cs);

            if (cleared == 4) {
                if (gameContext.getGameMode() == GameContext.GameMode.TIME_ATTACK && hasTimeBlock && !timeAttack.isFreezeActive) {
                    List<int[]> tbCells = boardEngine.getTimeBlockCells(fullRows);
                    fullRows.sort(Collections.reverseOrder());
                    boardEngine.clearRows(fullRows);
                    addScore(4);
                    addLines(4);
                    updateLevel();
                    renderer.emitScorePopup(4, avgRow);
                    SoundManager.getInstance().playSE(SoundType.TETRIS);
                    if (!timeStopSoundPlayed) {
                        timeStopSoundPlayed = true;
                        SoundManager.getInstance().playSE(SoundType.TIME_STOP, 0.70f);
                    }
                    renderer.triggerPreFreezeCinematic(tbCells, () -> timeAttack.triggerFreezeIfNeeded(true));
                    canHold = true;
                    spawnAndCheckGameOver();
                    return;
                }

                fullRows.sort(Collections.reverseOrder());
                frozenRowFlash = fullRows.stream().mapToInt(Integer::intValue).toArray();
                pendingTetrisClear = frozenRowFlash;
                pendingTetrisClearHasTimeBlock = hasTimeBlock;
                pendingTimeBlockPositions = hasTimeBlock ? boardEngine.getTimeBlockCells(fullRows) : List.of();
                freezeUntil = System.nanoTime() + 700_000_000L;
                renderer.triggerTetrisFlash(frozenRowFlash);
                SoundManager.getInstance().playSE(SoundType.TETRIS);
                PauseTransition delay = new PauseTransition(Duration.millis(300));
                delay.setOnFinished(e -> SoundManager.getInstance().playSE(SoundType.TETRIS2));
                delay.play();
                if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
                    hardMode.trySetFlavourTetris();
                    if (bulbSwing != null)
                        bulbSwing.trigger(4);
                    renderer.triggerSideParticles(4);
                }
                return;
            }

            List<int[]> tbCells = hasTimeBlock ? boardEngine.getTimeBlockCells(fullRows) : List.of();
            fullRows.sort(Collections.reverseOrder());
            boardEngine.clearRows(fullRows);
            addScore(cleared);
            addLines(cleared);
            updateLevel();
            renderer.emitScorePopup(cleared, avgRow);
            if (hasTimeBlock && !timeAttack.isFreezeActive) {
                SoundManager.getInstance().playSE(SoundType.TIME_STOP);
                renderer.triggerPreFreezeCinematic(tbCells, () -> timeAttack.triggerFreezeIfNeeded(true));
            } else {
                timeAttack.triggerFreezeIfNeeded(false);
            }

            if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
                boolean flavSet = comboCount >= 2 && hardMode.trySetFlavourCombo();
                if (!flavSet)
                    hardMode.trySetFlavourClear();
                if (bulbSwing != null)
                    bulbSwing.trigger(cleared);
                renderer.triggerSideParticles(cleared);
            }
        } else {
            comboCount = 0;
            if (comboLabel != null)
                comboLabel.setText("x0");
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
                if (candidate.getType() != TetrominoType.BOMB) {
                    currentPiece = candidate;
                    break;
                }
                nextQueue.addLast(candidate);
            }
        }

        if (currentPiece.getType() == TetrominoType.TRANSPARENT) {
            SoundManager.getInstance().playSE(SoundType.TRANSPARENT_PIECE);
        }
        if (currentPiece instanceof RandomBlock) {
            SoundManager.getInstance().playSE(SoundType.RANDOM_PIECE);
        }

        currentPiece.setX(Constants.BOARD_WIDTH / 2 - 2);
        currentPiece.setY(0);
        nextQueue.addLast(randomPiece());
        startRandomBlockIfNeeded(currentPiece);
        if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE)
            hardMode.resetBlackoutOnSpawn();
    }

    private Piece randomPiece() {
        boolean isStandard = gameContext.getGameMode() == GameContext.GameMode.STANDARD;
        boolean isHardMode = gameContext.getGameMode() == GameContext.GameMode.HARD_MODE;
        if (bag.isEmpty()) {
            for (TetrominoType t : TetrominoType.values()) {
                if (t == TetrominoType.BOMB)
                    continue;
                if (t == TetrominoType.TRANSPARENT && (isStandard || (isHardMode && !hardMode.shouldSpawnTransparentInBag())))
                    continue;
                bag.add(t);
            }
            Collections.shuffle(bag);
        }
        TetrominoType type = bag.remove(0);
        double chance = isHardMode ? hardMode.getRandomBlockChance() : RANDOM_BLOCK_CHANCE;
        if (!isStandard && randomBlockEnabled && type != TetrominoType.BOMB && type != TetrominoType.TRANSPARENT && rng.nextDouble() < chance) {
            RandomBlock block = new RandomBlock(type, 0, 0, RANDOM_BLOCK_INTERVAL_MS);
            block.setTypeValidator((p, t) -> boardEngine.canPlaceType(p, t));
            return block;
        }
        return new Piece(type, 0, 0);
    }

    private void startRandomBlockIfNeeded(Piece piece) {
        if (piece instanceof RandomBlock rb)
            rb.startTimer();
    }

    private void stopRandomBlockIfNeeded(Piece piece) {
        if (piece instanceof RandomBlock rb)
            rb.lockBlock();
    }

    private void detonateBomb(int centerX, int centerY) {
        int cs = Constants.BLOCK_SIZE;
        List<BoardEngine.ClearedCell> cells = boardEngine.clearBombRadius(centerX, centerY);
        SoundManager.getInstance().stopLooping();
        fuseLoopPlaying = false;
        renderer.onBombExplode((centerX + 0.5) * cs, (centerY + 0.5) * cs, cs, cells);
        SoundManager.getInstance().playSE(SoundType.BOMB_EXPLODE);
        if (bulbSwing != null)
            bulbSwing.trigger(4);
        renderer.triggerSideParticles(4);
    }

    private void updateSuspendedFall() {
        if (suspendedPieces.isEmpty())
            return;
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
        if (fullRows.isEmpty())
            return;
        boolean hasTimeBlock = fullRows.stream().anyMatch(boardEngine::rowHasTimeBlock);
        List<int[]> tbCells = hasTimeBlock ? boardEngine.getTimeBlockCells(fullRows) : List.of();
        fullRows.sort(Collections.reverseOrder());
        boardEngine.clearRows(fullRows);
        comboCount = 0;
        if (comboLabel != null)
            comboLabel.setText("x0");
        addScore(fullRows.size());
        addLines(fullRows.size());
        updateLevel();
        if (hasTimeBlock && !timeAttack.isFreezeActive) {
            SoundManager.getInstance().playSE(SoundType.TIME_STOP);
            renderer.triggerPreFreezeCinematic(tbCells, () -> timeAttack.triggerFreezeIfNeeded(true));
        } else {
            timeAttack.triggerFreezeIfNeeded(false);
        }
    }

    // --- Score & level ---

    private void addScore(int linesCleared) {
        int bonus = switch (linesCleared) {
            case 1 -> 100;
            case 2 -> 300;
            case 3 -> 500;
            case 4 -> 800;
            default -> 0;
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
        if (isGameOver)
            return;
        KeyCode code = event.getCode();

        // ĐÃ FIX CONFLICT: Đồng bộ luồng kiểm tra phím ESCAPE
        if (code == KeyCode.ESCAPE) {
            if (isTutorialOverlayVisible()) {
                closeTutorial();
            } else if (isSettingsOverlayVisible()) {
                closePauseSettings();
            } else {
                openPauseSettings();
            }
            event.consume();
            return;
        }
        if (isSettingsOverlayVisible() || isTutorialOverlayVisible()) {
            event.consume();
            return;
        }

        // ĐÃ FIX CONFLICT: Format gọn gàng cho luồng Pause và Freeze
        if (code == KeyCode.P) {
            handlePause();
            event.consume();
            return;
        }
        if (freezeUntil > 0) {
            event.consume();
            return;
        }

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
            case DOWN, S -> {
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
                if (hardMode.blackoutState != HardModeHandler.BlackoutState.BLACKOUT)
                    hardDrop();
            }
            case B -> useBombSkill();
            case C, SHIFT -> holdCurrentPiece();
            default -> { return; }
        }
        event.consume();
    }

    private void handleMouseMoved(MouseEvent event) {
        if (gamePaused || isGameOver)
            return;
        mouseTargetColumn = (int) (event.getX() / Constants.BLOCK_SIZE);
        event.consume();
    }

    private void applyMouseTarget() {
        if (mouseTargetColumn < 0)
            return;
        int targetX = getTargetPieceX(mouseTargetColumn);
        int currentX = currentPiece.getX();
        if (targetX == currentX)
            return;
        int step = Integer.compare(targetX, currentX);
        if (boardEngine.canMove(currentPiece, step, 0, currentPiece.getRotation(), suspendedPieces))
            currentPiece.setX(currentX + step);
    }

    private void handleMouseClicked(MouseEvent event) {
        if (gamePaused || isGameOver)
            return;
        if (freezeUntil > 0) {
            event.consume();
            return;
        }
        gamePane.requestFocus();
        if (event.getButton() == MouseButton.PRIMARY) {
            if (hardMode.blackoutState != HardModeHandler.BlackoutState.BLACKOUT)
                hardDrop();
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
        for (int dx : new int[] { 0, -1, 1, -2, 2 }) {
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

    private void startFlavourTextCycle(String[] tips) {
        if (flavourLabel == null || tips.length == 0) return;
        flavourTipIndex = 0;
        flavourLabel.setText(tips[0]);
        if (tips.length == 1) return;
        flavourTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(7), e -> {
                    flavourTipIndex = (flavourTipIndex + 1) % tips.length;
                    flavourLabel.setText(tips[flavourTipIndex]);
                }));
        flavourTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        flavourTimeline.play();
    }

    private void applyGameModeTheme() {
        gamePane.getStyleClass().removeAll(
                "screen-standard-game",
                "screen-timeatk-game",
                "screen-hard-game",
                "blackout");

        GameContext.GameMode mode = gameContext.getGameMode();
        if (mode == GameContext.GameMode.TIME_ATTACK) {
            gamePane.getStyleClass().add("screen-timeatk-game");
            if (mainFrameView != null) mainFrameView.setImage(timeAttackMainImage);
            startFlavourTextCycle(TIME_ATTACK_TIPS);
        } else if (mode == GameContext.GameMode.HARD_MODE) {
            gamePane.getStyleClass().add("screen-hard-game");
            if (mainFrameView != null) mainFrameView.setImage(hardMainImage);
        } else {
            gamePane.getStyleClass().add("screen-standard-game");
            if (mainFrameView != null) mainFrameView.setImage(standardMainImage);
            startFlavourTextCycle(STANDARD_TIPS);
        }
    }

    // --- Actions ---

    private void hardDrop() {
        dropStartRow = currentPiece.getY();
        renderer.onHardDrop();
        int drop = boardEngine.getDropDistance(currentPiece, suspendedPieces);
        if (timeAttack.isFreezeActive) renderer.onHardDropTrail(currentPiece, drop);
        currentPiece.setY(currentPiece.getY() + drop);
        lockAndSpawn();
    }

    private void holdCurrentPiece() {
        if (!canHold || currentPiece == null || isGameOver || currentPiece.getType() == TetrominoType.BOMB) {
            return;
        }

        canHold = false;
        stopRandomBlockIfNeeded(currentPiece);
        TetrominoType currentType = currentPiece.getType();

        if (holdType == null) {
            holdType = currentType;
            spawnPiece();
        } else {
            TetrominoType swapType = holdType;
            holdType = currentType;
            currentPiece = new Piece(swapType, Constants.BOARD_WIDTH / 2 - 2, 0);
            dropStartRow = -1;
            if (!boardEngine.canMove(currentPiece, 0, 0, currentPiece.getRotation(), suspendedPieces)) {
                isGameOver = true;
                stopRandomBlockIfNeeded(currentPiece);
                gameOverFreezeUntil = System.nanoTime() + 500_000_000L;
                renderer.onGameOver();
            }
        }
    }

    public void gameOver() {
        handleGameOver();
    }

    private void useBombSkill() {
        if (isGameOver || gamePaused || currentPiece == null
                || currentPiece.getType() == TetrominoType.BOMB
                || gameContext.getGameMode() == GameContext.GameMode.STANDARD) return;

        GameContext.GameMode currentMode = gameContext.getGameMode();

        if (currentMode == GameContext.GameMode.TIME_ATTACK) {
            if (bombsRemaining <= 0) return;
            bombsRemaining--;
            if (bombsLabel != null) bombsLabel.setText("💣 x" + bombsRemaining);
            changeCurrentPieceToBomb();
        } else if (currentMode == GameContext.GameMode.HARD_MODE) {
            if (!hardMode.canUseBomb()) return;
            hardMode.triggerBombCooldown();
            changeCurrentPieceToBomb();
        }
    }

    private void changeCurrentPieceToBomb() {
        stopRandomBlockIfNeeded(currentPiece);

        currentPiece = new Piece(TetrominoType.BOMB, currentPiece.getX(), currentPiece.getY());

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
        if (gamePaused)
            timeAttack.pause();
        else
            timeAttack.resume();
    }

    @FXML
    private void onSettingsOverlayBackdropClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        closePauseSettings();
    }

    private void openPauseSettings() {
        if (settingsOverlay == null) return;
        settingsPausedGame = !gamePaused;
        if (settingsPausedGame) {
            gamePaused = true;
            timeAttack.pause();
        }
        if (settingsPopupController != null) {
            settingsPopupController.setScoreVisible(true);
            settingsPopupController.setScoreValue(gameContext.getScore());
        }
        double ow = gamePane.getWidth(), oh = gamePane.getHeight();
        settingsOverlay.resize(ow, oh);
        if (settingsPopupShell != null) {
            settingsPopupShell.setLayoutX((ow - settingsPopupShell.getPrefWidth()) / 2);
            settingsPopupShell.setLayoutY((oh - settingsPopupShell.getPrefHeight()) / 2);
        }
        settingsOverlay.layout();
        settingsOverlay.setVisible(true);
        settingsOverlay.toFront();
        Platform.runLater(settingsOverlay::requestFocus);
    }

    private void closePauseSettings() {
        if (settingsOverlay != null) {
            settingsOverlay.setVisible(false);
        }
        if (settingsPausedGame && gamePaused && !isGameOver) {
            gamePaused = false;
            timeAttack.resume();
        }
        settingsPausedGame = false;
        Platform.runLater(gamePane::requestFocus);
    }

    private boolean isSettingsOverlayVisible() {
        return settingsOverlay != null && settingsOverlay.isVisible();
    }

    @FXML
    private void onTutorialButtonClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        openTutorial();
    }

    @FXML
    private void onTutorialOverlayBackdropClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        closeTutorial();
    }

    @FXML
    private void onTutorialCloseClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        closeTutorial();
    }

    private void openTutorial() {
        if (tutorialOverlay == null || isSettingsOverlayVisible()) return;
        tutorialPausedGame = !gamePaused;
        if (tutorialPausedGame) {
            gamePaused = true;
            timeAttack.pause();
        }
        if (tutorialTitleLabel != null) {
            tutorialTitleLabel.setText("How to Play");
        }
        if (tutorialBodyLabel != null) {
            tutorialBodyLabel.setText(buildTutorialText());
        }
        double ow = gamePane.getWidth(), oh = gamePane.getHeight();
        tutorialOverlay.resize(ow, oh);
        if (tutorialPopupShell != null) {
            tutorialPopupShell.setLayoutX((ow - tutorialPopupShell.getPrefWidth()) / 2);
            tutorialPopupShell.setLayoutY((oh - tutorialPopupShell.getPrefHeight()) / 2);
        }
        tutorialOverlay.layout();
        tutorialOverlay.setVisible(true);
        tutorialOverlay.toFront();
        Platform.runLater(tutorialOverlay::requestFocus);
    }

    private void closeTutorial() {
        if (tutorialOverlay != null) {
            tutorialOverlay.setVisible(false);
        }
        if (tutorialPausedGame && gamePaused && !isGameOver) {
            gamePaused = false;
            timeAttack.resume();
        }
        tutorialPausedGame = false;
        Platform.runLater(gamePane::requestFocus);
    }

    private boolean isTutorialOverlayVisible() {
        return tutorialOverlay != null && tutorialOverlay.isVisible();
    }

    private String buildTutorialText() {
        String common = """
                Controls
                - Move: A / D or Left / Right
                - Rotate: W / Up or right click
                - Soft drop: S / Down
                - Hard drop: Space or left click
                - Hold piece: C or Shift
                - Pause and settings: Esc
                - Mouse aim: move the cursor over the board to guide the active piece
                """;

        String modeGuide = switch (gameContext.getGameMode()) {
            case TIME_ATTACK -> """

                    Time Attack
                    - You start with 02:00 on the clock.
                    - Clear lines to gain bonus time. More lines at once give more time.
                    - Some locked blocks become time blocks. Clear a line containing one to freeze the clock for 10 seconds.
                    - Press B to turn the current piece into a bomb. You have 3 bombs, so save them for dangerous stacks.
                    """;
            case HARD_MODE -> """

                    Hard Mode
                    - The board becomes faster and more hostile than Standard Mode.
                    - Blackout events can hide the board and force quick decisions.
                    - Bomb and transparent pieces begin appearing as the run progresses.
                    - Press B when the bomb skill is ready to turn the current piece into a bomb.
                    """;
            case STANDARD -> """

                    Standard Mode
                    - Clear horizontal lines to score points and build combos.
                    - Every 10 cleared lines increases the level and falling speed.
                    - Use Hold and Next previews to prepare clean Tetris clears.
                    """;
        };

        return common + modeGuide;
    }

    private void quitToMainMenuFromSettings() {
        stopRandomBlockIfNeeded(currentPiece);
        timeAttack.stopAll();
        if (flavourTimeline != null) flavourTimeline.stop();
        SoundManager.getInstance().stopLooping();
        if (gameLoop != null)
            gameLoop.stop();
        gameContext.reset();
        sceneManager.clearSceneCache();
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }

    private void handleGameOver() {
        if (isGameOver) return;
        isGameOver = true;
        stopRandomBlockIfNeeded(currentPiece);
        timeAttack.stopAll();
        SoundManager.getInstance().playSE(SoundType.GAME_OVER);
        SoundManager.getInstance().stopMusic();
        gameOverFreezeUntil = System.nanoTime() + 500_000_000L;
    }
}
