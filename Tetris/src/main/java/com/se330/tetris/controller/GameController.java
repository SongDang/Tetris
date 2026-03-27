package com.se330.tetris.controller;

import com.se330.tetris.core.TetrisApp;
import com.se330.tetris.game.Piece;
import com.se330.tetris.game.TetrominoType;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class GameController {
    private final Canvas boardCanvas;
    private final Canvas nextCanvas;
    private final GraphicsContext boardGc;
    private final GraphicsContext nextGc;
    private AnimationTimer timer;
    private Piece currentPiece;
    private Piece nextPiece;
    private final int[][] board = new int[TetrisApp.BOARD_HEIGHT][TetrisApp.BOARD_WIDTH];

    private long lastFallTime = 0;
    private final java.util.Random rng = new java.util.Random();
    private final long fallIntervalNs = 500_000_000L;

    public GameController(Canvas boardCanvas, Canvas nextCanvas) {
        this.boardCanvas = boardCanvas;
        this.nextCanvas = nextCanvas;
        this.boardGc = boardCanvas.getGraphicsContext2D();
        this.nextGc = nextCanvas.getGraphicsContext2D();
        spawnPiece();
    }

    public void start() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long interval = fallIntervalNs;
                if (now - lastFallTime >= interval) {
                    updateFall();
                    lastFallTime = now;
                }
                render();
            }
        };
        timer.start();
    }

    private void render() {
        boardGc.setFill(Color.web("#0f0d1a"));
        boardGc.fillRect(0, 0, boardCanvas.getWidth(), boardCanvas.getHeight());

        boardGc.setStroke(Color.web("#2b2740"));
        for (int x = 0; x <= TetrisApp.BOARD_WIDTH; x++) {
            double px = x * TetrisApp.CELL_SIZE;
            boardGc.strokeLine(px, 0, px, TetrisApp.BOARD_HEIGHT * TetrisApp.CELL_SIZE);
        }
        for (int y = 0; y <= TetrisApp.BOARD_HEIGHT; y++) {
            double py = y * TetrisApp.CELL_SIZE;
            boardGc.strokeLine(0, py, TetrisApp.BOARD_WIDTH * TetrisApp.CELL_SIZE, py);
        }

        drawBoard();
        drawPiece(currentPiece);
        drawGhost(currentPiece);
        drawNext();
    }

    private void drawPiece(Piece piece) {
        int[][] shape = piece.getType().getShape(piece.getRotation());
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int x = piece.getX() + col;
                    int y = piece.getY() + row;
                    double px = x * TetrisApp.CELL_SIZE;
                    double py = y * TetrisApp.CELL_SIZE;

                    boardGc.setFill(piece.getType().getColor());
                    boardGc.fillRect(px, py, TetrisApp.CELL_SIZE, TetrisApp.CELL_SIZE);

                    boardGc.setStroke(Color.web("#111111"));
                    boardGc.strokeRect(px, py, TetrisApp.CELL_SIZE, TetrisApp.CELL_SIZE);
                }
            }
        }
    }

    private void spawnPiece() {
        if (nextPiece == null) {
            nextPiece = randomPiece();
        }
        currentPiece = nextPiece;
        nextPiece = randomPiece();
        currentPiece.setX(3);
        currentPiece.setY(0);
    }

    private void updateFall() {
        if (canMove(0, 1, currentPiece.getRotation())) {
            currentPiece.setY(currentPiece.getY() + 1);
        } else {
            lockPiece();
            spawnPiece();
            lastFallTime = System.nanoTime();
        }
    }

    private boolean canMove(int dx, int dy, int newRotation) {
        int[][] shape = currentPiece.getType().getShape(newRotation);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int x = currentPiece.getX() + col + dx;
                    int y = currentPiece.getY() + row + dy;

                    if (x < 0 || x >= TetrisApp.BOARD_WIDTH || y < 0 || y >= TetrisApp.BOARD_HEIGHT) {
                        return false;
                    }
                    if (board[y][x] != 0) {
                        return false;
                    }
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
                    if (y >= 0 && y < TetrisApp.BOARD_HEIGHT && x >= 0 && x < TetrisApp.BOARD_WIDTH) {
                        board[y][x] = typeId(currentPiece.getType());
                    }
                }
            }
        }
    }

    private void drawBoard() {
        for (int y = 0; y < TetrisApp.BOARD_HEIGHT; y++) {
            for (int x = 0; x < TetrisApp.BOARD_WIDTH; x++) {
                if (board[y][x] != 0) {
                    TetrominoType type = idToType(board[y][x]);
                    if (type != null) {
                        double px = x * TetrisApp.CELL_SIZE;
                        double py = y * TetrisApp.CELL_SIZE;

                        boardGc.setFill(type.getColor());
                        boardGc.fillRect(px, py, TetrisApp.CELL_SIZE, TetrisApp.CELL_SIZE);
                        boardGc.setStroke(Color.web("#111111"));
                        boardGc.strokeRect(px, py, TetrisApp.CELL_SIZE, TetrisApp.CELL_SIZE);
                    }
                }
            }
        }
    }

    public void onKeyPressed(javafx.scene.input.KeyEvent event) {
        switch (event.getCode()) {
            case LEFT -> {
                if (canMove(-1, 0, currentPiece.getRotation())) {
                    currentPiece.setX(currentPiece.getX() - 1);
                }
            }
            case RIGHT -> {
                if (canMove(1, 0, currentPiece.getRotation())) {
                    currentPiece.setX(currentPiece.getX() + 1);
                }
            }
            case DOWN -> hardDrop();
            case UP -> {
                int newRot = (currentPiece.getRotation() + 1) % 4;
                if (canMove(0, 0, newRot)) {
                    currentPiece.setRotation(newRot);
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

    private int getDropDistance(Piece piece) {
        int distance = 0;
        while (canMove(0, distance + 1, piece.getRotation())) {
            distance++;
        }
        return distance;
    }

    private void drawGhost(Piece piece) {
        int drop = getDropDistance(piece);
        int[][] shape = piece.getType().getShape(piece.getRotation());

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int x = piece.getX() + col;
                    int y = piece.getY() + row + drop;
                    double px = x * TetrisApp.CELL_SIZE;
                    double py = y * TetrisApp.CELL_SIZE;

                    boardGc.setFill(piece.getType().getColor().deriveColor(0, 1, 1, 0.25));
                    boardGc.fillRect(px, py, TetrisApp.CELL_SIZE, TetrisApp.CELL_SIZE);

                    boardGc.setStroke(Color.web("#111111"));
                    boardGc.strokeRect(px, py, TetrisApp.CELL_SIZE, TetrisApp.CELL_SIZE);
                }
            }
        }
    }

    private void hardDrop() {
        int drop = getDropDistance(currentPiece);
        currentPiece.setY(currentPiece.getY() + drop);
        lockPiece();
        spawnPiece();
        lastFallTime = System.nanoTime();
    }

    private Piece randomPiece() {
        TetrominoType[] types = TetrominoType.values();
        TetrominoType type = types[rng.nextInt(types.length)];
        return new Piece(type, 0, 0);
    }

    private void drawNext() {
        nextGc.setFill(Color.web("#0f0d1a"));
        nextGc.fillRect(0, 0, nextCanvas.getWidth(), nextCanvas.getHeight());

        int[][] shape = nextPiece.getType().getShape(nextPiece.getRotation());

        int cell = TetrisApp.CELL_SIZE;
        int offsetX = 1;
        int offsetY = 1;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    double px = (col + offsetX) * cell;
                    double py = (row + offsetY) * cell;

                    nextGc.setFill(nextPiece.getType().getColor());
                    nextGc.fillRect(px, py, cell, cell);
                    nextGc.setStroke(Color.web("#111111"));
                    nextGc.strokeRect(px, py, cell, cell);
                }
            }
        }
    }
}
