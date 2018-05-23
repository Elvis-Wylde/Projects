package com.wylde.model;

import org.bson.Document;
import org.bson.types.Binary;

public class TileModel {

    private String dataSet;
    private long level;
    private long x;
    private long y;
    private byte[] bytes;
    private boolean isAvailable = true;
    private int attempts = 0;

    public TileModel(String dataSet, long level, long x, long y) {
        this.dataSet = dataSet;
        this.level = level;
        this.x = x;
        this.y = y;
        this.bytes = null;
        this.isAvailable = true;
        this.attempts = 0;
    }

    private TileModel(String dataSet, long level, long x, long y, byte[] bytes, boolean isAvailable, int attempts) {
        this.dataSet = dataSet;
        this.level = level;
        this.x = x;
        this.y = y;
        this.bytes = bytes;
        this.isAvailable = isAvailable;
        this.attempts = attempts;
    }

    public TileModel(String collection, Document record) {
        this(collection,
                record.getLong("level"), record.getLong("x"), record.getLong("y"),
                record.get("bytes", Binary.class) == null ? null : record.get("bytes", Binary.class).getData(),
                !record.getBoolean("failure"), record.getInteger("attempts"));
    }

    public String getDataSet() {
        return dataSet;
    }

    public long getLevel() {
        return level;
    }

    public long getX() {
        return x;
    }

    public long getY() {
        return y;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public void setLevel(long level) {
        this.level = level;
    }

    public void setX(long x) {
        this.x = x;
    }

    public void setY(long y) {
        this.y = y;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String convertQuadkey() {
        String quadKey = "";
        long level = this.level - 1;
        long y = this.y;
        long x = this.x;
        for (long i = level; i >= 0; --i) {
            int bitmask = 1 << i;
            int digit = 0;

            if ((x & bitmask) != 0) {
                digit |= 1;
            }
            if ((y & bitmask) != 0) {
                digit |= 2;
            }
            quadKey += digit;
        }
        return quadKey;
    }

}
