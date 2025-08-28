package org.overb.arkanoidfx.entities;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EntityRepository {
    private final Map<Integer, BrickEntity> bricksById = new HashMap<>();
    private final Map<Integer, SurpriseEntity> surprisesById = new HashMap<>();
    private final Map<Integer, BallEntity> ballsById = new HashMap<>();
    private final Map<Integer, PaddleEntity> paddlesById = new HashMap<>();

    public void addBrick(BrickEntity def) { bricksById.put(def.id, def); }
    public void addSurprise(SurpriseEntity def) { surprisesById.put(def.id, def); }
    public void addBall(BallEntity def) { ballsById.put(def.id, def); }
    public void addPaddle(PaddleEntity def) { paddlesById.put(def.id, def); }

    public Map<Integer, BrickEntity> getBricks() { return Collections.unmodifiableMap(bricksById); }
    public Map<Integer, SurpriseEntity> getSurprises() { return Collections.unmodifiableMap(surprisesById); }
    public Map<Integer, BallEntity> getBalls() { return Collections.unmodifiableMap(ballsById); }
    public Map<Integer, PaddleEntity> getPaddles() { return Collections.unmodifiableMap(paddlesById); }

    public BrickEntity getBrick(int id) { return bricksById.get(id); }
    public SurpriseEntity getSurprise(int id) { return surprisesById.get(id); }
    public BallEntity getBall(int id) { return ballsById.get(id); }
    public PaddleEntity getPaddle(int id) { return paddlesById.get(id); }
}
