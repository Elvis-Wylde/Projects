package com.wylde.dao;

import com.wylde.model.TileModel;
import org.bson.Document;

public interface TileDao {

    Document findRecord(String dataSet, long level, long x, long y);

    Document insertTile(TileModel tile);

    void updateTile(Document record, TileModel tile);

    void setUnavailable(Document record, TileModel tile);

    int increaseAttempts(Document record, TileModel tile);
}
