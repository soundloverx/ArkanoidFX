package org.overb.arkanoidfx.audio;

import lombok.Getter;
import lombok.extern.java.Log;
import org.overb.arkanoidfx.entities.LevelEntity;

@Log
public final class MusicService {

    @Getter
    private boolean levelMusicStarted = false;

    public void startLevelMusicIfNeeded(LevelEntity level) {
        if (levelMusicStarted || level == null || level.music == null || level.music.isBlank()) {
            return;
        }
        stopCurrentMusic();
        MusicBus.getInstance().loop(level.music);
        levelMusicStarted = true;
    }

    public void stopCurrentMusic() {
        MusicBus.getInstance().stop();
        levelMusicStarted = false;
    }
}