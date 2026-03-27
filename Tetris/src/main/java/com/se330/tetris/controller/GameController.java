package com.se330.tetris.controller;

import com.se330.tetris.core.TetrisApp;
import com.se330.tetris.game.Piece;
import com.se330.tetris.game.TetrominoType;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class GameController {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private AnimationTimer timer;
    private Piece currentPiece;
    private final int[][] board = new int[TetrisApp.BOARD_HEIGHT][TetrisApp.BOARD_WIDTH];

    private long lastFallTime = 0;
    private final long fallIntervalNs = 500_000_000L;
    private final java.util.Random rng = new java.util.Random();

    public GameController(Canvas canvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        currentPiece = new Piece(TetrominoType.T, 3, 0);
    }

    public void start() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastFallTime >= fallIntervalNs) {
                    updateFall();
                    lastFallTime = now;
                }
                render();
            }
        };
        timer.start();
    }

    private void render() {
        gc.setFill(Color.web("#0f0d1a"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setStroke(Color.web("#2b2740"));
        for (int x = 0; x <= TetrisApp.BOARD_WIDTH; x++) {
            double px = x * TetrisApp.CELL_SIZE;
            gc.strokeLine(px, 0, px, TetrisApp.BOARD_HEIGHT * TetrisApp.CELL_SIZE);
        }
        for (int y = 0; y <= TetrisApp.BOARD_HEIGHT; y++) {
            double py = y * TetrisApp.CELL_SIZE;
            gc.strokeLine(0, py, TetrisApp.BOARD_WIDTH * TetrisApp.CELL_SIZE, py);
        }

        drawBoard();
        drawPiece(currentPiece);
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

                    gc.setFill(piece.getType().getColor());
                    gc.fillRect(px, py, TetrisApp.CELL_SIZE, TetrisApp.CELL_SIZE);

                    gc.setStroke(Color.web("#111111"));
                    gc.strokeRect(px, py, TetrisApp.CELL_SIZE, TetrisApp.CELL_SIZE);
                }
            }
        }
    }

    private void spawnPiece() {
        TetrominoType[] types = TetrominoType.values();
        TetrominoType type = types[rng.nextInt(types.length)];
        currentPiece = new Piece(type, 3, 0);
    }

    private void updateFall() {
        if (canMove(0, 1, currentPiece.getRotation())) {
            currentPiece.setY(currentPiece.getY() + 1);
        } else {
            lockPiece();
            spawnPiece();
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

                        gc.setFill(type.getColor());
                        gc.fillRect(px, py, TetrisApp.CELL_SIZE, TetrisApp.CELL_SIZE);
                        gc.setStroke(Color.web("#111111"));
                        gc.strokeRect(px, py, TetrisApp.CELL_SIZE, TetrisApp.CELL_SIZE);
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
            case DOWN -> {
                if (canMove(0, 1, currentPiece.getRotation())) {
                    currentPiece.setY(currentPiece.getY() + 1);
                }
            }
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
}
