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
    private static final String PANE_BASE_STYLE = "";
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
    private final Image timeBlockSprite;
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
    private double tetrisFlashAlpha = 0;
    private int[]  tetrisFlashRows  = null;
    private double postClearAlpha   = 0;
    private int[]  postClearRows    = null;
    private double ghostParticleTimer = 0;
    private double randomBlockSparkTimer = 0;
    private double freezeScale = 1.0; // 1=normal, 0=fully frozen; lerps on transition
    private static final double FREEZE_LERP_SPEED = 3.0;

    // Pre-freeze cinematic (darkness + time block glow before freeze activates)
    private boolean preFreezeActive = false;
    private double preFreezeElapsed = 0;
    private java.util.List<int[]> preFreezePositions = java.util.List.of();
    private Runnable preFreezeCallback = null;
    private static final double PREFREEZE_DUR = 1.00;
    private double freezeFlashAlpha = 0; // strong green pulse at cinematic→freeze transition

    // Startup glitch
    double startupGlitchElapsed = 0;
    private Canvas startupCanvas;

    // Pause glitch
    private double pauseGlitchElapsed = 0;
    private GraphicsContext startupGc;

    // Eye animation during blackout
    private Image[] eyeFrames;
    private static final int EYE_FRAMES = 9;
    private static final double EYE_FPS = 8.0;
    private Canvas bgEffectCanvas;
    private GraphicsContext bgGc;
    private boolean wasInBlackout = false;
    private long lastBgRenderNs = 0;

    private static class EyeInstance {
        double baseX, baseY, x, y;
        double size, rotation, frameTimer, alpha;
        int frame;
        boolean flipX, flipY, fadingOut;
    }
    private final List<EyeInstance> eyes = new ArrayList<>();
    private double eyeSpawnTimer = 0;
    private static final double EYE_SPAWN_INTERVAL = 0.2;
    private static final double STRIP_W = 260.0;

    // Bulb motes
    private static class BulbMote {
        double baseX, y;    // baseX is the sine-wave center
        double sineAmp, sineFreq, sinePhase;
        double vy, life, maxLife, size;
        boolean white;
        double x() { return baseX + Math.sin(sinePhase + life * sineFreq) * sineAmp; }
    }
    private final List<BulbMote> bulbMotes = new ArrayList<>();
    private double bulbMoteTimer = 0;

    // Side-strip particle burst on line clear / bomb
    private static final double PARTICLE_GRAVITY = 750.0;
    private static final double CENTER_LEFT_X = 260.0;
    private double centerRightX() { return bgEffectCanvas != null ? bgEffectCanvas.getWidth() - STRIP_W : 1180.0; }
    private static final Color[] SPARK_COLORS = {
        Color.web("#FF8C00"), Color.web("#FFA500"), Color.web("#FF6B00"),
        Color.web("#FF4500"), Color.web("#FFD700"), Color.web("#FF3300")
    };
    private final List<SideParticle> sideParticles = new ArrayList<>();

    private static class SideParticle {
        double x, y, vx, vy;
        double alpha, size, life, maxLife;
        Color color;
    }

    // Hard-drop trail
    private static class HardDropTrail {
        int pieceX, pieceY, dropDistance;
        int[][] shape;
        double alpha;
    }
    private final List<HardDropTrail> hardDropTrails = new ArrayList<>();

    // Rain (Time Attack background)
    private static class RainDrop {
        double x, y, vx, vy;
    }
    private final List<RainDrop> rainDrops = new ArrayList<>();
    double rainTimeScale = 1.0;
    private static final int    RAIN_COUNT  = 420;
    private static final int    RAIN_TRAIL  = 14;
    private static final double RAIN_DT     = 0.006;

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
        this.timeBlockSprite = new Image(getClass().getResourceAsStream("/assets/timeblock.png"));
        var eyeStream = getClass().getResourceAsStream("/assets/eyes.png");
        if (eyeStream != null) {
            Image sheet = new Image(eyeStream);
            int fw = (int) sheet.getWidth();
            int fh = (int) (sheet.getHeight() / EYE_FRAMES);
            javafx.scene.image.PixelReader pr = sheet.getPixelReader();
            this.eyeFrames = new Image[EYE_FRAMES];
            for (int i = 0; i < EYE_FRAMES; i++)
                this.eyeFrames[i] = new javafx.scene.image.WritableImage(pr, 0, i * fh, fw, fh);
        }
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
    }

    void setBgEffectCanvas(Canvas canvas) {
        this.bgEffectCanvas = canvas;
        this.bgGc = canvas.getGraphicsContext2D();
    }

    private void spawnEyes() {
        eyes.clear();
        eyeSpawnTimer = 0;
        spawnEyeBatch(8);
    }

    private void spawnEyeBatch(int count) {
        double ch = bgEffectCanvas.getHeight();
        for (int i = 0; i < count; i++) {
            double size = 50 + rng.nextDouble() * 50;
            boolean left = rng.nextBoolean();
            // Try up to 12 positions to avoid overlap
            double bx = 0, by = 0;
            for (int attempt = 0; attempt < 12; attempt++) {
                double cw = bgEffectCanvas.getWidth();
                bx = left
                        ? STRIP_W * (0.1 + rng.nextDouble() * 0.8)
                        : (cw - STRIP_W) + STRIP_W * (0.1 + rng.nextDouble() * 0.8);
                by = ch * rng.nextDouble();
                boolean overlaps = false;
                for (EyeInstance ex : eyes) {
                    double minDist = (size + ex.size) * 0.6;
                    double dx = bx - ex.baseX, dy = by - ex.baseY;
                    if (dx * dx + dy * dy < minDist * minDist) { overlaps = true; break; }
                }
                if (!overlaps) break;
            }
            EyeInstance e = new EyeInstance();
            e.baseX = bx; e.baseY = by;
            e.x = bx; e.y = by;
            e.size = size;
            e.rotation = rng.nextDouble() * 360;
            e.flipX = rng.nextBoolean();
            e.flipY = rng.nextBoolean();
            e.frame = 0;
            e.frameTimer = 0;
            e.alpha = 1.0;
            e.fadingOut = false;
            eyes.add(e);
        }
    }

    private void renderEyes(double dt) {
        if (eyeFrames == null) return;
        boolean inBlackout = hardMode.blackoutState == HardModeHandler.BlackoutState.BLACKOUT;
        if (!inBlackout) {
            if (wasInBlackout) eyes.clear();
            wasInBlackout = false;
            return;
        }
        if (!wasInBlackout) {
            spawnEyes();
            wasInBlackout = true;
        }

        // Continuous spawning — one at a time
        eyeSpawnTimer += dt;
        if (eyeSpawnTimer >= EYE_SPAWN_INTERVAL) {
            eyeSpawnTimer = 0;
            spawnEyeBatch(1);
        }

        eyes.removeIf(e -> e.alpha <= 0);

        for (EyeInstance e : eyes) {
            // Rumble
            e.x = e.baseX + (rng.nextDouble() - 0.5) * 6;
            e.y = e.baseY + (rng.nextDouble() - 0.5) * 6;

            // Advance frame
            if (!e.fadingOut) {
                e.frameTimer += dt;
                if (e.frameTimer >= 1.0 / EYE_FPS) {
                    e.frameTimer -= 1.0 / EYE_FPS;
                    e.frame++;
                    if (e.frame >= EYE_FRAMES) {
                        e.frame = EYE_FRAMES - 1;
                        e.fadingOut = true;
                    }
                }
            } else {
                e.alpha = Math.max(0, e.alpha - dt * 2.0);
            }

            Image frame = eyeFrames[e.frame];
            double aspect = frame.getWidth() / frame.getHeight();
            double drawH = e.size;
            double drawW = drawH * aspect;
            bgGc.save();
            bgGc.setGlobalAlpha(e.alpha);
            bgGc.translate(e.x, e.y);
            bgGc.rotate(e.rotation);
            bgGc.scale(e.flipX ? -1 : 1, e.flipY ? -1 : 1);
            bgGc.drawImage(frame, -drawW / 2, -drawH / 2, drawW, drawH);
            bgGc.restore();
        }
    }

    private void renderBgEffects() {
        if (bgGc == null) return;
        long now = System.nanoTime();
        double dt = lastBgRenderNs == 0 ? 0 : (now - lastBgRenderNs) / 1_000_000_000.0;
        lastBgRenderNs = now;
        bgGc.clearRect(0, 0, bgEffectCanvas.getWidth(), bgEffectCanvas.getHeight());
        renderBulbGlow();
        renderBulbMotes(dt);
        renderEyes(dt);
        renderSideParticles(dt);
        if (gameOverFlashAlpha > 0) {
            bgGc.setFill(Color.color(1, 1, 1, gameOverFlashAlpha));
            bgGc.fillRect(CENTER_LEFT_X, 0, centerRightX() - CENTER_LEFT_X, bgEffectCanvas.getHeight());
        }
    }

    void triggerPreFreezeCinematic(java.util.List<int[]> cells, Runnable onComplete) {
        preFreezePositions = cells;
        preFreezeElapsed   = 0;
        preFreezeActive    = true;
        preFreezeCallback  = onComplete;
    }

    void triggerTetrisFlash() {
        tetrisFlashAlpha = 1.0;
    }

    void triggerTetrisFlash(int[] rows) {
        tetrisFlashRows = rows;
        tetrisFlashAlpha = 1.0;
    }

    void triggerPostClearFlash(int[] rows) {
        postClearRows  = rows;
        postClearAlpha = 1.0;
        tetrisFlashAlpha = 1.0;
    }

    void triggerSideParticles(int intensity) {
        if (bgGc == null) return;
        int count = switch (intensity) {
            case 1 -> 10;
            case 2 -> 18;
            case 3 -> 26;
            default -> 45;
        };
        double canvasH = bgEffectCanvas.getHeight();
        for (int side = 0; side < 2; side++) {
            double originX = side == 0 ? CENTER_LEFT_X : centerRightX();
            double dirX = side == 0 ? -1 : 1;
            for (int i = 0; i < count; i++) {
                SideParticle p = new SideParticle();
                p.x = originX;
                p.y = 80 + rng.nextDouble() * (canvasH - 280);
                double speed = 220 + rng.nextDouble() * 320;
                double angle = rng.nextDouble() * 50 - 10; // -10° to +40°, mostly horizontal
                double rad = Math.toRadians(angle);
                p.vx = dirX * speed * Math.cos(rad);
                p.vy = speed * Math.sin(rad);
                p.size = 2 + rng.nextDouble() * 5;
                p.life = 0;
                p.maxLife = 0.6 + rng.nextDouble() * 0.6;
                p.alpha = 1.0;
                p.color = SPARK_COLORS[rng.nextInt(SPARK_COLORS.length)];
                sideParticles.add(p);
            }
        }
    }

    private void renderBulbMotes(double dt) {
        if (lightBulbView == null || !lightBulbView.isVisible()) return;
        if (hardMode != null && hardMode.blackoutState == HardModeHandler.BlackoutState.BLACKOUT) {
            bulbMotes.clear();
            return;
        }
        javafx.geometry.Bounds bScene = lightBulbView.localToScene(lightBulbView.getBoundsInLocal());
        javafx.geometry.Bounds bCanvas = bgEffectCanvas.sceneToLocal(bScene);
        double bulbCx = bCanvas.getMinX() + bCanvas.getWidth() * 0.5;
        double bulbBottom = bCanvas.getMinY() + bCanvas.getHeight() * 0.78;

        bulbMoteTimer -= dt;
        if (bulbMoteTimer <= 0) {
            bulbMoteTimer = 0.15 + rng.nextDouble() * 0.20;
            BulbMote m = new BulbMote();
            m.baseX = bulbCx + (rng.nextDouble() - 0.5) * 30;
            m.y = bulbBottom + rng.nextDouble() * 10;
            m.vy = 18 + rng.nextDouble() * 28;
            m.sineAmp = 8 + rng.nextDouble() * 22;
            m.sineFreq = 1.2 + rng.nextDouble() * 2.0;
            m.sinePhase = rng.nextDouble() * Math.PI * 2;
            m.maxLife = 2.0 + rng.nextDouble() * 2.5;
            m.life = 0;
            m.size = 1.5 + rng.nextDouble() * 1.5;
            m.white = rng.nextDouble() < 0.35;
            bulbMotes.add(m);
        }
        bulbMotes.removeIf(m -> m.life >= m.maxLife);
        for (BulbMote m : bulbMotes) {
            m.life += dt;
            m.y += m.vy * dt;
            double t = m.life / m.maxLife;
            double alpha = t < 0.12 ? t / 0.12 : (t > 0.65 ? (1.0 - t) / 0.35 : 1.0);
            bgGc.setGlobalAlpha(alpha * 0.85);
            bgGc.setFill(m.white ? Color.web("#FFFDE0") : Color.web("#FFD966"));
            bgGc.fillRect(m.x(), m.y, m.size, m.size);
        }
        bgGc.setGlobalAlpha(1.0);
    }

    private void renderSideParticles(double dt) {
        if (hardMode != null && hardMode.blackoutState == HardModeHandler.BlackoutState.BLACKOUT) {
            sideParticles.clear();
            return;
        }
        if (sideParticles.isEmpty()) return;
        sideParticles.removeIf(p -> p.alpha <= 0);
        for (SideParticle p : sideParticles) {
            p.vy += PARTICLE_GRAVITY * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.life += dt;
            p.alpha = Math.max(0, 1.0 - p.life / p.maxLife);
            bgGc.setGlobalAlpha(p.alpha);
            bgGc.setFill(p.color);
            bgGc.fillOval(p.x - p.size / 2, p.y - p.size / 2, p.size, p.size);
        }
        bgGc.setGlobalAlpha(1.0);
    }

    // --- Triggered events from game logic ---

    void onRotate() { rotationPulse = 1.0; }

    void onHardDrop() {
        shakeIntensity = 4.0;
        shakeInitDuration = 0.18;
        shakeDuration = 0.18;
    }

    void onHardDropTrail(Piece piece, int drop) {
        if (drop <= 0) return;
        HardDropTrail t = new HardDropTrail();
        t.pieceX       = piece.getX();
        t.pieceY       = piece.getY();
        t.dropDistance = drop;
        t.shape        = piece.getType().getShape(piece.getRotation());
        t.alpha        = 1.0;
        hardDropTrails.add(t);
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

    void update(double dt, boolean gamePaused, boolean isGameOver, Piece currentPiece, long freezeUntil, boolean isFreezeActive) {
        if (gamePaused && !isGameOver) pauseGlitchElapsed += dt;
        else pauseGlitchElapsed = 0;

        if (glitchTearEffect != null) glitchTearEffect.update(dt);
        if (gameOverFlashAlpha > 0) gameOverFlashAlpha = Math.max(0, gameOverFlashAlpha - dt * 4.0);
        if (startupGlitchElapsed < STARTUP_GLITCH_DUR) startupGlitchElapsed += dt;
        if (glitchExplosionEffect != null) {
            glitchExplosionEffect.update(dt);
            if (glitchExplosionEffect.isDone()) glitchExplosionEffect = null;
        }

        if (!gamePaused && !isGameOver) {
            if (preFreezeActive) {
                preFreezeElapsed += dt;
                if (preFreezeElapsed >= PREFREEZE_DUR) {
                    preFreezeActive = false;
                    freezeFlashAlpha = 1.0;
                    if (preFreezeCallback != null) { preFreezeCallback.run(); preFreezeCallback = null; }
                }
            }
            if (freezeFlashAlpha > 0) freezeFlashAlpha = Math.max(0, freezeFlashAlpha - dt * 5.5);
            if (isFreezeActive) freezeScale = Math.max(0.0, freezeScale - dt * FREEZE_LERP_SPEED);
            else                freezeScale = Math.min(1.0, freezeScale + dt * FREEZE_LERP_SPEED);
            particleSystem.update(dt);
            scorePopups.removeIf(p -> p.life <= 0);
            for (ScorePopup p : scorePopups) {
                p.x += p.vx * dt; p.y += p.vy * dt; p.life -= dt / 0.7;
            }
            if (freezeUntil > 0) {
                gameCanvas.setTranslateX(0); gameCanvas.setTranslateY(0);
                vfxCanvas.setTranslateX(0); vfxCanvas.setTranslateY(0);
            } else {
                updateScreenShake(dt);
            }

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
            if (currentPiece instanceof RandomBlock && !currentPiece.isAreaBomb()) {
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
            if (tetrisFlashAlpha > 0) tetrisFlashAlpha = Math.max(0, tetrisFlashAlpha - dt * 12.0);
            if (postClearAlpha  > 0) postClearAlpha   = Math.max(0, postClearAlpha  - dt * 5.0);
            hardDropTrails.removeIf(t -> t.alpha <= 0);
            for (HardDropTrail t : hardDropTrails) t.alpha = Math.max(0, t.alpha - dt * 2.5);
            if (gameContext.getGameMode() == GameContext.GameMode.TIME_ATTACK) {
                if (rainDrops.isEmpty()) initRain();
                double rcw = bgEffectCanvas != null ? bgEffectCanvas.getWidth()  : 1440;
                double rch = bgEffectCanvas != null ? bgEffectCanvas.getHeight() : 1024;
                for (RainDrop r : rainDrops) {
                    r.x += r.vx * dt * freezeScale * rainTimeScale;
                    r.y += r.vy * dt * freezeScale * rainTimeScale;
                    if (r.y > rch + 60) { r.y = -60; r.x = rng.nextDouble() * rcw; }
                    if (r.x > rcw + 40)   r.x -= rcw + 40;
                    if (r.x < -40)        r.x += rcw + 40;
                }
            }
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
                long freezeUntil, int[] frozenRowFlash, boolean isFreezeActive,
                int floatingBombX, int floatingBombY) {

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

        drawGameBoard(currentPiece, suspendedPieces, gamePaused, isGameOver, freezeUntil, frozenRowFlash,
                floatingBombX, floatingBombY);
        renderHardDropTrails();

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

        if (hardModeActive) {
            hardMode.renderFlavourText(rng);
            renderBgEffects();
        } else if (gameContext.getGameMode() == GameContext.GameMode.TIME_ATTACK && bgGc != null) {
            renderRainBg();
        }

        renderStartupGlitch();

        if (startupGc != null && startupGlitchElapsed >= STARTUP_GLITCH_DUR) {
            double sw = startupCanvas.getWidth(), sh = startupCanvas.getHeight();
            startupGc.clearRect(0, 0, sw, sh);
            if (preFreezeActive) {
                double darkT = Math.min(1.0, preFreezeElapsed / 0.25);
                startupGc.setFill(Color.color(0, 0, 0, darkT * 0.82));
                startupGc.fillRect(0, 0, sw, sh);
                drawPreFreezeCinematic();
            } else if (freezeScale < 1.0) {
                startupGc.setFill(Color.color(0.0, 1.0, 0.47, (1.0 - freezeScale) * 0.12));
                startupGc.fillRect(0, 0, sw, sh);
                if (!gamePaused && !isGameOver) {
                    int cs = Constants.BLOCK_SIZE;
                    int[][] shape = currentPiece.getType().getShape(currentPiece.getRotation());
                    for (int row = 0; row < 4; row++)
                        for (int col = 0; col < 4; col++)
                            if (shape[row][col] == 1) {
                                javafx.geometry.Point2D sp = startupCanvas.sceneToLocal(
                                    gameCanvas.localToScene(
                                        (currentPiece.getX() + col) * cs,
                                        (currentPiece.getY() + row) * cs));
                                startupGc.clearRect(sp.getX(), sp.getY(), cs, cs);
                            }
                }
            }
            if (freezeFlashAlpha > 0) {
                startupGc.setFill(Color.color(0.0, 1.0, 0.40, freezeFlashAlpha * 0.85));
                startupGc.fillRect(0, 0, sw, sh);
            }
            if (gamePaused && !isGameOver) {
                drawPauseGlitch();
            } else if (frozenRowFlash != null && freezeUntil > 0) {
                startupGc.setFill(Color.color(1, 1, 1, 0.10));
                startupGc.fillRect(0, 0, sw, sh);
            } else if (tetrisFlashAlpha > 0) {
                startupGc.setFill(Color.color(1, 1, 1, tetrisFlashAlpha));
                startupGc.fillRect(0, 0, sw, sh);
            }
        }
    }

    // --- Board rendering ---
    private void drawGameBoard(Piece currentPiece, List<Piece> suspendedPieces,
                               boolean gamePaused, boolean isGameOver,
                               long freezeUntil, int[] frozenRowFlash,
                               int floatingBombX, int floatingBombY) {
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

        if (freezeScale < 1.0) {
            double tintA = (1.0 - freezeScale) * 0.90;
            gameGc.setGlobalAlpha(tintA);
            gameGc.setFill(Color.color(0.0, 1.0, 0.47));
            for (int y = 0; y < Constants.BOARD_HEIGHT; y++)
                for (int x = 0; x < Constants.BOARD_WIDTH; x++)
                    if (board[y][x] != 0)
                        gameGc.fillRect(x * cs, y * cs, cs, cs);
            gameGc.setGlobalAlpha(1.0);
        }

        if (frozenRowFlash != null && freezeUntil > 0) {
            gameGc.setFill(Color.WHITE);
            for (int row : frozenRowFlash) gameGc.fillRect(0, row * cs, w, cs);
        }

        if (postClearRows != null && postClearAlpha > 0) {
            Color modeColor = switch (gameContext.getGameMode()) {
                case TIME_ATTACK -> Color.web("#00CCCC");
                case HARD_MODE   -> Color.web("#FF6600");
                default          -> Color.web("#6655EE");
            };
            gameGc.setGlobalAlpha(postClearAlpha);
            gameGc.setFill(modeColor);
            for (int row : postClearRows) gameGc.fillRect(0, row * cs, w, cs);
            gameGc.setGlobalAlpha(1.0);
        }

        if (gameOverFlashAlpha > 0) {
            gameGc.setFill(Color.color(1, 1, 1, gameOverFlashAlpha));
            gameGc.fillRect(0, 0, w, h);
        }

        if (isGameOver) return;

        if (currentPiece.isBomb() && freezeUntil == 0 && bState != HardModeHandler.BlackoutState.BLACKOUT) {
            drawBombImpactZone(currentPiece, suspendedPieces, cs);
        }

        renderSuspendedPieces(suspendedPieces, cs);
        renderFloatingBombPickup(floatingBombX, floatingBombY, bState, cs);
        drawCurrentPiece(currentPiece, bState, cs, suspendedPieces, freezeUntil);
    }

    private void drawCurrentPiece(Piece currentPiece, HardModeHandler.BlackoutState bState, int cs,
                                   List<Piece> suspendedPieces, long freezeUntil) {
        int[][] shape = currentPiece.getType().getShape(currentPiece.getRotation());
        boolean isBomb = currentPiece.isBomb();
        boolean isGhost = currentPiece.getType() == TetrominoType.TRANSPARENT;
        boolean isRandomBlock = currentPiece instanceof RandomBlock && !currentPiece.isAreaBomb();
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

        // Ghost piece (drop preview) — hidden during blackout
        if (freezeUntil == 0 && !inBlackout) {
            int drop = boardEngine.getDropDistance(currentPiece, suspendedPieces);
            int[][] ghostShape = currentPiece.getType().getShape(currentPiece.getRotation());
            for (int row = 0; row < 4; row++)
                for (int col = 0; col < 4; col++)
                    if (ghostShape[row][col] == 1) {
                        if (currentPiece.getType() == TetrominoType.TRANSPARENT) {
                            gameGc.setGlobalAlpha(0.25);
                            gameGc.drawImage(ghostSprite, (currentPiece.getX() + col) * cs, (currentPiece.getY() + row + drop) * cs, cs, cs);
                            gameGc.setGlobalAlpha(1.0);
                        } else if (currentPiece.isBomb()) {
                            gameGc.setGlobalAlpha(0.25);
                            gameGc.drawImage(bombSprite, (currentPiece.getX() + col) * cs,
                                    (currentPiece.getY() + row + drop) * cs, cs, cs);
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
            gameGc.setGlobalAlpha(0.50);
            gameGc.setFill(Color.web("#AC2547"));
            for (int row = 0; row < 4; row++)
                for (int col = 0; col < 4; col++)
                    if (shape[row][col] == 1)
                        gameGc.fillRect((currentPiece.getX() + col) * cs - caOffset,
                                (currentPiece.getY() + row) * cs, cs, cs);
            gameGc.setFill(Color.web("#EEA9BA"));
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
        if (isRandomBlock && !inBlackout && freezeScale >= 1.0) {
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
        if (holdType != null) drawPreview(holdGc, holdBlockCanvas, new Piece(holdType, 0, 0), true);
        else drawPreview(holdGc, holdBlockCanvas, null, true);
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
        drawPreview(gc, canvas, piece, false);
    }

    private void drawPreview(GraphicsContext gc, Canvas canvas, Piece piece, boolean showActual) {
        double w = canvas.getWidth(), h = canvas.getHeight();
        HardModeHandler.BlackoutState bState = hardMode.blackoutState;

        if (gameContext.getGameMode() != GameContext.GameMode.HARD_MODE) {
            gc.clearRect(0, 0, w, h);
        } else if (!showActual) {
            gc.setFill(Color.web(
                    bState == HardModeHandler.BlackoutState.BLACKOUT ? "#000000" : "#280C00"));
            gc.fillRect(0, 0, w, h);
            drawQuestionMark(gc, canvas); return;
        } else {
            gc.setFill(Color.web(
                    bState == HardModeHandler.BlackoutState.BLACKOUT ? "#000000" : "#280C00"));
            gc.fillRect(0, 0, w, h);
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

    private void renderFloatingBombPickup(int bombX, int bombY, HardModeHandler.BlackoutState bState, int cs) {
        if (bombX < 0 || bombY < 0 || bState == HardModeHandler.BlackoutState.BLACKOUT) {
            return;
        }

        double px = bombX * cs;
        double py = bombY * cs;
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 1000.0 * 6.5);
        double cx = px + cs / 2.0;

        gameGc.save();
        gameGc.setGlobalAlpha(0.30 + pulse * 0.25);
        gameGc.setStroke(Color.web("#ffcc66"));
        gameGc.setLineWidth(1.5);
        gameGc.strokeLine(cx, 0, cx, py + cs * 0.22);

        gameGc.setGlobalAlpha(0.18 + pulse * 0.20);
        gameGc.setFill(Color.web("#ff5500"));
        double glow = cs * (1.6 + pulse * 0.4);
        gameGc.fillOval(cx - glow / 2.0, py + cs / 2.0 - glow / 2.0, glow, glow);

        gameGc.setGlobalAlpha(1.0);
        double bob = Math.sin(System.currentTimeMillis() / 1000.0 * 4.0) * 2.0;
        gameGc.drawImage(bombSprite, px, py + bob, cs, cs);
        gameGc.restore();
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


    private void drawPreFreezeCinematic() {
        if (startupGc == null || preFreezePositions.isEmpty()) return;
        int cs = Constants.BLOCK_SIZE;
        double t = Math.min(1.0, preFreezeElapsed / PREFREEZE_DUR);
        double appear = Math.min(1.0, preFreezeElapsed / 0.15);

        for (int[] cell : preFreezePositions) {
            javafx.geometry.Point2D sp = startupCanvas.sceneToLocal(
                    gameCanvas.localToScene(cell[0] * cs, cell[1] * cs));
            double px = sp.getX(), py = sp.getY();

            // Soft outer glow — stacked large filled rects, appears once and holds
            for (int ring = 7; ring >= 1; ring--) {
                double expand = ring * cs * 1.1;
                double a = (0.55 / ring) * appear;
                startupGc.setGlobalAlpha(Math.min(1, a));
                startupGc.setFill(Color.color(0.0, 1.0, 0.50));
                startupGc.fillRect(px - expand / 2, py - expand / 2, cs + expand, cs + expand);
            }

            // Core cell — solid neon green
            startupGc.setGlobalAlpha(appear);
            startupGc.setFill(Color.color(0.0, 1.0, 0.55));
            startupGc.fillRect(px, py, cs, cs);

            // Hot white center
            startupGc.setGlobalAlpha(appear * 0.90);
            startupGc.setFill(Color.WHITE);
            double inset = cs * 0.22;
            startupGc.fillRect(px + inset, py + inset, cs - inset * 2, cs - inset * 2);

            // Expanding ring borders — single outward burst, fade as they grow
            for (int ring = 1; ring <= 4; ring++) {
                double expand = t * cs * ring * 3.0;
                double a = Math.max(0, (1.0 - t) * (0.90 / ring));
                startupGc.setGlobalAlpha(a);
                startupGc.setStroke(ring % 2 == 0 ? Color.color(0.0, 1.0, 0.80) : Color.color(0.4, 1.0, 0.6));
                startupGc.setLineWidth(2.5);
                startupGc.strokeRect(px - expand / 2, py - expand / 2, cs + expand, cs + expand);
            }
        }
        startupGc.setGlobalAlpha(1.0);
    }

    private void drawPauseGlitch() {
        if (startupGc == null) return;
        double w = startupCanvas.getWidth();
        double h = startupCanvas.getHeight();
        double burst = Math.max(0.0, 1.0 - pauseGlitchElapsed / 0.35);
        double t = System.nanoTime() / 1_000_000_000.0;

        // Dark overlay — heavier on entry
        startupGc.setFill(Color.color(0, 0, 0, 0.42 + burst * 0.25));
        startupGc.fillRect(0, 0, w, h);

        // Pulse: periodic glitch spikes after the burst settles
        double pulse = Math.max(0, Math.sin(t * 2.1) * Math.sin(t * 5.9));
        double noise = burst + pulse * 0.6;

        // Chromatic aberration horizontal bands
        int numBands = (int)(3 + burst * 6 + noise * 5);
        for (int i = 0; i < numBands; i++) {
            double by   = rng.nextDouble() * h;
            double bh   = 1 + rng.nextDouble() * h * 0.04;
            double xOff = (rng.nextDouble() - 0.5) * w * (0.18 + burst * 0.28);
            double a    = (0.12 + rng.nextDouble() * 0.22) * Math.max(0.2, noise);
            startupGc.setFill(Color.color(1.0, 0.08, 0.08, Math.min(1, a)));
            startupGc.fillRect(xOff - 7, by, w + 14, bh);
            startupGc.setFill(Color.color(0.08, 1.0, 1.0, Math.min(1, a * 0.9)));
            startupGc.fillRect(xOff + 7, by, w + 14, bh);
            startupGc.setFill(Color.color(1.0, 1.0, 1.0, Math.min(1, a * 0.55)));
            startupGc.fillRect(xOff, by, w, bh);
        }

        // Noise pixels
        int pixCount = (int)(12 + 40 * Math.max(noise, 0.15));
        for (int i = 0; i < pixCount; i++) {
            double nx = Math.floor(rng.nextDouble() * w / 2) * 2;
            double ny = Math.floor(rng.nextDouble() * h / 2) * 2;
            startupGc.setFill(Color.color(1, 1, 1, 0.2 + rng.nextDouble() * 0.6));
            startupGc.fillRect(nx, ny, 2, 2);
        }

        // Occasional full-screen flicker
        double flicker = Math.sin(t * 17.1) * Math.sin(t * 43.3);
        double threshold = 0.72 - burst * 0.25;
        if (flicker > threshold) {
            double fa = (flicker - threshold) / (1.0 - threshold) * (0.22 + burst * 0.35);
            startupGc.setFill(Color.color(1, 1, 1, Math.min(1, fa)));
            startupGc.fillRect(0, 0, w, h);
        }

        // Entry burst flash
        if (burst > 0) {
            startupGc.setFill(Color.color(1, 1, 1, burst * 0.55));
            startupGc.fillRect(0, 0, w, h);
        }
    }

    // --- Hard-drop trail ---

    private void renderHardDropTrails() {
        if (hardDropTrails.isEmpty()) return;
        int cs = Constants.BLOCK_SIZE;
        for (HardDropTrail trail : hardDropTrails) {
            for (int col = 0; col < 4; col++) {
                int minRow = -1;
                for (int row = 0; row < 4; row++)
                    if (trail.shape[row][col] == 1) { minRow = row; break; }
                if (minRow < 0) continue;

                double baseX  = (trail.pieceX + col) * cs;
                double topPx  = (trail.pieceY + minRow) * cs;
                int    steps  = trail.dropDistance;
                if (steps <= 0) continue;

                double trailH = steps * cs;
                javafx.scene.paint.LinearGradient grad = new javafx.scene.paint.LinearGradient(
                    0, topPx, 0, topPx + trailH, false,
                    javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0.0,  Color.color(0.0, 1.0,  0.40, trail.alpha * 0.85)),
                    new javafx.scene.paint.Stop(0.5,  Color.color(0.0, 0.78, 0.78, trail.alpha * 0.70)),
                    new javafx.scene.paint.Stop(1.0,  Color.color(0.0, 0.18, 0.55, trail.alpha * 0.50))
                );
                gameGc.setFill(grad);
                gameGc.fillRect(baseX, topPx, cs, trailH);
            }
        }
        gameGc.setGlobalAlpha(1.0);
    }

    // --- Rain (Time Attack) ---

    private void initRain() {
        double cw = bgEffectCanvas != null ? bgEffectCanvas.getWidth()  : 1440;
        double ch = bgEffectCanvas != null ? bgEffectCanvas.getHeight() : 1024;
        for (int i = 0; i < RAIN_COUNT; i++) {
            RainDrop r = new RainDrop();
            r.x = rng.nextDouble() * cw;
            r.y = rng.nextDouble() * ch;
            double speed = 380 + rng.nextDouble() * 220;
            double angle = Math.PI / 2.0 + 0.12 + (rng.nextDouble() - 0.5) * 0.08;
            r.vx = Math.cos(angle) * speed;
            r.vy = Math.sin(angle) * speed;
            rainDrops.add(r);
        }
    }

    private void renderRainBg() {
        if (bgGc == null || rainDrops.isEmpty()) return;
        double cw = bgEffectCanvas.getWidth(), ch = bgEffectCanvas.getHeight();
        bgGc.clearRect(0, 0, cw, ch);
        int trail = Math.max(1, (int)(RAIN_TRAIL * freezeScale));
        for (RainDrop r : rainDrops) {
            for (int i = 0; i < trail; i++) {
                double tx = r.x - r.vx * (i * RAIN_DT);
                double ty = r.y - r.vy * (i * RAIN_DT);
                double alpha = (1.0 - (double) i / trail) * 0.60;
                bgGc.setFill(Color.color(0.53, 0.80, 0.93, alpha));
                bgGc.fillRect((int) tx, (int) ty, 1, 1);
            }
        }
    }

    void setRainTimeScale(double scale) { this.rainTimeScale = scale; }

    // --- Cell drawing helpers ---

    private void drawBombImpactZone(Piece bomb, List<Piece> suspended, int cs) {
        int drop = boardEngine.getDropDistance(bomb, suspended);

        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 1000.0 * 7.0);

        if (bomb.isAreaBomb()) {
            int[][] shape = bomb.getType().getShape(bomb.getRotation());
            int minX = Constants.BOARD_WIDTH;
            int maxX = -1;
            int minY = Constants.BOARD_HEIGHT;
            int maxY = -1;
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    if (shape[row][col] == 1) {
                        int x = bomb.getX() + col;
                        int y = bomb.getY() + row + drop;
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            if (maxX < minX || maxY < minY) {
                return;
            }

            int fromX = Math.max(0, minX - 2);
            int toX = Math.min(Constants.BOARD_WIDTH - 1, maxX + 2);
            int fromY = Math.max(0, minY - 2);
            int toY = Math.min(Constants.BOARD_HEIGHT - 1, maxY + 2);

            gameGc.setFill(Color.web("#ff4400", 0.06 + pulse * 0.10));
            for (int y = fromY; y <= toY; y++) {
                for (int x = fromX; x <= toX; x++) {
                    gameGc.fillRect(x * cs, y * cs, cs, cs);
                }
            }

            gameGc.setStroke(Color.web("#ff6600", 0.45 + pulse * 0.45));
            gameGc.setLineWidth(2);
            gameGc.strokeRect(fromX * cs, fromY * cs, (toX - fromX + 1) * cs, (toY - fromY + 1) * cs);
            return;
        }

        int cx   = bomb.getX();
        int cy   = bomb.getY() + drop;

        // Tint each cell in the 3x3 zone
        gameGc.setFill(Color.web("#ff4400", 0.08 + pulse * 0.12));
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                int bx = cx + dx, by = cy + dy;
                if (bx >= 0 && bx < Constants.BOARD_WIDTH && by >= 0 && by < Constants.BOARD_HEIGHT)
                    gameGc.fillRect(bx * cs, by * cs, cs, cs);
            }

        // Pulsing border around the full 3x3 area
        int x1 = Math.max(0,                     cx - 1) * cs;
        int y1 = Math.max(0,                     cy - 1) * cs;
        int x2 = Math.min(Constants.BOARD_WIDTH,  cx + 2) * cs;
        int y2 = Math.min(Constants.BOARD_HEIGHT, cy + 2) * cs;
        gameGc.setStroke(Color.web("#ff6600", 0.45 + pulse * 0.45));
        gameGc.setLineWidth(2);
        gameGc.strokeRect(x1, y1, x2 - x1, y2 - y1);
    }

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
        gameGc.drawImage(timeBlockSprite, x * cs, y * cs, cs, cs);
    }


    private void drawQuestionMark(GraphicsContext gc, Canvas canvas) {
        double w = canvas.getWidth(), h = canvas.getHeight();
        gc.setFill(Color.web("#00ff00"));
        gc.setFont(Font.font("VT323", FontWeight.BOLD, 40));
        gc.fillText("?", w / 2 - 8, h / 2 + 12);
        gc.setEffect(new DropShadow(10, Color.web("#00ff00")));
    }

    private static final DropShadow BULB_GLOW = createBulbGlow();
    private static DropShadow createBulbGlow() {
        // capped at ~127px by JavaFX internally — just for the close-up halo on the image
        return new DropShadow(javafx.scene.effect.BlurType.GAUSSIAN, Color.web("#FFE8A0"), 100, 0.0, 0, 20);
    }

    private static final double BULB_GLOW_RADIUS = 700;
    private double cachedBulbGlowX = -1, cachedBulbGlowY = -1;
    private javafx.scene.paint.RadialGradient cachedBulbAmbientGlow = null;

    private void renderBulbGlow() {
        if (hardMode == null || lightBulbView == null || !lightBulbView.isVisible()) return;
        HardModeHandler.BlackoutState bState = hardMode.blackoutState;
        boolean off = switch (bState) {
            case BLACKOUT -> true;
            case FLICKER, POST_FLICKER -> ((int) hardMode.blackoutFlickerTimer) % 2 == 0;
            default -> false;
        };
        if (off) return;
        javafx.geometry.Bounds bScene = lightBulbView.localToScene(lightBulbView.getBoundsInLocal());
        javafx.geometry.Bounds bCanvas = bgEffectCanvas.sceneToLocal(bScene);
        double gx = bCanvas.getMinX() + bCanvas.getWidth() * 0.5;
        double gy = bCanvas.getMinY() + bCanvas.getHeight() * 0.62;
        if (cachedBulbAmbientGlow == null || Math.abs(gx - cachedBulbGlowX) > 1 || Math.abs(gy - cachedBulbGlowY) > 1) {
            cachedBulbGlowX = gx;
            cachedBulbGlowY = gy;
            cachedBulbAmbientGlow = new javafx.scene.paint.RadialGradient(
                0, 0, gx, gy, BULB_GLOW_RADIUS, false,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0.0, Color.web("#FFE8A0", 0.50)),
                new javafx.scene.paint.Stop(0.3, Color.web("#FFD060", 0.18)),
                new javafx.scene.paint.Stop(1.0, Color.TRANSPARENT)
            );
        }
        bgGc.setFill(cachedBulbAmbientGlow);
        bgGc.fillRect(CENTER_LEFT_X, 0, centerRightX() - CENTER_LEFT_X, bgEffectCanvas.getHeight());
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
        lightBulbView.setEffect(off ? null : BULB_GLOW);
    }
}
