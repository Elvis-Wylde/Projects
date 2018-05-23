package com.wylde.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.wylde.model.TileModel;
import org.bson.Document;

public class TileDaoImpl implements TileDao {

    private MongoDatabase db = MongoManager.getInstance().getMongoDatabase();

    @Override
    public Document findRecord(String dataSet, long level, long x, long y) {
        BasicDBObject obj = new BasicDBObject();
        obj.put("level", level);
        obj.put("x", x);
        obj.put("y", y);
        MongoCursor<Document> iterator = db.getCollection(dataSet).find(obj).iterator();
        if (iterator.hasNext()) {
            // return new TileModel(dataSet, iterator.next());
            return iterator.next();
        }
        return null;
    }

    @Override
    public Document insertTile(TileModel tile) {
        Document record = new Document("level", tile.getLevel()).
                append("x", tile.getX()).
                append("y", tile.getY()).
                append("attempts", tile.getAttempts()).
                append("failure", !tile.isAvailable());
        byte[] bytes = tile.getBytes();
        if (bytes != null && bytes.length > 0) {
            record.append("bytes", bytes);
        }
        db.getCollection(tile.getDataSet()).insertOne(record);
        return record;
    }

    @Override
    public void updateTile(Document record, TileModel tile) {
        String collection = tile.getDataSet();
        Document update = new Document("attempts", tile.getAttempts()).
                append("failure", !tile.isAvailable());
        byte[] bytes = tile.getBytes();
        if (bytes != null && bytes.length > 0) {
            update.append("bytes", bytes);
        }
        db.getCollection(collection).updateOne(record, new Document("$set", update));
    }

    @Override
    public void setUnavailable(Document record, TileModel tile) {
        String collection = tile.getDataSet();
        db.getCollection(collection).updateOne(record, new Document("$set", new Document("failure", true)));
    }

    @Override
    public int increaseAttempts(Document record, TileModel tile) {
        String collection = tile.getDataSet();
        int attempts = record.getInteger("attempts") + 1;
        db.getCollection(collection).updateOne(record, new Document("$set", new Document("attempts", attempts)));
        return attempts;
    }
}
