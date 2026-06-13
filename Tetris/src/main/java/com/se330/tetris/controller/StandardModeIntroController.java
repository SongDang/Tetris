package com.se330.tetris.controller;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import com.se330.tetris.service.SceneManager;

public class StandardModeIntroController {

    @FXML private AnchorPane introPane;
    @FXML private Canvas mainCanvas;

    private GraphicsContext gc;
    private AnimationTimer timer;
    private double elapsed = 0;
    private long lastNano = 0;

    private static final double SPIN_START = 0.5;
    private static final double STEP_DUR   = 0.70;
    private static final double ROT_FRAC   = 0.72;
    private static final int    NUM_STEPS  = 4;
    private static final double SPIN_END   = SPIN_START + NUM_STEPS * STEP_DUR;
    private static final double FLASH_AT   = SPIN_END - 0.55;
    private static final double DONE_AT    = FLASH_AT + 0.45;

    private static final Color BLOCK_FILL = Color.web("#30285B");

    private static final int    CELL      = 92;
    private static final int    GAP       = 3;
    private static final double CY_OFFSET = 0.25 * CELL;

    private static final double[][] T_CELLS = {
        {0, -1}, {-1, 0}, {0, 0}, {1, 0}
    };

    private double rotAngle = 0;
    private double scale    = 0;

    @FXML
    private void initialize() {
        gc = mainCanvas.getGraphicsContext2D();
        mainCanvas.widthProperty().bind(introPane.widthProperty());
        mainCanvas.heightProperty().bind(introPane.heightProperty());

        lastNano = System.nanoTime();
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                double dt = Math.min((now - lastNano) / 1_000_000_000.0, 0.05);
                lastNano = now;
                elapsed += dt;
                tick(dt);
            }
        };
        timer.start();
    }

    private void tick(double dt) {
        double w = mainCanvas.getWidth();
        double h = mainCanvas.getHeight();
        double cx = w / 2, cy = h / 2;

        double t0 = Math.min(elapsed / SPIN_START, 1.0);
        scale = 1.0 - Math.pow(1.0 - t0, 3);

        if (elapsed >= SPIN_START && elapsed < SPIN_END) {
            double se    = elapsed - SPIN_START;
            int    step  = Math.min((int)(se / STEP_DUR), NUM_STEPS - 1);
            double stepT = (se - step * STEP_DUR) / STEP_DUR;
            rotAngle = step * (Math.PI / 2) + stepEase(stepT) * (Math.PI / 2);
        } else if (elapsed >= SPIN_END) {
            rotAngle = NUM_STEPS * (Math.PI / 2);
        }

        gc.clearRect(0, 0, w, h);

        gc.save();
        gc.translate(cx, cy);
        gc.scale(scale, scale);
        gc.rotate(Math.toDegrees(rotAngle));
        drawBlock();
        gc.restore();

        if (elapsed >= FLASH_AT) {
            double fa = Math.min((elapsed - FLASH_AT) / (DONE_AT - FLASH_AT), 1.0);
            gc.setFill(Color.WHITE.deriveColor(0, 1, 1, fa));
            gc.fillRect(0, 0, w, h);
        }

        if (elapsed >= DONE_AT) {
            timer.stop();
            SceneManager.getInstance().switchToScene(SceneManager.GAME_SCENE);
        }
    }

    private double stepEase(double t) {
        if (t >= ROT_FRAC) return 1.0;
        double s = t / ROT_FRAC;
        if (s < 0.18) {
            double p = s / 0.18;
            return -(0.5 - 0.5 * Math.cos(Math.PI * p)) * 0.13;
        } else if (s < 0.72) {
            double p = (s - 0.18) / 0.54;
            return -0.13 + (0.5 - 0.5 * Math.cos(Math.PI * p)) * 1.26;
        } else {
            double p = (s - 0.72) / 0.28;
            return 1.13 - (0.5 - 0.5 * Math.cos(Math.PI * p)) * 0.13;
        }
    }

    private void drawBlock() {
        gc.setFill(BLOCK_FILL);
        for (double[] cell : T_CELLS) {
            double x = cell[0] * CELL - CELL / 2.0 + GAP;
            double y = cell[1] * CELL + CY_OFFSET - CELL / 2.0 + GAP;
            double s = CELL - GAP * 2;
            gc.fillRect(x, y, s, s);
        }
    }
}
