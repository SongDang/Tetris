package com.se330.tetris.controller;

import com.se330.tetris.game.BorderPulseEffect;
import com.se330.tetris.game.LevelUpEffect;
import com.se330.tetris.game.ParticleSystem;
import com.se330.tetris.game.Piece;
import com.se330.tetris.game.TetrominoType;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;
import com.se330.tetris.util.Constants;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GameController {

    @FXML private HBox gamePane;
    @FXML private Canvas      gameCanvas;
    @FXML private Canvas      vfxCanvas;
    @FXML private Canvas      nextBlockCanvas;
    @FXML private Label       scoreLabel;
    @FXML private Label       levelLabel;
    @FXML private Label       linesLabel;

    private SceneManager sceneManager;
    private GameContext  gameContext;

    private GraphicsContext gameGc;
    private GraphicsContext nextGc;
    private GraphicsContext vfxGc;

    // vfxCanvas extends 150 px beyond each board edge (canvas width = 600, layoutX = -150)
    private static final double VFX_MARGIN = 150.0;

    private final int[][] board = new int[Constants.BOARD_HEIGHT][Constants.BOARD_WIDTH];
    private Piece currentPiece;
    private Piece nextPiece;

    private final ParticleSystem particleSystem = new ParticleSystem();
    private LevelUpEffect     levelUpEffect     = null;
    private BorderPulseEffect borderPulseEffect = null;

    private AnimationTimer gameLoop;
    private boolean gamePaused  = false;
    private boolean isGameOver  = false;

    private long lastFallTime   = 0;
    private long lastFrameTime  = 0;
    private long fallIntervalNs = 500_000_000L;
    private long freezeUntil    = 0;  // nanosecond timestamp; game logic frozen until this time
    private int[] frozenRowFlash = null;  // row indices to flash white during Tetris freeze

    private int    comboCount      = 0;
    private int    comboDisplay    = 0;
    private double comboFloatX     = 0;
    private double comboFloatY     = 0;
    private double comboFloatAlpha  = 0;
    private double comboFloatPhase  = 0;
    private double comboShakeAmount = 0;
    private Color  comboColor       = Color.WHITE;

    private int    dropStartRow      = -1;
    private double shakeIntensity    = 0;
    private double shakeDuration     = 0;
    private double shakeInitDuration = 0.18;
    private double rotationPulse     = 0;    // 1.0 = full brightness pulse, 0 = normal
    private double flashIntensity    = 0;    // 0 = none, >0 = bright background flash

    private final java.util.Random rng = new java.util.Random();

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext  = GameContext.getInstance();
        gameContext.reset();

        gameGc = gameCanvas.getGraphicsContext2D();
        nextGc = nextBlockCanvas.getGraphicsContext2D();
        vfxGc  = vfxCanvas.getGraphicsContext2D();

        // Spawn hai mảnh đầu tiên
        nextPiece = randomPiece();
        spawnPiece();

        // Cập nhật labels ban đầu
        refreshLabels();

        // Key handler
        gamePane.setOnKeyPressed(this::handleKeyPressed);
        Platform.runLater(() -> {
            gamePane.requestFocus();
        });

        startGameLoop();
    }

    private void startGameLoop() {
        lastFallTime  = System.nanoTime();
        lastFrameTime = System.nanoTime();
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = (now - lastFrameTime) / 1_000_000_000.0;
                lastFrameTime = now;

                if (freezeUntil > 0 && now >= freezeUntil) {
                    lastFallTime   = now;  // don't let the piece instantly drop after freeze
                    freezeUntil    = 0;
                    frozenRowFlash = null;
                }
                if (!gamePaused && !isGameOver && freezeUntil == 0) {
                    if (now - lastFallTime >= fallIntervalNs) {
                        updateFall();
                        lastFallTime = now;
                    }
                    particleSystem.update(dt);
                    updateScreenShake(dt);
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
                        int dangerRow = (int)(Constants.BOARD_HEIGHT * 0.35); // top 35% = stack >= 65%
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
        lockPiece();

        //emit burst VFX from each cell
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
            // Emit particle burst from every cell in the cleared rows
            for (int row : fullRows) {
                for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                    TetrominoType t = idToType(board[row][x]);
                    if (t != null)
                        particleSystem.emitRowBurst(x * cs, row * cs, t.getColor(), 8);
                }
            }
            // Horizontal beams shooting out from each cleared row's left/right edges
            double leftBase  = VFX_MARGIN;                               
            double rightBase = VFX_MARGIN + Constants.BOARD_WIDTH * cs;  
            Color pieceCol   = currentPiece.getType().getColor();
            for (int row : fullRows) {
                particleSystem.emitRowBeams(leftBase, rightBase, row * cs, cs, pieceCol);
            }
            // Clear rows instantly 
            fullRows.sort(java.util.Collections.reverseOrder());
            for (int row : fullRows) {
                for (int r = row; r > 0; r--) board[r] = board[r - 1].clone();
                board[0] = new int[Constants.BOARD_WIDTH];
            }
            addScore(cleared);
            addLines(cleared);
            updateLevel();

            // Freeze game logic briefly on Tetris
            if (cleared == 4) {
                freezeUntil   = System.nanoTime() + 300_000_000L;
                frozenRowFlash = fullRows.stream().mapToInt(Integer::intValue).toArray();
            }

            // Scale shake + flash with lines cleared, Tetris gets extra violence
            double t          = cleared / 4.0;
            flashIntensity    = 0.12 + t * 0.28;
            shakeIntensity    = 5.0  + t * 13.0  + (cleared == 4 ? 12.0 : 0);
            shakeInitDuration = 0.18 + t * 0.17  + (cleared == 4 ? 0.10 : 0);
            shakeDuration     = shakeInitDuration;

            // Combo tracking
            comboCount++;
            if (comboCount >= 2) {
                int avgRow = fullRows.stream().mapToInt(Integer::intValue).sum() / fullRows.size();
                comboDisplay    = comboCount;
                comboFloatX     = gameCanvas.getWidth() / 2.0 - 50;
                comboFloatY     = avgRow * cs;
                comboFloatAlpha  = 1.0;
                comboFloatPhase  = 0;
                comboShakeAmount = 10 + comboDisplay * 5;
                TetrominoType[] types = TetrominoType.values();
                comboColor = types[rng.nextInt(types.length)].getColor();
            }
        } else {
            comboCount = 0;
        }

        spawnAndCheckGameOver();
    }

    private void spawnAndCheckGameOver() {
        spawnPiece();
        if (!canMove(0, 0, currentPiece.getRotation())) {
            isGameOver = true;
            gameLoop.stop();
            sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
        }
    }

    private void spawnPiece() {
        currentPiece = nextPiece;
        currentPiece.setX(Constants.BOARD_WIDTH / 2 - 2);
        currentPiece.setY(0);
        nextPiece = randomPiece();
    }

    private Piece randomPiece() {
        TetrominoType[] types = TetrominoType.values();
        return new Piece(types[rng.nextInt(types.length)], 0, 0);
    }

    private boolean canMove(int dx, int dy, int rotation) {
        int[][] shape = currentPiece.getType().getShape(rotation);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int nx = currentPiece.getX() + col + dx;
                    int ny = currentPiece.getY() + row + dy;
                    if (nx < 0 || nx >= Constants.BOARD_WIDTH) return false;
                    if (ny < 0 || ny >= Constants.BOARD_HEIGHT) return false;
                    if (board[ny][nx] != 0) return false;
                }
            }
        }
        return true;
    }

    private void lockPiece() {
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
                        if (x < minCol) minCol = x;
                        if (x > maxCol) maxCol = x;
                        if (y > maxRow) maxRow = y;
                    }
                }
            }
        }

        // Single wide light column on hard drop only
        if (dropStartRow >= 0 && minCol <= maxCol) {
            double leftPixelX  = minCol * cs;
            double spanWidth   = (maxCol - minCol + 1) * cs;
            double fromPixelY  = dropStartRow * cs;
            double toPixelY    = (maxRow + 1) * cs;
            particleSystem.emitLightColumn(
                leftPixelX, fromPixelY, toPixelY, spanWidth,
                currentPiece.getType().getColor()
            );
            dropStartRow = -1;
        }
    }

    private int clearLines() {
        int cleared = 0;
        for (int y = Constants.BOARD_HEIGHT - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                if (board[y][x] == 0) { full = false; break; }
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
            currentPiece.getType().getColor()
        );
        rotationPulse = 1.0;
    }

    private void updateScreenShake(double dt) {
        if (shakeDuration <= 0) return;
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
        dropStartRow      = currentPiece.getY();
        shakeIntensity    = 4.0;
        shakeInitDuration = 0.18;
        shakeDuration     = 0.18;
        int drop = getDropDistance();
        currentPiece.setY(currentPiece.getY() + drop);
        lockAndSpawn();
    }

    private int getDropDistance() {
        int distance = 0;
        while (canMove(0, distance + 1, currentPiece.getRotation())) distance++;
        return distance;
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        switch (code) {
            case LEFT,  A     -> { if (canMove(-1, 0, currentPiece.getRotation()))
                currentPiece.setX(currentPiece.getX() - 1); }
            case RIGHT, D     -> { if (canMove( 1, 0, currentPiece.getRotation()))
                currentPiece.setX(currentPiece.getX() + 1); }
            case DOWN,  S     -> hardDrop();
            case UP,    W     -> { int nr = (currentPiece.getRotation() + 1) % 4;
                if (canMove(0, 0, nr)) {
                    currentPiece.setRotation(nr);
                    emitRotationArc();
                }
            }
            case SPACE        -> hardDrop();
            case P            -> handlePause();
            default           -> { return; }
        }
        event.consume();
    }

    private void handlePause() {
        gamePaused = !gamePaused;
    }

    // Base background color components for gamePane (#0f0d1a)
    private static final double BG_R = 0x0f / 255.0;
    private static final double BG_G = 0x0d / 255.0;
    private static final double BG_B = 0x1a / 255.0;
    private static final String PANE_BASE_STYLE =
        "-fx-padding: 20; -fx-spacing: 20; -fx-alignment: center;";

    private void render() {
        // Flash outer background behind the board
        if (flashIntensity > 0) {
            double r = BG_R + flashIntensity * (1.0 - BG_R);
            double g = BG_G + flashIntensity * (1.0 - BG_G);
            double b = BG_B + flashIntensity * (1.0 - BG_B);
            String hex = String.format("#%02x%02x%02x",
                (int)(r * 255), (int)(g * 255), (int)(b * 255));
            gamePane.setStyle("-fx-background-color: " + hex + "; " + PANE_BASE_STYLE);
        } else {
            gamePane.setStyle("-fx-background-color: #0f0d1a; " + PANE_BASE_STYLE);
        }
        drawGameBoard();
        if (levelUpEffect != null)
            levelUpEffect.render(gameGc, gameCanvas.getWidth(), gameCanvas.getHeight());
        particleSystem.render(gameGc);
        particleSystem.renderBeams(vfxGc);
        if (borderPulseEffect != null)
            borderPulseEffect.render(gameGc, vfxGc, gameCanvas.getWidth(), gameCanvas.getHeight(), VFX_MARGIN);
        drawNextBlock();
    }

    public void drawGameBoard() {
        GraphicsContext gc = gameGc;
        double w = gameCanvas.getWidth();
        double h = gameCanvas.getHeight();
        int cs   = Constants.BLOCK_SIZE;

        // Background
        gc.setFill(Color.web("#0f0d1a"));
        gc.fillRect(0, 0, w, h);

        // Grid lines
        gc.setStroke(Color.web("#2b2740"));
        gc.setLineWidth(0.5);
        for (int x = 0; x <= Constants.BOARD_WIDTH;  x++) gc.strokeLine(x * cs, 0, x * cs, h);
        for (int y = 0; y <= Constants.BOARD_HEIGHT; y++) gc.strokeLine(0, y * cs, w, y * cs);

        // Border
        gc.setStroke(Color.web("#00ff00"));
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, w, h);

        // Locked cells
        for (int y = 0; y < Constants.BOARD_HEIGHT; y++) {
            for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                if (board[y][x] != 0) {
                    TetrominoType t = idToType(board[y][x]);
                    if (t != null) drawCell(gc, x, y, t.getColor(), 1.0);
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

        // Game Over overlay
        if (isGameOver) {
            gc.setFill(Color.color(0, 0, 0, 0.6));
            gc.fillRect(0, 0, w, h);
            gc.setFill(Color.web("#ff4444"));
            gc.setFont(Font.font("Courier New", 28));
            gc.fillText("GAME OVER", w / 2 - 80, h / 2);
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

    public void drawNextBlock() {
        GraphicsContext gc = nextGc;
        double w = nextBlockCanvas.getWidth();
        double h = nextBlockCanvas.getHeight();

        gc.setFill(Color.web("#0f0d1a"));
        gc.fillRect(0, 0, w, h);

        gc.setStroke(Color.web("#00ff00"));
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, w, h);

        int[][] shape = nextPiece.getType().getShape(nextPiece.getRotation());
        int cs = Constants.BLOCK_SIZE;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    drawCell(gc, col + 1, row + 1, nextPiece.getType().getColor(), 1.0, nextGc, cs);
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

    private int typeId(TetrominoType type) {
        return switch (type) {
            case I -> 1; case O -> 2; case T -> 3;
            case S -> 4; case Z -> 5; case J -> 6; case L -> 7;
        };
    }

    private TetrominoType idToType(int id) {
        return switch (id) {
            case 1 -> TetrominoType.I; case 2 -> TetrominoType.O;
            case 3 -> TetrominoType.T; case 4 -> TetrominoType.S;
            case 5 -> TetrominoType.Z; case 6 -> TetrominoType.J;
            case 7 -> TetrominoType.L; default -> null;
        };
    }

    public void gameOver() {
        if (gameLoop != null) gameLoop.stop();
        sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
    }
}