package org.overb.arkanoidfx.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SurpriseEntity {
    public int id;
    public String name;
    public Sprite visual;
    public double fallSpeed;
    public String effect;
    public String sound;
    public double spawnChance;
}