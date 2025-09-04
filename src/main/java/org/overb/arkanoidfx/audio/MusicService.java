package org.overb.arkanoidfx.audio;

import lombok.Getter;
import lombok.extern.java.Log;

@Log
public final class MusicService {

    private static final MusicService INSTANCE = new MusicService();

    public static MusicService getInstance() {
        return INSTANCE;
    }

    public void play(String song) {
        if (song == null || song.isBlank()) {
            return;
        }
        stopCurrentMusic();
        MusicBus.getInstance().loop(song);
    }

    public void stopCurrentMusic() {
        MusicBus.getInstance().stop();
    }
}