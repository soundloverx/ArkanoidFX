package org.overb.arkanoidfx.audio;

import lombok.Getter;

@Getter
public final class AudioMixer {

    private static final AudioMixer INSTANCE = new AudioMixer();
    private double masterVolume = 0.8;
    private double musicVolume = 0.5;
    private double sfxVolume = 1.0;

    public static AudioMixer getInstance() {
        return INSTANCE;
    }

    public void setMasterVolume(double v) {
        masterVolume = clamp01(v);
        applyMusicVolume();
    }

    public void setMusicVolume(double v) {
        musicVolume = clamp01(v);
        applyMusicVolume();
    }

    public void setSfxVolume(double v) {
        sfxVolume = clamp01(v);
    }

    public double getEffectiveMusicVolume() {
        return clamp01(masterVolume * musicVolume);
    }

    public double getEffectiveSfxVolume() {
        return clamp01(masterVolume * sfxVolume);
    }

    private void applyMusicVolume() {
        MusicBus.getInstance().refreshVolume();
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}