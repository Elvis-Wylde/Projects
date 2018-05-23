package com.wylde.service;

import com.wylde.dao.TileDao;
import com.wylde.dao.TileDaoImpl;
import com.wylde.model.Configuration;
import com.wylde.model.RemoteSourceManager;
import com.wylde.model.TileModel;
import org.bson.Document;
import sun.misc.BASE64Encoder;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.nio.ByteBuffer;
import java.util.HashMap;

@Path("imagery")
public class Imagery {

    private int defaultAttempts = Configuration.getInteger("attempts", 100);
    private BASE64Encoder encoder = new BASE64Encoder();
    private static HashMap<String, String> CONTENT_TYPE_MAP = new HashMap<>();
    private static HashMap<String, String> EMPTY_IMG_STR = new HashMap<>();

//    public static void main(String[] args) {
//        Imagery img = new Imagery();
//        img.getImagery("BingAerial", 1,1,1);
//    }

    @GET
    @Path("{dataSet}/{z}/{x}/{y}")
    public Response getImagery(@PathParam("dataSet") String dataSet, @PathParam("z") long z, @PathParam("x") long x, @PathParam("y") long y) {
        // String result = "user.dir: " + System.getProperty("user.dir") + "; z: " + z + "; x: " + x + ";y: " + y;
        Response response;
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
            // 获取图片资源
            result = tile.getBytes();
            if (result == null) {
                // 图片资源不存在，进行请求
                result = requestRemoteResource(tile);
            }
            if (result != null) {
                // 能够请求到图片，但有可能为空图片
                if (isResourceUnavailable(tile, result)) {
                    // 如果图片为空图片
                    result = null;
                    mongoAccessor.setUnavailable(record, tile);
                } else {
                    tile.setBytes(result);
                    mongoAccessor.updateTile(record, tile);
                }
            } else {
                // 无法请求到图片，增加请求记数
                int attempts = mongoAccessor.increaseAttempts(record, tile);
                if (attempts >= defaultAttempts) {
                    // 如果求情记数超过设定阈值，标识为不可用
                    mongoAccessor.setUnavailable(record, tile);
                }
            }
        }

        int statusCode = result == null ? 204 : 200;
        response = Response.status(statusCode).entity(result).
                header("Content-Type", getContentType(dataSet)).
                header("Cache-Control", "max-age=2592000").
                header("Access-Control-Allow-Origin", "*").
                header("Access-Control-Allow-Methods", "*").build();

        return response;
    }

    private byte[] requestRemoteResource(TileModel tile) {
        ByteBuffer result = RemoteSourceManager.getData(tile);
        return result == null ? null : result.array();
    }

    private boolean isResourceUnavailable(TileModel tile, byte[] result) {
        String lineBreaker = System.getProperty("line.separator");
        String empty = getEmptyImgStr(tile.getDataSet());
        String resultStr = encoder.encode(result).replace(lineBreaker, "");
        return resultStr.equals(empty);
    }

    private String getContentType(String dataSet) {
        if (CONTENT_TYPE_MAP.containsKey(dataSet)) {
            return CONTENT_TYPE_MAP.get(dataSet);
        } else {
            String contentType = Configuration.get(dataSet + "ContentType", "image/jpeg");
            CONTENT_TYPE_MAP.put(dataSet, contentType);
            return contentType;
        }
    }

    private String getEmptyImgStr(String dataSet) {
        if (EMPTY_IMG_STR.containsKey(dataSet)) {
            return EMPTY_IMG_STR.get(dataSet);
        } else {
            String empty = Configuration.get(dataSet + "EmptyByte", null);
            EMPTY_IMG_STR.put(dataSet, empty);
            return empty;
        }
    }

}
