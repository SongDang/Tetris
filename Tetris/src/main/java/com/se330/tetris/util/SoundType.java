package com.se330.tetris.util;

public enum SoundType {
    //music
    MAIN_THEME("/sound/main_theme_music.wav"),
    GAMEPLAY_THEME("/sound/gameplay_theme.wav"),
    HARD_THEME("/sound/hard_mode_music.wav"),
    TIME_MODE_MUSIC("/sound/time_mode_music.wav"),
    //sound effect
    BUTTON_CLICK("/sound/button_click_sound.wav"),
    CLICK("/sound/click.wav"),
    HOVER("/sound/hover.wav"),
    ENTER_MODE("/sound/enter_mode_sound.wav"),
    GAME_OVER("/sound/game_over_sound.wav"),
    BLOCK_DROP("/sound/block_drop_sound.wav"),
    SCORE("/sound/score_sound.wav"),
    BOMB_EXPLODE("/sound/boom17.wav"),
    TETRIS("/sound/tetris.wav"),
    TETRIS2("/sound/tetris2.wav"),
    FUSE("/sound/fuse.wav"),
    RESULT_GLITCH("/sound/result_glitch.wav"),
    TRANSPARENT_PIECE("/sound/transparent_piece_sound.wav"),
    RANDOM_PIECE("/sound/random_piece_sound.wav");

    private final String path;

    SoundType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}