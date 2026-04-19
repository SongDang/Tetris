package com.se330.tetris.controller;

import com.se330.tetris.core.TetrisApp;
import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.Constants;
import com.se330.tetris.util.SoundType;
import com.se330.tetris.game.BorderPulseEffect;
import com.se330.tetris.game.GlitchTearEffect;
import com.se330.tetris.game.LevelUpEffect;
import com.se330.tetris.game.ParticleSystem;
import com.se330.tetris.game.Piece;
import com.se330.tetris.game.TetrominoType;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.scene.text.FontWeight;

import java.util.Collections;

public class GameController {

    @FXML
    private HBox gamePane;
    @FXML
    private Canvas gameCanvas;
    @FXML
    private Canvas vfxCanvas;
    @FXML
    private Canvas nextBlockCanvas;
    @FXML
    private Canvas nextBlockCanvas1;
    @FXML
    private Canvas nextBlockCanvas2;
    @FXML
    private Canvas nextBlockCanvas3;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label levelLabel;
    @FXML
    private Label linesLabel;
    @FXML
    private Canvas holdBlockCanvas;

    @FXML private Label       bombsLabel;
    @FXML private VBox        bombInventoryBox;

    private SceneManager sceneManager;
    private GameContext gameContext;

    private GraphicsContext gameGc;

    private GraphicsContext nextGc;
    private GraphicsContext vfxGc;
    // vfxCanvas extends 150 px beyond each board edge (canvas width = 600, layoutX
    // = -150)
    private static final double VFX_MARGIN = 150.0;
    private GraphicsContext holdGc;

    private final int[][] board = new int[Constants.BOARD_HEIGHT][Constants.BOARD_WIDTH];
    private Piece currentPiece;
    private Piece nextPiece;
    private TetrominoType holdType;
    private boolean canHold = true;
    private int bombsRemaining = 3;
    private String bombInventoryBaseStyle = "";

    private final ParticleSystem particleSystem = new ParticleSystem();
    private LevelUpEffect     levelUpEffect     = null;
    private BorderPulseEffect borderPulseEffect = null;
    private GlitchTearEffect  glitchTearEffect  = null;

    private GraphicsContext nextGc1;
    private GraphicsContext nextGc2;
    private GraphicsContext nextGc3;

    private final java.util.ArrayDeque<Piece> nextQueue = new java.util.ArrayDeque<>();

    private AnimationTimer gameLoop;
    private boolean gamePaused = false;
    private boolean isGameOver = false;

    private long lastFallTime = 0;

    private long lastFrameTime = 0;
    private long fallIntervalNs = 500_000_000L;
    private long freezeUntil         = 0;  // nanosecond timestamp; game logic frozen until this time
    private long   gameOverFreezeUntil  = 0;   // 0.5s static pause before glitch fires
    private double gameOverFlashAlpha   = 0;   // white flash on board at moment of game over
    private int[]  pendingTetrisClear   = null; // Tetris rows cleared after freeze ends
    private int[] frozenRowFlash = null;  // row indices to flash white during Tetris freeze

    private int    comboCount      = 0;
    private int    comboDisplay    = 0;
    private double comboFloatX     = 0;
    private double comboFloatY     = 0;
    private double comboFloatAlpha  = 0;
    private double comboFloatPhase  = 0;
    private double comboShakeAmount = 0;
    private Color  comboColor       = Color.WHITE;

    private int dropStartRow = -1;
    private double shakeIntensity = 0;
    private double shakeDuration = 0;
    private double shakeInitDuration = 0.18;
    private double rotationPulse = 0; // 1.0 = full brightness pulse, 0 = normal
    private double flashIntensity = 0; // 0 = none, >0 = bright background flash

    private java.util.List<TetrominoType> bag = new java.util.ArrayList<>();

    private int mouseTargetColumn = -1;

    private final java.util.Random rng = new java.util.Random();

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext = GameContext.getInstance();
        gameContext.reset();

