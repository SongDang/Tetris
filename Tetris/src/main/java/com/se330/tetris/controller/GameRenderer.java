package com.se330.tetris.controller;

import com.se330.tetris.game.*;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.util.Constants;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles all canvas rendering for the game.
 * Reads game state from GameController and visual state maintained here.
 */
class GameRenderer {

    private static final double VFX_MARGIN = 150.0;
    private static final double BG_R = 0x0f / 255.0;
    private static final double BG_G = 0x0d / 255.0;
    private static final double BG_B = 0x1a / 255.0;
    private static final String PANE_BASE_STYLE = "-fx-padding: 20; -fx-spacing: 20; -fx-alignment: center;";
    private static final double STARTUP_GLITCH_DUR = 0.75;

    // Canvas refs
    private final Canvas gameCanvas;
    private final Canvas vfxCanvas;
    private final Canvas holdBlockCanvas;
    private final Canvas nextBlockCanvas1;
    private final Canvas nextBlockCanvas2;
    private final Canvas nextBlockCanvas3;
    private final GraphicsContext gameGc;
    private final GraphicsContext vfxGc;
    private final GraphicsContext holdGc;
    private final GraphicsContext nextGc1;
    private final GraphicsContext nextGc2;
    private final GraphicsContext nextGc3;

    // UI refs
    private final Pane gamePane;
    private final ImageView lightBulbView;
    private final ImageView mainFrameView;

    // Images
    private final Image bombSprite;
    private final Image ghostSprite;
    private final Image lightOnImage;
    private final Image lightOffImage;
    private final Image hardMainImage;
    private final Image darkMainImage;

    // Shared context
    private final GameContext gameContext;
    private final BoardEngine boardEngine;
    private final HardModeHandler hardMode;
    private final Random rng;

    // Effects
    final ParticleSystem particleSystem = new ParticleSystem();
    LevelUpEffect levelUpEffect = null;
    BorderPulseEffect borderPulseEffect = null;
    GlitchTearEffect glitchTearEffect = null;
    com.se330.tetris.game.GlitchExplosionEffect glitchExplosionEffect = null;

    // Visual state
    double shakeIntensity = 0;
    double shakeDuration = 0;
    double shakeInitDuration = 0.18;
    double rotationPulse = 0;
    double flashIntensity = 0;
    double gameOverFlashAlpha = 0;
    private double ghostParticleTimer = 0;
    private double randomBlockSparkTimer = 0;

    // Startup glitch
    double startupGlitchElapsed = 0;
    private Canvas startupCanvas;
    private GraphicsContext startupGc;

    // Countdown
    private String countdownText = null;
    private double countdownPulse = 0;
    private Font countdownFont;

    // Combo float
    private int comboDisplay = 0;
    private double comboFloatX = 0;
    private double comboFloatY = 0;
    double comboFloatAlpha = 0;
    private double comboFloatPhase = 0;
    private double comboShakeAmount = 0;
    private Color comboColor = Color.WHITE;

    // Score popups
    private static class ScorePopup {
        String text;
        double x, y, vx, vy, life;
        Color color;
        int fontSize;
    }
    private final List<ScorePopup> scorePopups = new ArrayList<>();

    GameRenderer(Canvas gameCanvas, Canvas vfxCanvas, Canvas holdBlockCanvas,
                 Canvas nextBlockCanvas1, Canvas nextBlockCanvas2, Canvas nextBlockCanvas3,
                 Pane gamePane, ImageView lightBulbView, ImageView mainFrameView,
                 Image bombSprite, Image ghostSprite, Image lightOnImage, Image lightOffImage,
                 Image hardMainImage, Image darkMainImage,
                 GameContext gameContext, BoardEngine boardEngine, HardModeHandler hardMode, Random rng) {
        this.gameCanvas = gameCanvas;
        this.vfxCanvas = vfxCanvas;
        this.holdBlockCanvas = holdBlockCanvas;
        this.nextBlockCanvas1 = nextBlockCanvas1;
        this.nextBlockCanvas2 = nextBlockCanvas2;
        this.nextBlockCanvas3 = nextBlockCanvas3;
        this.gamePane = gamePane;
        this.lightBulbView = lightBulbView;
        this.mainFrameView = mainFrameView;
        this.bombSprite = bombSprite;
        this.ghostSprite = ghostSprite;
        this.lightOnImage = lightOnImage;
        this.lightOffImage = lightOffImage;
        this.hardMainImage = hardMainImage;
        this.darkMainImage = darkMainImage;
        this.gameContext = gameContext;
        this.boardEngine = boardEngine;
        this.hardMode = hardMode;
        this.rng = rng;

        this.gameGc = gameCanvas.getGraphicsContext2D();
        this.vfxGc = vfxCanvas.getGraphicsContext2D();
        this.holdGc = holdBlockCanvas.getGraphicsContext2D();
        this.nextGc1 = nextBlockCanvas1 != null ? nextBlockCanvas1.getGraphicsContext2D() : null;
        this.nextGc2 = nextBlockCanvas2 != null ? nextBlockCanvas2.getGraphicsContext2D() : null;
        this.nextGc3 = nextBlockCanvas3 != null ? nextBlockCanvas3.getGraphicsContext2D() : null;
    }

    void setStartupCanvas(Canvas canvas) {
        this.startupCanvas = canvas;
        this.startupGc = canvas.getGraphicsContext2D();
        this.startupGlitchElapsed = 0;
        countdownFont = Font.loadFont(
                getClass().getResourceAsStream("/fonts/VT323-Regular.ttf"), 14);
    }

