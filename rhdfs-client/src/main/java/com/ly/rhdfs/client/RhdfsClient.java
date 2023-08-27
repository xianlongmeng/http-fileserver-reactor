package com.ly.rhdfs.client;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.server.ServerAddressInfo;
import com.ly.common.domain.token.TokenInfo;
import com.ly.rhdfs.client.tool.ToolUtils;

import okhttp3.*;

public class RhdfsClient {

    private final OkHttpClient okHttpClient;

    private int chunkPieceSize = 1024 * 1024;
    private int retryTimes = 10;

    public RhdfsClient() {
        okHttpClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();
    }

    public int getChunkPieceSize() {
        return chunkPieceSize;
    }

    public void setChunkPieceSize(int chunkPieceSize) {
        this.chunkPieceSize = chunkPieceSize;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public boolean uploadDFSFile(String fileName, String serverUrl, String path, String serverFileName)
            throws IOException {
        return uploadDFSFile(fileName, serverUrl, path, serverFileName, chunkPieceSize);
    }

    private TokenFileInfo httpRequestToken(String serverUrl,String requestUrl){
        if (StringUtils.isEmpty(serverUrl) || StringUtils.isEmpty(requestUrl))
            return null;
        String[] svrUrls=serverUrl.split(";");
        for (String srvUrl : svrUrls) {
            if (StringUtils.isEmpty(srvUrl))
                continue;
            String reqUrl;
            if (!srvUrl.startsWith("http"))
                reqUrl = "http://" + srvUrl;
            else
                reqUrl = srvUrl;
            reqUrl = ToolUtils.clearEndSlash(reqUrl) + "/" + ToolUtils.clearBeginSlash(requestUrl);
            Request request = new Request.Builder().url(reqUrl).build();
            Response response = null;
            try {
                response = okHttpClient.newCall(request).execute();
                if (!response.isSuccessful() || response.body() == null)
                    return null;
            } catch (IOException e) {
                continue;
            }
            TokenFileInfo tokenFileInfo = null;
            try {
                tokenFileInfo = JSON.parseObject(response.body().string(), TokenFileInfo.class);
            } catch (IOException e) {
                return null;
            }
            return tokenFileInfo;
        }
        return null;
    }

    private Response httpRequest(String serverUrl,String requestUrl){
        if (StringUtils.isEmpty(serverUrl) || StringUtils.isEmpty(requestUrl))
            return null;
        String[] svrUrls=serverUrl.split(";");
        for (String srvUrl : svrUrls) {
            if (StringUtils.isEmpty(srvUrl))
                continue;
            String reqUrl;
            if (!srvUrl.startsWith("http"))
                reqUrl = "http://" + srvUrl;
            else
                reqUrl = srvUrl;
            reqUrl = ToolUtils.clearEndSlash(reqUrl) + "/" + ToolUtils.clearBeginSlash(requestUrl);
            Request request = new Request.Builder().url(reqUrl).build();
            Response response = null;
            try {
                response = okHttpClient.newCall(request).execute();
                return response;
            } catch (IOException e) {
            }
        }
        return null;
    }
    public boolean uploadDFSFile(String fileName, String serverUrl, String path, String serverFileName,
            int chunkPieceSize) throws IOException {
        // file exist
        if (StringUtils.isEmpty(fileName) || StringUtils.isEmpty(serverUrl) || StringUtils.isEmpty(serverFileName))
            return false;
        File file = new File(fileName);
        if (!file.exists())
            return false;

        if (!StringUtils.isEmpty(path))
            path = path.replace("/", "\\");
        String uploadRequestUrl = "/dfs/upload-request";
        if (!StringUtils.isEmpty(path)) {
            if (!path.startsWith("/"))
                uploadRequestUrl += "/";
            uploadRequestUrl += path;
        }
        uploadRequestUrl = ToolUtils.clearEndSlash(uploadRequestUrl);
        // file size
        long fileSize = file.length();
        uploadRequestUrl += "?fileName=" + URLEncoder.encode(serverFileName, "utf-8") + "&fileSize=" + fileSize;
        // get server info
        TokenFileInfo tokenFileInfo = httpRequestToken(serverUrl,uploadRequestUrl);
        if (tokenFileInfo == null || tokenFileInfo.getTokenInfo() == null || tokenFileInfo.getFileInfo() == null)
            return false;
        TokenInfo tokenInfo = tokenFileInfo.getTokenInfo();
        FileInfo fileInfo = tokenFileInfo.getFileInfo();
        if (tokenInfo == null || fileInfo == null)
            return false;
        BlockingQueue<FileChunkPieceTransfer> blockingQueue = ToolUtils.buildTransferQueue(fileInfo, tokenInfo,
                chunkPieceSize);
        if (blockingQueue.isEmpty())
            return false;
        while (!blockingQueue.isEmpty()) {
            FileChunkPieceTransfer fileChunkPieceTransfer = blockingQueue.poll();
            if (fileChunkPieceTransfer == null)
                continue;
            if (fileChunkPieceTransfer.getTimes() > retryTimes)
                return false;
            if (!sendFileChunkPiece(fileChunkPieceTransfer, file, chunkPieceSize)) {
                fileChunkPieceTransfer.setTimes(fileChunkPieceTransfer.getTimes() + 1);
                blockingQueue.add(fileChunkPieceTransfer);
            }
        }
        // send finish
        String uploadFinishRequestUrl = "/dfs/upload-finish";
        if (!StringUtils.isEmpty(path)) {
            if (!path.startsWith("/"))
                uploadFinishRequestUrl += "/";
            uploadFinishRequestUrl += path;
        }
        uploadFinishRequestUrl = ToolUtils.clearEndSlash(uploadFinishRequestUrl);
        uploadFinishRequestUrl += "&token=" + tokenInfo.getToken();
        String etag = DigestUtils.md5Hex(new FileInputStream(file));
        uploadFinishRequestUrl += "&etag=" + etag;
        return httpRequest(serverUrl,uploadFinishRequestUrl).isSuccessful();
    }

    private boolean sendFileChunkPiece(FileChunkPieceTransfer fileChunkPieceTransfer, File file, int chunkPieceSize) {
        if (fileChunkPieceTransfer == null || fileChunkPieceTransfer.getFileInfo() == null
                || fileChunkPieceTransfer.getTokenInfo() == null || file == null || !file.exists())
            return false;
        long startPos = (long) fileChunkPieceTransfer.getChunk() * fileChunkPieceTransfer.getFileInfo().getChunkSize()
                + fileChunkPieceTransfer.getChunkPiece() * chunkPieceSize;
        try (RandomAccessFile accessFile = new RandomAccessFile(file, "r")) {
            accessFile.seek(startPos);
            byte[] block = new byte[fileChunkPieceTransfer.getSize()];
            accessFile.readFully(block);
            // MD5
            String etag = DigestUtils.md5Hex(block);
            // param
            ServerAddressInfo serverAddressInfo = fileChunkPieceTransfer.getServerAddressInfo();

            String requestUrl = String.format("%s/dfs/upload-file/", serverAddressInfo.getHostUri());
            String path = fileChunkPieceTransfer.getFileInfo().getPath();
            String serverFileName = fileChunkPieceTransfer.getFileInfo().getFileName();
            if (!StringUtils.isEmpty(path)) {
                if (!path.startsWith("/"))
                    requestUrl += "/";
                requestUrl += path;
            }
            requestUrl = ToolUtils.clearEndSlash(requestUrl);
            requestUrl += "?fileName=" + URLEncoder.encode(serverFileName, StandardCharsets.UTF_8);
            requestUrl += "&chunkIndex=" + fileChunkPieceTransfer.getChunk();
            requestUrl += "&chunk=" + fileChunkPieceTransfer.getChunkPiece();
            requestUrl += "&chunkSize=" + chunkPieceSize;
            requestUrl += "&chunkCount=" + fileChunkPieceTransfer.getChunkCount();
            requestUrl += "&token=" + fileChunkPieceTransfer.getTokenInfo().getToken();
            requestUrl += "&etag=" + etag;

            // send
            RequestBody rbFileBlock = RequestBody.create(block);
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(), rbFileBlock).build();
            Request request = new Request.Builder().url(requestUrl).post(requestBody).build();
            Response response = okHttpClient.newCall(request).execute();
            return response.isSuccessful();
        } catch (IOException e) {
            return false;
        }

    }

