package com.wylde.service;

import com.wylde.dao.TileDao;
import com.wylde.dao.TileDaoImpl;
import com.wylde.model.Configuration;
import com.wylde.model.RemoteSourceManager;
import com.wylde.model.TileModel;
import org.bson.Document;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.ByteBuffer;

@Path("terrain")
public class Terrain {

    private int defaultAttempts = Configuration.getInteger("attempts", 100);

    @GET
    @Path("{dataSet}/layer.json")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetaJson() {
        Response response;
        // application/vnd.quantized-mesh,application/octet-stream;q=0.9,*/*;q=0.01
        // application/vnd.quantized-mesh

        // application/vnd.quantized-mesh;extensions=watermask,application/octet-stream;q=0.9,*/*;q=0.01
        // application/vnd.quantized-mesh;extensions=watermask

        // application/vnd.quantized-mesh;extensions=octvertexnormals-watermask,application/octet-stream;q=0.9,*/*;q=0.01
        // application/vnd.quantized-mesh;extensions=octvertexnormals-watermask

        byte[] result = Configuration.getTerrainMeta();

        response = Response.ok(result).
                header("Content-Type", "application/json").
                header("Cache-Control", "max-age=2592000").
                header("Access-Control-Allow-Origin", "*").
                header("Access-Control-Allow-Methods", "*").build();

        return response;
    }

    @GET
    @Path("{dataSet}/{z}/{x}/{y}.terrain")
    public Response getTerrain(@PathParam("dataSet") String dataSet, @PathParam("z") long z, @PathParam("x") long x, @PathParam("y") long y) {
        Response response;
        // application/vnd.quantized-mesh,application/octet-stream;q=0.9,*/*;q=0.01
        // application/vnd.quantized-mesh

        // application/vnd.quantized-mesh;extensions=watermask,application/octet-stream;q=0.9,*/*;q=0.01
        // application/vnd.quantized-mesh;extensions=watermask

        // application/vnd.quantized-mesh;extensions=octvertexnormals-watermask,application/octet-stream;q=0.9,*/*;q=0.01
        // application/vnd.quantized-mesh;extensions=octvertexnormals-watermask

        TileModel tile;

        TileDao mongoAccessor = new TileDaoImpl();
        Document record = mongoAccessor.findRecord(dataSet, z, x, y);

        if (record != null) {
            // 能够在mongo中找到记录
            tile = new TileModel(dataSet, record);
        } else {
            // mongo中不存在该记录
            tile = new TileModel(dataSet, z, x, y);
            // 新建记录
            record = mongoAccessor.insertTile(tile);
        }
        byte[] result = null;
        if (tile.isAvailable()) {
            // 未被标识为不可获取
            // 获取地形瓦片资源
            result = tile.getBytes();
            if (result == null) {
                // 地形瓦片资源不存在，进行请求
                result = requestRemoteResource(tile);
            }
            if (result != null) {
                tile.setBytes(result);
                mongoAccessor.updateTile(record, tile);
            } else {
                // 无法请求到地形瓦片，增加请求记数
                int attempts = mongoAccessor.increaseAttempts(record, tile);
                if (attempts >= defaultAttempts) {
                    // 如果请求记数超过设定阈值，标识为不可用
                    mongoAccessor.setUnavailable(record, tile);
                }
            }
        }

        int statusCode = result == null ? 204 : 200;

        response = Response.status(statusCode).entity(result).
                header("Content-Type", "application/vnd.quantized-mesh").
                header("Content-Encoding", "gzip").
                header("Cache-Control", "max-age=2592000").
                header("Access-Control-Allow-Origin", "*").
                header("Access-Control-Allow-Methods", "*").build();

        return response;
    }

    private byte[] requestRemoteResource(TileModel tile) {
        ByteBuffer result = RemoteSourceManager.getData(tile);
        return result == null ? null : result.array();
    }

}
