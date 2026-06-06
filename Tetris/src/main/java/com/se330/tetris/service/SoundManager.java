package com.se330.tetris.service;

import com.se330.tetris.util.SoundType;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private static SoundManager instance;
    private Clip musicClip;
    private Clip loopingClip;
    private final Map<SoundType, URL> soundMap = new HashMap<>();

    private float musicVolume = 0.8f;
    private float seVolume = 1.0f;

    public SoundManager()
    {
        for (SoundType type : SoundType.values()) {
            URL url = getClass().getResource(type.getPath());
            if (url != null) {
                soundMap.put(type, url);
            } else {
                System.err.println("Warning: Sound file not found: " + type.getPath());
            }
        }
    }

    public static SoundManager getInstance() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    public void stopMusic() {
        if (musicClip != null) {
            musicClip.stop();
            musicClip.close();
            musicClip = null;
        }
    }

    public void playMusic(SoundType type) {
        stopMusic();

        try {
            URL url = soundMap.get(type);
            if (url == null) return;

            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            musicClip = AudioSystem.getClip();
            musicClip.open(ais);
            applyVolume(musicClip, musicVolume);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
            musicClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playSE(SoundType type) {
        URL url = soundMap.get(type);
        if (url == null) return;
        float vol = seVolume;
        Thread t = new Thread(() -> {
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(url);
                Clip seClip = AudioSystem.getClip();
                seClip.open(ais);
                ais.close();
                applyVolume(seClip, vol);
                seClip.start();
                seClip.addLineListener(event -> {
                    if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP)
                        seClip.close();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void playLooping(SoundType type) {
        stopLooping();
        try {
            URL url = soundMap.get(type);
            if (url == null) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            loopingClip = AudioSystem.getClip();
            loopingClip.open(ais);
            applyVolume(loopingClip, seVolume);
            loopingClip.loop(Clip.LOOP_CONTINUOUSLY);
            loopingClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopLooping() {
        if (loopingClip != null) {
            loopingClip.stop();
            loopingClip.close();
            loopingClip = null;
        }
    }

    public void pauseMusic() {
        if (musicClip != null && musicClip.isRunning()) {
            musicClip.stop();
        }
    }

    public void resumeMusic() {
        if (musicClip != null && musicClip.isOpen() && !musicClip.isRunning()) {
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
            musicClip.start();
        }
    }

    public void setMusicVolume(float volume) {
        this.musicVolume = clampVolume(volume);
        if (musicClip != null && musicClip.isOpen()) {
            applyVolume(musicClip, musicVolume);
        }
    }

    public void setSEVolume(float volume) {
        this.seVolume = clampVolume(volume);
        if (loopingClip != null && loopingClip.isOpen()) {
            applyVolume(loopingClip, seVolume);
        }
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public float getSEVolume() {
        return seVolume;
    }

    private float clampVolume(float volume) {
        return Math.max(0.0f, Math.min(1.0f, volume));
    }

    private void applyVolume(Clip clip, float volume) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log(volume <= 0.0 ? 0.0001 : volume) / Math.log(10.0) * 20.0);
                gainControl.setValue(dB);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
