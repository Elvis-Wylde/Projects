package com.wylde.dao;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.wylde.model.Configuration;

import java.util.ArrayList;
import java.util.List;

public class MongoManager {

    private final static MongoManager instance = new MongoManager();

    private MongoClient mg;

    private MongoDatabase mongoDatabase;

    public static MongoManager getInstance() {
        return instance;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public MongoClient getMongoClient() {
        return mg;
    }

    /*
    public static void setMongoDatabase(MongoDatabase mongoDatabase) {
        MongoManager.mongoDatabase = mongoDatabase;
    }
    */

    private MongoManager() {
        MongoClientOptions options = MongoClientOptions.builder()
                .connectionsPerHost(Configuration.getInteger("connectionsPerHost", 10))
                .maxWaitTime(Configuration.getInteger("maxWaitTime", 100 * 60 * 5))
                .socketTimeout(Configuration.getInteger("socketTimeout", 100000))
                .maxConnectionLifeTime(Configuration.getInteger("maxConnectionLifeTime", 100 * 60 * 5))
                .connectTimeout(Configuration.getInteger("connectTimeout", 1000 * 60 * 20)).build();

        int hostLen = Configuration.getInteger("len", 1);

        //所有主机
        List<ServerAddress> hosts = new ArrayList<>();
        for (int i = 1; i <= hostLen; i++) {
            String host = Configuration.get("host" + i, "localhost");
            // String port = mongoProperties.getString("port"+i);
            hosts.add(new ServerAddress(host));
        }
        if (Configuration.get("authentication", "0").equals("1")) {
            // 需要验证
            MongoCredential credential = MongoCredential.createCredential(
                    Configuration.get("userName", ""), Configuration.get("authDB", "admin"),
                    Configuration.get("pwd", "").toCharArray());
            mg = new MongoClient(hosts, credential, options);
        } else {
            mg = new MongoClient(hosts, options);
        }

        mongoDatabase = mg.getDatabase(Configuration.get("dbName", "Tiles"));
    }


}
