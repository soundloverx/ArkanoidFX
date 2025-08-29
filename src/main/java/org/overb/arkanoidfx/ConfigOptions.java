package org.overb.arkanoidfx;

public class ConfigOptions {
    public int width = 1920;
    public int height = 1080;
    public String fullscreenMode = "WINDOWED";
    public int nativeW = 1920;
    public int nativeH = 1080;

    public AudioCfg audio = new AudioCfg();

    public static class AudioCfg {
        public double master = 1.0;
        public double music = 0.7;
        public double sfx = 0.8;
    }
}
