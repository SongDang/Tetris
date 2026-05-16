package com.se330.tetris.controller;

import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.Constants;
import com.se330.tetris.util.SoundType;
import com.se330.tetris.game.BorderPulseEffect;
import com.se330.tetris.game.GlitchTearEffect;
import com.se330.tetris.game.LevelUpEffect;
import com.se330.tetris.game.ParticleSystem;
import com.se330.tetris.game.Piece;
import com.se330.tetris.game.RandomBlock;
import com.se330.tetris.game.TetrominoType;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GameController {

    @FXML
    private Pane gamePane;
    @FXML
    private Canvas gameCanvas;
    @FXML
    private Canvas vfxCanvas;
    @FXML
    private Canvas nextBlockCanvas;
    @FXML
    private Canvas nextBlockCanvas1;
    @FXML
    private Canvas nextBlockCanvas2;
    @FXML
    private Canvas nextBlockCanvas3;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label levelLabel;
    @FXML
    private Label linesLabel;
    @FXML
    private Canvas holdBlockCanvas;
    @FXML
    private javafx.scene.image.ImageView lightBulbView;
    @FXML
    private javafx.scene.image.ImageView mainFrameView;
    @FXML
    private javafx.scene.control.Label flavourLabel;
    @FXML
    private javafx.scene.control.Label comboLabel;

    @FXML
    private Label bombsLabel;
    @FXML
    private VBox bombInventoryBox;

    private javafx.scene.image.Image lightOnImage;
    private javafx.scene.image.Image lightOffImage;
    private javafx.scene.image.Image hardMainImage;
    private javafx.scene.image.Image darkMainImage;

    private SceneManager sceneManager;
    private GameContext gameContext;

    private GraphicsContext gameGc;

    private GraphicsContext nextGc;
    private GraphicsContext vfxGc;
    // vfxCanvas extends 150 px beyond each board edge (canvas width = 600, layoutX
    // = -150)
    private static final double VFX_MARGIN = 150.0;
    private GraphicsContext holdGc;

    private final int[][] board = new int[Constants.BOARD_HEIGHT][Constants.BOARD_WIDTH];
    private Piece currentPiece;
    private Piece nextPiece;
    private TetrominoType holdType;
    private boolean canHold = true;
    private final ParticleSystem particleSystem = new ParticleSystem();
    private LevelUpEffect levelUpEffect = null;
    private BorderPulseEffect borderPulseEffect = null;
    private int bombsRemaining = 3;
    private String bombInventoryBaseStyle = "";

    private GlitchTearEffect glitchTearEffect = null;
    private com.se330.tetris.game.GlitchExplosionEffect glitchExplosionEffect = null;
    private javafx.scene.image.Image bombSprite;
    private javafx.scene.image.Image ghostSprite;

    private GraphicsContext nextGc1;
    private GraphicsContext nextGc2;
    private GraphicsContext nextGc3;

    private final java.util.ArrayDeque<Piece> nextQueue = new java.util.ArrayDeque<>();

    private AnimationTimer gameLoop;
    private boolean gamePaused = false;
    private boolean isGameOver = false;

    private long lastFallTime = 0;

    private long lastFrameTime = 0;
    private long fallIntervalNs = 500_000_000L;
    private long freezeUntil = 0; // nanosecond timestamp; game logic frozen until this time
    private long gameOverFreezeUntil = 0; // 0.5s static pause before glitch fires
    private double gameOverFlashAlpha = 0; // white flash on board at moment of game over
    private int[] pendingTetrisClear = null; // Tetris rows cleared after freeze ends
    private int[] frozenRowFlash = null; // row indices to flash white during Tetris freeze

    private boolean softDropping = false;
    private boolean fuseLoopPlaying = false;

    private int comboCount = 0;
    private int comboDisplay = 0;
    private double comboFloatX = 0;
    private double comboFloatY = 0;
    private double comboFloatAlpha = 0;
    private double comboFloatPhase = 0;
    private double comboShakeAmount = 0;
    private Color comboColor = Color.WHITE;

    private int dropStartRow = -1;
    private double shakeIntensity = 0;
    private double shakeDuration = 0;
    private double shakeInitDuration = 0.18;
    private double rotationPulse = 0; // 1.0 = full brightness pulse, 0 = normal
    private double flashIntensity = 0; // 0 = none, >0 = bright background flash
    private double startupGlitchElapsed = 0;
    private static final double STARTUP_GLITCH_DUR = 0.75;
    private double blackoutDropTimer = 0;
    private static final double BLACKOUT_AUTO_DROP = 1.5;
    private Canvas startupCanvas;
    private GraphicsContext startupGc;

    // --- HARD MODE BLACKOUT ---
    private double blackoutTimer = 0;
    private double blackoutDuration = 0;
    private double blackoutFlickerTimer = 0;

    private enum BlackoutState {
        NORMAL,
        FLICKER,      // warning flicker before blackout
        PRE_BLACKOUT, // calm gap before blackout
        BLACKOUT,     // screen goes dark
        POST_FLICKER  // flicker as light comes back on
    }

    private BlackoutState blackoutState = BlackoutState.NORMAL;

    private double flavourWaveTime    = 0;
    private double ghostParticleTimer    = 0;
    private double randomBlockSparkTimer = 0;

    private double flavourTypeElapsed = 0;
    private double flavourCooldown = 0;
    private int placesSinceFlavour = 0;
    private boolean lightsOutMode = false;
    private boolean darknessLoomsMode = false;
    private final java.util.List<javafx.scene.control.Label> flavourChars = new java.util.ArrayList<>();
    private javafx.scene.layout.HBox flavourHBox;
    private javafx.scene.layout.StackPane flavourPane;

    private static final double FLAVOUR_BASE_CD = 7.0;
    private static final double FLAVOUR_CHAR_DELAY = 0.055;
    private static final String[] FLAVOUR_PLACE = {
            "Bold choice. truly.",
            "That's one way to do it.",
            "You sure about that one.",
            "The audacity."
    };
    private static final String[] FLAVOUR_CLEAR = {
            "Fine. you get one.",
            "Don't let it go to your head.",
            "Was that on purpose? be honest."
    };
    private static final String[] FLAVOUR_TETRIS = {
            "Okay. i see you.",
            "Don't make it weird by celebrating.",
            "Four lines. i'll allow it."
    };
    private static final String[] FLAVOUR_STACK = {
            "You did this to yourself.",
            "This is a you problem.",
            "I'd look away but i can't.",
            "Are you okay? genuinely asking."
    };
    private static final String[] FLAVOUR_COMBO = {
            "Oh so you can do this.",
            "Where was this ten moves ago.",
            "Keep going. i dare you.",
            "Now you're showing off."
    };
    private static final String[] FLAVOUR_POST_BLACKOUT = {
            "The lights return. assess the damage.",
            "Welcome back. the stack didn't wait.",
            "Darkness lifted. problems haven't.",
            "You survived the blackout. barely counts.",
            "The board kept going without you.",
            "Light's back. make it count this time."
    };

    private java.util.List<TetrominoType> bag = new java.util.ArrayList<>();

    private int mouseTargetColumn = -1;

    private final java.util.Random rng = new java.util.Random();

    private static final long RANDOM_BLOCK_INTERVAL_MS = 2000L;
    private static final double RANDOM_BLOCK_CHANCE = 0.30;
    private boolean randomBlockEnabled = true;

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext = GameContext.getInstance();
        gameContext.reset();

        gameGc = gameCanvas.getGraphicsContext2D();
        nextGc1 = nextBlockCanvas1 != null ? nextBlockCanvas1.getGraphicsContext2D() : null;
        nextGc2 = nextBlockCanvas2 != null ? nextBlockCanvas2.getGraphicsContext2D() : null;
        nextGc3 = nextBlockCanvas3 != null ? nextBlockCanvas3.getGraphicsContext2D() : null;

        vfxGc = vfxCanvas.getGraphicsContext2D();
        holdGc = holdBlockCanvas.getGraphicsContext2D();
        // Sync vfxCanvas.layoutY to gameCanvas's actual post-layout position so
        // their y=0 coordinates map to the same screen row.
        Platform.runLater(() ->
                vfxCanvas.setLayoutY(gameCanvas.getBoundsInParent().getMinY()));
        bombSprite   = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/bomb.png"));
        ghostSprite  = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/GhostBlock.png"));

        lightOnImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/lightson.png"));
        lightOffImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/lightsoff.png"));
        hardMainImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/hardmain.png"));
        darkMainImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/darkmain.png"));

        // HARD MODE
        if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
            // Increase speed
            fallIntervalNs = 300_000_000L;
            lightBulbView.setImage(lightOnImage);
            lightBulbView.setVisible(true);
            Platform.runLater(this::buildFlavourChars);
        }

        // Spawn hai mảnh đầu tiên
        holdType = null;
        canHold = true;

        nextPiece = randomPiece();
        // Seed queue for current + 3 previews.
        nextQueue.clear();
        for (int i = 0; i < 4; i++) {
            nextQueue.addLast(randomPiece());
        }
        spawnPiece();

        // Cập nhật labels ban đầu
        refreshLabels();

        // Key handler
        gamePane.setOnKeyPressed(this::handleKeyPressed);
        gamePane.setOnKeyReleased(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.S) softDropping = false;
        });
        gameCanvas.setOnMouseMoved(this::handleMouseMoved);
        gameCanvas.setOnMouseClicked(this::handleMouseClicked);
        Platform.runLater(() -> {
            gamePane.requestFocus();
            startupCanvas = new Canvas(gamePane.getWidth(), gamePane.getHeight());
            startupCanvas.setManaged(false);
            startupCanvas.setMouseTransparent(true);
            startupGc = startupCanvas.getGraphicsContext2D();
            gamePane.getChildren().add(startupCanvas);
            startupGlitchElapsed = 0; // restart so flashes begin once the canvas is ready
            gamePane.layoutBoundsProperty().addListener((obs, o, n) -> {
                if (startupGlitchElapsed < STARTUP_GLITCH_DUR) {
                    startupCanvas.setWidth(n.getWidth());
                    startupCanvas.setHeight(n.getHeight());
                }
            });
        });

        startGameLoop();
    }

    private void startGameLoop() {
        lastFallTime = System.nanoTime();
        lastFrameTime = System.nanoTime();
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = (now - lastFrameTime) / 1_000_000_000.0;
                lastFrameTime = now;

                if (freezeUntil > 0 && now >= freezeUntil) {
                    lastFallTime = now;
                    freezeUntil = 0;
                    frozenRowFlash = null;
                    if (pendingTetrisClear != null) {
                        int[] rows = pendingTetrisClear;
                        pendingTetrisClear = null;
                        int sum = 0;
                        for (int r : rows) sum += r;
                        int avgRow = sum / rows.length;
                        for (int row : rows) {
                            for (int r = row; r > 0; r--)
                                board[r] = board[r - 1].clone();
                            board[0] = new int[Constants.BOARD_WIDTH];
                        }
                        addScore(4);
                        addLines(4);
                        updateLevel();
                        emitScorePopup(4, avgRow);
                        spawnAndCheckGameOver();
                    }
                }

                if (glitchTearEffect != null)
                    glitchTearEffect.update(dt);
                if (gameOverFlashAlpha > 0)
                    gameOverFlashAlpha = Math.max(0, gameOverFlashAlpha - dt * 4.0);
                if (startupGlitchElapsed < STARTUP_GLITCH_DUR) startupGlitchElapsed += dt;
                if (glitchExplosionEffect != null) {
                    glitchExplosionEffect.update(dt);
                    if (glitchExplosionEffect.isDone()) glitchExplosionEffect = null;
                }

                if (isGameOver && gameOverFreezeUntil > 0 && now >= gameOverFreezeUntil) {
                    gameOverFreezeUntil = 0;
                    boolean hardMode = gameContext.getGameMode() == GameContext.GameMode.HARD_MODE;
                    Color tearBg = Color.web(hardMode ? "#280C00" : "#0f0d1a");
                    glitchTearEffect = new GlitchTearEffect(board, Constants.BLOCK_SIZE, tearBg, () -> {
                        gameLoop.stop();
                        sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
                    });
                }

                // --- LOGIC GAME CHÍNH ---
                if (!gamePaused && !isGameOver && freezeUntil == 0) {
                    applyMouseTarget(now);

                    if (blackoutState == BlackoutState.BLACKOUT) {
                        blackoutDropTimer += dt;
                        if (blackoutDropTimer >= BLACKOUT_AUTO_DROP) {
                            blackoutDropTimer = 0;
                            hardDrop();
                        }
                    } else {
                        long effectiveInterval = softDropping ? 50_000_000L : fallIntervalNs;
                        if (now - lastFallTime >= effectiveInterval) {
                            updateFall();
                            lastFallTime = now;
                        }
                    }

                    particleSystem.update(dt);
                    scorePopups.removeIf(p -> p.life <= 0);
                    for (ScorePopup p : scorePopups) {
                        p.x += p.vx * dt;
                        p.y += p.vy * dt;
                        p.life -= dt / 0.7;
                    }
                    updateScreenShake(dt);

                    // Fuse sound + sparks while bomb is falling
                    if (currentPiece.getType() == TetrominoType.BOMB) {
                        if (!fuseLoopPlaying) {
                            SoundManager.getInstance().playLooping(SoundType.FUSE);
                            fuseLoopPlaying = true;
                        }
                    } else if (fuseLoopPlaying) {
                        SoundManager.getInstance().stopLooping();
                        fuseLoopPlaying = false;
                    }
                    if (currentPiece.getType() == TetrominoType.BOMB) {
                        int cs = Constants.BLOCK_SIZE;
                        double bx = (currentPiece.getX() + 0.5) * cs;
                        double by = (currentPiece.getY() + 0.5) * cs;
                        particleSystem.emitFuseSparks(bx, by, cs, currentPiece.getRotation());
                    }
                    if (currentPiece.getType() == TetrominoType.TRANSPARENT) {
                        int cs = Constants.BLOCK_SIZE;
                        int[][] gShape = currentPiece.getType().getShape(currentPiece.getRotation());
                        ghostParticleTimer -= dt;
                        if (ghostParticleTimer <= 0) {
                            ghostParticleTimer = 0.06;
                            for (int row = 0; row < 4; row++)
                                for (int col = 0; col < 4; col++)
                                    if (gShape[row][col] == 1)
                                        particleSystem.emitGhostDrift(
                                            (currentPiece.getX() + col + 0.5) * cs,
                                            (currentPiece.getY() + row + 0.5) * cs,
                                            cs);
                        }
                    }

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
                                            (currentPiece.getY() + row + 0.5) * cs,
                                            cs);
                        }
                    }

                    // Cập nhật các hiệu ứng VFX mới
                    if (rotationPulse > 0)
                        rotationPulse = Math.max(0, rotationPulse - dt * 8.0);
                    if (flashIntensity > 0)
                        flashIntensity = Math.max(0, flashIntensity - dt * 6.0);

                    if (levelUpEffect != null) {
                        levelUpEffect.update(dt);
                        if (levelUpEffect.isDone())
                            levelUpEffect = null;
                    }
                    if (borderPulseEffect != null) {
                        borderPulseEffect.update(dt);
                        if (borderPulseEffect.isDone())
                            borderPulseEffect = null;
                    } else {
                        // Check cảnh báo nguy hiểm (stack cao)
                        int dangerRow = (int) (Constants.BOARD_HEIGHT * 0.35);
                        outer:
                        for (int y = 0; y <= dangerRow; y++)
                            for (int x = 0; x < Constants.BOARD_WIDTH; x++)
                                if (board[y][x] != 0) {
                                    borderPulseEffect = new BorderPulseEffect();
                                    break outer;
                                }
                    }

                    if (comboFloatAlpha > 0) {
                        comboFloatY -= dt * 60;
                        comboFloatPhase += dt * 5.0;
                        comboShakeAmount = Math.max(0, comboShakeAmount - dt * 100);
                        comboFloatAlpha = Math.max(0, comboFloatAlpha - dt * 1.1);
                    }
                }

                if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE && !isGameOver && !gamePaused) {
                    updateBlackout(dt);
                    flavourWaveTime += dt;
                    flavourTypeElapsed += dt;
                    flavourCooldown -= dt;
                }

                render();
            }
        };
        gameLoop.start();
    }

    private void updateFall() {
        if (canMove(0, 1, currentPiece.getRotation())) {
            currentPiece.setY(currentPiece.getY() + 1);
        } else {
            lockAndSpawn();
        }
    }

    private void lockAndSpawn() {
        stopRandomBlockIfNeeded(currentPiece);
        // --- GIỮ LOGIC BOMB TỪ HEAD ---
        if (currentPiece.getType() == TetrominoType.BOMB) {
            detonateBomb(currentPiece.getX(), currentPiece.getY());
        } else {
            if (currentPiece.getType() == TetrominoType.TRANSPARENT) {
                int cs = Constants.BLOCK_SIZE;
                int[][] gShape = currentPiece.getType().getShape(currentPiece.getRotation());
                for (int row = 0; row < 4; row++) {
                    for (int col = 0; col < 4; col++) {
                        if (gShape[row][col] != 1) continue;
                        int bx = currentPiece.getX() + col;
                        int lockedY = currentPiece.getY() + row;
                        if (bx < 0 || bx >= Constants.BOARD_WIDTH) continue;
                        for (int scanY = 0; scanY < lockedY; scanY++) {
                            if (scanY < Constants.BOARD_HEIGHT && board[scanY][bx] != 0)
                                particleSystem.emitGhostTrail(
                                    (bx + 0.5) * cs, (scanY + 0.5) * cs, cs);
                        }
                    }
                }
            }
            lockPiece();
        }

        int cs = Constants.BLOCK_SIZE;
        java.util.List<Integer> fullRows = new java.util.ArrayList<>();
        for (int y = Constants.BOARD_HEIGHT - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                if (board[y][x] == 0) {
                    full = false;
                    break;
                }
            }
            if (full)
                fullRows.add(y);
        }

        if (!fullRows.isEmpty()) {
            int cleared = fullRows.size();
            for (int row : fullRows) {
                for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                    TetrominoType t = idToType(board[row][x]);
                    if (t != null)
                        particleSystem.emitRowBurst(x * cs, row * cs, t.getColor(), 8);
                }
            }

            double leftBase = VFX_MARGIN;
            double rightBase = VFX_MARGIN + Constants.BOARD_WIDTH * cs;
            Color pieceCol = currentPiece.getType().getColor();
            for (int row : fullRows) {
                particleSystem.emitRowBeams(leftBase, rightBase, row * cs, cs, pieceCol);
            }

            // --- LOGIC COMBO & SHAKE MỚI ---
            double t = cleared / 4.0;
            flashIntensity = 0.12 + t * 0.28;
            shakeIntensity = 5.0 + t * 13.0 + (cleared == 4 ? 12.0 : 0);
            shakeInitDuration = 0.18 + t * 0.17 + (cleared == 4 ? 0.10 : 0);
            shakeDuration = shakeInitDuration;

            comboCount++;
            if (comboLabel != null) comboLabel.setText("x" + comboCount);
            if (comboCount >= 2) {
                int avgRow = fullRows.stream().mapToInt(Integer::intValue).sum() / fullRows.size();
                comboDisplay = comboCount;
                comboFloatX = gameCanvas.getWidth() / 2.0 - 50;
                comboFloatY = avgRow * cs;
                comboFloatAlpha = 1.0;
                comboFloatPhase = 0;
                comboShakeAmount = 10 + comboDisplay * 5;
                comboColor = TetrominoType.values()[rng.nextInt(7)].getColor();
            }

            if (cleared == 4) {
                fullRows.sort(java.util.Collections.reverseOrder());
                frozenRowFlash = fullRows.stream().mapToInt(Integer::intValue).toArray();
                pendingTetrisClear = frozenRowFlash;

                freezeUntil = System.nanoTime() + 300_000_000L;
                SoundManager.getInstance().playSE(SoundType.TETRIS);
                PauseTransition delay = new PauseTransition(Duration.millis(300));
                delay.setOnFinished(e -> SoundManager.getInstance().playSE(SoundType.TETRIS2));
                delay.play();
                trySetFlavour(FLAVOUR_TETRIS, FLAVOUR_BASE_CD); // always fires

                return;
            }

            int avgRow = fullRows.stream().mapToInt(Integer::intValue).sum() / fullRows.size();
            fullRows.sort(java.util.Collections.reverseOrder());
            for (int row : fullRows) {
                for (int r = row; r > 0; r--)
                    board[r] = board[r - 1].clone();
                board[0] = new int[Constants.BOARD_WIDTH];
            }

            addScore(cleared);
            addLines(cleared);
            updateLevel();
            emitScorePopup(cleared, avgRow);
            // Combo takes priority over plain clear message
            boolean flavSet = comboCount >= 2 && trySetFlavour(FLAVOUR_COMBO, 2.0);
            if (!flavSet) trySetFlavour(FLAVOUR_CLEAR, 0.0);
        } else {
            comboCount = 0;
            if (comboLabel != null) comboLabel.setText("x0");
            // Trigger place/stack flavour every ~7 locks
            placesSinceFlavour++;
            if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE && placesSinceFlavour >= 7) {
                placesSinceFlavour = 0;
                boolean isHigh = getStackTopRow() < 7;
                trySetFlavour(isHigh ? FLAVOUR_STACK : FLAVOUR_PLACE, 0.0);
            }
        }

        canHold = true;
        spawnAndCheckGameOver();
    }

    private void spawnAndCheckGameOver() {
        spawnPiece();
        if (!canMove(0, 0, currentPiece.getRotation())) {
            isGameOver = true;
            stopRandomBlockIfNeeded(currentPiece);
            SoundManager.getInstance().playSE(SoundType.GAME_OVER);
            SoundManager.getInstance().stopMusic();
            gameLoop.stop();
            sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
            gameOverFreezeUntil = System.nanoTime() + 500_000_000L;
            gameOverFlashAlpha = 0.6;
        }
    }

    private void spawnPiece() {
        currentPiece = nextQueue.removeFirst();
        // No bombs during blackout — push it to the back and take the next non-bomb
        if (blackoutState == BlackoutState.BLACKOUT && currentPiece.getType() == TetrominoType.BOMB) {
            nextQueue.addLast(currentPiece);
            int qSize = nextQueue.size();
            for (int i = 0; i < qSize; i++) {
                Piece candidate = nextQueue.removeFirst();
                if (candidate.getType() != TetrominoType.BOMB) {
                    currentPiece = candidate;
                    break;
                }
                nextQueue.addLast(candidate);
            }
        }
        currentPiece.setX(Constants.BOARD_WIDTH / 2 - 2);
        currentPiece.setY(0);
        nextQueue.addLast(randomPiece());

        startRandomBlockIfNeeded(currentPiece);

        // --- RESET BLACKOUT ---
        if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
            if (blackoutState != BlackoutState.NORMAL) {
                blackoutState = BlackoutState.NORMAL;

                // reset
                blackoutDuration = 0;
                blackoutFlickerTimer = 0;
            }
        }
    }

    private Piece createSpawnedPiece(TetrominoType type) {
        Piece piece = new Piece(type, Constants.BOARD_WIDTH / 2 - 2, 0);
        return piece;
    }

    private Piece randomPiece() {
        // Refill bag
        if (bag.isEmpty()) {
            bag.addAll(java.util.Arrays.asList(TetrominoType.values()));
            java.util.Collections.shuffle(bag);
        }

        // Pop the first piece
        TetrominoType type = bag.remove(0);
        if (randomBlockEnabled && isRandomBlockCandidate(type) && rng.nextDouble() < RANDOM_BLOCK_CHANCE) {
            return createRandomBlock(type);
        }
        return new Piece(type, 0, 0);
    }

    private boolean isRandomBlockCandidate(TetrominoType type) {
        return type != TetrominoType.BOMB && type != TetrominoType.TRANSPARENT;
    }

    private RandomBlock createRandomBlock(TetrominoType initialType) {
        RandomBlock block = new RandomBlock(initialType, 0, 0, RANDOM_BLOCK_INTERVAL_MS);
        block.setTypeValidator(this::canPlaceType);
        return block;
    }

    private void startRandomBlockIfNeeded(Piece piece) {
        if (piece instanceof RandomBlock randomBlock) {
            randomBlock.startTimer();
        }
    }

    private void stopRandomBlockIfNeeded(Piece piece) {
        if (piece instanceof RandomBlock randomBlock) {
            randomBlock.lockBlock();
        }
    }

    private void detonateBomb(int centerX, int centerY) {
        int cs = Constants.BLOCK_SIZE;
        double pixelCx = (centerX + 0.5) * cs;
        double pixelCy = (centerY + 0.5) * cs;

        // Snapshot destroyed cells before clearing (for glitch effect ghost rendering)
        java.util.List<com.se330.tetris.game.GlitchExplosionEffect.CellSnap> snaps = new java.util.ArrayList<>();
        for (int y = centerY - 1; y <= centerY + 1; y++) {
            if (y < 0 || y >= Constants.BOARD_HEIGHT) continue;
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                if (x < 0 || x >= Constants.BOARD_WIDTH) continue;
                if (board[y][x] != 0) {
                    TetrominoType t = idToType(board[y][x]);
                    if (t != null && t != TetrominoType.BOMB)
                        snaps.add(new com.se330.tetris.game.GlitchExplosionEffect.CellSnap(
                                x * cs, y * cs, cs, t.getColor()));
                }
                board[y][x] = 0;
            }
        }

        SoundManager.getInstance().stopLooping();
        fuseLoopPlaying = false;
        glitchExplosionEffect = new com.se330.tetris.game.GlitchExplosionEffect(pixelCx, pixelCy, cs, snaps);
        flashIntensity = 0.45;
        shakeIntensity = 18;
        shakeInitDuration = 0.30;
        shakeDuration = 0.30;
        SoundManager.getInstance().playSE(SoundType.BOMB_EXPLODE);
    }


    private boolean canMove(int dx, int dy, int rotation) {
        int[][] shape = currentPiece.getType().getShape(rotation);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int nx = currentPiece.getX() + col + dx;
                    int ny = currentPiece.getY() + row + dy;
                    if (nx < 0 || nx >= Constants.BOARD_WIDTH)
                        return false;
                    if (ny < 0 || ny >= Constants.BOARD_HEIGHT)
                        return false;
                    if (currentPiece.getType() != TetrominoType.TRANSPARENT) {
                        if (board[ny][nx] != 0)
                            return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean canPlaceType(Piece piece, TetrominoType type) {
        int[][] shape = type.getShape(piece.getRotation());
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int nx = piece.getX() + col;
                    int ny = piece.getY() + row;
                    if (nx < 0 || nx >= Constants.BOARD_WIDTH)
                        return false;
                    if (ny < 0 || ny >= Constants.BOARD_HEIGHT)
                        return false;
                    if (board[ny][nx] != 0)
                        return false;
                }
            }
        }
        return true;
    }

    private void lockPiece() {
        SoundManager.getInstance().playSE(SoundType.BLOCK_DROP);

        int[][] shape = currentPiece.getType().getShape(currentPiece.getRotation());
        int cs = Constants.BLOCK_SIZE;

        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int x = currentPiece.getX() + col;
                    int y = currentPiece.getY() + row;
                    if (y >= 0 && y < Constants.BOARD_HEIGHT
                            && x >= 0 && x < Constants.BOARD_WIDTH) {
                        board[y][x] = typeId(currentPiece.getType());
                        if (dropStartRow >= 0) {
                            particleSystem.emitLockParticles(
                                    x * cs, y * cs, currentPiece.getType().getColor(), 18);
                        }
                        if (x < minCol) minCol = x;
                        if (x > maxCol) maxCol = x;
                        if (y < minRow) minRow = y;
                        if (y > maxRow) maxRow = y;
                    }
                }
            }
        }


        // Single wide light column on hard drop only
        if (dropStartRow >= 0 && minCol <= maxCol) {
            double leftPixelX = minCol * cs;
            double spanWidth = (maxCol - minCol + 1) * cs;
            double fromPixelY = dropStartRow * cs;
            double toPixelY = (maxRow + 1) * cs;
            particleSystem.emitLightColumn(
                    leftPixelX, fromPixelY, toPixelY, spanWidth,
                    currentPiece.getType().getColor());
            dropStartRow = -1;
        }
    }

    private int clearLines() {
        int cleared = 0;
        for (int y = Constants.BOARD_HEIGHT - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                if (board[y][x] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                cleared++;
                for (int row = y; row > 0; row--) {
                    board[row] = board[row - 1].clone();
                }
                board[0] = new int[Constants.BOARD_WIDTH];
                y++; // kiểm tra lại dòng vừa kéo xuống
            }
        }
        return cleared;
    }

    private void addScore(int linesCleared) {
        int bonus = switch (linesCleared) {
            case 1 -> 100;
            case 2 -> 300;
            case 3 -> 500;
            case 4 -> 800;
            default -> 0;
        };
        int newScore = gameContext.getScore() + bonus;
        gameContext.setScore(newScore);
        scoreLabel.setText(String.valueOf(newScore));

        SoundManager.getInstance().playSE(SoundType.SCORE);
    }

    private void addLines(int count) {
        int newLines = gameContext.getLines() + count;
        gameContext.setLines(newLines);
        linesLabel.setText(String.valueOf(newLines));
    }

    private void updateLevel() {
        // Mỗi 10 dòng tăng 1 level, tối đa level 10
        int newLevel = Math.min(gameContext.getLines() / 10 + 1, 10);
        if (newLevel != gameContext.getLevel()) {
            gameContext.setLevel(newLevel);
            levelLabel.setText(String.valueOf(newLevel));
            // Tăng tốc độ rơi
            long base = (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE)
                    ? 300_000_000L
                    : 500_000_000L;

            fallIntervalNs = Math.max(100_000_000L, base - (newLevel - 1) * 40_000_000L);
            levelUpEffect = new LevelUpEffect(newLevel, () -> {
                shakeIntensity = 18;
                shakeInitDuration = 0.25;
                shakeDuration = 0.25;
            });
        }
    }

    private void refreshLabels() {
        scoreLabel.setText(String.valueOf(gameContext.getScore()));
        levelLabel.setText(String.valueOf(gameContext.getLevel()));
        linesLabel.setText(String.valueOf(gameContext.getLines()));
    }

    private void emitRotationArc() {
        particleSystem.emitCornerSparks(
                currentPiece.getX(),
                currentPiece.getY(),
                currentPiece.getType().getShape(currentPiece.getRotation()),
                Constants.BLOCK_SIZE,
                currentPiece.getType().getColor());
        rotationPulse = 1.0;
    }

    private void updateScreenShake(double dt) {
        if (shakeDuration <= 0)
            return;
        shakeDuration -= dt;
        if (shakeDuration <= 0) {
            gameCanvas.setTranslateX(0);
            gameCanvas.setTranslateY(0);
            vfxCanvas.setTranslateX(0);
            vfxCanvas.setTranslateY(0);
        } else {
            double factor = shakeDuration / shakeInitDuration; // eases out as duration drops
            double sx = (rng.nextDouble() - 0.5) * 2 * shakeIntensity * factor;
            double sy = (rng.nextDouble() - 0.5) * 2 * shakeIntensity * factor;
            gameCanvas.setTranslateX(sx);
            gameCanvas.setTranslateY(sy);
            vfxCanvas.setTranslateX(sx);
            vfxCanvas.setTranslateY(sy);
        }
    }

    private void hardDrop() {
        dropStartRow = currentPiece.getY();
        shakeIntensity = 4.0;
        shakeInitDuration = 0.18;
        shakeDuration = 0.18;
        int drop = getDropDistance();
        currentPiece.setY(currentPiece.getY() + drop);
        lockAndSpawn();
    }

    private void holdCurrentPiece() {
        if (!canHold || currentPiece == null || isGameOver || currentPiece.getType() == TetrominoType.BOMB) {
            return;
        }

        canHold = false;
        stopRandomBlockIfNeeded(currentPiece);
        TetrominoType currentType = currentPiece.getType();

        if (holdType == null) {
            holdType = currentType;
            spawnPiece();
        } else {
            TetrominoType swapped = holdType;
            holdType = currentType;
            currentPiece = createSpawnedPiece(swapped);
        }

        if (!canMove(0, 0, currentPiece.getRotation())) {
            isGameOver = true;
            gameLoop.stop();
            sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
        }
    }

    private int getDropDistance() {
        int distance = 0;
        while (canMove(0, distance + 1, currentPiece.getRotation()))
            distance++;
        return distance;
    }

    private void useBombSkill() {
        if (isGameOver || gamePaused || bombsRemaining <= 0 || currentPiece == null) {
            return;
        }
        if (currentPiece.getType() == TetrominoType.BOMB) {
            return;
        }

        bombsRemaining--;
        stopRandomBlockIfNeeded(currentPiece);
        currentPiece = new Piece(TetrominoType.BOMB, currentPiece.getX(), currentPiece.getY());
        updateBombInventoryUI();
        flashBombInventory();
    }

    private void updateBombInventoryUI() {
        if (bombsLabel != null) {
            bombsLabel.setText("\uD83D\uDCA3 x" + bombsRemaining);
        }
    }

    private void flashBombInventory() {
        if (bombInventoryBox == null) {
            return;
        }

        bombInventoryBox.setStyle(
                bombInventoryBaseStyle
                        + "; -fx-border-color: #ffcc00; -fx-border-width: 2;"
                        + " -fx-effect: dropshadow(gaussian, #ffcc00, 10, 0.6, 0, 0);");

        PauseTransition reset = new PauseTransition(Duration.millis(160));
        reset.setOnFinished(e -> bombInventoryBox.setStyle(bombInventoryBaseStyle));
        reset.play();
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (isGameOver) return;
        KeyCode code = event.getCode();
        switch (code) {
            case LEFT, A -> {
                if (canMove(-1, 0, currentPiece.getRotation()))
                    currentPiece.setX(currentPiece.getX() - 1);
            }
            case RIGHT, D -> {
                if (canMove(1, 0, currentPiece.getRotation()))
                    currentPiece.setX(currentPiece.getX() + 1);
            }
            case DOWN -> {
                if (blackoutState != BlackoutState.BLACKOUT) hardDrop();
            }
            case S -> {
                if (blackoutState != BlackoutState.BLACKOUT) softDropping = true;
            }
            case UP, W -> {
                int nr = (currentPiece.getRotation() + 1) % 4;
                if (canMove(0, 0, nr)) {
                    currentPiece.setRotation(nr);
                    emitRotationArc();
                }
            }
            case SPACE -> {
                if (blackoutState != BlackoutState.BLACKOUT) hardDrop();
            }
            case P -> handlePause();
            case B -> useBombSkill();
            case C, SHIFT -> holdCurrentPiece();
            case ESCAPE -> handleExit();
            default -> {
                return;
            }
        }
        event.consume();
    }

    private void handleMouseMoved(MouseEvent event) {
        if (gamePaused || isGameOver)
            return;

        // Only update target; movement is applied in the game loop.
        mouseTargetColumn = (int) (event.getX() / Constants.BLOCK_SIZE);
        event.consume();
    }

    private void applyMouseTarget(long now) {
        if (mouseTargetColumn < 0)
            return;

        int targetX = getTargetPieceX(mouseTargetColumn);
        int currentX = currentPiece.getX();
        if (targetX == currentX)
            return;

        // Limit horizontal speed to 1 column per frame to avoid jitter.
        int step = Integer.compare(targetX, currentX);
        if (canMove(step, 0, currentPiece.getRotation())) {
            currentPiece.setX(currentX + step);
        }
    }

    private void handleMouseClicked(MouseEvent event) {
        if (gamePaused || isGameOver)
            return;

        // Keep keyboard control active after interacting with the canvas.
        gamePane.requestFocus();

        if (event.getButton() == MouseButton.PRIMARY) {
            if (blackoutState != BlackoutState.BLACKOUT) hardDrop();
            event.consume();
            return;
        }

        if (event.getButton() == MouseButton.SECONDARY) {
            tryRotateWithWallKick();
            event.consume();
        }
    }

    private boolean tryRotateWithWallKick() {
        int nextRotation = (currentPiece.getRotation() + 1) % 4;

        // Basic wall-kick offsets to allow rotation when touching side walls.
        int[] kickOffsets = {0, -1, 1, -2, 2};
        for (int dx : kickOffsets) {
            if (canMove(dx, 0, nextRotation)) {
                currentPiece.setX(currentPiece.getX() + dx);
                currentPiece.setRotation(nextRotation);
                return true;
            }
        }

        return false;
    }

    private int getTargetPieceX(int targetColumn) {
        int clampedTarget = Math.max(0, Math.min(Constants.BOARD_WIDTH - 1, targetColumn));
        int rotation = currentPiece.getRotation();
        int[][] shape = currentPiece.getType().getShape(rotation);

        int minCol = 4;
        int maxCol = -1;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);
                }
            }
        }

        if (maxCol < 0)
            return currentPiece.getX();

        int centerCol = (minCol + maxCol) / 2;
        int desiredX = clampedTarget - centerCol;

        int minX = -minCol;
        int maxX = Constants.BOARD_WIDTH - 1 - maxCol;
        return Math.max(minX, Math.min(maxX, desiredX));
    }

    private void handlePause() {
        gamePaused = !gamePaused;
    }

    private void handleExit() {
        System.out.println("Exiting to main menu");
        stopRandomBlockIfNeeded(currentPiece);

        SoundManager.getInstance().stopLooping();

        if (gameLoop != null) {
            gameLoop.stop();
        }
        gameContext.reset();
        sceneManager.clearSceneCache();
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }

    // ── Score popups ──────────────────────────────────────────────────────────

    private static class ScorePopup {
        String text;
        double x, y, vx, vy, life;
        Color color;
        int fontSize;
    }

    private final java.util.List<ScorePopup> scorePopups = new java.util.ArrayList<>();

    private void renderTopLayer() {
        GraphicsContext gc = gameGc;
        if (comboFloatAlpha > 0) {
            double cx = comboFloatX + (rng.nextDouble() - 0.5) * 2 * comboShakeAmount;
            double cy = comboFloatY + (rng.nextDouble() - 0.5) * 2 * comboShakeAmount * 0.4;
            double flash = (Math.sin(comboFloatPhase * 3.0) + 1.0) / 2.0;
            Color drawColor = comboColor.interpolate(Color.WHITE, flash);
            int fontSize = (int) Math.min(75, 32 + comboDisplay * 7);
            gc.setGlobalAlpha(comboFloatAlpha);
            gc.setFont(Font.font("VT323", FontWeight.BOLD, fontSize));
            gc.setFill(drawColor);
            gc.fillText("COMBO x" + comboDisplay, cx, cy);
            gc.setGlobalAlpha(1.0);
        }
        renderScorePopups();
    }

    private void renderScorePopups() {
        if (scorePopups.isEmpty()) return;
        GraphicsContext gc = gameGc;
        for (ScorePopup p : scorePopups) {
            gc.save();
            gc.setGlobalAlpha(Math.max(0, p.life));
            gc.setFont(Font.font("VT323", FontWeight.BOLD, p.fontSize));
            gc.setFill(p.color);
            gc.fillText(p.text, p.x, p.y);
            gc.restore();
        }
    }

    private void emitScorePopup(int cleared, int avgRow) {
        int bonus = switch (cleared) {
            case 1 -> 100;
            case 2 -> 300;
            case 3 -> 500;
            case 4 -> 800;
            default -> 0;
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
            case 4 -> Color.web("#00ffff");
            case 3 -> Color.web("#ffaa00");
            default -> Color.web("#ffffff");
        };
        scorePopups.add(p);
    }

    // Base background color components for gamePane (#0f0d1a)
    private static final double BG_R = 0x0f / 255.0;
    private static final double BG_G = 0x0d / 255.0;
    private static final double BG_B = 0x1a / 255.0;
    private static final String PANE_BASE_STYLE = "-fx-padding: 20; -fx-spacing: 20; -fx-alignment: center;";

    private void render() {
        // Swap hard-mode UI panels to dark assets during blackout
        if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
            boolean inBlackout = blackoutState == BlackoutState.BLACKOUT;
            var classes = gamePane.getStyleClass();
            if (inBlackout && !classes.contains("blackout")) {
                classes.add("blackout");
                if (mainFrameView != null) mainFrameView.setImage(darkMainImage);
            } else if (!inBlackout && classes.contains("blackout")) {
                classes.remove("blackout");
                if (mainFrameView != null) mainFrameView.setImage(hardMainImage);
            }
        }

        if (gameContext.getGameMode() != GameContext.GameMode.HARD_MODE) {
            if (flashIntensity > 0) {
                double r = BG_R + flashIntensity * (1.0 - BG_R);
                double g = BG_G + flashIntensity * (1.0 - BG_G);
                double b = BG_B + flashIntensity * (1.0 - BG_B);
                String hex = String.format("#%02x%02x%02x", (int) (r * 255), (int) (g * 255), (int) (b * 255));
                gamePane.setStyle("-fx-background-color: " + hex + "; " + PANE_BASE_STYLE);
            } else {
                gamePane.setStyle("-fx-background-color: #0f0d1a; " + PANE_BASE_STYLE);
            }
        }

        drawGameBoard();

        if (glitchExplosionEffect != null) glitchExplosionEffect.render(gameGc);

        // --- XỬ LÝ RENDER THEO THỨ TỰ LỚP (Layering) ---
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
        drawHoldBlock();
        drawNextBlocks();
        updateLightBulb();

        // Flavour text render
        if (lightsOutMode) {
            // Vibrate the whole line
            if (flavourHBox != null) {
                flavourHBox.setTranslateX((rng.nextDouble() - 0.5) * 8);
                flavourHBox.setTranslateY((rng.nextDouble() - 0.5) * 5);
            }
            // Per-char glitch: random opacity drops and color flashes
            for (javafx.scene.control.Label l : flavourChars) {
                if (rng.nextDouble() < 0.12) {
                    l.setOpacity(rng.nextDouble() < 0.4 ? 0.0 : 1.0);
                } else {
                    l.setOpacity(1.0);
                }
                String col = rng.nextDouble() < 0.06
                        ? (rng.nextBoolean() ? "#ffffff" : "#880000")
                        : "#FF0000";
                l.setStyle("-fx-font-family: 'VT323'; -fx-font-size: 40; -fx-text-fill: " + col + ";");
            }
        } else if (darknessLoomsMode) {
            if (flavourHBox != null) {
                flavourHBox.setTranslateX(0);
                flavourHBox.setTranslateY(0);
            }
            int nVisible = Math.min(flavourChars.size(), (int) (flavourTypeElapsed / (FLAVOUR_CHAR_DELAY * 1.8)));
            for (int i = 0; i < flavourChars.size(); i++) {
                javafx.scene.control.Label l = flavourChars.get(i);
                boolean visible = i < nVisible;
                // Subtle flicker on revealed chars
                if (visible && rng.nextDouble() < 0.04)
                    l.setOpacity(0.4 + rng.nextDouble() * 0.3);
                else
                    l.setOpacity(visible ? 1.0 : 0.0);
                if (visible)
                    l.setTranslateY(Math.sin(flavourWaveTime * 1.2 + i * 0.45) * 5.0);
            }
        } else {
            if (flavourHBox != null) {
                flavourHBox.setTranslateX(0);
                flavourHBox.setTranslateY(0);
            }
            int nVisible = Math.min(flavourChars.size(), (int) (flavourTypeElapsed / FLAVOUR_CHAR_DELAY));
            for (int i = 0; i < flavourChars.size(); i++) {
                javafx.scene.control.Label l = flavourChars.get(i);
                boolean visible = i < nVisible;
                l.setOpacity(visible ? 1.0 : 0.0);
                if (visible)
                    l.setTranslateY(Math.sin(flavourWaveTime * 2.5 + i * 0.45) * 2.5);
            }
        }

        renderStartupGlitch();
    }

    private void renderStartupGlitch() {
        if (startupGlitchElapsed >= STARTUP_GLITCH_DUR || startupGc == null) return;
        double t = startupGlitchElapsed / STARTUP_GLITCH_DUR;
        double alpha = (1.0 - t * t) * 0.55;
        double w = startupCanvas.getWidth();
        double h = startupCanvas.getHeight();

        startupGc.clearRect(0, 0, w, h);

        // clean scanline bands — each drifts sideways at its own phase
        int bands = 6 + rng.nextInt(5);
        for (int i = 0; i < bands; i++) {
            double by = rng.nextDouble() * h;
            double bh = rng.nextDouble() * (h * 0.05) + 1;
            int pick = rng.nextInt(3);
            double cr = pick == 0 ? 0 : 1;
            double cg = pick == 0 ? 1 : 1;
            double cb = pick == 0 ? 1 : 0;
            double baseA = pick == 0 ? 0.5 : pick == 1 ? 0.4 : 0.3;
            double xOffset = Math.sin(t * Math.PI * 5 + i * 1.9) * w * 0.18;
            double pad = Math.abs(xOffset);
            startupGc.setFill(Color.color(cr, cg, cb, Math.min(1, alpha * baseA)));
            startupGc.fillRect(xOffset - pad, by, w + pad * 2, bh);
            // a couple of bright accent pixels on the band
            for (int j = 0; j < 3; j++) {
                double hx = rng.nextDouble() * w + xOffset;
                startupGc.setFill(Color.color(1, 1, 1, alpha * (0.6 + rng.nextDouble() * 0.4)));
                startupGc.fillRect(hx, by, rng.nextDouble() * 6 + 2, bh);
            }
        }
        // light pixel scatter
        int pixels = (int) (250 * (1.0 - t));
        for (int i = 0; i < pixels; i++) {
            double px = rng.nextDouble() * w;
            double py = rng.nextDouble() * h;
            startupGc.setFill(Color.color(1, 1, 1, alpha * (rng.nextDouble() * 0.6 + 0.2)));
            startupGc.fillRect(px, py, 2, 2);
        }
        // white flashes at t=0.0, 0.22, 0.48 — sharp spike then fast decay
        double[] flashTimes = {0.0, 0.14, 0.28};
        double[] flashPeaks = {0.90, 0.42, 0.42};
        double flashHalf = 0.07;
        double totalFlash = 0;
        for (int fi = 0; fi < flashTimes.length; fi++) {
            double d = Math.abs(t - flashTimes[fi]);
            if (d < flashHalf)
                totalFlash = Math.max(totalFlash, (1.0 - d / flashHalf) * flashPeaks[fi]);
        }
        if (totalFlash > 0) {
            startupGc.setFill(Color.color(1, 1, 1, totalFlash));
            startupGc.fillRect(0, 0, w, h);
        }
    }

    public void drawGameBoard() {
        GraphicsContext gc = gameGc;
        double w = gameCanvas.getWidth();
        double h = gameCanvas.getHeight();
        int cs = Constants.BLOCK_SIZE;

        // Background
        boolean hardMode = gameContext.getGameMode() == GameContext.GameMode.HARD_MODE;
        gc.setFill(Color.web(hardMode ? "#280C00" : "#0f0d1a"));
        gc.fillRect(0, 0, w, h);

        // Grid lines
        gc.setStroke(Color.web(hardMode ? "#3d1500" : "#2b2740"));
        gc.setLineWidth(0.5);
        for (int x = 0; x <= Constants.BOARD_WIDTH; x++)
            gc.strokeLine(x * cs, 0, x * cs, h);
        for (int y = 0; y <= Constants.BOARD_HEIGHT; y++)
            gc.strokeLine(0, y * cs, w, y * cs);

        // Border — hidden in hard mode (wooden frame provides it)
        if (gameContext.getGameMode() != GameContext.GameMode.HARD_MODE) {
            gc.setStroke(Color.web("#00ff00"));
            gc.setLineWidth(2);
            gc.strokeRect(0, 0, w, h);
        }

        // HARD MODE
        boolean isDark = false;
        if (blackoutState == BlackoutState.FLICKER) {
            // chớp nhanh: 10 lần / giây
            isDark = ((int) (blackoutFlickerTimer * 10)) % 2 == 0;
        }
        if (blackoutState == BlackoutState.BLACKOUT) {
            isDark = true;
        }
        if (!isDark) {
            // Locked cells
            for (int y = 0; y < Constants.BOARD_HEIGHT; y++) {
                for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                    if (board[y][x] != 0) {
                        TetrominoType t = idToType(board[y][x]);
                        if (t != null)
                            drawCell(gc, x, y, t.getColor(), 1.0);
                    }
                }
            }
        }

        // Locked cells
        for (int y = 0; y < Constants.BOARD_HEIGHT; y++) {
            for (int x = 0; x < Constants.BOARD_WIDTH; x++) {
                if (board[y][x] != 0) {
                    TetrominoType t = idToType(board[y][x]);
                    if (t == null) continue;
                    if (t == TetrominoType.BOMB)
                        gc.drawImage(bombSprite, x * cs, y * cs, cs, cs);
                    else if (t == TetrominoType.TRANSPARENT)
                        gc.drawImage(ghostSprite, x * cs, y * cs, cs, cs);
                    else
                        drawCell(gc, x, y, t.getColor(), 1.0);
                }
            }
        }

        // Flash cleared rows white during Tetris freeze
        if (frozenRowFlash != null && freezeUntil > 0) {
            gc.setFill(Color.WHITE);
            for (int row : frozenRowFlash) {
                gc.fillRect(0, row * cs, w, cs);
            }
        }

        // White flash on board at moment of game over
        if (gameOverFlashAlpha > 0) {
            gc.setFill(Color.color(1, 1, 1, gameOverFlashAlpha));
            gc.fillRect(0, 0, w, h);
        }

        // Ghost + current piece hidden during game over (glitch effect takes over)
        if (isGameOver)
            return;

        // Ghost
        if (freezeUntil == 0) {
            int drop = getDropDistance();
            int[][] ghostShape = currentPiece.getType().getShape(currentPiece.getRotation());
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    if (ghostShape[row][col] == 1) {
                        drawCell(gc,
                                currentPiece.getX() + col,
                                currentPiece.getY() + row + drop,
                                currentPiece.getType().getColor().deriveColor(0, 1, 1, 0.25),
                                1.0);
                    }
                }
            }
        }

        // Current piece - apply rotation pulse brightness
        // Current piece - always drawn on top, visible even during blackout
        int[][] shape = currentPiece.getType().getShape(currentPiece.getRotation());
        Color pieceColor = currentPiece.getType().getColor()
                .deriveColor(0, 1, 1.0 + rotationPulse * 1.8, 1);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    drawCell(gc,
                            currentPiece.getX() + col,
                            currentPiece.getY() + row,
                            pieceColor,
                            1.0);
                }
            }
        }

        if (currentPiece instanceof RandomBlock randomBlock && randomBlock.isIndicatorOn()) {
            drawRandomBlockIndicator(randomBlock, gc, cs);
        }

        // Floating combo text
        if (comboFloatAlpha > 0) {
            double cx = comboFloatX + (rng.nextDouble() - 0.5) * 2 * comboShakeAmount;
            double cy = comboFloatY + (rng.nextDouble() - 0.5) * 2 * comboShakeAmount * 0.4;

            double flash = (Math.sin(comboFloatPhase * 3.0) + 1.0) / 2.0; // 0..1

            Color drawColor = comboColor.interpolate(Color.WHITE, flash);
            int fontSize = (int) Math.min(75, 32 + comboDisplay * 7);
            gc.setGlobalAlpha(comboFloatAlpha);
            gc.setFont(Font.font("VT323", FontWeight.BOLD, fontSize));
            gc.setFill(drawColor);
            gc.fillText("COMBO x" + comboDisplay, cx, cy);
            gc.setGlobalAlpha(1.0);
        }
        // Hard mode blackout overlays
        if (blackoutState == BlackoutState.BLACKOUT) {
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, w, h);
        } else if (blackoutState == BlackoutState.FLICKER || blackoutState == BlackoutState.POST_FLICKER) {
            if (((int) blackoutFlickerTimer) % 2 == 0) {
                gc.setFill(Color.BLACK);
                gc.fillRect(0, 0, w, h);
            }
        }

        boolean isBomb       = currentPiece.getType() == TetrominoType.BOMB;
        boolean isGhost      = currentPiece.getType() == TetrominoType.TRANSPARENT;
        boolean isRandomBlock = currentPiece instanceof RandomBlock;

        double chargeOffsetX = 0, chargeOffsetY = 0, charge = 0;
        if (blackoutState == BlackoutState.BLACKOUT) {
            charge = blackoutDropTimer / BLACKOUT_AUTO_DROP;
            double freq = 6.0 + charge * 10.0;
            chargeOffsetY = Math.sin(blackoutDropTimer * Math.PI * freq) * charge * 5.0;
            chargeOffsetX = (rng.nextDouble() - 0.5) * charge * 4.0;
            Color base = isBomb ? Color.color(1.0, 0.15, 0.15) : currentPiece.getType().getColor();
            double bright = charge * 0.75;
            pieceColor = Color.color(
                    Math.min(1.0, base.getRed() + bright),
                    Math.min(1.0, base.getGreen() + bright),
                    Math.min(1.0, base.getBlue() + bright)
            );
        } else {
            pieceColor = currentPiece.getType().getColor()
                    .deriveColor(0, 1, 1.0 + rotationPulse * 1.8, 1);
        }

        gc.save();
        gc.translate(chargeOffsetX, chargeOffsetY);

        // Expanding aura glow as charge builds
        if (blackoutState == BlackoutState.BLACKOUT && charge > 0) {
            double expansion = charge * 5.0;
            gc.setGlobalAlpha(charge * 0.35);
            gc.setFill(pieceColor);
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    if (shape[row][col] == 1) {
                        int px = currentPiece.getX() + col;
                        int py = currentPiece.getY() + row;
                        gc.fillRect(px * cs - expansion, py * cs - expansion,
                                cs + expansion * 2, cs + expansion * 2);
                    }
                }
            }
            gc.setGlobalAlpha(1.0);
        }

        // Chromatic aberration during blackout — red left, cyan right, grows with charge
        if (blackoutState == BlackoutState.BLACKOUT) {
            double caOffset = 3.0 + charge * 7.0;
            Color base = currentPiece.getType().getColor();
            gc.setGlobalAlpha(0.50);
            gc.setFill(Color.color(Math.min(1.0, base.getRed() + 0.4), 0, 0));
            for (int row = 0; row < 4; row++)
                for (int col = 0; col < 4; col++)
                    if (shape[row][col] == 1)
                        gc.fillRect((currentPiece.getX() + col) * cs - caOffset,
                                (currentPiece.getY() + row) * cs, cs, cs);
            gc.setFill(Color.color(0, Math.min(1.0, base.getGreen() * 0.2 + 0.6),
                    Math.min(1.0, base.getBlue() + 0.4)));
            for (int row = 0; row < 4; row++)
                for (int col = 0; col < 4; col++)
                    if (shape[row][col] == 1)
                        gc.fillRect((currentPiece.getX() + col) * cs + caOffset,
                                (currentPiece.getY() + row) * cs, cs, cs);
            gc.setGlobalAlpha(1.0);
        }

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    int px = currentPiece.getX() + col;
                    int py = currentPiece.getY() + row;
                    if (isRandomBlock) {
                        double t = flavourWaveTime;
                        double erratic = Math.sin(t * 23.0) * Math.sin(t * 11.7) * Math.sin(t * 5.3);
                        double shift = 2.0 + Math.abs(erratic) * 8.0;
                        double redOffY  = Math.sin(t * 17.0) * 2.5;
                        double blueOffY = Math.sin(t * 13.3 + 1.1) * 2.5;
                        gc.save();
                        gc.setGlobalAlpha(0.35 + Math.abs(erratic) * 0.3);
                        gc.setFill(Color.color(1, 0, 0));
                        gc.fillRect(px * cs - shift, py * cs + redOffY, cs, cs);
                        gc.setFill(Color.color(0, 0.4, 1));
                        gc.fillRect(px * cs + shift, py * cs + blueOffY, cs, cs);
                        gc.setGlobalAlpha(1.0);
                        gc.restore();
                        drawCell(gc, px, py, pieceColor, 1.0);
                    } else if (isBomb && blackoutState != BlackoutState.BLACKOUT) {
                        double bcx = px * cs + cs / 2.0;
                        double bcy = py * cs + cs / 2.0;
                        gc.save();
                        gc.translate(bcx, bcy);
                        gc.rotate(currentPiece.getRotation() * 90.0);
                        gc.drawImage(bombSprite, -cs / 2.0, -cs / 2.0, cs, cs);
                        gc.restore();
                    } else if (isGhost) {
                        double gx = px * cs, gy = py * cs;
                        double dx1 = Math.sin(flavourWaveTime * 35.0) * 5;
                        double dy1 = Math.cos(flavourWaveTime * 28.0) * 4;
                        double dx2 = -Math.sin(flavourWaveTime * 42.0 + 1.2) * 4;
                        double dy2 = -Math.cos(flavourWaveTime * 38.0) * 3;
                        // lavender displacement copy
                        gc.save();
                        gc.setGlobalAlpha(0.55);
                        gc.drawImage(ghostSprite, gx + dx1, gy + dy1, cs, cs);
                        gc.setFill(Color.web("#9966cc", 0.5));
                        gc.fillRect(gx + dx1, gy + dy1, cs, cs);
                        // white displacement copy
                        gc.setGlobalAlpha(0.45);
                        gc.drawImage(ghostSprite, gx + dx2, gy + dy2, cs, cs);
                        gc.setFill(Color.web("#ffffff", 0.45));
                        gc.fillRect(gx + dx2, gy + dy2, cs, cs);
                        // main sprite
                        gc.setGlobalAlpha(1.0);
                        gc.drawImage(ghostSprite, gx, gy, cs, cs);
                        gc.restore();
                    }
                    else
                        drawCell(gc, px, py, pieceColor, 1.0);
                }
            }
        }
        gc.restore();

        // RandomBlock morph hint — glitch flash of next shape
        if (isRandomBlock) {
            RandomBlock rb = (RandomBlock) currentPiece;
            TetrominoType preview = rb.getPreviewType();
            double progress = rb.getMorphProgress();
            if (preview != null && progress > 0.3) {
                // erratic flicker: product of multiple sines at coprime freqs
                double t = flavourWaveTime;
                double glitch = Math.sin(t * 19.0) * Math.sin(t * 7.3) * Math.sin(t * 31.0);
                double threshold = 0.35 - progress * 0.32;
                if (glitch > threshold) {
                    double flashAlpha = Math.min(1.0, (glitch - threshold) / (1.0 - threshold) * 2.0);
                    int[][] previewShape = preview.getShape(currentPiece.getRotation());
                    gc.save();
                    for (int row = 0; row < 4; row++) {
                        if (previewShape[row][0] + previewShape[row][1] + previewShape[row][2] + previewShape[row][3] == 0) continue;
                        double rowGlitch = (rng.nextDouble() - 0.5) * 10 * (1.0 - progress);
                        for (int col = 0; col < 4; col++) {
                            if (previewShape[row][col] != 1) continue;
                            double cx = (currentPiece.getX() + col) * cs + rowGlitch;
                            double cy = (currentPiece.getY() + row) * cs;
                            gc.setGlobalAlpha(Math.min(1.0, flashAlpha * (0.75 + rng.nextDouble() * 0.5)));
                            gc.setFill(Color.WHITE);
                            gc.fillRect(cx, cy, cs, cs);
                        }
                    }
                    gc.setGlobalAlpha(1.0);
                    gc.restore();
                }
            }
        }

        // Paused overlay
        if (gamePaused) {
            gc.setFill(Color.color(0, 0, 0, 0.5));
            gc.fillRect(0, 0, w, h);
            gc.setFill(Color.web("#00ff00"));
            gc.setFont(Font.font("Courier New", 28));
            gc.fillText("PAUSED", w / 2 - 55, h / 2);
        }
    }

    public void drawHoldBlock() {
        if (holdType != null) {
            drawPreview(holdGc, holdBlockCanvas, new Piece(holdType, 0, 0));
        } else {
            drawPreview(holdGc, holdBlockCanvas, null);
        }
    }

    public void drawNextBlocks() {
        java.util.Iterator<Piece> it = nextQueue.iterator();
        if (nextGc1 != null) drawPreview(nextGc1, nextBlockCanvas1, it.hasNext() ? it.next() : null);
        else if (it.hasNext()) it.next();
        if (nextGc2 != null) drawPreview(nextGc2, nextBlockCanvas2, it.hasNext() ? it.next() : null);
        else if (it.hasNext()) it.next();
        if (nextGc3 != null) drawPreview(nextGc3, nextBlockCanvas3, it.hasNext() ? it.next() : null);
        else if (it.hasNext()) it.next();
    }

    private void drawPreview(GraphicsContext gc, Canvas canvas, Piece piece) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.web(
                blackoutState == BlackoutState.BLACKOUT ? "#000000" :
                        gameContext.getGameMode() == GameContext.GameMode.HARD_MODE ? "#280C00" : "#0f0d1a"));
        gc.fillRect(0, 0, w, h);
        // HARD MODE
        if (gameContext.getGameMode() == GameContext.GameMode.HARD_MODE) {
            drawQuestionMark(gc, canvas);
            return;
        }
        // Xóa nền và vẽ viền


        if (piece == null)
            return;

        TetrominoType type = piece.getType();
        int[][] shape = type.getShape(0); // Luôn vẽ ở góc xoay mặc định

        // 1. Tính toán vùng bao (Bounding Box) để căn giữa khối gạch
        int minRow = 4, maxRow = -1, minCol = 4, maxCol = -1;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);
                }
            }
        }

        if (maxRow < 0 || maxCol < 0)
            return;

        // 2. Tính toán vị trí vẽ để khối luôn nằm chính giữa Canvas
        int previewCellSize = 24; // Kích thước ô nhỏ hơn cho vùng Preview
        int pieceWidth = (maxCol - minCol + 1) * previewCellSize;
        int pieceHeight = (maxRow - minRow + 1) * previewCellSize;
        double offsetX = (w - pieceWidth) / 2.0;
        double offsetY = (h - pieceHeight) / 2.0;

        // 3. Vẽ từng ô của khối
        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                if (shape[row][col] == 1) {
                    double px = offsetX + (col - minCol) * previewCellSize;
                    double py = offsetY + (row - minRow) * previewCellSize;
                    if (type == TetrominoType.BOMB)
                        gc.drawImage(bombSprite, px, py, previewCellSize, previewCellSize);
                    else
                        drawCellAtPixel(gc, px, py, previewCellSize, type.getColor(), 1.0);
                }
            }
        }
    }

    // Helper: vẽ 1 ô trên gameGc
    private void drawCell(GraphicsContext gc, int x, int y, Color color, double opacity) {
        drawCell(gc, x, y, color, opacity, gc, Constants.BLOCK_SIZE);
    }

    private void drawCell(GraphicsContext gc, int x, int y, Color color,
                          double opacity, GraphicsContext target, int cs) {
        double px = x * cs;
        double py = y * cs;
        target.setFill(color.deriveColor(0, 1, 1, opacity));
        target.fillRect(px, py, cs, cs);
        target.setStroke(Color.web("#111111"));
        target.setLineWidth(1);
        target.strokeRect(px, py, cs, cs);
    }

    private void drawCellAtPixel(GraphicsContext target, double px, double py,
                                 int size, Color color, double opacity) {
        target.setFill(color.deriveColor(0, 1, 1, opacity));
        target.fillRect(px, py, size, size);
        target.setStroke(Color.web("#111111"));
        target.setLineWidth(1);
        target.strokeRect(px, py, size, size);
    }

    private void drawRandomBlockIndicator(Piece piece, GraphicsContext gc, int cs) {
        int[][] shape = piece.getType().getShape(piece.getRotation());
        gc.setStroke(Color.web("#00e5ff"));
        gc.setLineWidth(2.2);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (shape[row][col] == 1) {
                    double px = (piece.getX() + col) * cs;
                    double py = (piece.getY() + row) * cs;
                    gc.strokeRect(px + 1, py + 1, cs - 2, cs - 2);
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
            case BOMB -> 8;
            case TRANSPARENT -> 9;
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
            case 8 -> TetrominoType.BOMB;
            case 9 -> TetrominoType.TRANSPARENT;
            default -> null;
        };
    }

    public void gameOver() {
        stopRandomBlockIfNeeded(currentPiece);
        if (gameLoop != null)
            gameLoop.stop();
        sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
    }

    private void drawQuestionMark(GraphicsContext gc, Canvas canvas) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.web("#00ff00")); // cùng màu viền cho đồng bộ
        gc.setFont(Font.font("VT323", FontWeight.BOLD, 40));

        String text = "?";

        // canh giữa tương đối
        gc.fillText(text, w / 2 - 8, h / 2 + 12);
        gc.setEffect(new DropShadow(10, Color.web("#00ff00")));
    }

    private void updateLightBulb() {
        if (!lightBulbView.isVisible()) return;
        boolean off = switch (blackoutState) {
            case BLACKOUT -> true;
            case FLICKER, POST_FLICKER -> ((int) blackoutFlickerTimer) % 2 == 0;
            default -> false;
        };
        lightBulbView.setImage(off ? lightOffImage : lightOnImage);
    }

    private void buildFlavourChars() {
        if (flavourLabel == null) return;
        flavourLabel.setVisible(false);
        flavourPane = (javafx.scene.layout.StackPane) flavourLabel.getParent();
        if (flavourPane == null) return;
        flavourHBox = new javafx.scene.layout.HBox(0);
        flavourHBox.setAlignment(javafx.geometry.Pos.CENTER);
        flavourPane.getChildren().add(flavourHBox);
        rebuildFlavourChars(flavourLabel.getText());
    }

    private void rebuildFlavourChars(String text) {
        darknessLoomsMode = false;
        rebuildFlavourChars(text, null);
    }

    private void rebuildFlavourChars(String text, String hexColor) {
        if (flavourHBox == null) return;
        flavourChars.clear();
        flavourHBox.getChildren().clear();
        for (char c : text.toCharArray()) {
            javafx.scene.control.Label l = new javafx.scene.control.Label(String.valueOf(c));
            l.getStyleClass().add("hard-flavour-text");
            if (hexColor != null)
                l.setStyle("-fx-text-fill: " + hexColor + ";");
            l.setOpacity(0);
            flavourChars.add(l);
            flavourHBox.getChildren().add(l);
        }
        flavourTypeElapsed = 0;
    }

    private boolean trySetFlavour(String[] pool, double threshold) {
        if (lightsOutMode || flavourCooldown > threshold || flavourHBox == null) return false;
        rebuildFlavourChars(pool[rng.nextInt(pool.length)]);
        flavourCooldown = FLAVOUR_BASE_CD;
        placesSinceFlavour = 0;
        return true;
    }

    private int getStackTopRow() {
        for (int r = 0; r < Constants.BOARD_HEIGHT; r++)
            for (int c = 0; c < Constants.BOARD_WIDTH; c++)
                if (board[r][c] != 0) return r;
        return Constants.BOARD_HEIGHT;
    }

    private void setLightsOut() {
        if (flavourHBox == null) return;
        lightsOutMode = true;
        darknessLoomsMode = false;
        flavourCooldown = Double.MAX_VALUE;
        flavourChars.clear();
        flavourHBox.getChildren().clear();
        for (char c : "LIGHTS OUT".toCharArray()) {
            javafx.scene.control.Label l = new javafx.scene.control.Label(String.valueOf(c));
            l.setStyle("-fx-font-family: 'VT323'; -fx-font-size: 40; -fx-text-fill: #FF0000;");
            l.setOpacity(1.0);
            flavourChars.add(l);
            flavourHBox.getChildren().add(l);
        }
    }

    private void clearLightsOut() {
        lightsOutMode = false;
        if (flavourHBox != null) {
            flavourHBox.setTranslateX(0);
            flavourHBox.setTranslateY(0);
        }
        rebuildFlavourChars(FLAVOUR_POST_BLACKOUT[rng.nextInt(FLAVOUR_POST_BLACKOUT.length)]);
        flavourCooldown = FLAVOUR_BASE_CD;
    }

    private void updateBlackout(double dt) {
        blackoutTimer += dt;

        switch (blackoutState) {

            case NORMAL -> {
                if (blackoutTimer >= 20.0) {
                    blackoutTimer = 0;
                    blackoutState = BlackoutState.FLICKER;
                    blackoutDuration = 0.3;
                    blackoutFlickerTimer = 0;
                    darknessLoomsMode = true;
                    rebuildFlavourChars("Darkness looms.", "#40A3FF");
                    flavourCooldown = FLAVOUR_BASE_CD;
                }
            }

            case FLICKER -> {
                blackoutDuration -= dt;
                blackoutFlickerTimer += dt * 10.0;
                if (blackoutDuration <= 0) {
                    blackoutState = BlackoutState.PRE_BLACKOUT;
                    blackoutDuration = 5.0;
                }
            }

            case PRE_BLACKOUT -> {
                blackoutDuration -= dt;
                if (blackoutDuration <= 0) {
                    blackoutState = BlackoutState.BLACKOUT;
                    blackoutDuration = 5.0;
                    blackoutDropTimer = 0;
                    SoundManager.getInstance().pauseMusic();
                    setLightsOut();
                }
            }

            case BLACKOUT -> {
                blackoutDuration -= dt;
                if (blackoutDuration <= 0) {
                    blackoutState = BlackoutState.POST_FLICKER;
                    blackoutDuration = 0.3;
                    blackoutFlickerTimer = 0;
                    SoundManager.getInstance().resumeMusic();
                    clearLightsOut();
                }
            }

            case POST_FLICKER -> {
                blackoutDuration -= dt;
                blackoutFlickerTimer += dt * 10.0;
                if (blackoutDuration <= 0) {
                    blackoutState = BlackoutState.NORMAL;
                    blackoutTimer = 0;
                }
            }
        }
    }
}