    public boolean downloadDFSFile(String serverUrl, String path, String fileName, String localFileName,
            int chunkPieceSize, boolean recover) throws IOException {
        // file exist
        if (StringUtils.isEmpty(fileName) || StringUtils.isEmpty(serverUrl) || StringUtils.isEmpty(localFileName))
            return false;
        File file = new File(localFileName);
        if (!file.exists()) {
            if (recover)
                file.delete();
            else
                return false;
        }

        if (!StringUtils.isEmpty(path))
            path = path.replace("/", "\\");
        String downloadRequestUrl = "/dfs/download-request";
        if (!StringUtils.isEmpty(path)) {
            if (!path.startsWith("/"))
                downloadRequestUrl += "/";
            downloadRequestUrl += path;
        }
        downloadRequestUrl = ToolUtils.clearEndSlash(downloadRequestUrl);
        // file size
        long fileSize = file.length();
        downloadRequestUrl += "?fileName=" + URLEncoder.encode(downloadRequestUrl, "utf-8");
        TokenFileInfo tokenFileInfo = httpRequestToken(serverUrl,downloadRequestUrl);
        if (tokenFileInfo == null || tokenFileInfo.getTokenInfo() == null || tokenFileInfo.getFileInfo() == null)
            return false;
        TokenInfo tokenInfo = tokenFileInfo.getTokenInfo();
        FileInfo fileInfo = tokenFileInfo.getFileInfo();
        if (tokenInfo == null || fileInfo == null)
            return false;
        BlockingQueue<FileChunkPieceTransfer> blockingQueue = ToolUtils.buildTransferQueue(fileInfo, tokenInfo,
                chunkPieceSize);
        if (blockingQueue.isEmpty())
            return false;
        while (!blockingQueue.isEmpty()) {
            FileChunkPieceTransfer fileChunkPieceTransfer = blockingQueue.poll();
            if (fileChunkPieceTransfer == null)
                continue;
            if (fileChunkPieceTransfer.getTimes() > retryTimes * fileChunkPieceTransfer.getFileInfo().getFileChunkList()
                    .get(fileChunkPieceTransfer.getChunk()).getChunkServerIdList().size())
                return false;
            if (fileChunkPieceTransfer.getTimes() % retryTimes == 0) {
                fileChunkPieceTransfer.setServerAddressInfo(
                        fileChunkPieceTransfer.getFileInfo().getFileChunkList().get(fileChunkPieceTransfer.getChunk())
                                .getChunkServerIdList().get(fileChunkPieceTransfer.getTimes() / retryTimes));
            }
            if (!receiveFileChunkPiece(fileChunkPieceTransfer, localFileName, chunkPieceSize)) {
                fileChunkPieceTransfer.setTimes(fileChunkPieceTransfer.getTimes() + 1);
                blockingQueue.add(fileChunkPieceTransfer);
            }
        }
        // send finish
        String downloadFinishRequestUrl = "/dfs/download-finish";
        if (!StringUtils.isEmpty(path)) {
            if (!path.startsWith("/"))
                downloadFinishRequestUrl += "/";
            downloadFinishRequestUrl += path;
        }
        downloadFinishRequestUrl = ToolUtils.clearEndSlash(downloadFinishRequestUrl);
        downloadFinishRequestUrl += "&token=" + tokenInfo.getToken();
        httpRequest(serverUrl,downloadFinishRequestUrl);
        if (!StringUtils.isEmpty(fileInfo.getEtag())) {
            String etag = DigestUtils.md5Hex(new FileInputStream(localFileName));
            return fileInfo.getEtag().equals(etag);
        }
        // verify
        return true;
    }

