package com.se330.tetris.controller;

import com.se330.tetris.core.TetrisApp;
import com.se330.tetris.util.Constants;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import com.se330.tetris.game.Piece;
import com.se330.tetris.game.TetrominoType;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;

public class GameController {

    @FXML
    private HBox gamePane;
    @FXML
    private Canvas gameCanvas;
    @FXML
    private Canvas nextBlockCanvas;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label levelLabel;
    @FXML
    private Label linesLabel;

    private SceneManager sceneManager;
    private GameContext gameContext;

    private GraphicsContext gameGc;
    private GraphicsContext nextGc;

    private final int[][] board = new int[Constants.BOARD_HEIGHT][Constants.BOARD_WIDTH];
    private Piece currentPiece;
    private Piece nextPiece;

    private AnimationTimer gameLoop;
    private boolean gamePaused = false;
    private boolean isGameOver = false;

    private long lastFallTime = 0;
    private long fallIntervalNs = 500_000_000L;

    private int mouseTargetColumn = -1;

    private final java.util.Random rng = new java.util.Random();

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext = GameContext.getInstance();
        gameContext.reset();

        gameGc = gameCanvas.getGraphicsContext2D();
        nextGc = nextBlockCanvas.getGraphicsContext2D();

        // Spawn hai mảnh đầu tiên
        nextPiece = randomPiece();
        spawnPiece();

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
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (gamePaused || isGameOver)
                    return;

                applyMouseTarget(now);

                if (now - lastFallTime >= fallIntervalNs) {
                    updateFall();
                    lastFallTime = now;
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

        int cleared = clearLines();
        if (cleared > 0) {
            addScore(cleared);
            addLines(cleared);
            updateLevel();
        }

        spawnPiece();

        if (!canMove(0, 0, currentPiece.getRotation())) {
            isGameOver = true;
            gameLoop.stop();
            sceneManager.switchToScene(SceneManager.RESULTS_SCENE); // từ HEAD
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
        int[][] shape = currentPiece.getType().getShape(currentPiece.getRotation());
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int x = currentPiece.getX() + col;
                    int y = currentPiece.getY() + row;
                    if (y >= 0 && y < Constants.BOARD_HEIGHT
                            && x >= 0 && x < Constants.BOARD_WIDTH) {
                        board[y][x] = typeId(currentPiece.getType());
                    }
                }
            }
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
        }
    }

    private void refreshLabels() {
        scoreLabel.setText(String.valueOf(gameContext.getScore()));
        levelLabel.setText(String.valueOf(gameContext.getLevel()));
        linesLabel.setText(String.valueOf(gameContext.getLines()));
    }

    private void hardDrop() {
        int drop = getDropDistance();
        currentPiece.setY(currentPiece.getY() + drop);
        lockAndSpawn();
    }

    private int getDropDistance() {
        int distance = 0;
        while (canMove(0, distance + 1, currentPiece.getRotation()))
            distance++;
        return distance;
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
                if (canMove(0, 0, nr))
                    currentPiece.setRotation(nr);
            }
            case SPACE -> hardDrop();
            case P -> handlePause();
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
            int nextRotation = (currentPiece.getRotation() + 1) % 4;
            if (canMove(0, 0, nextRotation)) {
                currentPiece.setRotation(nextRotation);
            }
            event.consume();
        }
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

    private void render() {
        drawGameBoard();
        drawNextBlock();
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

        // Ghost
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

        // Current piece
        int[][] shape = currentPiece.getType().getShape(currentPiece.getRotation());
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    drawCell(gc,
                            currentPiece.getX() + col,
                            currentPiece.getY() + row,
                            currentPiece.getType().getColor(),
                            1.0);
                }
            }
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
            case I -> 1;
            case O -> 2;
            case T -> 3;
            case S -> 4;
            case Z -> 5;
            case J -> 6;
            case L -> 7;
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
            default -> null;
        };
    }

    public void gameOver() {
        if (gameLoop != null)
            gameLoop.stop();
        sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
    }
}