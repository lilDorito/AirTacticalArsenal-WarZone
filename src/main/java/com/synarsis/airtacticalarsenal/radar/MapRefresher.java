package com.synarsis.airtacticalarsenal.radar;

import com.synarsis.airtacticalarsenal.config.ShahedConfig;
import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;

final class MapRefresher {

    private static final Logger LOGGER = LoggerFactory.getLogger("ATA-MapRefresh");

    private enum State { IDLE, DECODING, SCANNING, ENCODING }

    private final int size, step, originX, originZ;
    private State state = State.IDLE;
    private long lastDoneMs = System.currentTimeMillis();
    private CompletableFuture<int[]> decoding;
    private CompletableFuture<byte[]> encoding;
    private int[] pixels;
    private int index;
    private int changed;

    MapRefresher(int size, int step, int originX, int originZ) {
        this.size = size;
        this.step = step;
        this.originX = originX;
        this.originZ = originZ;
    }

    boolean isActive() {
        return state != State.IDLE;
    }

    boolean isDue() {
        int minutes = ShahedConfig.getMapRefreshMinutes();
        return state == State.IDLE && minutes > 0
                && System.currentTimeMillis() - lastDoneMs >= minutes * 60_000L;
    }

    void cancel() {
        state = State.IDLE;
        decoding = null;
        encoding = null;
        pixels = null;
        markFresh();
    }

    void markFresh() {
        lastDoneMs = System.currentTimeMillis();
    }

    void start(byte[] png) {
        if (png == null) {
            markFresh();
            return;
        }
        decoding = CompletableFuture.supplyAsync(() -> decode(png, size), Util.backgroundExecutor());
        state = State.DECODING;
    }

    byte[] tick(ServerLevel level, long budgetMs) {
        long start = System.currentTimeMillis();
        switch (state) {
            case DECODING -> {
                if (!decoding.isDone()) return null;
                pixels = decoding.getNow(null);
                decoding = null;
                if (pixels == null) {
                    finish();
                    return null;
                }
                index = 0;
                changed = 0;
                state = State.SCANNING;
            }
            case SCANNING -> {
                MapPixelSampler.Sample tmp = new MapPixelSampler.Sample();
                int total = size * size;
                while (index < total) {
                    int wx = originX + (index % size) * step;
                    int wz = originZ + (index / size) * step;
                    int c = MapPixelSampler.sampleLoaded(level, wx, wz, step, tmp);
                    if (c != 0 && c != pixels[index]) {
                        pixels[index] = c;
                        changed++;
                    }
                    index++;
                    if ((index & 255) == 0 && System.currentTimeMillis() - start > budgetMs) return null;
                }
                if (changed == 0) {
                    pixels = null;
                    finish();
                    return null;
                }
                int[] p = pixels;
                pixels = null;
                encoding = CompletableFuture.supplyAsync(() -> encode(p, size), Util.backgroundExecutor());
                state = State.ENCODING;
            }
            case ENCODING -> {
                if (!encoding.isDone()) return null;
                byte[] png = encoding.getNow(null);
                encoding = null;
                finish();
                return png;
            }
            default -> { }
        }
        return null;
    }

    private void finish() {
        state = State.IDLE;
        markFresh();
    }

    static int[] decode(byte[] png, int size) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
            if (img == null || img.getWidth() != size || img.getHeight() != size) return null;
            return img.getRGB(0, 0, size, size, null, 0, size);
        } catch (Exception e) {
            LOGGER.warn("[ATA] Failed to decode map for refresh", e);
            return null;
        }
    }

    static byte[] encode(int[] pixels, int size) {
        try {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, size, size, pixels, 0, size);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            LOGGER.error("[ATA] Failed to encode map", e);
            return null;
        }
    }
}