        gameGc = gameCanvas.getGraphicsContext2D();
        nextGc1 = nextBlockCanvas1.getGraphicsContext2D();
        nextGc2 = nextBlockCanvas2.getGraphicsContext2D();
        nextGc3 = nextBlockCanvas3.getGraphicsContext2D();

        vfxGc = vfxCanvas.getGraphicsContext2D();
        holdGc = holdBlockCanvas.getGraphicsContext2D();

        // Spawn hai mảnh đầu tiên
        holdType = null;
        canHold = true;

        bombsRemaining = 3;

        nextPiece = randomPiece();
        // Seed queue for current + 3 previews.
        nextQueue.clear();
        for (int i = 0; i < 4; i++) {
            nextQueue.addLast(randomPiece());
        }
        spawnPiece();

        if (bombInventoryBox != null) {
            bombInventoryBaseStyle = bombInventoryBox.getStyle() == null ? "" : bombInventoryBox.getStyle();
        }
        updateBombInventoryUI();

        // Cập nhật labels ban đầu
        refreshLabels();

        // Key handler
        gamePane.setOnKeyPressed(this::handleKeyPressed);
        gameCanvas.setOnMouseMoved(this::handleMouseMoved);
        gameCanvas.setOnMouseClicked(this::handleMouseClicked);
        Platform.runLater(() -> {
            gamePane.requestFocus();
        });

