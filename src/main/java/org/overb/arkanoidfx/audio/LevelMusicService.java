package org.overb.arkanoidfx.audio;

import lombok.Getter;
import lombok.extern.java.Log;

@Log
public final class LevelMusicService {

    private static final LevelMusicService INSTANCE = new LevelMusicService();
    @Getter
    private boolean levelMusicStarted = false;

    public static LevelMusicService getInstance() {
        return INSTANCE;
    }

    public void play(String song) {
        if (levelMusicStarted || song == null || song.isBlank()) {
            return;
        }
        stopCurrentMusic();
        MusicBus.getInstance().loop(song);
        levelMusicStarted = true;
    }

    public void stopCurrentMusic() {
        MusicBus.getInstance().stop();
        levelMusicStarted = false;
    }
}