package com.se330.tetris.game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GlitchTearEffect {

    private enum Phase { TEAR, WHITEOUT, DONE }

    private static final double TEAR_DUR     = 0.55;
    private static final double WHITEOUT_DUR = 0.15;
    private static final double T_WHITEOUT   = TEAR_DUR;
    private static final double T_DONE       = T_WHITEOUT + WHITEOUT_DUR;

    private final int[][]  boardSnapshot;
    private final int      boardCols, boardRows;
    private final int      blockSize;
    private final Color    bgColor;
    private final Runnable onDone;
    private final Random   rng = new Random();

    // Strip data
    private final int[]    sY;       // top pixel y of each strip
    private final int[]    sH;       // height in pixels
    private final double[] sDir;     // +1 = right, -1 = left
    private final double[] sMaxOff;  // max pixel displacement at peak

    private double  elapsed   = 0;
    private Phase   phase     = Phase.TEAR;
    private boolean doneFired = false;

    public GlitchTearEffect(int[][] board, int blockSize, Color bgColor, Runnable onDone) {
        this.boardCols = board[0].length;
        this.boardRows = board.length;
        this.blockSize = blockSize;
        this.bgColor   = bgColor;
        this.onDone    = onDone;

        boardSnapshot = new int[boardRows][boardCols];
        for (int r = 0; r < boardRows; r++) boardSnapshot[r] = board[r].clone();

        // Build strips of 1-3 rows covering the full board height
        int totalH = boardRows * blockSize;
        List<Integer> starts  = new ArrayList<>();
        List<Integer> heights = new ArrayList<>();
        int y = 0;
        while (y < totalH) {
            int h = Math.min((1 + rng.nextInt(3)) * blockSize, totalH - y);
            starts.add(y);
            heights.add(h);
            y += h;
        }

        int n   = starts.size();
        sY      = new int[n];
        sH      = new int[n];
        sDir    = new double[n];
        sMaxOff = new double[n];
        for (int i = 0; i < n; i++) {
            sY[i]      = starts.get(i);
            sH[i]      = heights.get(i);
            sDir[i]    = rng.nextBoolean() ? 1.0 : -1.0;
            sMaxOff[i] = 30 + rng.nextDouble() * 70;
        }
    }

    public void update(double dt) {
        if (phase == Phase.DONE) return;
        elapsed += dt;
        if      (elapsed < T_WHITEOUT) phase = Phase.TEAR;
        else if (elapsed < T_DONE)     phase = Phase.WHITEOUT;
        else {
            phase = Phase.DONE;
            if (!doneFired) { doneFired = true; onDone.run(); }
        }
    }

    public boolean isDone() { return phase == Phase.DONE; }

    /** Returns 0 outside the whiteout phase; peaks at 1.0 at the midpoint. */
    public double getWhiteoutAlpha() {
        if (phase != Phase.WHITEOUT) return 0;
        double prog = (elapsed - T_WHITEOUT) / WHITEOUT_DUR;
        return Math.max(0, prog < 0.4 ? prog / 0.4 : 1.0 - (prog - 0.4) / 0.6);
    }

    public void render(GraphicsContext gc, double cw, double ch) {
        if (phase == Phase.DONE) return;

        double boardW = boardCols * blockSize;
        double boardH = boardRows * blockSize;

        // Clear board area with background color
        gc.setFill(bgColor);
        gc.fillRect(0, 0, boardW, boardH);

        if (phase == Phase.TEAR) {
            double tearProg = Math.min(1.0, elapsed / TEAR_DUR);
            double t2       = tearProg * tearProg;       // ease-in: slow start, fast end
            double caOff    = 3.0 + tearProg * 12.0;    // chromatic aberration grows over time

            for (int i = 0; i < sY.length; i++) {
                double baseOff = sDir[i] * sMaxOff[i] * t2;
                // 20% chance of per-strip random jitter this frame
                if (rng.nextDouble() < 0.20) {
                    baseOff += (rng.nextDouble() - 0.5) * 30 * tearProg;
                }

                gc.save();
                // Clip to this strip's y band; wide x so content can fly off the sides
                gc.beginPath();
                gc.rect(-5000, sY[i], boardW + 10000, sH[i]);
                gc.clip();

                // Red fringe: offset opposite to travel direction
                drawStripCells(gc, i, baseOff - caOff * sDir[i], true, false, 0.65);
                // Cyan fringe: offset further in travel direction
                drawStripCells(gc, i, baseOff + caOff * sDir[i], false, true, 0.65);
                // Normal strip on top
                drawStripCells(gc, i, baseOff, false, false, 1.0);

                gc.restore();
            }

            // Whole-board flicker overlay
            if (rng.nextDouble() < 0.35) {
                gc.setFill(Color.color(1, 1, 1, rng.nextDouble() * 0.20 * tearProg));
                gc.fillRect(0, 0, boardW, boardH);
            }
            // Occasional single-strip bright flash
            if (rng.nextDouble() < 0.15) {
                int fi = rng.nextInt(sY.length);
                gc.setFill(Color.color(1, 1, 1, 0.5 * tearProg));
                gc.fillRect(0, sY[fi], boardW, sH[fi]);
            }

        } else if (phase == Phase.WHITEOUT) {
            // Peaks at midpoint, then fades to nothing so the scene switch is invisible
            double prog  = (elapsed - T_WHITEOUT) / WHITEOUT_DUR;
            double alpha = prog < 0.4 ? prog / 0.4 : 1.0 - (prog - 0.4) / 0.6;
            gc.setFill(Color.color(1, 1, 1, Math.max(0, alpha)));
            gc.fillRect(0, 0, boardW, boardH);
        }
    }

    private void drawStripCells(GraphicsContext gc, int stripIdx,
                                double xOff, boolean redTint, boolean cyanTint, double alpha) {
        int rowStart = sY[stripIdx] / blockSize;
        int rowEnd   = (sY[stripIdx] + sH[stripIdx]) / blockSize;

        gc.setGlobalAlpha(alpha);
        for (int r = rowStart; r < rowEnd; r++) {
            for (int c = 0; c < boardCols; c++) {
                if (boardSnapshot[r][c] == 0) continue;
                TetrominoType type = idToType(boardSnapshot[r][c]);
                if (type == null) continue;
                Color col = type.getColor();
                if (redTint) {
                    col = Color.color(
                        Math.min(1.0, col.getRed()   * 0.4 + 0.6),
                        col.getGreen() * 0.1,
                        col.getBlue()  * 0.1
                    );
                } else if (cyanTint) {
                    col = Color.color(
                        col.getRed()   * 0.1,
                        Math.min(1.0, col.getGreen() * 0.4 + 0.6),
                        Math.min(1.0, col.getBlue()  * 0.4 + 0.6)
                    );
                }
                gc.setFill(col);
                gc.fillRect(c * blockSize + xOff, r * blockSize, blockSize, blockSize);
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    private TetrominoType idToType(int id) {
        return switch (id) {
            case 1 -> TetrominoType.I; case 2 -> TetrominoType.O;
            case 3 -> TetrominoType.T; case 4 -> TetrominoType.S;
            case 5 -> TetrominoType.Z; case 6 -> TetrominoType.J;
            case 7 -> TetrominoType.L; default -> null;
        };
    }
}
