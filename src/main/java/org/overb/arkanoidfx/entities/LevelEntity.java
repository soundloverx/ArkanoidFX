package org.overb.arkanoidfx.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class LevelEntity {
    public int cols;
    public int rows;
    public String music;
    public String background;
    public List<Cell> cells = new ArrayList<>();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Cell {
        public int col;
        public int row;
        public int brickId;
    }
}