package main.java;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;
import java.util.Optional;

public class Sound {
    Clip clip;
    URL soundURL[] = new URL[20];

    public Sound()
    {
        soundURL[0] = getClass().getResource("/sound/main_theme_music.wav");
        soundURL[1] = getClass().getResource("/sound/enter_mode_sound");
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
        clip.stop();
    }
}
