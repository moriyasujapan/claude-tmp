package com.example.nfs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * 分散されたディレクトリからファイルを読み込むクラス
 * 各ファイルをOpenしてCloseする処理を行う
 */
public class FileReader {
    private static final Logger logger = Logger.getLogger(FileReader.class.getName());

    private final List<Path> sourceDirectories;
    private final ExecutorService executorService;
    private final boolean validateFiles;

    public FileReader(List<Path> sourceDirectories, boolean validateFiles) {
        this.sourceDirectories = sourceDirectories;
        this.validateFiles = validateFiles;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /**
     * ファイル読み込み処理を実行
     */
    public void readFiles() throws IOException {
        logger.info("ファイル読み込み処理を開始します");

        // すべてのソースディレクトリからファイルをリストアップ
        List<Path> allFiles = new ArrayList<>();
        for (Path directory : sourceDirectories) {
            if (Files.exists(directory)) {
                List<Path> files = listFiles(directory);
                allFiles.addAll(files);
                logger.info(String.format("ディレクトリ %s から %d ファイルを検出", directory, files.size()));
            } else {
                logger.warning("ディレクトリが存在しません: " + directory);
            }
        }

        logger.info("合計検出ファイル数: " + allFiles.size());

        if (allFiles.isEmpty()) {
            logger.warning("処理対象のファイルが見つかりませんでした");
            return;
        }

        // 各ファイルをOpenしてCloseする処理を並列実行
        List<Future<FileReadResult>> futures = new ArrayList<>();
        for (Path file : allFiles) {
            Future<FileReadResult> future = executorService.submit(() -> openAndCloseFile(file));
            futures.add(future);
        }

        // すべてのタスクの完了を待機
        int successCount = 0;
        int failureCount = 0;
        long totalBytesRead = 0;

        for (Future<FileReadResult> future : futures) {
            try {
                FileReadResult result = future.get();
                if (result.success) {
                    successCount++;
                    totalBytesRead += result.bytesRead;
                } else {
                    failureCount++;
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.log(Level.SEVERE, "ファイル読み込みでエラーが発生しました", e);
                failureCount++;
            }
        }

        logger.info(String.format("読み込み処理完了 - 成功: %d, 失敗: %d, 合計読み込みバイト数: %d",
                successCount, failureCount, totalBytesRead));
    }

    /**
     * ディレクトリ内のすべてのファイルをリストアップ
     */
    private List<Path> listFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    files.add(entry);
                } else if (Files.isDirectory(entry)) {
                    // サブディレクトリも再帰的に処理
                    files.addAll(listFiles(entry));
                }
            }
        }

        return files;
    }

    /**
     * ファイルをOpenしてCloseする
     */
    private FileReadResult openAndCloseFile(Path file) {
        FileReadResult result = new FileReadResult();
        result.filePath = file;

        try {
            long startTime = System.currentTimeMillis();

            // ファイルをオープン
            try (InputStream inputStream = Files.newInputStream(file)) {
                logger.fine("ファイルをオープンしました: " + file);

                if (validateFiles) {
                    // オプション: ファイルの内容を検証（全バイト読み込み）
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytes = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        totalBytes += bytesRead;
                    }

                    result.bytesRead = totalBytes;
                    logger.fine(String.format("ファイルを読み込みました: %s (%d バイト)", file, totalBytes));
                } else {
                    // ファイルサイズのみ取得
                    result.bytesRead = Files.size(file);
                }

                // InputStreamはtry-with-resourcesで自動的にクローズされる
                logger.fine("ファイルをクローズしました: " + file);
            }

            long endTime = System.currentTimeMillis();
            result.processingTimeMs = endTime - startTime;
            result.success = true;

            logger.info(String.format("処理完了: %s (%d バイト, %d ms)",
                    file.getFileName(), result.bytesRead, result.processingTimeMs));

        } catch (IOException e) {
            logger.log(Level.SEVERE, "ファイルの読み込みに失敗しました: " + file, e);
            result.success = false;
            result.error = e.getMessage();
        }

        return result;
    }

    /**
     * シャットダウン処理
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * ファイル読み込み結果を保持するクラス
     */
    private static class FileReadResult {
        Path filePath;
        boolean success;
        long bytesRead;
        long processingTimeMs;
        String error;
    }

    /**
     * メインメソッド
     */
    public static void main(String[] args) {
        try {
            // プロパティファイルから設定を読み込み
            Properties properties = new Properties();
            Path configPath = Paths.get("config-reader.properties");

            if (Files.exists(configPath)) {
                properties.load(Files.newInputStream(configPath));
            } else {
                logger.warning("config-reader.propertiesが見つかりません。デフォルト設定を使用します。");
                // デフォルト設定
                properties.setProperty("source.directories", "/mnt/dest1,/mnt/dest2,/mnt/dest3");
                properties.setProperty("validate.files", "false");
            }

            // 設定値の取得
            String[] sourceDirs = properties.getProperty("source.directories").split(",");
            List<Path> sourceDirectories = new ArrayList<>();
            for (String dir : sourceDirs) {
                sourceDirectories.add(Paths.get(dir.trim()));
            }

            boolean validateFiles = Boolean.parseBoolean(
                    properties.getProperty("validate.files", "false"));

            // ファイル読み込み処理の実行
            FileReader reader = new FileReader(sourceDirectories, validateFiles);
            try {
                reader.readFiles();
            } finally {
                reader.shutdown();
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "エラーが発生しました", e);
            System.exit(1);
        }
    }
}