    private boolean receiveFileChunkPiece(FileChunkPieceTransfer fileChunkPieceTransfer, String localFileName,
            int chunkPieceSize) throws IOException {
        if (fileChunkPieceTransfer == null || fileChunkPieceTransfer.getFileInfo() == null
                || fileChunkPieceTransfer.getTokenInfo() == null || StringUtils.isEmpty(localFileName))
            return false;
        long rangeBegin = fileChunkPieceTransfer.getChunkPiece() * chunkPieceSize;
        long rangeEnd = rangeBegin + fileChunkPieceTransfer.getSize() - 1;
        long startPos = fileChunkPieceTransfer.getChunk() * fileChunkPieceTransfer.getFileInfo().getChunkSize()
                + fileChunkPieceTransfer.getChunkPiece() * chunkPieceSize;

        // param
        ServerAddressInfo serverAddressInfo = fileChunkPieceTransfer.getServerAddressInfo();

        String requestUrl = String.format("%s/dfs/download-file/", serverAddressInfo.getHostUri());
        String path = fileChunkPieceTransfer.getFileInfo().getPath();
        String serverFileName = fileChunkPieceTransfer.getFileInfo().getFileName();
        if (!StringUtils.isEmpty(path)) {
            if (!path.startsWith("/"))
                requestUrl += "/";
            requestUrl += path;
        }
        requestUrl = ToolUtils.clearEndSlash(requestUrl);
        requestUrl += "?fileName=" + URLEncoder.encode(serverFileName, StandardCharsets.UTF_8);
        // send
        Request request = new Request.Builder().url(requestUrl)
                .header("Range", String.format("bytes=%d-%d", rangeBegin, rangeEnd)).build();
        Response response = okHttpClient.newCall(request).execute();
        if (!response.isSuccessful())
            return false;
        try (RandomAccessFile accessFile = new RandomAccessFile(localFileName, "rw")) {
            InputStream in = response.body().byteStream();
            accessFile.seek(startPos);
            byte[] block = new byte[2048];
            int len;
            while ((len = in.read(block)) != -1) {
                accessFile.write(block, 0, len);
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
