package org.overb.arkanoidfx.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BrickEntity {
    public int id;
    public String name;
    public Sprite visual;
    public int hp;
    public int points;
    public String hitSound;
    public String destroySound;
    public boolean animated;
    public boolean damageAdvancesFrame;
    public Sprite breakAnim;     // nullable
    public double speedEffect;   // e.g., +0.05 => +5%
}