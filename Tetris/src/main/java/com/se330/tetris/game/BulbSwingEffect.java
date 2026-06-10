package com.se330.tetris.game;

import javafx.animation.AnimationTimer;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Rotate;

public class BulbSwingEffect {

    private static final double FREQUENCY = 3.0;
    private static final double DAMPING   = 1.8;
    private static final double DURATION  = 2.0;

    // The string ImageView is managed=false, translateX=170, translateY=-580, fitWidth=10.
    // Its top-centre in the parent VBox's local coordinate space: (170 + 10/2, -580) = (175, -580).
    // Rotating the VBox around this point makes the full string+bulb unit swing as one.
    private static final double PIVOT_X = 175.0;
    private static final double PIVOT_Y = -580.0;

    private final Rotate         pivot;
    private final AnimationTimer timer;

    private double maxAngle  = 0;
    private long   triggerNs = 0;
    private int    swingSign = 1;

    public BulbSwingEffect(ImageView bulbView) {
        javafx.scene.Parent parent = bulbView.getParent();
        if (parent != null) {
            pivot = new Rotate(0, PIVOT_X, PIVOT_Y);
            parent.getTransforms().add(pivot);
        } else {
            pivot = new Rotate(0, bulbView.getFitWidth() / 2.0, 0);
            bulbView.getTransforms().add(pivot);
        }

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (maxAngle == 0) return;
                double t = (now - triggerNs) / 1_000_000_000.0;
                if (t >= DURATION) {
                    pivot.setAngle(0);
                    maxAngle = 0;
                    return;
                }
                pivot.setAngle(maxAngle * Math.sin(t * FREQUENCY) * Math.exp(-DAMPING * t));
            }
        };
        timer.start();
    }

    /** Trigger a swing scaled by lines cleared (1 = subtle, 4 = dramatic). Alternates direction. */
    public void trigger(int lines) {
        maxAngle = swingSign * switch (lines) {
            case 1  -> 3.0;
            case 2  -> 5.0;
            case 3  -> 7.0;
            default -> 10.0;
        };
        swingSign = -swingSign;
        triggerNs = System.nanoTime();
    }

    public void stop() { timer.stop(); }
}
