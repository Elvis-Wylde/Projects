package com.wylde.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteSourceManager {

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 8000;
    private static Random rand = new Random();
    // private static Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("202.111.31.158", 8080));

    public static ByteBuffer getData(TileModel tile) {
        String urlStr = getRealSourceUrl(tile);
        ByteBuffer byteBuffer;
        try {
            byteBuffer = RemoteSourceManager.read(RemoteSourceManager.openConnection(new URL(urlStr)));
        } catch (Exception e) {
            System.err.println("can not get tile: " + urlStr);
            System.err.println(e.getMessage());
            return null;
        }
        return byteBuffer;
    }

    private static String getRealSourceUrl(TileModel tile) {
        String dataSet = tile.getDataSet();
        String url = Configuration.get(dataSet + "Url", null);
        int serverpart = Configuration.getInteger(dataSet + "ServerPart", 1);
        int i = rand.nextInt(serverpart);
        if (dataSet.equals("BingAerial")) {
            String quadKey = tile.convertQuadkey();
            url = url.replace("{$quadkey}", quadKey);
        }
        url = url.replace("{$serverpart}", i + "").replace("{$x}", tile.getX() + "").replace("{$y}", tile.getY() + "").replace("{$z}", tile.getLevel() + "");
        return url;
    }

    private static URLConnection openConnection(URL url) throws IOException {
        URLConnection connection;
        connection = url.openConnection();
        connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36");
        connection.setRequestProperty("Accept", "image/webp,image/*,*/*;q=0.8");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate, sdch");
        connection.setRequestProperty("Accept-Language", "en,en-US;q=0.8,zh-CN;q=0.6,zh;q=0.4,zh-TW;q=0.2");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        return connection;
    }

//    private static URLConnection openTerrainConnection(URL url) throws IOException {
//        URLConnection connection;
//        try {
//            connection = url.openConnection();
//            connection.setRequestProperty("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
//            String extensions = "octvertexnormals-watermask";
//            String header = "application/vnd.quantized-mesh;extensions=" + extensions + ",application/octet-stream;q=0.9,*/*;q=0.01";
//            connection.setRequestProperty("Accept", header);
//            connection.setRequestProperty("Accept-Encoding", "gzip, deflate, sdch, br");
//            connection.setRequestProperty("Accept-Language", "en,en-US;q=0.8,zh-CN;q=0.6,zh;q=0.4,zh-TW;q=0.2");
//        } catch (IOException e) {
//            throw e;
//        }
//        connection.setConnectTimeout(CONNECT_TIMEOUT);
//        connection.setReadTimeout(READ_TIMEOUT);
//        return connection;
//    }

    private static ByteBuffer read(URLConnection connection) throws Exception {
        return doRead(connection);
    }

    private static ByteBuffer doRead(URLConnection connection) throws Exception {
        if (connection == null) {
            throw new IllegalArgumentException();
        }
        ByteBuffer buffer;
        InputStream inputStream;
        inputStream = connection.getInputStream();
        if (inputStream == null) {
            return null;
        }
        buffer = readNonSpecificStream(inputStream, connection);
        closeStream(inputStream);
        return buffer;
    }

    private static ByteBuffer readNonSpecificStream(InputStream inputStream, URLConnection connection) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException();
        }
        int contentLength = connection.getContentLength();
        if (contentLength < 1) {
            return readNonSpecificStreamUnknownLength(inputStream);
        }
        ReadableByteChannel channel = Channels.newChannel(inputStream);
        ByteBuffer buffer = ByteBuffer.allocate(contentLength);
        int numBytesRead = 0;
        AtomicInteger contentLengthRead = new AtomicInteger(0);
        while (!Thread.currentThread().isInterrupted() && numBytesRead >= 0 && numBytesRead < buffer.limit()) {
            int count = channel.read(buffer);
            if (count > 0) {
                contentLengthRead.getAndAdd(numBytesRead += count);
            }
        }
        buffer.flip();
        return buffer;
    }

    private static ByteBuffer readNonSpecificStreamUnknownLength(InputStream inputStream) throws IOException {
        final int pageSize = (int) Math.ceil(Math.pow(2, 15));

        ReadableByteChannel channel = Channels.newChannel(inputStream);
        ByteBuffer buffer = ByteBuffer.allocate(pageSize);

        int count = 0;
        int numBytesRead = 0;
        AtomicInteger contentLengthRead = new AtomicInteger(0);
        while (!Thread.currentThread().isInterrupted() && count >= 0) {
            count = channel.read(buffer);
            if (count > 0) {
                contentLengthRead.getAndAdd(numBytesRead += count);
            }
            if (count > 0 && !buffer.hasRemaining()) {
                ByteBuffer biggerBuffer = ByteBuffer.allocate(buffer.limit() + pageSize);
                biggerBuffer.put((ByteBuffer) buffer.rewind());
                buffer = biggerBuffer;
            }
        }
        buffer.flip();
        return buffer;
    }

    private static void closeStream(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
