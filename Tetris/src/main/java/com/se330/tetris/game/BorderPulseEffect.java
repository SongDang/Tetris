package com.se330.tetris.game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class BorderPulseEffect {

    private enum Phase { PULSE1, GAP, PULSE2, COOLDOWN, DONE }

    private static final double PULSE_DUR    = 0.40;
    private static final double GAP_DUR      = 0.03;
    private static final double COOLDOWN_DUR = 0.40;

    private static final double T_P1_END   = PULSE_DUR;
    private static final double T_P2_START = T_P1_END   + GAP_DUR;
    private static final double T_P2_END   = T_P2_START + PULSE_DUR;
    private static final double T_DONE     = T_P2_END   + COOLDOWN_DUR;

    private double elapsed = 0;
    private Phase  phase   = Phase.PULSE1;

    public void update(double dt) {
        if (phase == Phase.DONE) return;
        elapsed += dt;
        if      (elapsed < T_P1_END)   phase = Phase.PULSE1;
        else if (elapsed < T_P2_START) phase = Phase.GAP;
        else if (elapsed < T_P2_END)   phase = Phase.PULSE2;
        else if (elapsed < T_DONE)     phase = Phase.COOLDOWN;
        else                           phase = Phase.DONE;
    }

    public boolean isDone() { return phase == Phase.DONE; }

    public void render(GraphicsContext gameGc, GraphicsContext vfxGc,
                       double cw, double ch, double boardXInVfx) {
        switch (phase) {
            case PULSE1 -> drawPulse(gameGc, cw, ch, elapsed);
            case PULSE2 -> drawPulse(gameGc, cw, ch, elapsed - T_P2_START);
            default     -> {}
        }
    }

    private static void drawPulse(GraphicsContext gc, double cw, double ch, double localT) {
        double alpha = pulseAlpha(localT);

        // Vignette: faint red fill bleeding inward from all four edges
        double vig = 28;
        gc.setFill(Color.color(1, 0, 0, alpha * 0.10));
        gc.fillRect(0,        0,        cw,  vig);
        gc.fillRect(0,        ch - vig, cw,  vig);
        gc.fillRect(0,        vig,      vig, ch - vig * 2);
        gc.fillRect(cw - vig, vig,      vig, ch - vig * 2);

        // Red border stroke — thick at peak, thins on decay
        gc.save();
        gc.setStroke(Color.color(1, 0, 0, alpha));
        gc.setLineWidth(2.5 + alpha * 5.0);
        gc.strokeRect(0, 0, cw, ch);
        gc.restore();
    }

    private static double pulseAlpha(double localT) {
        double attack = PULSE_DUR * 0.12;
        if (localT <= 0) return 0;
        if (localT < attack) return localT / attack;
        return Math.max(0, 1.0 - (localT - attack) / (PULSE_DUR - attack));
    }
}
