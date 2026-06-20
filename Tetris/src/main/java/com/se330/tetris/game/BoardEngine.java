package com.se330.tetris.game;

import com.se330.tetris.service.GameContext;
import com.se330.tetris.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BoardEngine {

    public static final int TIME_BLOCK_ID = 10;
    private static final double TIME_BLOCK_CHANCE = 0.05;

    private final int[][] board = new int[Constants.BOARD_HEIGHT][Constants.BOARD_WIDTH];
    private final GameContext gameContext;
    private final Random rng;

    public BoardEngine(GameContext gameContext, Random rng) {
        this.gameContext = gameContext;
        this.rng = rng;
    }

    public int[][] getBoard() { return board; }

    public void reset() {
        for (int[] row : board) java.util.Arrays.fill(row, 0);
    }

    // --- Collision detection ---

    public boolean canMove(Piece piece, int dx, int dy, int rotation, List<Piece> suspended) {
        int[][] shape = piece.getType().getShape(rotation);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int nx = piece.getX() + col + dx;
                    int ny = piece.getY() + row + dy;
                    if (nx < 0 || nx >= Constants.BOARD_WIDTH) return false;
                    if (ny < 0 || ny >= Constants.BOARD_HEIGHT) return false;
                    if (piece.getType() != TetrominoType.TRANSPARENT) {
                        if (board[ny][nx] != 0) return false;
                        if (isCellOccupiedBySuspended(nx, ny, piece, suspended)) return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean canMoveSuspended(Piece piece, int dx, int dy, Piece current, List<Piece> suspended) {
        int[][] shape = piece.getType().getShape(piece.getRotation());
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int nx = piece.getX() + col + dx;
                    int ny = piece.getY() + row + dy;
                    if (nx < 0 || nx >= Constants.BOARD_WIDTH) return false;
                    if (ny < 0 || ny >= Constants.BOARD_HEIGHT) return false;
                    if (board[ny][nx] != 0) return false;
                    if (isCellOccupiedBySuspended(nx, ny, piece, suspended)) return false;
                    if (isCellOccupiedByCurrent(nx, ny, current)) return false;
                }
            }
        }
        return true;
    }

    public boolean canPlaceType(Piece piece, TetrominoType type) {
        int[][] shape = type.getShape(piece.getRotation());
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int nx = piece.getX() + col;
                    int ny = piece.getY() + row;
                    if (nx < 0 || nx >= Constants.BOARD_WIDTH) return false;
                    if (ny < 0 || ny >= Constants.BOARD_HEIGHT) return false;
                    if (board[ny][nx] != 0) return false;
                }
            }
        }
        return true;
    }

    private boolean isCellOccupiedBySuspended(int x, int y, Piece ignore, List<Piece> suspended) {
        for (Piece p : suspended) {
            if (p == ignore) continue;
            int[][] shape = p.getType().getShape(p.getRotation());
            for (int row = 0; row < 4; row++)
                for (int col = 0; col < 4; col++)
                    if (shape[row][col] == 1 && p.getX() + col == x && p.getY() + row == y)
                        return true;
        }
        return false;
    }

    private boolean isCellOccupiedByCurrent(int x, int y, Piece current) {
        if (current == null) return false;
        int[][] shape = current.getType().getShape(current.getRotation());
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++)
                if (shape[row][col] == 1 && current.getX() + col == x && current.getY() + row == y)
                    return true;
        return false;
    }

    // --- Board modification ---

    public LockResult lockPiece(Piece piece) {
        int[][] shape = piece.getType().getShape(piece.getRotation());
        int minCol = Integer.MAX_VALUE, maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE, maxRow = Integer.MIN_VALUE;
        List<int[]> cells = new ArrayList<>();

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int x = piece.getX() + col;
                    int y = piece.getY() + row;
                    if (y >= 0 && y < Constants.BOARD_HEIGHT && x >= 0 && x < Constants.BOARD_WIDTH) {
                        board[y][x] = getLockedCellId(piece.getType());
                        cells.add(new int[]{x, y});
                        if (x < minCol) minCol = x;
                        if (x > maxCol) maxCol = x;
                        if (y < minRow) minRow = y;
                        if (y > maxRow) maxRow = y;
                    }
                }
            }
        }
        return new LockResult(minCol, maxCol, minRow, maxRow, cells);
    }

    public void mergePiece(Piece piece) {
        int[][] shape = piece.getType().getShape(piece.getRotation());
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++)
                if (shape[row][col] == 1) {
                    int x = piece.getX() + col;
                    int y = piece.getY() + row;
                    if (y >= 0 && y < Constants.BOARD_HEIGHT && x >= 0 && x < Constants.BOARD_WIDTH)
                        board[y][x] = getLockedCellId(piece.getType());
                }
    }

    public void clearRows(List<Integer> rows) {
        // Must process rows top-to-bottom (ascending index) so each shift-down
        // does not move a still-full row into a position we already skipped.
        List<Integer> sorted = new ArrayList<>(rows);
        Collections.sort(sorted);
        for (int row : sorted) {
            for (int r = row; r > 0; r--)
                board[r] = board[r - 1].clone();
            board[0] = new int[Constants.BOARD_WIDTH];
        }
    }

    /** Clears 3x3 radius around bomb center. Returns cells that were cleared (for visual effects). */
    public List<ClearedCell> clearBombRadius(int centerX, int centerY) {
        List<ClearedCell> snaps = new ArrayList<>();
        for (int y = centerY - 1; y <= centerY + 1; y++) {
            if (y < 0 || y >= Constants.BOARD_HEIGHT) continue;
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                if (x < 0 || x >= Constants.BOARD_WIDTH) continue;
                if (board[y][x] != 0) {
                    TetrominoType t = idToType(board[y][x]);
                    if (t != null && t != TetrominoType.BOMB)
                        snaps.add(new ClearedCell(x, y, t));
                }
                board[y][x] = 0;
            }
        }
        return snaps;
    }

    /** Clears the active piece bounds expanded by padding cells in every direction. */
    public List<ClearedCell> clearBombBounds(Piece piece, int padding) {
        int[][] shape = piece.getType().getShape(piece.getRotation());
        int minX = Constants.BOARD_WIDTH;
        int maxX = -1;
        int minY = Constants.BOARD_HEIGHT;
        int maxY = -1;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int x = piece.getX() + col;
                    int y = piece.getY() + row;
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        List<ClearedCell> snaps = new ArrayList<>();
        if (maxX < minX || maxY < minY) {
            return snaps;
        }

        int fromX = Math.max(0, minX - padding);
        int toX = Math.min(Constants.BOARD_WIDTH - 1, maxX + padding);
        int fromY = Math.max(0, minY - padding);
        int toY = Math.min(Constants.BOARD_HEIGHT - 1, maxY + padding);

        for (int y = fromY; y <= toY; y++) {
            for (int x = fromX; x <= toX; x++) {
                if (board[y][x] != 0) {
                    TetrominoType t = idToType(board[y][x]);
                    if (t != null && t != TetrominoType.BOMB) {
                        snaps.add(new ClearedCell(x, y, t));
                    }
                }
                board[y][x] = 0;
            }
        }
        return snaps;
    }

    // --- Board queries ---

    public List<Integer> findFullRows() {
        List<Integer> fullRows = new ArrayList<>();
        for (int y = Constants.BOARD_HEIGHT - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                if (board[y][x] == 0) { full = false; break; }
            }
            if (full) fullRows.add(y);
        }
        return fullRows;
    }

    public boolean rowHasTimeBlock(int row) {
        for (int x = 0; x < Constants.BOARD_WIDTH; x++)
            if (board[row][x] == TIME_BLOCK_ID) return true;
        return false;
    }

    public java.util.List<int[]> getTimeBlockCells(java.util.List<Integer> rows) {
        java.util.List<int[]> cells = new java.util.ArrayList<>();
        for (int row : rows)
            for (int x = 0; x < Constants.BOARD_WIDTH; x++)
                if (board[row][x] == TIME_BLOCK_ID)
                    cells.add(new int[]{x, row});
        return cells;
    }

    public int getDropDistance(Piece piece, List<Piece> suspended) {
        int d = 0;
        while (canMove(piece, 0, d + 1, piece.getRotation(), suspended)) d++;
        return d;
    }

    public int getStackTopRow() {
        for (int r = 0; r < Constants.BOARD_HEIGHT; r++)
            for (int c = 0; c < Constants.BOARD_WIDTH; c++)
                if (board[r][c] != 0) return r;
        return Constants.BOARD_HEIGHT;
    }

    // --- Type mapping ---

    public int typeId(TetrominoType type) {
        return switch (type) {
            case I -> 1; case O -> 2; case T -> 3; case S -> 4;
            case Z -> 5; case J -> 6; case L -> 7; case BOMB -> 8;
            case TRANSPARENT -> 9;
        };
    }

    public TetrominoType idToType(int id) {
        return switch (id) {
            case 1 -> TetrominoType.I; case 2 -> TetrominoType.O;
            case 3 -> TetrominoType.T; case 4 -> TetrominoType.S;
            case 5 -> TetrominoType.Z; case 6 -> TetrominoType.J;
            case 7 -> TetrominoType.L; case 8 -> TetrominoType.BOMB;
            case 9 -> TetrominoType.TRANSPARENT; default -> null;
        };
    }

    private int getLockedCellId(TetrominoType type) {
        if (gameContext.getGameMode() == GameContext.GameMode.TIME_ATTACK
                && type != TetrominoType.BOMB
                && rng.nextDouble() < TIME_BLOCK_CHANCE) {
            return TIME_BLOCK_ID;
        }
        return typeId(type);
    }

    // --- Inner result types ---

    public static class LockResult {
        public final int minCol, maxCol, minRow, maxRow;
        public final List<int[]> cells;

        LockResult(int minCol, int maxCol, int minRow, int maxRow, List<int[]> cells) {
            this.minCol = minCol; this.maxCol = maxCol;
            this.minRow = minRow; this.maxRow = maxRow;
            this.cells = cells;
        }
    }

    public static class ClearedCell {
        public final int boardX, boardY;
        public final TetrominoType type;

        ClearedCell(int x, int y, TetrominoType type) {
            this.boardX = x; this.boardY = y; this.type = type;
        }
    }
}
