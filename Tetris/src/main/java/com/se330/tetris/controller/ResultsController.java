package com.se330.tetris.controller;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.SoundType;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ResultsController {

    @FXML private AnchorPane resultsPane;
    @FXML private Label      finalScoreLabel;
    @FXML private Label      titleLabel;
    @FXML private Label      noteLabel;
    @FXML private StackPane  notePanel;
    @FXML private Label      gameModeLabel;
    @FXML private Label      timeLabel;
    @FXML private Label      linesLabel;
    @FXML private Label      levelLabel;
    @FXML private Label      blocksLabel;
    @FXML private Button     saveBtn;
    @FXML private Button     retryBtn;
    @FXML private Button     menuBtn;

    private SceneManager sceneManager;
    private GameContext  gameContext;

    private AnimationTimer    glitchTimer;
    private Region            blackOverlay;
    private double            elapsed  = 0;
    private long              lastNano = 0;
    private String            realScore;
    private final Random      rng = new Random();

    private final List<Label> noteChars = new ArrayList<>();

    private static final double DARK_HOLD    = 0.25;
    private static final double FADE_DUR     = 1.50;
    private static final double SCRAMBLE_END = DARK_HOLD + 0.55;
    private static final double TYPE_START   = DARK_HOLD + 0.30;
    private static final double CHAR_DELAY   = 0.055;

    private static final double WAVE_AMP   = 2.5;
    private static final double WAVE_SPEED = 2.5;
    private static final double WAVE_PHASE = 0.45;

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext  = GameContext.getInstance();
        populateResults();
        for (Button btn : new Button[]{saveBtn, retryBtn, menuBtn})
            if (btn != null) btn.setOnMouseEntered(e -> SoundManager.getInstance().playSE(SoundType.HOVER));
        PauseTransition glitchDelay = new PauseTransition(Duration.millis(300));
        glitchDelay.setOnFinished(e -> SoundManager.getInstance().playSE(SoundType.RESULT_GLITCH));
        glitchDelay.play();
        startGlitchIntro();
    }

    private void startGlitchIntro() {
        realScore = finalScoreLabel.getText();
        buildCharLabels();

        blackOverlay = new Region();
        blackOverlay.setStyle("-fx-background-color: black;");
        blackOverlay.setMouseTransparent(true);
        AnchorPane.setTopAnchor(blackOverlay, 0.0);
        AnchorPane.setBottomAnchor(blackOverlay, 0.0);
        AnchorPane.setLeftAnchor(blackOverlay, 0.0);
        AnchorPane.setRightAnchor(blackOverlay, 0.0);
        resultsPane.getChildren().add(blackOverlay);

        lastNano = System.nanoTime();
        glitchTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                double dt = (now - lastNano) / 1_000_000_000.0;
                lastNano = now;
                elapsed += dt;
                tick(dt);
            }
        };
        glitchTimer.start();
    }

    private void buildCharLabels() {
        // Note text — hide original label, add per-char HBox centered in the same StackPane
        noteLabel.setVisible(false);
        HBox noteBox = new HBox(0);
        noteBox.setAlignment(Pos.CENTER);
        for (char c : noteLabel.getText().toCharArray()) {
            Label l = new Label(String.valueOf(c));
            l.setFont(Font.font("VT323", 32));
            l.setTextFill(Color.WHITE);
            l.setOpacity(0);
            noteChars.add(l);
            noteBox.getChildren().add(l);
        }
        notePanel.getChildren().add(noteBox);
    }

    private void tick(double dt) {
        // Overlay fade: hold black briefly, then smoothstep ease-out to transparent
        if (elapsed <= DARK_HOLD) {
            blackOverlay.setOpacity(1.0);
        } else {
            double t     = Math.min(1.0, (elapsed - DARK_HOLD) / FADE_DUR);
            double eased = t * t * (3.0 - 2.0 * t); // smoothstep
            blackOverlay.setOpacity(1.0 - eased);
            if (t >= 1.0) blackOverlay.setVisible(false);
        }

        // Score scramble while dark/early
        if (elapsed < SCRAMBLE_END) {
            finalScoreLabel.setText(scrambleDigits(realScore));
        } else {
            finalScoreLabel.setText(realScore);
        }

        // Glitch jitter + color flash on score — heavy early, rare late
        double rate = elapsed < DARK_HOLD + FADE_DUR ? 18.0 : 1.5;
        if (rng.nextDouble() < rate * dt) {
            double jx  = (rng.nextDouble() - 0.5) * 28;
            double jy  = (rng.nextDouble() - 0.5) * 6;
            finalScoreLabel.setTranslateX(jx);
            finalScoreLabel.setTranslateY(jy);
            String color = rng.nextBoolean() ? "#00ffff" : "#ff4444";
            finalScoreLabel.setStyle(
                "-fx-font-family: 'VT323'; -fx-font-size: 128; -fx-text-fill: " + color + ";"
            );
        } else {
            finalScoreLabel.setTranslateX(0);
            finalScoreLabel.setTranslateY(0);
            finalScoreLabel.setStyle(
                "-fx-font-family: 'VT323'; -fx-font-size: 128; -fx-text-fill: white;"
            );
        }

        // Stop score glitch after 8s but keep wave running
        if (elapsed > 8.0) {
            finalScoreLabel.setTranslateX(0);
            finalScoreLabel.setTranslateY(0);
            finalScoreLabel.setStyle(
                "-fx-font-family: 'VT323'; -fx-font-size: 128; -fx-text-fill: white;"
            );
        }

        // Typing effect for note text
        double typeElapsed = elapsed - TYPE_START;
        if (typeElapsed > 0) {
            int nNote = Math.min(noteChars.size(), (int)(typeElapsed / CHAR_DELAY));
            for (int i = 0; i < noteChars.size(); i++)
                noteChars.get(i).setOpacity(i < nNote ? 1.0 : 0.0);
        }

        // Wave: sine-offset each visible note character
        for (int i = 0; i < noteChars.size(); i++) {
            if (noteChars.get(i).getOpacity() > 0)
                noteChars.get(i).setTranslateY(Math.sin(elapsed * WAVE_SPEED + i * WAVE_PHASE) * WAVE_AMP);
        }
    }

    private void fadeToBlack(Runnable onDone) {
        if (glitchTimer != null) glitchTimer.stop();
        resultsPane.setMouseTransparent(true);
        blackOverlay.setOpacity(0);
        blackOverlay.setVisible(true);
        blackOverlay.setMouseTransparent(false);
        FadeTransition ft = new FadeTransition(Duration.millis((long)(FADE_DUR * 1000)), blackOverlay);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setOnFinished(e -> onDone.run());
        ft.play();
    }

    private String scrambleDigits(String original) {
        StringBuilder sb = new StringBuilder();
        for (char c : original.toCharArray())
            sb.append(Character.isDigit(c) ? (char)('0' + rng.nextInt(10)) : c);
        return sb.toString();
    }

    private void populateResults() {
        int demoScore = gameContext.getScore();
        ;
        int demoLevel = gameContext.getLevel();
        int demoLines = gameContext.getLines();
        int demoBlocks = 156;
        long demoTime = 332000;

        finalScoreLabel.setText(String.valueOf(demoScore));
        noteLabel.setText(demoScore < 1000 ? "Really? That's it?" :
                          demoScore >= 8000 ? "Impressive."        :
                          "Not bad, but you can do better");
        gameModeLabel.setText(gameContext.getGameMode().getDisplayName());
        levelLabel.setText(String.valueOf(demoLevel));
        linesLabel.setText(String.valueOf(demoLines));
        blocksLabel.setText(String.valueOf(demoBlocks));

        long totalSeconds = demoTime / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        timeLabel.setText(String.format("Time: %d:%02d", minutes, seconds));

        System.out.println("Results displayed:");
        System.out.println("  Final Score: " + demoScore);
        System.out.println("  Mode: " + gameContext.getGameMode().getDisplayName());
        System.out.println("  Level: " + demoLevel);
        System.out.println("  Lines: " + demoLines);
        System.out.println("  Time: " + String.format("%d:%02d", minutes, seconds));
    }

    @FXML
    private void onSaveClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        fadeToBlack(() -> {
            int score = Integer.parseInt(finalScoreLabel.getText());
            System.out.println("Saving score: " + score + " (" + gameModeLabel.getText() + ")");
            System.out.println("Save completed, returning to main menu");
            gameContext.reset();
            sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
        });
    }

    @FXML
    private void onRetryClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        fadeToBlack(() -> {
            System.out.println("Retrying with mode: " + gameContext.getGameMode().getDisplayName());
            gameContext.reset();
            sceneManager.clearSceneCache();
            boolean hardMode = gameContext.getGameMode() == GameContext.GameMode.HARD_MODE;
            sceneManager.switchToScene(hardMode ? SceneManager.HARD_GAME_SCENE : SceneManager.GAME_SCENE);
            SoundManager.getInstance().playMusic(SoundType.GAMEPLAY_THEME);
        });
    }

    @FXML
    private void onMenuClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        fadeToBlack(() -> {
            System.out.println("Returning to main menu");
            gameContext.reset();
            sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
        });
    }
}
