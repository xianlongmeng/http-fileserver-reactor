package com.ly.rhdfs.client.tool;

import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.token.TokenInfo;
import com.ly.rhdfs.client.FileChunkPieceTransfer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ToolUtils {
    public static String clearBothSlash(String s) {
        if (s == null)
            return null;
        String res = clearBeginSlash(s);
        res=clearEndSlash(res);
        return res;
    }

    public static String clearBeginSlash(String s) {
        if (s == null)
            return null;
        String res = s;
        while (res.startsWith("/")) {
            res = res.substring(1);
        }
        return res;
    }

    public static String clearEndSlash(String s) {
        if (s == null)
            return null;
        String res = s;
        while (res.endsWith("/")) {
            res = res.substring(0, s.length() - 1);
        }
        return res;
    }
    public static BlockingQueue<FileChunkPieceTransfer> buildTransferQueue(FileInfo fileInfo, TokenInfo tokenInfo,
                                                                  int chunkPieceSize){
        List<FileChunkPieceTransfer> transferList= buildTransferList(fileInfo,tokenInfo,chunkPieceSize);
        BlockingQueue<FileChunkPieceTransfer> blockingQueue = new ArrayBlockingQueue<FileChunkPieceTransfer>(transferList.size());
        blockingQueue.addAll(transferList);
        return blockingQueue;
    }
    public static List<FileChunkPieceTransfer> buildTransferList(FileInfo fileInfo, TokenInfo tokenInfo,
                                                                     int chunkPieceSize) {
        if (fileInfo == null || tokenInfo == null)
            return null;
        int chunkPieceTotal = (int) computerPieceCount(fileInfo, chunkPieceSize);
        List<FileChunkPieceTransfer> transferList = new ArrayList<>(chunkPieceTotal);
        long totalSize = 0;
        for (int i = 0; i < fileInfo.getChunkCount() - 1; i++) {
            int cSize = 0;
            int count = 0;
            if (i == fileInfo.getChunkCount() - 1)
                count = (int) ((fileInfo.getSize() - totalSize) + chunkPieceSize - 1) / chunkPieceSize;
            else
                count = (fileInfo.getChunkSize() + chunkPieceSize - 1) / chunkPieceSize;
            for (int j = 0; j < count - 1; j++) {
                transferList.add(
                        new FileChunkPieceTransfer(fileInfo.getFileChunkList().get(i).getChunkServerIdList().get(0), i,
                                j, chunkPieceSize, count, tokenInfo, fileInfo));
                cSize += chunkPieceSize;
            }
            if (i == fileInfo.getChunkCount() - 1)
                transferList.add(
                        new FileChunkPieceTransfer(fileInfo.getFileChunkList().get(i).getChunkServerIdList().get(0), i,
                                count - 1, count, (int) (fileInfo.getSize() - totalSize - cSize), tokenInfo, fileInfo));
            else
                transferList.add(
                        new FileChunkPieceTransfer(fileInfo.getFileChunkList().get(i).getChunkServerIdList().get(0), i,
                                count - 1, count, fileInfo.getChunkSize() - cSize, tokenInfo, fileInfo));
            totalSize += fileInfo.getChunkSize();
        }
        return transferList;
    }

    public static long computerPieceCount(FileInfo fileInfo, int chunkPieceSize) {
        if (fileInfo == null)
            return 0;
        return (fileInfo.getChunkCount() - 1) * ((fileInfo.getChunkSize() + chunkPieceSize - 1) / chunkPieceSize)
                + (fileInfo.getSize() % fileInfo.getChunkSize() + 1) / chunkPieceSize;
    }
}
