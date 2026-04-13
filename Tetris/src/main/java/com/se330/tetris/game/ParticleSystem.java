package com.se330.tetris.game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ParticleSystem {

    private static class LightColumn {
        double x, fromY, toY, width, life;
        Color color;

        LightColumn(double x, double fromY, double toY, double width, Color color) {
            this.x = x; this.fromY = fromY; this.toY = toY;
            this.width = width; this.life = 1.0; this.color = color;
        }

        void update(double dt) { life -= dt * 3.5; }
        boolean isDead() { return life <= 0; }
    }

    private static class HorizontalBeam {
        double x, y, width, height, life;
        Color  color;
        boolean leftSide; // true = left of board, false = right

        HorizontalBeam(double x, double y, double width, double height,
                       boolean leftSide, Color color) {
            this.x = x; this.y = y;
            this.width = width; this.height = height;
            this.leftSide = leftSide;
            this.life = 1.0; this.color = color;
        }

        void update(double dt) { life = Math.max(0, life - dt * 4.0); }
        boolean isDead()       { return life <= 0; }
    }

    private final List<Particle>       particles = new ArrayList<>();
    private final List<LightColumn>    columns     = new ArrayList<>();
    private final List<HorizontalBeam> beams       = new ArrayList<>();
    private final Random rng = new Random();

    public void emitLockParticles(double pixelX, double pixelY, Color color, int count) {
        for (int i = 0; i < count; i++) {
            double vy    = -(rng.nextDouble() * 280 + 180);
            double vx    = (rng.nextDouble() - 0.5) * 30;
            double px    = pixelX + rng.nextDouble() * 28 + 1;
            double py    = pixelY + rng.nextDouble() * 10;
            double decay = rng.nextDouble() * 1.5 + 3.0;
            double size  = rng.nextDouble() * 3.0 + 2.0;
            particles.add(new Particle(px, py, vx, vy, decay, color, size));
        }
    }

    public void emitLightColumn(double leftPixelX, double fromPixelY, double toPixelY,
                                double spanWidth, Color color) {
        columns.add(new LightColumn(leftPixelX, fromPixelY, toPixelY, spanWidth, color));
    }

    /**
     * Emit sparks from the corners of each actual occupied cell in the piece.
     *
     * @param pieceX    piece grid X origin
     * @param pieceY    piece grid Y origin
     * @param shape     4x4 shape array
     * @param blockSize size of one cell in pixels
     * @param color     tetromino color
     */
    public void emitRowBurst(double cellPixelX, double cellPixelY, Color color, int count) {
        for (int i = 0; i < count; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double speed = rng.nextDouble() * 220 + 80;
            double vx    = Math.cos(angle) * speed;
            double vy    = Math.sin(angle) * speed;
            double px    = cellPixelX + rng.nextDouble() * 26 + 1;
            double py    = cellPixelY + rng.nextDouble() * 26 + 1;
            double decay = rng.nextDouble() * 2.0 + 3.5;
            double size  = rng.nextDouble() * 3.5 + 1.5;
            Color  c     = (i % 3 == 0) ? Color.WHITE : color.brighter();
            particles.add(new Particle(px, py, vx, vy, decay, c, size));
        }
    }

    public void emitCornerSparks(int pieceX, int pieceY, int[][] shape, int blockSize, Color color) {
        // Corner offsets (dx, dy) within a cell and their outward directions
        int[][] cellCorners = {
            { 0, 0, -1, -1 }, // top-left     → NW
            { 1, 0,  1, -1 }, // top-right    → NE
            { 1, 1,  1,  1 }, // bottom-right → SE
            { 0, 1, -1,  1 }, // bottom-left  → SW
        };

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] != 1) continue;
                int bx = pieceX + col;
                int by = pieceY + row;

                for (int[] corner : cellCorners) {
                    double px   = (bx + corner[0]) * blockSize;
                    double py   = (by + corner[1]) * blockSize;
                    double dirX = corner[2];
                    double dirY = corner[3];

                    // 2 sparks per corner
                    for (int i = 0; i < 2; i++) {
                        double spread = Math.toRadians(rng.nextDouble() * 35 - 17.5);
                        double cos    = Math.cos(spread), sin = Math.sin(spread);
                        double bvx    = dirX * cos - dirY * sin;
                        double bvy    = dirX * sin + dirY * cos;
                        double speed  = rng.nextDouble() * 100 + 70;
                        double decay  = rng.nextDouble() * 2.0 + 4.0;
                        double size   = rng.nextDouble() * 1.5 + 1.0;
                        Color c2      = (i % 2 == 0) ? color.brighter() : Color.WHITE;
                        particles.add(new Particle(px, py, bvx * speed, bvy * speed, decay, c2, size));
                    }
                }
            }
        }
    }

    /**
     * Emit a pair of horizontal beams (left + right) from the board edges at the given row.
     *
     * @param leftBaseX  x pixel of the board's left edge in vfxCanvas coordinates
     * @param rightBaseX x pixel of the board's right edge in vfxCanvas coordinates
     * @param rowY       top pixel of the cleared row (in vfxCanvas y, same as game y)
     * @param rowH       height of one row in pixels
     * @param color      beam color
     */
    public void emitRowBeams(double leftBaseX, double rightBaseX,
                              double rowY, double rowH, Color color) {
        double flashW = 120.0; // width of the flash bar extending outward from each edge
        Color  bright = color.brighter().brighter();
        // Left flash: extends leftward from the board's left edge
        beams.add(new HorizontalBeam(leftBaseX - flashW, rowY, flashW, rowH, true,  bright));
        // Right flash: extends rightward from the board's right edge
        beams.add(new HorizontalBeam(rightBaseX,         rowY, flashW, rowH, false, bright));
    }

    public void update(double dt) {
        Iterator<Particle>       pit = particles.iterator();
        while (pit.hasNext()) { Particle p = pit.next(); p.update(dt); if (p.isDead()) pit.remove(); }

        Iterator<LightColumn>    cit = columns.iterator();
        while (cit.hasNext()) { LightColumn c = cit.next(); c.update(dt); if (c.isDead()) cit.remove(); }

        Iterator<HorizontalBeam> bit = beams.iterator();
        while (bit.hasNext()) { HorizontalBeam b = bit.next(); b.update(dt); if (b.isDead()) bit.remove(); }

    }

    public void render(GraphicsContext gc) {
        // Light columns
        for (LightColumn c : columns) {
            double a = Math.max(0, c.life);
            double beamHeight = c.toY - c.fromY;
            if (beamHeight <= 0) continue;

            LinearGradient outer = new LinearGradient(
                0, c.fromY, 0, c.toY, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(c.color.getRed(), c.color.getGreen(), c.color.getBlue(), 0.0)),
                new Stop(1.0, Color.color(c.color.getRed(), c.color.getGreen(), c.color.getBlue(), 0.45 * a))
            );
            gc.setFill(outer);
            gc.fillRect(c.x, c.fromY, c.width, beamHeight);

        }

        // Particles
        for (Particle p : particles) {
            gc.setGlobalAlpha(Math.max(0, p.life));
            gc.setFill(p.color);
            gc.fillRect(p.x, p.y, p.size, p.size);
        }
        gc.setGlobalAlpha(1.0);
    }

    /** Renders horizontal beams onto the vfxCanvas (cleared each frame by caller). */
    public void renderBeams(GraphicsContext gc) {
        double cw = gc.getCanvas().getWidth();
        double ch = gc.getCanvas().getHeight();
        gc.clearRect(0, 0, cw, ch);

        for (HorizontalBeam b : beams) {
            if (b.life <= 0) continue;
            double a = 0.85 * b.life;
            double r = b.color.getRed(), g = b.color.getGreen(), bl = b.color.getBlue();
            // Left flash: bright at right edge (board edge), transparent at left
            // Right flash: bright at left edge (board edge), transparent at right
            double brightX = b.leftSide ? b.x + b.width : b.x;
            double fadeX   = b.leftSide ? b.x           : b.x + b.width;
            LinearGradient grad = new LinearGradient(
                brightX, 0, fadeX, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(r, g, bl, a)),
                new Stop(1.0, Color.color(r, g, bl, 0.0))
            );
            gc.setFill(grad);
            gc.fillRect(b.x, b.y, b.width, b.height);
        }
    }

    public void clear() {
        particles.clear();
        columns.clear();
        beams.clear();
    }
}
