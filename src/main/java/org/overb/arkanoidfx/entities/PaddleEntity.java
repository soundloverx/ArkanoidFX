package org.overb.arkanoidfx.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaddleEntity {
    public int id;
    public String name;
    public int sizeW;
    public int sizeH;
    public Sprite visual;
}