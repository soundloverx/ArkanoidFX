package org.overb.arkanoidfx.audio;

import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SfxBus {

    private static final String BASE = "assets/sounds/";
    private static final int CHANNELS = 3;
    private static final SfxBus INSTANCE = new SfxBus();
    private final Map<String, AudioClip[]> pool = new HashMap<>();
    private final Map<String, Integer> rrIndex = new HashMap<>();

    public static SfxBus getInstance() {
        return INSTANCE;
    }

    public void play(String name) {
        if (name == null || name.isBlank()) return;
        AudioClip[] clips = pool.computeIfAbsent(name, this::loadClips);
        if (clips == null) return;
        int channelIndex = nextIndex(name);
        AudioClip clip = clips[channelIndex];
        if (clip != null) {
            clip.setVolume(AudioMixer.getInstance().getEffectiveSfxVolume());
            clip.play();
        }
    }

    private int nextIndex(String key) {
        int next = (rrIndex.getOrDefault(key, -1) + 1) % CHANNELS;
        rrIndex.put(key, next);
        return next;
    }

    private AudioClip[] loadClips(String key) {
        try {
            URL sfxFile = Objects.requireNonNull(
                    Thread.currentThread().getContextClassLoader().getResource(BASE + key),
                    "Sound not found on classpath: " + BASE + key
            );
            AudioClip[] sfxChannels = new AudioClip[CHANNELS];
            for (int i = 0; i < CHANNELS; i++) {
                sfxChannels[i] = new AudioClip(sfxFile.toExternalForm());
            }
            return sfxChannels;
        } catch (Exception e) {
            return null;
        }
    }
}