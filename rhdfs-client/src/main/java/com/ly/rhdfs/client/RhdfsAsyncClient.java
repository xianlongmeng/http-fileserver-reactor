package com.ly.rhdfs.client;

import com.ly.common.domain.server.ServerAddressInfo;
import com.ly.common.util.MyStringUtils;
import com.ly.rhdfs.client.exception.FileDownloadFailedException;
import com.ly.rhdfs.client.exception.FileUploadFailedException;
import com.ly.rhdfs.client.tool.ToolUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RhdfsAsyncClient {

    public final DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
    public int bufferLength = 1024;
    private int retryTimes = 10;

    public boolean uploadFileSync(String localFileName, String baseUrl, String path, String fileName,
                                  int chunkPieceSize) {
        AtomicBoolean res = new AtomicBoolean();
        uploadFileAsyncBool(localFileName, baseUrl, path, fileName, chunkPieceSize)
                .subscribe(res::set);
        return res.get();
    }

    public Mono<Boolean> uploadFileAsyncBool(String localFileName, String baseUrl, String path, String fileName,
                                             int chunkPieceSize) {
        return uploadFileAsync(localFileName, baseUrl, path, fileName, chunkPieceSize)
                .onErrorResume(t -> Mono.empty())
                .then(Mono.just(true))
                .switchIfEmpty(Mono.just(false));
    }

    public Mono<Void> uploadFileAsync(String localFileName, String baseUrl, String path, String fileName,
                                      int chunkPieceSize) {
        if (StringUtils.isEmpty(fileName) || StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(localFileName))
            return Mono.error(new FileUploadFailedException());
        if (chunkPieceSize == 0)
            chunkPieceSize = 1024 * 1024;
        File file = new File(localFileName);
        if (!file.exists())
            return Mono.error(new FileUploadFailedException());
        long fileSize = file.length();
        if (StringUtils.isEmpty(path))
            path = ToolUtils.clearBothSlash(path);
        fileName = ToolUtils.clearBeginSlash(fileName);
        AtomicInteger sendCount = new AtomicInteger();
        AtomicReference<TokenFileInfo> tokenFileInfoAtom = new AtomicReference<>();
        String finalPath = path;
        int finalChunkPieceSize = chunkPieceSize;
        AtomicInteger index = new AtomicInteger();
        String[] serverUrls = baseUrl.split(";");
        String finalFileName = fileName;
        return Mono
                .just(serverUrls)
                .flatMap(urls -> WebClient.create(urls[index.getAndIncrement()]).get()
                        .uri(UriComponentsBuilder.fromUriString("/dfs/upload-request/{path}").queryParam("fileName", finalFileName)
                                .queryParam("fileSize", fileSize).toUriString(), finalPath)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve().bodyToMono(TokenFileInfo.class))
                .retry(serverUrls.length)
                .flux()
                .flatMap(tokenFileInfo -> {
                    index.set(0);
                    tokenFileInfoAtom.set(tokenFileInfo);
                    List<FileChunkPieceTransfer> transferList = ToolUtils.buildTransferList(tokenFileInfo.getFileInfo(),
                            tokenFileInfo.getTokenInfo(), finalChunkPieceSize);
                    sendCount.set(transferList.size());
                    return Flux.fromIterable(transferList);
                }).parallel(
                        3)
                .runOn(Schedulers.parallel())
                .flatMap(
                        fileChunkPieceTransfer -> sendFileChunkPieceAsync(fileChunkPieceTransfer, file, finalChunkPieceSize))
                .sequential()
                .then(DataBufferUtils
                        .readAsynchronousFileChannel(
                                () -> AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ), 0,
                                dataBufferFactory, bufferLength)
                        .collect(() -> {
                                    try {
                                        return Optional.of(MessageDigest.getInstance("MD5"));
                                    } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
                                        return Optional.empty();
                                    }
                                },
                                (optional, dataBuffer) -> optional
                                        .ifPresent(md5 -> ((MessageDigest) md5).update(dataBuffer.asByteBuffer().array())))
                        .map(optional -> {
                            if (optional.isPresent()) {
                                return MyStringUtils.bytesToHexStr(((MessageDigest) optional.get()).digest());
                            } else {
                                return "";
                            }
                        })
                        .flatMap(etag -> WebClient.create(serverUrls[index.getAndIncrement()]).get()
                                .uri(UriComponentsBuilder.fromUriString("/dfs/upload-finish/{path}")
                                        .queryParam("token", tokenFileInfoAtom.get().getTokenInfo().getToken())
                                        .queryParam("etag", etag).toUriString(), finalPath)
                                .exchange()
                                .flatMap(clientResponse -> {
                                    if (clientResponse.statusCode().is2xxSuccessful())
                                        return Mono.empty().then();
                                    else
                                        return Mono.error(new FileDownloadFailedException());
                                }))
                        .retry(serverUrls.length));
    }

    private Mono<Void> sendFileChunkPieceAsync(FileChunkPieceTransfer fileChunkPieceTransfer, File file,
                                               int chunkPieceSize) {
        if (fileChunkPieceTransfer == null || fileChunkPieceTransfer.getFileInfo() == null
                || fileChunkPieceTransfer.getTokenInfo() == null || file == null || !file.exists() || chunkPieceSize == 0)
            return Mono.error(new NullPointerException());
        long startPos = (long) fileChunkPieceTransfer.getChunk() * fileChunkPieceTransfer.getFileInfo().getChunkSize()
                + (long) fileChunkPieceTransfer.getChunkPiece() * chunkPieceSize;
        ServerAddressInfo serverAddressInfo = fileChunkPieceTransfer.getServerAddressInfo();
        return Mono.create(sink -> {
            try {
                AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(file.toPath(),
                        StandardOpenOption.READ);
                ByteBuffer byteBuffer = ByteBuffer.allocate(fileChunkPieceTransfer.getSize());
                fileChannel.read(byteBuffer, startPos, byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {

                    @Override
                    public void completed(Integer result, ByteBuffer attachment) {
                        String etag = DigestUtils.md5Hex(byteBuffer.array());
                        WebClient
                                .create(String.format("http://%s:%d", serverAddressInfo.getAddress(),
                                        serverAddressInfo.getPort()))
                                .post()
                                .uri(UriComponentsBuilder.fromUriString("/dfs/upload-file/{path}")
                                        .queryParam("fileName", fileChunkPieceTransfer.getFileInfo().getFileName())
                                        .queryParam("chunkIndex", fileChunkPieceTransfer.getChunk())
                                        .queryParam("chunk", fileChunkPieceTransfer.getChunkPiece())
                                        .queryParam("chunkSize", chunkPieceSize)
                                        .queryParam("chunkCount", fileChunkPieceTransfer.getChunkCount())
                                        .queryParam("token", fileChunkPieceTransfer.getTokenInfo().getToken())
                                        .queryParam("etag", etag).toUriString())
                                .body(BodyInserters.fromMultipartData("file", byteBuffer.array())).exchange()
                                .retryWhen(Retry.fixedDelay(retryTimes, Duration.ofSeconds(30))).subscribe(clientResponse -> {
                            if (clientResponse.statusCode().is2xxSuccessful())
                                sink.success();
                            else
                                sink.error(new FileUploadFailedException());
                        });
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        sink.error(new FileDownloadFailedException());
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                sink.error(e);
            }
        });
    }

    public boolean downloadFileSync(String localFileName, String baseUrl, String path, String fileName,
                                    int chunkPieceSize, boolean recover) {
        AtomicBoolean res = new AtomicBoolean();
        downloadFileAsyncBool(localFileName, baseUrl, path, fileName, chunkPieceSize, recover)
                .subscribe(res::set);
        return res.get();
    }

    public Mono<Boolean> downloadFileAsyncBool(String localFileName, String baseUrl, String path, String fileName,
                                               int chunkPieceSize, boolean recover) {
        return downloadFileAsync(localFileName, baseUrl, path, fileName, chunkPieceSize, recover)
                .onErrorResume(t -> Mono.empty())
                .then(Mono.just(true))
                .switchIfEmpty(Mono.just(false));
    }

    public Mono<Void> downloadFileAsync(String localFileName, String baseUrl, String path, String fileName,
                                        int chunkPieceSize, boolean recover) {
        // file exist
        if (StringUtils.isEmpty(fileName) || StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(localFileName))
            return Mono.error(new FileDownloadFailedException());
        File file = new File(localFileName);
        if (!file.exists()) {
            if (recover)
                file.delete();
            else
                return Mono.error(new FileDownloadFailedException());
        }
        if (StringUtils.isEmpty(path))
            path = ToolUtils.clearBothSlash(path);
        fileName = ToolUtils.clearBeginSlash(fileName);
        AtomicInteger sendCount = new AtomicInteger();
        String finalFileName = fileName;
        String finalPath = path;
        AtomicReference<TokenFileInfo> tokenFileInfoAtom = new AtomicReference<>();
        AtomicInteger index = new AtomicInteger();
        String[] serverUrls = baseUrl.split(";");
        return Mono.just(serverUrls).
                flatMap(urls -> WebClient.create(urls[index.getAndIncrement()]).get()
                        .uri(UriComponentsBuilder.fromUriString("/dfs/download-request/{path}")
                                .queryParam("fileName", finalFileName).toUriString(), finalPath)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(TokenFileInfo.class))
                .retry(serverUrls.length)
                .flux()
                .flatMap(tokenFileInfo -> {
                    tokenFileInfoAtom.set(tokenFileInfo);
                    List<FileChunkPieceTransfer> transferList = ToolUtils.buildTransferList(tokenFileInfo.getFileInfo(),
                            tokenFileInfo.getTokenInfo(), chunkPieceSize);
                    sendCount.set(transferList.size());
                    return Flux.fromIterable(transferList);
                }).parallel(3).runOn(Schedulers.parallel())
                .flatMap(fileChunkPieceTransfer -> receiveFileChunkPieceAsync(fileChunkPieceTransfer, file,
                        chunkPieceSize))
                .sequential()
                .then(DataBufferUtils
                        .readAsynchronousFileChannel(
                                () -> AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ), 0,
                                dataBufferFactory, bufferLength)
                        .collect(() -> {
                                    try {
                                        return Optional.of(MessageDigest.getInstance("MD5"));
                                    } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
                                        return Optional.empty();
                                    }
                                },
                                (optional, dataBuffer) -> optional
                                        .ifPresent(md5 -> ((MessageDigest) md5).update(dataBuffer.asByteBuffer().array())))
                        .map(optional -> {
                            if (optional.isPresent()) {
                                return MyStringUtils.bytesToHexStr(((MessageDigest) optional.get()).digest());
                            } else {
                                return "";
                            }
                        })
                        .flatMap(etag -> WebClient.create(serverUrls[index.getAndIncrement()]).get()
                                .uri(UriComponentsBuilder.fromUriString("/dfs/download-finish/{path}")
                                        .queryParam("fileName", finalFileName).toUriString(), finalPath)
                                .exchange()
                                .flatMap(clientResponse -> {
                                    if (clientResponse.statusCode().is2xxSuccessful())
                                        return Mono.empty().then();
                                    else
                                        return Mono.error(new FileDownloadFailedException());
                                }))
                        .retry(serverUrls.length));
    }

    public Mono<Void> receiveFileChunkPieceAsync(FileChunkPieceTransfer fileChunkPieceTransfer, File file, int chunkPieceSize) {
        if (fileChunkPieceTransfer == null || fileChunkPieceTransfer.getFileInfo() == null
                || fileChunkPieceTransfer.getTokenInfo() == null || file == null || !file.exists() || chunkPieceSize == 0)
            return Mono.error(new NullPointerException());
        long startPos = (long) fileChunkPieceTransfer.getChunk() * fileChunkPieceTransfer.getFileInfo().getChunkSize()
                + (long) fileChunkPieceTransfer.getChunkPiece() * chunkPieceSize;
        fileChunkPieceTransfer.setTimes(0);
        List<ServerAddressInfo> serverAddressInfoList = fileChunkPieceTransfer.getFileInfo().getFileChunkList().get(fileChunkPieceTransfer.getChunk())
                .getChunkServerIdList();
        if (serverAddressInfoList == null || serverAddressInfoList.isEmpty())
            return Mono.error(new NullPointerException());
        long rangeBegin = (long) fileChunkPieceTransfer.getChunkPiece() * chunkPieceSize;
        long rangeEnd = rangeBegin + fileChunkPieceTransfer.getSize() - 1;
        return Mono
                .create(sink -> {
                    fileChunkPieceTransfer.setTimes(fileChunkPieceTransfer.getTimes() + 1);
                    int index = fileChunkPieceTransfer.getTimes() / retryTimes;
                    if (index >= serverAddressInfoList.size()) {
                        sink.error(new FileDownloadFailedException());
                        return;
                    }
                    fileChunkPieceTransfer.setServerAddressInfo(serverAddressInfoList.get(index));
                    ServerAddressInfo serverAddressInfo = fileChunkPieceTransfer.getServerAddressInfo();
                    WebClient
                            .create(String.format("http://%s:%d", serverAddressInfo.getAddress(),
                                    serverAddressInfo.getPort()))
                            .post()
                            .uri(UriComponentsBuilder.fromUriString("/dfs/upload-file/{path}")
                                    .queryParam("fileName", fileChunkPieceTransfer.getFileInfo().getFileName())
                                    .toUriString())
                            .header("Range", String.format("bytes=%d-%d", rangeBegin, rangeEnd))
                            .accept(MediaType.APPLICATION_OCTET_STREAM)
                            .exchange()
                            .subscribe(clientResponse -> {
                                if (clientResponse.statusCode().is2xxSuccessful()) {
                                    //save file chunk
                                    try {
                                        AsynchronousFileChannel asynchronousFileChannel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                                        DataBufferUtils
                                                .write(clientResponse.body(BodyExtractors.toDataBuffers()), asynchronousFileChannel,
                                                        startPos)
                                                .subscribe(DataBufferUtils::release,
                                                        e -> sink.error(new FileDownloadFailedException(e.getMessage(), e)),
                                                        sink::success);
                                    } catch (IOException e) {
                                        sink.error(e);
                                    }
                                } else
                                    sink.error(new FileDownloadFailedException());
                            });
                })
                .retryWhen(Retry.fixedDelay((long) retryTimes * fileChunkPieceTransfer.getFileInfo().getFileChunkList().get(fileChunkPieceTransfer.getChunk()).getChunkServerIdList().size(), Duration.ofSeconds(30)))
                .then();
    }
}
