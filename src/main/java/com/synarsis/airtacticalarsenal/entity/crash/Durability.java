package com.synarsis.airtacticalarsenal.entity.crash;

import net.minecraft.util.RandomSource;

public final class Durability {

    public static final int MIN_HITS_TO_FALL = 2;
    public static final int MAX_HITS_TO_FALL = 5;
    public static final int MIN_FALLING_HITS = 3;
    public static final int MAX_FALLING_HITS = 8;

    private float toughness = Float.NaN;
    private int hits;
    private int hitsToFall;
    private int fallingHits;
    private int fallingHitsToBreak;
    public float engineHp;
    public float wingLeftHp;
    public float wingRightHp;

    public void ensure(float maxHp, RandomSource random) {
        if (!Float.isNaN(this.toughness)) return;
        this.toughness = 1.0f + random.nextFloat() * 1.5f;
        this.engineHp = maxHp * 0.4f * (0.8f + random.nextFloat() * 0.4f);
        this.wingLeftHp = maxHp * 0.3f * (0.8f + random.nextFloat() * 0.4f);
        this.wingRightHp = maxHp * 0.3f * (0.8f + random.nextFloat() * 0.4f);
        this.hitsToFall = MIN_HITS_TO_FALL + random.nextInt(MAX_HITS_TO_FALL - MIN_HITS_TO_FALL + 1);
        this.fallingHitsToBreak = MIN_FALLING_HITS + random.nextInt(MAX_FALLING_HITS - MIN_FALLING_HITS + 1);
    }

    public float scale(float damage) {
        return damage / this.toughness;
    }

    public boolean hit() {
        this.hits++;
        return this.hits >= this.hitsToFall;
    }

    public boolean fallingHit() {
        this.fallingHits++;
        return this.fallingHits >= this.fallingHitsToBreak;
    }
}
