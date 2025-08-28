package org.overb.arkanoidfx.audio;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.Objects;

public final class MusicBus {

    private static final String BASE = "assets/music/";
    private static final MusicBus INSTANCE = new MusicBus();
    private MediaPlayer current;
    private String currentKey;

    public static MusicBus getInstance() {
        return INSTANCE;
    }

    public void loop(String name) {
        if (current != null && name.equals(currentKey)) {
            refreshVolume();
            return;
        }
        stop();
        try {
            URL musicFile = Objects.requireNonNull(
                    Thread.currentThread().getContextClassLoader().getResource(BASE + name),
                    "Music not found on classpath: " + BASE + name
            );
            Media media = new Media(musicFile.toExternalForm());
            current = new MediaPlayer(media);
            current.setCycleCount(MediaPlayer.INDEFINITE);
            current.setVolume(AudioMixer.getInstance().getEffectiveMusicVolume());
            current.play();
            currentKey = name;
        } catch (Exception e) {
            current = null;
            currentKey = null;
        }
    }

    public void stop() {
        if (current == null) {
            return;
        }
        try {
            current.stop();
        } catch (Exception ignore) {
        }
        try {
            current.dispose();
        } catch (Exception ignore) {
        }
        current = null;
        currentKey = null;
    }

    public void refreshVolume() {
        if (current != null) {
            current.setVolume(AudioMixer.getInstance().getEffectiveMusicVolume());
        }
    }
}