        startGameLoop();
    }

    private void startGameLoop() {
        lastFallTime = System.nanoTime();
        lastFrameTime = System.nanoTime();
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = (now - lastFrameTime) / 1_000_000_000.0;
                lastFrameTime = now;

                if (freezeUntil > 0 && now >= freezeUntil) {
                    lastFallTime   = now;
                    freezeUntil    = 0;
                    frozenRowFlash = null;
                    if (pendingTetrisClear != null) {
                        int[] rows = pendingTetrisClear;
                        pendingTetrisClear = null;
                        for (int row : rows) {
                            for (int r = row; r > 0; r--) board[r] = board[r - 1].clone();
                            board[0] = new int[Constants.BOARD_WIDTH];
                        }
                        addScore(4);
                        addLines(4);
                        updateLevel();
                        spawnAndCheckGameOver();
                    }
                }

                if (glitchTearEffect != null) glitchTearEffect.update(dt);
                if (gameOverFlashAlpha > 0) gameOverFlashAlpha = Math.max(0, gameOverFlashAlpha - dt * 4.0);

                if (isGameOver && gameOverFreezeUntil > 0 && now >= gameOverFreezeUntil) {
                    gameOverFreezeUntil = 0;
                    glitchTearEffect = new GlitchTearEffect(board, Constants.BLOCK_SIZE, () -> {
                        gameLoop.stop();
                        sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
                    });
                }

                // --- LOGIC GAME CHÍNH ---
                if (!gamePaused && !isGameOver && freezeUntil == 0) {
                    applyMouseTarget(now); // Giữ logic Mouse từ bản cũ

                    if (now - lastFallTime >= fallIntervalNs) {
                        updateFall();
                        lastFallTime = now;
                    }

                    particleSystem.update(dt);
                    updateScreenShake(dt);

                    // Cập nhật các hiệu ứng VFX mới
                    if (rotationPulse  > 0) rotationPulse  = Math.max(0, rotationPulse  - dt * 8.0);
                    if (flashIntensity > 0) flashIntensity = Math.max(0, flashIntensity - dt * 6.0);

                    if (levelUpEffect != null) {
                        levelUpEffect.update(dt);
                        if (levelUpEffect.isDone()) levelUpEffect = null;
                    }
                    if (borderPulseEffect != null) {
                        borderPulseEffect.update(dt);
                        if (borderPulseEffect.isDone()) borderPulseEffect = null;
                    } else {
                        // Check cảnh báo nguy hiểm (stack cao)
                        int dangerRow = (int)(Constants.BOARD_HEIGHT * 0.35);
                        outer:
                        for (int y = 0; y <= dangerRow; y++)
                            for (int x = 0; x < Constants.BOARD_WIDTH; x++)
                                if (board[y][x] != 0) { borderPulseEffect = new BorderPulseEffect(); break outer; }
                    }

                    if (comboFloatAlpha > 0) {
                        comboFloatY      -= dt * 60;
                        comboFloatPhase  += dt * 5.0;
                        comboShakeAmount  = Math.max(0, comboShakeAmount - dt * 100);
                        comboFloatAlpha   = Math.max(0, comboFloatAlpha - dt * 1.1);
                    }
                }
                render();
            }
        };
        gameLoop.start();
    }

    private void updateFall() {
        if (canMove(0, 1, currentPiece.getRotation())) {
            currentPiece.setY(currentPiece.getY() + 1);
        } else {
            lockAndSpawn();
        }
    }

    private void lockAndSpawn() {
        // --- GIỮ LOGIC BOMB TỪ HEAD ---
        if (currentPiece.getType() == TetrominoType.BOMB) {
            int impactX = currentPiece.getX();
            int impactY = getBombImpactY(impactX, currentPiece.getY());
            detonateBomb(impactX, impactY);
        } else {
            lockPiece();
        }

        int cs = Constants.BLOCK_SIZE;
        java.util.List<Integer> fullRows = new java.util.ArrayList<>();
        for (int y = Constants.BOARD_HEIGHT - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                if (board[y][x] == 0) { full = false; break; }
            }
            if (full) fullRows.add(y);
        }

        if (!fullRows.isEmpty()) {
            int cleared = fullRows.size();
            for (int row : fullRows) {
                for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                    TetrominoType t = idToType(board[row][x]);
                    if (t != null) particleSystem.emitRowBurst(x * cs, row * cs, t.getColor(), 8);
                }
            }

            double leftBase  = VFX_MARGIN;
            double rightBase = VFX_MARGIN + Constants.BOARD_WIDTH * cs;
            Color pieceCol   = currentPiece.getType().getColor();
            for (int row : fullRows) {
                particleSystem.emitRowBeams(leftBase, rightBase, row * cs, cs, pieceCol);
            }

            // --- LOGIC COMBO & SHAKE MỚI ---
            double t = cleared / 4.0;
            flashIntensity    = 0.12 + t * 0.28;
            shakeIntensity    = 5.0  + t * 13.0  + (cleared == 4 ? 12.0 : 0);
            shakeInitDuration = 0.18 + t * 0.17  + (cleared == 4 ? 0.10 : 0);
            shakeDuration     = shakeInitDuration;

            comboCount++;
            if (comboCount >= 2) {
                int avgRow = fullRows.stream().mapToInt(Integer::intValue).sum() / fullRows.size();
                comboDisplay    = comboCount;
                comboFloatX     = gameCanvas.getWidth() / 2.0 - 50;
                comboFloatY     = avgRow * cs;
                comboFloatAlpha  = 1.0;
                comboFloatPhase  = 0;
                comboShakeAmount = 10 + comboDisplay * 5;
                comboColor = TetrominoType.values()[rng.nextInt(7)].getColor();
            }

            if (cleared == 4) {
                fullRows.sort(java.util.Collections.reverseOrder());
                frozenRowFlash     = fullRows.stream().mapToInt(Integer::intValue).toArray();
                pendingTetrisClear = frozenRowFlash;
                freezeUntil        = System.nanoTime() + 300_000_000L;
                return;
            }

            fullRows.sort(java.util.Collections.reverseOrder());
            for (int row : fullRows) {
                for (int r = row; r > 0; r--) board[r] = board[r - 1].clone();
                board[0] = new int[Constants.BOARD_WIDTH];
            }

            addScore(cleared);
            addLines(cleared);
            updateLevel();
        } else {
            comboCount = 0;
        }

        canHold = true;
        spawnAndCheckGameOver();
    }

    private void spawnAndCheckGameOver() {
        spawnPiece();
        if (!canMove(0, 0, currentPiece.getRotation())) {
            isGameOver = true;
            SoundManager.getInstance().playSE(SoundType.GAME_OVER);
            SoundManager.getInstance().stopMusic();
            gameLoop.stop();
            sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
            gameOverFreezeUntil = System.nanoTime() + 500_000_000L;
            gameOverFlashAlpha  = 0.6;
        }
    }

    private void spawnPiece() {
        currentPiece = nextQueue.removeFirst();
        currentPiece.setX(Constants.BOARD_WIDTH / 2 - 2);
        currentPiece.setY(0);
        nextQueue.addLast(randomPiece());
    }

    private Piece createSpawnedPiece(TetrominoType type) {
        return new Piece(type, Constants.BOARD_WIDTH / 2 - 2, 0);
    }

    private Piece randomPiece() {
        // Refill bag
        if (bag.isEmpty()) {
            bag.addAll(java.util.Arrays.asList(TetrominoType.values()));
            java.util.Collections.shuffle(bag);
        }

        // Pop the first piece
        TetrominoType type = bag.remove(0);
        return new Piece(type, 0, 0);
    }

    private void detonateBomb(int centerX, int centerY) {
        for (int y = centerY - 1; y <= centerY + 1; y++) {
            if (y < 0 || y >= Constants.BOARD_HEIGHT) {
                continue;
            }
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                if (x < 0 || x >= Constants.BOARD_WIDTH) {
                    continue;
                }
                board[y][x] = 0;
            }
        }
    }

    private int getBombImpactY(int bombX, int bombY) {
        int candidateY = bombY + 1;

        if (candidateY >= Constants.BOARD_HEIGHT) {
            return Constants.BOARD_HEIGHT - 1;
        }

        if (bombX >= 0 && bombX < Constants.BOARD_WIDTH && board[candidateY][bombX] != 0) {
            return candidateY;
        }

        return bombY;
    }

    private boolean canMove(int dx, int dy, int rotation) {
        int[][] shape = currentPiece.getType().getShape(rotation);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int nx = currentPiece.getX() + col + dx;
                    int ny = currentPiece.getY() + row + dy;
                    if (nx < 0 || nx >= Constants.BOARD_WIDTH)
                        return false;
                    if (ny < 0 || ny >= Constants.BOARD_HEIGHT)
                        return false;
                    if (board[ny][nx] != 0)
                        return false;
                }
            }
        }
        return true;
    }

    private void lockPiece() {
        SoundManager.getInstance().playSE(SoundType.BLOCK_DROP);

        int[][] shape = currentPiece.getType().getShape(currentPiece.getRotation());
        int cs = Constants.BLOCK_SIZE;

        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int maxRow = Integer.MIN_VALUE;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int x = currentPiece.getX() + col;
                    int y = currentPiece.getY() + row;
                    if (y >= 0 && y < Constants.BOARD_HEIGHT
                            && x >= 0 && x < Constants.BOARD_WIDTH) {
                        board[y][x] = typeId(currentPiece.getType());
                        if (dropStartRow >= 0) {
                            particleSystem.emitLockParticles(
                                    x * cs, y * cs, currentPiece.getType().getColor(), 18);
                        }
                        if (x < minCol)
                            minCol = x;
                        if (x > maxCol)
                            maxCol = x;
                        if (y > maxRow)
                            maxRow = y;
                    }
                }
            }
        }

        // Single wide light column on hard drop only
        if (dropStartRow >= 0 && minCol <= maxCol) {
            double leftPixelX = minCol * cs;
            double spanWidth = (maxCol - minCol + 1) * cs;
            double fromPixelY = dropStartRow * cs;
            double toPixelY = (maxRow + 1) * cs;
            particleSystem.emitLightColumn(
                    leftPixelX, fromPixelY, toPixelY, spanWidth,
                    currentPiece.getType().getColor());
            dropStartRow = -1;
        }
    }

    private int clearLines() {
        int cleared = 0;
        for (int y = Constants.BOARD_HEIGHT - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                if (board[y][x] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                cleared++;
                for (int row = y; row > 0; row--) {
                    board[row] = board[row - 1].clone();
                }
                board[0] = new int[Constants.BOARD_WIDTH];
                y++; // kiểm tra lại dòng vừa kéo xuống
            }
        }
        return cleared;
    }

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
    }

    private void updateLevel() {
        // Mỗi 10 dòng tăng 1 level, tối đa level 10
        int newLevel = Math.min(gameContext.getLines() / 10 + 1, 10);
        if (newLevel != gameContext.getLevel()) {
            gameContext.setLevel(newLevel);
            levelLabel.setText(String.valueOf(newLevel));
            // Tăng tốc độ rơi
            fallIntervalNs = Math.max(100_000_000L, 500_000_000L - (newLevel - 1) * 40_000_000L);
            levelUpEffect = new LevelUpEffect(newLevel, () -> {
                shakeIntensity    = 18;
                shakeInitDuration = 0.25;
                shakeDuration     = 0.25;
            });
        }
    }

    private void refreshLabels() {
        scoreLabel.setText(String.valueOf(gameContext.getScore()));
        levelLabel.setText(String.valueOf(gameContext.getLevel()));
        linesLabel.setText(String.valueOf(gameContext.getLines()));
    }

    private void emitRotationArc() {
        particleSystem.emitCornerSparks(
                currentPiece.getX(),
                currentPiece.getY(),
                currentPiece.getType().getShape(currentPiece.getRotation()),
                Constants.BLOCK_SIZE,
                currentPiece.getType().getColor());
        rotationPulse = 1.0;
    }

    private void updateScreenShake(double dt) {
        if (shakeDuration <= 0)
            return;
        shakeDuration -= dt;
        if (shakeDuration <= 0) {
            gameCanvas.setTranslateX(0);
            gameCanvas.setTranslateY(0);
        } else {
            double factor = shakeDuration / shakeInitDuration; // eases out as duration drops
            gameCanvas.setTranslateX((rng.nextDouble() - 0.5) * 2 * shakeIntensity * factor);
            gameCanvas.setTranslateY((rng.nextDouble() - 0.5) * 2 * shakeIntensity * factor);
        }
    }

    private void hardDrop() {
        dropStartRow = currentPiece.getY();
        shakeIntensity = 4.0;
        shakeInitDuration = 0.18;
        shakeDuration = 0.18;
        int drop = getDropDistance();
        currentPiece.setY(currentPiece.getY() + drop);
        lockAndSpawn();
    }

    private void holdCurrentPiece() {
        if (!canHold || currentPiece == null || isGameOver || currentPiece.getType() == TetrominoType.BOMB) {
            return;
        }

        canHold = false;
        TetrominoType currentType = currentPiece.getType();

        if (holdType == null) {
            holdType = currentType;
            spawnPiece();
        } else {
            TetrominoType swapped = holdType;
            holdType = currentType;
            currentPiece = createSpawnedPiece(swapped);
        }

        if (!canMove(0, 0, currentPiece.getRotation())) {
            isGameOver = true;
            gameLoop.stop();
            sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
        }
    }

    private int getDropDistance() {
        int distance = 0;
        while (canMove(0, distance + 1, currentPiece.getRotation()))
            distance++;
        return distance;
    }

    private void useBombSkill() {
        if (isGameOver || gamePaused || bombsRemaining <= 0 || currentPiece == null) {
            return;
        }
        if (currentPiece.getType() == TetrominoType.BOMB) {
            return;
        }

        bombsRemaining--;
        currentPiece = new Piece(TetrominoType.BOMB, currentPiece.getX(), currentPiece.getY());
        updateBombInventoryUI();
        flashBombInventory();
    }

    private void updateBombInventoryUI() {
        if (bombsLabel != null) {
            bombsLabel.setText("\uD83D\uDCA3 x" + bombsRemaining);
        }
    }

    private void flashBombInventory() {
        if (bombInventoryBox == null) {
            return;
        }

        bombInventoryBox.setStyle(
                bombInventoryBaseStyle
                        + "; -fx-border-color: #ffcc00; -fx-border-width: 2;"
                        + " -fx-effect: dropshadow(gaussian, #ffcc00, 10, 0.6, 0, 0);"
        );

        PauseTransition reset = new PauseTransition(Duration.millis(160));
        reset.setOnFinished(e -> bombInventoryBox.setStyle(bombInventoryBaseStyle));
        reset.play();
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        switch (code) {
            case LEFT, A -> {
                if (canMove(-1, 0, currentPiece.getRotation()))
                    currentPiece.setX(currentPiece.getX() - 1);
            }
            case RIGHT, D -> {
                if (canMove(1, 0, currentPiece.getRotation()))
                    currentPiece.setX(currentPiece.getX() + 1);
            }
            case DOWN, S -> hardDrop();
            case UP, W -> {
                int nr = (currentPiece.getRotation() + 1) % 4;
                if (canMove(0, 0, nr)) {
                    currentPiece.setRotation(nr);
                    emitRotationArc();
                }
            }
            case SPACE -> hardDrop();
            case P -> handlePause();
            case B -> useBombSkill();
            case C, SHIFT      -> holdCurrentPiece();
            case ESCAPE -> handleExit();
            default -> {
                return;
            }
        }
        event.consume();
    }

    private void handleMouseMoved(MouseEvent event) {
        if (gamePaused || isGameOver)
            return;

        // Only update target; movement is applied in the game loop.
        mouseTargetColumn = (int) (event.getX() / Constants.BLOCK_SIZE);
        event.consume();
    }

    private void applyMouseTarget(long now) {
        if (mouseTargetColumn < 0)
            return;

        int targetX = getTargetPieceX(mouseTargetColumn);
        int currentX = currentPiece.getX();
        if (targetX == currentX)
            return;

        // Limit horizontal speed to 1 column per frame to avoid jitter.
        int step = Integer.compare(targetX, currentX);
        if (canMove(step, 0, currentPiece.getRotation())) {
            currentPiece.setX(currentX + step);
        }
    }

    private void handleMouseClicked(MouseEvent event) {
        if (gamePaused || isGameOver)
            return;

        // Keep keyboard control active after interacting with the canvas.
        gamePane.requestFocus();

        if (event.getButton() == MouseButton.PRIMARY) {
            hardDrop();
            event.consume();
            return;
        }

        if (event.getButton() == MouseButton.SECONDARY) {
            tryRotateWithWallKick();
            event.consume();
        }
    }

    private boolean tryRotateWithWallKick() {
        int nextRotation = (currentPiece.getRotation() + 1) % 4;

        // Basic wall-kick offsets to allow rotation when touching side walls.
        int[] kickOffsets = { 0, -1, 1, -2, 2 };
        for (int dx : kickOffsets) {
            if (canMove(dx, 0, nextRotation)) {
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
    }

    private void handleExit() {
        System.out.println("Exiting to main menu");
        if (gameLoop != null) {
            gameLoop.stop();
        }
        gameContext.reset();
        sceneManager.clearSceneCache();
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }

    // Base background color components for gamePane (#0f0d1a)
    private static final double BG_R = 0x0f / 255.0;
    private static final double BG_G = 0x0d / 255.0;
    private static final double BG_B = 0x1a / 255.0;
    private static final String PANE_BASE_STYLE = "-fx-padding: 20; -fx-spacing: 20; -fx-alignment: center;";

    private void render() {
        if (flashIntensity > 0) {
            double r = BG_R + flashIntensity * (1.0 - BG_R);
            double g = BG_G + flashIntensity * (1.0 - BG_G);
            double b = BG_B + flashIntensity * (1.0 - BG_B);
            String hex = String.format("#%02x%02x%02x", (int)(r*255), (int)(g*255), (int)(b*255));
            gamePane.setStyle("-fx-background-color: " + hex + "; " + PANE_BASE_STYLE);
        } else {
            gamePane.setStyle("-fx-background-color: #0f0d1a; " + PANE_BASE_STYLE);
        }

        drawGameBoard();

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

        drawHoldBlock();
        drawNextBlocks();
    }

    public void drawGameBoard() {
        GraphicsContext gc = gameGc;
        double w = gameCanvas.getWidth();
        double h = gameCanvas.getHeight();
        int cs = Constants.BLOCK_SIZE;

        // Background
        gc.setFill(Color.web("#0f0d1a"));
        gc.fillRect(0, 0, w, h);

        // Grid lines
        gc.setStroke(Color.web("#2b2740"));
        gc.setLineWidth(0.5);
        for (int x = 0; x <= Constants.BOARD_WIDTH; x++)
            gc.strokeLine(x * cs, 0, x * cs, h);
        for (int y = 0; y <= Constants.BOARD_HEIGHT; y++)
            gc.strokeLine(0, y * cs, w, y * cs);

        // Border
        gc.setStroke(Color.web("#00ff00"));
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, w, h);

        // Locked cells
        for (int y = 0; y < Constants.BOARD_HEIGHT; y++) {
            for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                if (board[y][x] != 0) {
                    TetrominoType t = idToType(board[y][x]);
                    if (t != null)
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
        if (isGameOver) return;

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

        // Current piece - apply rotation pulse brightness
        int[][] shape = currentPiece.getType().getShape(currentPiece.getRotation());
        Color pieceColor = currentPiece.getType().getColor()
                .deriveColor(0, 1, 1.0 + rotationPulse * 1.8, 1);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    drawCell(gc,
                            currentPiece.getX() + col,
                            currentPiece.getY() + row,
                            pieceColor,
                            1.0);
                }
            }
        }

        // Floating combo text
        if (comboFloatAlpha > 0) {
            double cx = comboFloatX + (rng.nextDouble() - 0.5) * 2 * comboShakeAmount;
            double cy = comboFloatY + (rng.nextDouble() - 0.5) * 2 * comboShakeAmount * 0.4;
            double flash = (Math.sin(comboFloatPhase * 3.0) + 1.0) / 2.0;  // 0..1
            Color drawColor = comboColor.interpolate(Color.WHITE, flash);
            int fontSize = (int) Math.min(75, 32 + comboDisplay * 7);
            gc.setGlobalAlpha(comboFloatAlpha);
            gc.setFont(Font.font("VT323", FontWeight.BOLD, fontSize));
            gc.setFill(drawColor);
            gc.fillText("COMBO x" + comboDisplay, cx, cy);
            gc.setGlobalAlpha(1.0);
        }

        // Paused overlay
        if (gamePaused) {
            gc.setFill(Color.color(0, 0, 0, 0.5));
            gc.fillRect(0, 0, w, h);
            gc.setFill(Color.web("#00ff00"));
            gc.setFont(Font.font("Courier New", 28));
            gc.fillText("PAUSED", w / 2 - 55, h / 2);
        }
    }

    public void drawHoldBlock() {
        if (holdType != null) {
            drawPreview(holdGc, holdBlockCanvas, new Piece(holdType, 0, 0));
        } else {
            drawPreview(holdGc, holdBlockCanvas, null);
        }
    }

    public void drawNextBlocks() {
        java.util.Iterator<Piece> it = nextQueue.iterator();
        drawPreview(nextGc1, nextBlockCanvas1, it.hasNext() ? it.next() : null);
        drawPreview(nextGc2, nextBlockCanvas2, it.hasNext() ? it.next() : null);
        drawPreview(nextGc3, nextBlockCanvas3, it.hasNext() ? it.next() : null);
    }

    private void drawPreview(GraphicsContext gc, Canvas canvas, Piece piece) {
            double w = canvas.getWidth();
            double h = canvas.getHeight();

            // Xóa nền và vẽ viền
            gc.setFill(Color.web("#0f0d1a"));
            gc.fillRect(0, 0, w, h);
            gc.setStroke(Color.web("#00ff00"));
            gc.setLineWidth(2);
            gc.strokeRect(0, 0, w, h);

            if (piece == null) return;

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

            if (maxRow < 0 || maxCol < 0) return;

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
                        drawCellAtPixel(gc, px, py, previewCellSize, type.getColor(), 1.0);
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
            default -> null;
        };
    }

    public void gameOver() {
        if (gameLoop != null)
            gameLoop.stop();
        sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
    }
}