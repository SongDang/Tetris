package com.se330.tetris.service;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

public class SoundManager {
    private static SoundManager instance;
    Clip clip;
    URL soundURL[] = new URL[20];

    public SoundManager()
    {
        soundURL[0] = getClass().getResource("/sound/main_theme_music.wav");
        soundURL[1] = getClass().getResource("/sound/enter_mode_sound.wav");
        soundURL[2] = getClass().getResource("/sound/gameplay_theme.wav");

    }

    public static SoundManager getInstance() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    public void setFile(int i)
    {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundURL[i]);
            clip = AudioSystem.getClip();
            clip.open(ais);
        } catch (Exception e){
        }
    }

    public void play()
    {
        clip.start();
    }
    public void loop()
    {
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }
    public void stop()
    {
        if (this.clip != null) {
            if (this.clip.isRunning()) {
                this.clip.stop();
            }
            this.clip.close();
        }
    }

    public void playMusic(int i) {
        stop();
        setFile(i);
        play();
        loop();
    }

    public void playSE(int i) { // SE = Sound Effect
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundURL[i]);
            Clip seClip = AudioSystem.getClip();
            seClip.open(ais);
            seClip.start();
        } catch (Exception e) {}
    }
}
