package org.overb.arkanoidfx.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Sprite {
    public String sprite;
    public int frames;
    public int frameW;
    public int frameH;
    public double frameDuration;
}