    void setCountdown(String text) {
        this.countdownText = text;
        this.countdownPulse = 0;
        if (text == null && startupGc != null)
            startupGc.clearRect(0, 0, startupCanvas.getWidth(), startupCanvas.getHeight());
    }

    // --- Triggered events from game logic ---

    void onRotate() { rotationPulse = 1.0; }

    void onHardDrop() {
        shakeIntensity = 4.0;
        shakeInitDuration = 0.18;
        shakeDuration = 0.18;
    }

    void onLineClear(int cleared, List<Integer> fullRows, Color pieceColor, int cs) {
        double t = cleared / 4.0;
        flashIntensity = 0.12 + t * 0.28;
        shakeIntensity = 5.0 + t * 13.0 + (cleared == 4 ? 12.0 : 0);
        shakeInitDuration = 0.18 + t * 0.17 + (cleared == 4 ? 0.10 : 0);
        shakeDuration = shakeInitDuration;

        for (int row : fullRows) {
            for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                TetrominoType type = boardEngine.idToType(boardEngine.getBoard()[row][x]);
                if (type != null) particleSystem.emitRowBurst(x * cs, row * cs, type.getColor(), 8);
            }
        }
        double leftBase = VFX_MARGIN;
        double rightBase = VFX_MARGIN + Constants.BOARD_WIDTH * cs;
        for (int row : fullRows)
            particleSystem.emitRowBeams(leftBase, rightBase, row * cs, cs, pieceColor);
    }

    void onBombExplode(double pixelCx, double pixelCy, int cs,
                       List<BoardEngine.ClearedCell> cells) {
        List<GlitchExplosionEffect.CellSnap> snaps = new ArrayList<>();
        for (BoardEngine.ClearedCell c : cells)
            snaps.add(new GlitchExplosionEffect.CellSnap(c.boardX * cs, c.boardY * cs, cs, c.type.getColor()));
        glitchExplosionEffect = new GlitchExplosionEffect(pixelCx, pixelCy, cs, snaps);
        flashIntensity = 0.45;
        shakeIntensity = 18;
        shakeInitDuration = 0.30;
        shakeDuration = 0.30;
    }

    void onLevelUp(int newLevel) {
        levelUpEffect = new LevelUpEffect(newLevel, () -> {
            shakeIntensity = 18;
            shakeInitDuration = 0.25;
            shakeDuration = 0.25;
        });
    }

    void onCombo(int comboCount, int avgRow, int cs) {
        comboDisplay = comboCount;
        comboFloatX = gameCanvas.getWidth() / 2.0 - 50;
        comboFloatY = avgRow * cs;
        comboFloatAlpha = 1.0;
        comboFloatPhase = 0;
        comboShakeAmount = 10 + comboDisplay * 5;
        comboColor = TetrominoType.values()[rng.nextInt(7)].getColor();
    }

    void onGameOver() {
        gameOverFlashAlpha = 0.6;
    }

    void onLockParticles(int x, int y, Color color, int cs) {
        particleSystem.emitLockParticles(x * cs, y * cs, color, 18);
    }

    void onLightColumn(double leftX, double fromY, double toY, double spanWidth, Color color) {
        particleSystem.emitLightColumn(leftX, fromY, toY, spanWidth, color);
    }

    void onFuseSparks(double bx, double by, int cs, int rotation) {
        particleSystem.emitFuseSparks(bx, by, cs, rotation);
    }

    void onRotationArc(Piece piece, int cs) {
        particleSystem.emitCornerSparks(
                piece.getX(), piece.getY(),
                piece.getType().getShape(piece.getRotation()),
                cs, piece.getType().getColor());
    }

    void triggerGameOverTear(int[][] board, int cs, Color tearBg, Runnable onDone) {
        glitchTearEffect = new GlitchTearEffect(board, cs, tearBg, onDone);
    }

    void emitScorePopup(int cleared, int avgRow) {
        int bonus = switch (cleared) {
            case 1 -> 100; case 2 -> 300; case 3 -> 500; case 4 -> 800; default -> 0;
        };
        if (bonus == 0) return;
        double speed = 90 + rng.nextDouble() * 40;
        double rad = Math.toRadians(30 + rng.nextDouble() * 120);
        ScorePopup p = new ScorePopup();
        p.text = "+" + bonus;
        p.x = 8;
        p.y = avgRow * Constants.BLOCK_SIZE - 4;
        p.vx = Math.cos(rad) * speed;
        p.vy = -Math.sin(rad) * speed;
        p.life = 1.0;
        p.fontSize = 20 + cleared * 3;
        p.color = switch (cleared) {
            case 4 -> Color.web("#00ffff"); case 3 -> Color.web("#ffaa00"); default -> Color.web("#ffffff");
        };
        scorePopups.add(p);
    }

    // --- Per-frame update ---

    void update(double dt, boolean gamePaused, boolean isGameOver, Piece currentPiece) {
        if (glitchTearEffect != null) glitchTearEffect.update(dt);
        if (gameOverFlashAlpha > 0) gameOverFlashAlpha = Math.max(0, gameOverFlashAlpha - dt * 4.0);
        if (startupGlitchElapsed < STARTUP_GLITCH_DUR) startupGlitchElapsed += dt;
        if (countdownText != null) countdownPulse += dt;
        if (glitchExplosionEffect != null) {
            glitchExplosionEffect.update(dt);
            if (glitchExplosionEffect.isDone()) glitchExplosionEffect = null;
        }

        if (!gamePaused && !isGameOver) {
            particleSystem.update(dt);
            scorePopups.removeIf(p -> p.life <= 0);
            for (ScorePopup p : scorePopups) {
                p.x += p.vx * dt; p.y += p.vy * dt; p.life -= dt / 0.7;
            }
            updateScreenShake(dt);

            // --- HIỆU ỨNG HẠT CHO KHỐI TÀNG HÌNH (TRANSPARENT) ---
            if (currentPiece.getType() == TetrominoType.TRANSPARENT) {
                int cs = Constants.BLOCK_SIZE;
                int[][] gShape = currentPiece.getType().getShape(currentPiece.getRotation());
                ghostParticleTimer -= dt;
                if (ghostParticleTimer <= 0) {
                    ghostParticleTimer = 0.06; // Khoảng 0.06s một lần
                    for (int row = 0; row < 4; row++)
                        for (int col = 0; col < 4; col++)
                            if (gShape[row][col] == 1)
                                particleSystem.emitGhostDrift(
                                        (currentPiece.getX() + col + 0.5) * cs,
                                        (currentPiece.getY() + row + 0.5) * cs, cs);
                }
            }

            // --- HIỆU ỨNG TIA LỬA CHO KHỐI NGẪU NHIÊN (RANDOMBLOCK) ---
            if (currentPiece instanceof RandomBlock) {
                randomBlockSparkTimer -= dt;
                if (randomBlockSparkTimer <= 0) {
                    randomBlockSparkTimer = 0.05;
                    int cs = Constants.BLOCK_SIZE;
                    int[][] rbShape = currentPiece.getType().getShape(currentPiece.getRotation());
                    for (int row = 0; row < 4; row++)
                        for (int col = 0; col < 4; col++)
                            if (rbShape[row][col] == 1)
                                particleSystem.emitRandomBlockSparks(
                                        (currentPiece.getX() + col + 0.5) * cs,
                                        (currentPiece.getY() + row + 0.5) * cs, cs);
                }
            }

            if (rotationPulse > 0) rotationPulse = Math.max(0, rotationPulse - dt * 8.0);
            if (flashIntensity > 0) flashIntensity = Math.max(0, flashIntensity - dt * 6.0);
            if (levelUpEffect != null) { levelUpEffect.update(dt); if (levelUpEffect.isDone()) levelUpEffect = null; }
            if (borderPulseEffect != null) {
                borderPulseEffect.update(dt);
                if (borderPulseEffect.isDone()) borderPulseEffect = null;
            } else {
                int dangerRow = (int) (Constants.BOARD_HEIGHT * 0.35);
                outer: for (int y = 0; y <= dangerRow; y++)
                    for (int x = 0; x < Constants.BOARD_WIDTH; x++)
                        if (boardEngine.getBoard()[y][x] != 0) { borderPulseEffect = new BorderPulseEffect(); break outer; }
            }
            if (comboFloatAlpha > 0) {
                comboFloatY -= dt * 60;
                comboFloatPhase += dt * 5.0;
                comboShakeAmount = Math.max(0, comboShakeAmount - dt * 100);
                comboFloatAlpha = Math.max(0, comboFloatAlpha - dt * 1.1);
            }
        }
    }

    private void updateScreenShake(double dt) {
        if (shakeDuration <= 0) return;
        shakeDuration -= dt;
        if (shakeDuration <= 0) {
            gameCanvas.setTranslateX(0); gameCanvas.setTranslateY(0);
            vfxCanvas.setTranslateX(0); vfxCanvas.setTranslateY(0);
        } else {
            double factor = shakeDuration / shakeInitDuration;
            double sx = (rng.nextDouble() - 0.5) * 2 * shakeIntensity * factor;
            double sy = (rng.nextDouble() - 0.5) * 2 * shakeIntensity * factor;
            gameCanvas.setTranslateX(sx); gameCanvas.setTranslateY(sy);
            vfxCanvas.setTranslateX(sx); vfxCanvas.setTranslateY(sy);
        }
    }

    // --- Main render ---

    void render(Piece currentPiece, java.util.Deque<Piece> nextQueue, TetrominoType holdType,
                List<Piece> suspendedPieces, boolean gamePaused, boolean isGameOver,
                long freezeUntil, int[] frozenRowFlash) {

        boolean hardModeActive = gameContext.getGameMode() == GameContext.GameMode.HARD_MODE;

        // Swap UI panels during blackout
        if (hardModeActive) {
            boolean inBlackout = hardMode.blackoutState == HardModeHandler.BlackoutState.BLACKOUT;
            var classes = gamePane.getStyleClass();
            if (inBlackout && !classes.contains("blackout")) {
                classes.add("blackout");
                if (mainFrameView != null) mainFrameView.setImage(darkMainImage);
            } else if (!inBlackout && classes.contains("blackout")) {
                classes.remove("blackout");
                if (mainFrameView != null) mainFrameView.setImage(hardMainImage);
            }
        }

        // Background flash
        if (!hardModeActive) {
            if (flashIntensity > 0) {
                double r = BG_R + flashIntensity * (1.0 - BG_R);
                double g = BG_G + flashIntensity * (1.0 - BG_G);
                double b = BG_B + flashIntensity * (1.0 - BG_B);
                String hex = String.format("#%02x%02x%02x", (int)(r*255), (int)(g*255), (int)(b*255));
                gamePane.setStyle("-fx-background-color: " + hex + "; " + PANE_BASE_STYLE);
            } else {
                gamePane.setStyle("-fx-background-color: #0f0d1a; " + PANE_BASE_STYLE);
            }
        }

        drawGameBoard(currentPiece, suspendedPieces, gamePaused, isGameOver, freezeUntil, frozenRowFlash);

        if (glitchExplosionEffect != null) glitchExplosionEffect.render(gameGc);

        if (glitchTearEffect != null) {
            vfxGc.clearRect(0, 0, vfxCanvas.getWidth(), vfxCanvas.getHeight());
            glitchTearEffect.render(gameGc, gameCanvas.getWidth(), gameCanvas.getHeight());
            double wa = glitchTearEffect.getWhiteoutAlpha();
            if (wa > 0) {
                flashIntensity = wa;
                vfxGc.setFill(Color.color(1, 1, 1, wa));
                vfxGc.fillRect(0, 0, vfxCanvas.getWidth(), vfxCanvas.getHeight());
            }
        } else {
            if (levelUpEffect != null)
                levelUpEffect.render(gameGc, gameCanvas.getWidth(), gameCanvas.getHeight());
            particleSystem.render(gameGc);
            particleSystem.renderBeams(vfxGc);
            if (borderPulseEffect != null)
                borderPulseEffect.render(gameGc, vfxGc, gameCanvas.getWidth(), gameCanvas.getHeight(), VFX_MARGIN);
        }

        renderTopLayer();
        drawHoldBlock(holdType);
        drawNextBlocks(nextQueue);
        updateLightBulb();

        if (hardModeActive) hardMode.renderFlavourText(rng);

        renderStartupGlitch();
        renderCountdown();
    }

    // --- Board rendering ---
    private void drawGameBoard(Piece currentPiece, List<Piece> suspendedPieces,
                               boolean gamePaused, boolean isGameOver,
                               long freezeUntil, int[] frozenRowFlash) {
        int[][] board = boardEngine.getBoard();
        double w = gameCanvas.getWidth();
        double h = gameCanvas.getHeight();
        int cs = Constants.BLOCK_SIZE;
        boolean isHardMode = gameContext.getGameMode() == GameContext.GameMode.HARD_MODE;

        gameGc.setFill(Color.web(isHardMode ? "#280C00" : "#0f0d1a"));
        gameGc.fillRect(0, 0, w, h);

        gameGc.setStroke(Color.web(isHardMode ? "#3d1500" : "#2b2740"));
        gameGc.setLineWidth(0.5);
        for (int x = 0; x <= Constants.BOARD_WIDTH; x++)
            gameGc.strokeLine(x * cs, 0, x * cs, h);
        for (int y = 0; y <= Constants.BOARD_HEIGHT; y++)
            gameGc.strokeLine(0, y * cs, w, y * cs);

        if (!isHardMode) {
            gameGc.setStroke(Color.web("#00ff00"));
            gameGc.setLineWidth(2);
            gameGc.strokeRect(0, 0, w, h);
        }

        boolean isDark = false;
        HardModeHandler.BlackoutState bState = hardMode.blackoutState;
        if (bState == HardModeHandler.BlackoutState.FLICKER)
            isDark = ((int)(hardMode.blackoutFlickerTimer * 10)) % 2 == 0;
        if (bState == HardModeHandler.BlackoutState.BLACKOUT)
            isDark = true;

        // Vẽ các khối tĩnh đã khóa trên bảng
        if (!isDark) {
            for (int y = 0; y < Constants.BOARD_HEIGHT; y++) {
                for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                    if (board[y][x] != 0) {
                        if (board[y][x] == BoardEngine.TIME_BLOCK_ID) { drawTimeBlockCell(x, y, cs); continue; }
                        TetrominoType t = boardEngine.idToType(board[y][x]);
                        if (t == null) continue;

                        // Đổ Sprite đặc biệt cho khối tĩnh từ HEAD
                        if (t == TetrominoType.BOMB) gameGc.drawImage(bombSprite, x * cs, y * cs, cs, cs);
                        else if (t == TetrominoType.TRANSPARENT) gameGc.drawImage(ghostSprite, x * cs, y * cs, cs, cs);
                        else drawCell(x, y, t.getColor(), 1.0);
                    }
                }
            }
        }

        if (frozenRowFlash != null && freezeUntil > 0) {
            gameGc.setFill(Color.WHITE);
            for (int row : frozenRowFlash) gameGc.fillRect(0, row * cs, w, cs);
        }

        if (gameOverFlashAlpha > 0) {
            gameGc.setFill(Color.color(1, 1, 1, gameOverFlashAlpha));
            gameGc.fillRect(0, 0, w, h);
        }

        if (isGameOver) return;

        renderSuspendedPieces(suspendedPieces, cs);
        drawCurrentPiece(currentPiece, bState, cs, suspendedPieces, freezeUntil);

        if (currentPiece instanceof RandomBlock randomBlock && randomBlock.isIndicatorOn())
            drawRandomBlockIndicator(randomBlock, cs);

        if (gamePaused) {
            gameGc.setFill(Color.color(0, 0, 0, 0.5));
            gameGc.fillRect(0, 0, w, h);
            gameGc.setFill(Color.web("#00ff00"));
            gameGc.setFont(Font.font("Courier New", 28));
            gameGc.fillText("PAUSED", w / 2 - 55, h / 2);
        }
    }

    private void drawCurrentPiece(Piece currentPiece, HardModeHandler.BlackoutState bState, int cs,
                                   List<Piece> suspendedPieces, long freezeUntil) {
        int[][] shape = currentPiece.getType().getShape(currentPiece.getRotation());
        boolean isBomb = currentPiece.getType() == TetrominoType.BOMB;
        boolean isGhost = currentPiece.getType() == TetrominoType.TRANSPARENT;
        boolean isRandomBlock = currentPiece instanceof RandomBlock;
        boolean inBlackout = bState == HardModeHandler.BlackoutState.BLACKOUT;

        // Blackout overlays
        double w = gameCanvas.getWidth(), h = gameCanvas.getHeight();
        if (inBlackout) {
            gameGc.setFill(Color.BLACK);
            gameGc.fillRect(0, 0, w, h);
        } else if (bState == HardModeHandler.BlackoutState.FLICKER || bState == HardModeHandler.BlackoutState.POST_FLICKER) {
            if (((int) hardMode.blackoutFlickerTimer) % 2 == 0) {
                gameGc.setFill(Color.BLACK);
                gameGc.fillRect(0, 0, w, h);
            }
        }

        // Ghost piece (Bóng đổ vị trí rơi) - rendered after blackout overlay so it's always visible
        if (freezeUntil == 0) {
            int drop = boardEngine.getDropDistance(currentPiece, suspendedPieces);
            int[][] ghostShape = currentPiece.getType().getShape(currentPiece.getRotation());
            for (int row = 0; row < 4; row++)
                for (int col = 0; col < 4; col++)
                    if (ghostShape[row][col] == 1) {
                        if (currentPiece.getType() == TetrominoType.TRANSPARENT) {
                            gameGc.setGlobalAlpha(0.25);
                            gameGc.drawImage(ghostSprite, (currentPiece.getX() + col) * cs, (currentPiece.getY() + row + drop) * cs, cs, cs);
                            gameGc.setGlobalAlpha(1.0);
                        } else {
                            drawCell(currentPiece.getX() + col, currentPiece.getY() + row + drop,
                                    currentPiece.getType().getColor().deriveColor(0, 1, 1, 0.25), 1.0);
                        }
                    }
        }

        double charge = 0;
        double chargeOffsetX = 0, chargeOffsetY = 0;
        Color pieceColor;

        if (inBlackout) {
            charge = hardMode.blackoutDropTimer / 1.5;
            double freq = 6.0 + charge * 10.0;
            chargeOffsetY = Math.sin(hardMode.blackoutDropTimer * Math.PI * freq) * charge * 5.0;
            chargeOffsetX = (rng.nextDouble() - 0.5) * charge * 4.0;
            Color base = isBomb ? Color.color(1.0, 0.15, 0.15) : currentPiece.getType().getColor();
            double bright = charge * 0.75;
            pieceColor = Color.color(
                    Math.min(1.0, base.getRed() + bright),
                    Math.min(1.0, base.getGreen() + bright),
                    Math.min(1.0, base.getBlue() + bright));
        } else {
            pieceColor = currentPiece.getType().getColor().deriveColor(0, 1, 1.0 + rotationPulse * 1.8, 1);
        }

        gameGc.save();
        gameGc.translate(chargeOffsetX, chargeOffsetY);

        // Aura glow during blackout
        if (inBlackout && charge > 0) {
            double expansion = charge * 5.0;
            gameGc.setGlobalAlpha(charge * 0.35);
            gameGc.setFill(pieceColor);
            for (int row = 0; row < 4; row++)
                for (int col = 0; col < 4; col++)
                    if (shape[row][col] == 1) {
                        int px = currentPiece.getX() + col, py = currentPiece.getY() + row;
                        gameGc.fillRect(px * cs - expansion, py * cs - expansion, cs + expansion * 2, cs + expansion * 2);
                    }
            gameGc.setGlobalAlpha(1.0);
        }

        // Chromatic aberration (Sắc sai RGB) during blackout
        if (inBlackout) {
            double caOffset = 3.0 + charge * 7.0;
            Color base = currentPiece.getType().getColor();
            gameGc.setGlobalAlpha(0.50);
            gameGc.setFill(Color.color(Math.min(1.0, base.getRed() + 0.4), 0, 0));
            for (int row = 0; row < 4; row++)
                for (int col = 0; col < 4; col++)
                    if (shape[row][col] == 1)
                        gameGc.fillRect((currentPiece.getX() + col) * cs - caOffset,
                                (currentPiece.getY() + row) * cs, cs, cs);
            gameGc.setFill(Color.color(0, Math.min(1.0, base.getGreen() * 0.2 + 0.6),
                    Math.min(1.0, base.getBlue() + 0.4)));
            for (int row = 0; row < 4; row++)
                for (int col = 0; col < 4; col++)
                    if (shape[row][col] == 1)
                        gameGc.fillRect((currentPiece.getX() + col) * cs + caOffset,
                                (currentPiece.getY() + row) * cs, cs, cs);
            gameGc.setGlobalAlpha(1.0);
        }

        // Vẽ khối đang điều khiển hiện tại kèm hiệu ứng nâng cao từ HEAD
        long timeFactor = System.currentTimeMillis();
        double timeSec = timeFactor / 1000.0;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int px = currentPiece.getX() + col, py = currentPiece.getY() + row;
                    if (isBomb && !inBlackout) {
                        double bcx = px * cs + cs / 2.0, bcy = py * cs + cs / 2.0;
                        gameGc.save();
                        gameGc.translate(bcx, bcy);
                        gameGc.rotate(currentPiece.getRotation() * 90.0);
                        gameGc.drawImage(bombSprite, -cs / 2.0, -cs / 2.0, cs, cs);
                        gameGc.restore();
                    } else if (isGhost) {
                        // Khôi phục hiệu ứng phân tách ảnh ảo màu tím/trắng đặc trưng của khối tàng hình
                        double gx = px * cs, gy = py * cs;
                        double dx1 = Math.sin(timeSec * 35.0) * 5;
                        double dy1 = Math.cos(timeSec * 28.0) * 4;
                        double dx2 = -Math.sin(timeSec * 42.0 + 1.2) * 4;
                        double dy2 = -Math.cos(timeSec * 38.0) * 3;

                        gameGc.save();
                        gameGc.setGlobalAlpha(0.55);
                        gameGc.drawImage(ghostSprite, gx + dx1, gy + dy1, cs, cs);
                        gameGc.setFill(Color.web("#9966cc", 0.5));
                        gameGc.fillRect(gx + dx1, gy + dy1, cs, cs);

                        gameGc.setGlobalAlpha(0.45);
                        gameGc.drawImage(ghostSprite, gx + dx2, gy + dy2, cs, cs);
                        gameGc.setFill(Color.web("#ffffff", 0.45));
                        gameGc.fillRect(gx + dx2, gy + dy2, cs, cs);

                        gameGc.setGlobalAlpha(1.0);
                        gameGc.drawImage(ghostSprite, gx, gy, cs, cs);
                        gameGc.restore();
                    } else if (isRandomBlock) {
                        // Khôi phục hiệu ứng rung nhấp nháy ma mị của khối ngẫu nhiên
                        double erratic = Math.sin(timeSec * 23.0) * Math.sin(timeSec * 11.7) * Math.sin(timeSec * 5.3);
                        double shift = 2.0 + Math.abs(erratic) * 8.0;
                        double redOffY  = Math.sin(timeSec * 17.0) * 2.5;
                        double blueOffY = Math.sin(timeSec * 13.3 + 1.1) * 2.5;
                        gameGc.save();
                        gameGc.setGlobalAlpha(0.35 + Math.abs(erratic) * 0.3);
                        gameGc.setFill(Color.color(1, 0, 0));
                        gameGc.fillRect(px * cs - shift, py * cs + redOffY, cs, cs);
                        gameGc.setFill(Color.color(0, 0.4, 1));
                        gameGc.fillRect(px * cs + shift, py * cs + blueOffY, cs, cs);
                        gameGc.setGlobalAlpha(1.0);
                        gameGc.restore();
                        drawCell(px, py, pieceColor, 1.0);
                    } else {
                        drawCell(px, py, pieceColor, 1.0);
                    }
                }
            }
        }
        gameGc.restore();

        // --- KHÔI PHỤC HIỆU ỨNG GLITCH DỰ ĐOÁN HÌNH DẠNG (MORPH HINT) CỦA RANDOMBLOCK ---
        if (isRandomBlock) {
            RandomBlock rb = (RandomBlock) currentPiece;
            TetrominoType preview = rb.getPreviewType();
            double progress = rb.getMorphProgress();
            if (preview != null && progress > 0.3) {
                double glitch = Math.sin(timeSec * 19.0) * Math.sin(timeSec * 7.3) * Math.sin(timeSec * 31.0);
                double threshold = 0.35 - progress * 0.32;
                if (glitch > threshold) {
                    double flashAlpha = Math.min(1.0, (glitch - threshold) / (1.0 - threshold) * 2.0);
                    int[][] previewShape = preview.getShape(currentPiece.getRotation());
                    gameGc.save();
                    for (int row = 0; row < 4; row++) {
                        if (previewShape[row][0] + previewShape[row][1] + previewShape[row][2] + previewShape[row][3] == 0) continue;
                        double rowGlitch = (rng.nextDouble() - 0.5) * 10 * (1.0 - progress);
                        for (int col = 0; col < 4; col++) {
                            if (previewShape[row][col] != 1) continue;
                            double cx = (currentPiece.getX() + col) * cs + rowGlitch;
                            double cy = (currentPiece.getY() + row) * cs;
                            gameGc.setGlobalAlpha(Math.min(1.0, flashAlpha * (0.75 + rng.nextDouble() * 0.5)));
                            gameGc.setFill(Color.WHITE);
                            gameGc.fillRect(cx, cy, cs, cs);
                        }
                    }
                    gameGc.setGlobalAlpha(1.0);
                    gameGc.restore();
                }
            }
        }
    }

    void drawHoldBlock(TetrominoType holdType) {
        if (holdType != null) drawPreview(holdGc, holdBlockCanvas, new Piece(holdType, 0, 0));
        else drawPreview(holdGc, holdBlockCanvas, null);
    }

    void drawNextBlocks(java.util.Deque<Piece> nextQueue) {
        java.util.Iterator<Piece> it = nextQueue.iterator();
        if (nextGc1 != null) drawPreview(nextGc1, nextBlockCanvas1, it.hasNext() ? it.next() : null);
        else if (it.hasNext()) it.next();
        if (nextGc2 != null) drawPreview(nextGc2, nextBlockCanvas2, it.hasNext() ? it.next() : null);
        else if (it.hasNext()) it.next();
        if (nextGc3 != null) drawPreview(nextGc3, nextBlockCanvas3, it.hasNext() ? it.next() : null);
    }

    private void drawPreview(GraphicsContext gc, Canvas canvas, Piece piece) {
        double w = canvas.getWidth(), h = canvas.getHeight();
        HardModeHandler.BlackoutState bState = hardMode.blackoutState;
        gc.setFill(Color.web(
                bState == HardModeHandler.BlackoutState.BLACKOUT ? "#000000"
                        : gameContext.getGameMode() == GameContext.GameMode.HARD_MODE ? "#280C00" : "#0f0d1a"));
        gc.fillRect(0, 0, w, h);
        if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
            drawQuestionMark(gc, canvas); return;
        }
        if (piece == null) return;
        boolean isRandomPreview = piece instanceof RandomBlock;
        TetrominoType type = piece.getType();
        int[][] shape = type.getShape(0);
        int minRow = 4, maxRow = -1, minCol = 4, maxCol = -1;
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++)
                if (shape[row][col] == 1) {
                    minRow = Math.min(minRow, row); maxRow = Math.max(maxRow, row);
                    minCol = Math.min(minCol, col); maxCol = Math.max(maxCol, col);
                }
        if (maxRow < 0 || maxCol < 0) return;
        int previewCellSize = 24;
        int pieceWidth = (maxCol - minCol + 1) * previewCellSize;
        int pieceHeight = (maxRow - minRow + 1) * previewCellSize;
        double offsetX = (w - pieceWidth) / 2.0;
        double offsetY = (h - pieceHeight) / 2.0;
        for (int row = minRow; row <= maxRow; row++)
            for (int col = minCol; col <= maxCol; col++)
                if (shape[row][col] == 1) {
                    double px = offsetX + (col - minCol) * previewCellSize;
                    double py = offsetY + (row - minRow) * previewCellSize;
                    if (type == TetrominoType.BOMB) gc.drawImage(bombSprite, px, py, previewCellSize, previewCellSize);
                    else drawCellAtPixel(gc, px, py, previewCellSize, type.getColor(), 1.0);
                }
        if (isRandomPreview) {
            gc.setStroke(Color.web("#00e5ff"));
            gc.setLineWidth(2.0);
            for (int row = minRow; row <= maxRow; row++)
                for (int col = minCol; col <= maxCol; col++)
                    if (shape[row][col] == 1) {
                        double px = offsetX + (col - minCol) * previewCellSize;
                        double py = offsetY + (row - minRow) * previewCellSize;
                        gc.strokeRect(px + 1, py + 1, previewCellSize - 2, previewCellSize - 2);
                    }
        }
    }

    private void renderSuspendedPieces(List<Piece> suspendedPieces, int cs) {
        for (Piece piece : suspendedPieces) {
            int[][] shape = piece.getType().getShape(piece.getRotation());
            for (int row = 0; row < 4; row++)
                for (int col = 0; col < 4; col++)
                    if (shape[row][col] == 1) {
                        int px = piece.getX() + col, py = piece.getY() + row;
                        if (piece.getType() == TetrominoType.BOMB)
                            gameGc.drawImage(bombSprite, px * cs, py * cs, cs, cs);
                        else drawCell(px, py, piece.getType().getColor(), 1.0);
                    }
        }
    }

    private void renderTopLayer() {
        if (comboFloatAlpha > 0) {
            double cx = comboFloatX + (rng.nextDouble() - 0.5) * 2 * comboShakeAmount;
            double cy = comboFloatY + (rng.nextDouble() - 0.5) * 2 * comboShakeAmount * 0.4;
            double flash = (Math.sin(comboFloatPhase * 3.0) + 1.0) / 2.0;
            Color drawColor = comboColor.interpolate(Color.WHITE, flash);
            int fontSize = (int) Math.min(75, 32 + comboDisplay * 7);
            gameGc.setGlobalAlpha(comboFloatAlpha);
            gameGc.setFont(Font.font("VT323", FontWeight.BOLD, fontSize));
            gameGc.setFill(drawColor);
            gameGc.fillText("COMBO x" + comboDisplay, cx, cy);
            gameGc.setGlobalAlpha(1.0);
        }
        renderScorePopups();
    }

    private void renderScorePopups() {
        if (scorePopups.isEmpty()) return;
        for (ScorePopup p : scorePopups) {
            gameGc.save();
            gameGc.setGlobalAlpha(Math.max(0, p.life));
            gameGc.setFont(Font.font("VT323", FontWeight.BOLD, p.fontSize));
            gameGc.setFill(p.color);
            gameGc.fillText(p.text, p.x, p.y);
            gameGc.restore();
        }
    }

    private void renderStartupGlitch() {
        if (startupGlitchElapsed >= STARTUP_GLITCH_DUR || startupGc == null) return;
        double t = startupGlitchElapsed / STARTUP_GLITCH_DUR;
        double alpha = (1.0 - t * t) * 0.55;
        double w = startupCanvas.getWidth(), h = startupCanvas.getHeight();
        startupGc.clearRect(0, 0, w, h);
        int bands = 6 + rng.nextInt(5);
        for (int i = 0; i < bands; i++) {
            double by = rng.nextDouble() * h, bh = rng.nextDouble() * (h * 0.05) + 1;
            int pick = rng.nextInt(3);
            double cr = pick == 0 ? 0 : 1, cg = pick == 0 ? 1 : 1, cb = pick == 0 ? 1 : 0;
            double baseA = pick == 0 ? 0.5 : pick == 1 ? 0.4 : 0.3;
            double xOffset = Math.sin(t * Math.PI * 5 + i * 1.9) * w * 0.18;
            double pad = Math.abs(xOffset);
            startupGc.setFill(Color.color(cr, cg, cb, Math.min(1, alpha * baseA)));
            startupGc.fillRect(xOffset - pad, by, w + pad * 2, bh);
            for (int j = 0; j < 3; j++) {
                double hx = rng.nextDouble() * w + xOffset;
                startupGc.setFill(Color.color(1, 1, 1, alpha * (0.6 + rng.nextDouble() * 0.4)));
                startupGc.fillRect(hx, by, rng.nextDouble() * 6 + 2, bh);
            }
        }
        int pixels = (int) (250 * (1.0 - t));
        for (int i = 0; i < pixels; i++) {
            startupGc.setFill(Color.color(1, 1, 1, alpha * (rng.nextDouble() * 0.6 + 0.2)));
            startupGc.fillRect(rng.nextDouble() * w, rng.nextDouble() * h, 2, 2);
        }
        double[] flashTimes = {0.0, 0.14, 0.28};
        double[] flashPeaks = {0.90, 0.42, 0.42};
        double flashHalf = 0.07, totalFlash = 0;
        for (int fi = 0; fi < flashTimes.length; fi++) {
            double d = Math.abs(t - flashTimes[fi]);
            if (d < flashHalf) totalFlash = Math.max(totalFlash, (1.0 - d / flashHalf) * flashPeaks[fi]);
        }
        if (totalFlash > 0) {
            startupGc.setFill(Color.color(1, 1, 1, totalFlash));
            startupGc.fillRect(0, 0, w, h);
        }
    }

    private void renderCountdown() {
        if (countdownText == null || startupGc == null) return;
        double w = startupCanvas.getWidth(), h = startupCanvas.getHeight();
        startupGc.clearRect(0, 0, w, h);

        boolean isGo = "GO!".equals(countdownText);
        double scale = 1.0 + Math.sin(countdownPulse * Math.PI * 3.0) * 0.06;
        double fontSize = (isGo ? 110 : 130) * scale;

        startupGc.setFill(Color.color(0, 0, 0, 0.5));
        startupGc.fillRect(0, 0, w, h);

        Font font = countdownFont != null
                ? Font.font("VT323", fontSize)
                : Font.font("Courier New", FontWeight.BOLD, fontSize * 0.65);
        startupGc.setFont(font);

        double estimatedWidth = countdownText.length() * fontSize * 0.58;
        double tx = (w - estimatedWidth) / 2.0;
        double ty = h / 2.0 + fontSize * 0.32;

        Color glowColor = isGo ? Color.color(0, 0.9, 0.45, 0.55) : Color.color(0.85, 0.85, 1.0, 0.45);
        startupGc.setFill(glowColor);
        startupGc.fillText(countdownText, tx + 4, ty + 4);
        startupGc.fillText(countdownText, tx - 4, ty + 4);

        startupGc.setFill(isGo ? Color.web("#00ff88") : Color.WHITE);
        startupGc.fillText(countdownText, tx, ty);
    }

    // --- Cell drawing helpers ---

    private void drawCell(int x, int y, Color color, double opacity) {
        int cs = Constants.BLOCK_SIZE;
        double px = x * cs, py = y * cs;
        double arc = cs * 0.25;
        gameGc.setFill(color.deriveColor(0, 1, 1, opacity));
        gameGc.fillRoundRect(px, py, cs, cs, arc, arc);
        gameGc.setStroke(Color.web("#111111"));
        gameGc.setLineWidth(1);
        gameGc.strokeRoundRect(px, py, cs, cs, arc, arc);
    }

    private void drawCellAtPixel(GraphicsContext target, double px, double py, int size, Color color, double opacity) {
        double arc = size * 0.25;
        target.setFill(color.deriveColor(0, 1, 1, opacity));
        target.fillRoundRect(px, py, size, size, arc, arc);
        target.setStroke(Color.web("#111111"));
        target.setLineWidth(1);
        target.strokeRoundRect(px, py, size, size, arc, arc);
    }

    private void drawTimeBlockCell(int x, int y, int cs) {
        drawCell(x, y, Color.web("#33ccff"), 1.0);
        gameGc.setStroke(Color.web("#e6ffff"));
        gameGc.setLineWidth(1.0);
        double px = x * cs + 4, py = y * cs + 4;
        gameGc.strokeOval(px, py, cs - 8, cs - 8);
        gameGc.strokeLine(x * cs + cs / 2.0, y * cs + 6, x * cs + cs / 2.0, y * cs + cs / 2.0);
        gameGc.strokeLine(x * cs + cs / 2.0, y * cs + cs / 2.0, x * cs + cs - 8, y * cs + cs / 2.0);
    }

    private void drawRandomBlockIndicator(Piece piece, int cs) {
        int[][] shape = piece.getType().getShape(piece.getRotation());
        gameGc.setStroke(Color.web("#00e5ff"));
        gameGc.setLineWidth(2.2);
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++)
                if (shape[row][col] == 1) {
                    double px = (piece.getX() + col) * cs, py = (piece.getY() + row) * cs;
                    gameGc.strokeRect(px + 1, py + 1, cs - 2, cs - 2);
                }
    }

    private void drawQuestionMark(GraphicsContext gc, Canvas canvas) {
        double w = canvas.getWidth(), h = canvas.getHeight();
        gc.setFill(Color.web("#00ff00"));
        gc.setFont(Font.font("VT323", FontWeight.BOLD, 40));
        gc.fillText("?", w / 2 - 8, h / 2 + 12);
        gc.setEffect(new DropShadow(10, Color.web("#00ff00")));
    }

    private void updateLightBulb() {
        if (lightBulbView == null || !lightBulbView.isVisible()) return;
        HardModeHandler.BlackoutState bState = hardMode.blackoutState;
        boolean off = switch (bState) {
            case BLACKOUT -> true;
            case FLICKER, POST_FLICKER -> ((int) hardMode.blackoutFlickerTimer) % 2 == 0;
            default -> false;
        };
        lightBulbView.setImage(off ? lightOffImage : lightOnImage);
    }
}
