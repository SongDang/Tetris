package com.se330.tetris.service;

import com.se330.tetris.util.SoundType;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private static SoundManager instance;
    private Clip musicClip;
    private final Map<SoundType, URL> soundMap = new HashMap<>();

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
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
            musicClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playSE(SoundType type) {
        try {
            URL url = soundMap.get(type);
            if (url == null) return;

            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Clip seClip = AudioSystem.getClip();
            seClip.open(ais);
            seClip.start();

            seClip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    seClip.close();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}