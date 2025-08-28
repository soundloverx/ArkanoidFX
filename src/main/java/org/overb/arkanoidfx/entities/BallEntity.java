package org.overb.arkanoidfx.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BallEntity {
    public int id;
    public String name;
    public int sizeW;
    public int sizeH;
    public Sprite visual;
    public Sounds sounds;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sounds {
        public String hitWall;
        public String hitPaddle;
        public String hitLightning;
        public String lost;
    }
}