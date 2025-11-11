package com.example.nfs;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * NFSディレクトリからファイルを読み込んで複数の宛先に分散するメインクラス
 */
public class FileDistributor {
    private static final Logger logger = Logger.getLogger(FileDistributor.class.getName());

    private final Path sourceDirectory;
    private final List<Path> destinationDirectories;
    private final DistributionStrategy strategy;
    private final ExecutorService executorService;

    public FileDistributor(Path sourceDirectory, List<Path> destinationDirectories, DistributionStrategy strategy) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectories = destinationDirectories;
        this.strategy = strategy;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /**
     * ファイルの分散処理を実行
     */
    public void distribute() throws IOException {
        logger.info("ファイル分散処理を開始します: " + sourceDirectory);

        // NFSディレクトリからすべてのファイルを取得
        List<Path> files = listFiles(sourceDirectory);
        logger.info("検出されたファイル数: " + files.size());

        if (files.isEmpty()) {
            logger.warning("処理対象のファイルが見つかりませんでした");
            return;
        }

        // 各ファイルを分散先にコピー
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            Path sourceFile = files.get(i);
            Path destination = strategy.selectDestination(sourceFile, destinationDirectories, i);

            Future<Boolean> future = executorService.submit(() -> copyFile(sourceFile, destination));
            futures.add(future);
        }

        // すべてのタスクの完了を待機
        int successCount = 0;
        int failureCount = 0;
        for (Future<Boolean> future : futures) {
            try {
                if (future.get()) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.log(Level.SEVERE, "ファイルコピーでエラーが発生しました", e);
                failureCount++;
            }
        }

        logger.info(String.format("分散処理完了 - 成功: %d, 失敗: %d", successCount, failureCount));
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
     * ファイルを宛先にコピー
     */
    private boolean copyFile(Path source, Path destinationDir) {
        try {
            // 宛先ディレクトリが存在しない場合は作成
            if (!Files.exists(destinationDir)) {
                Files.createDirectories(destinationDir);
            }

            // ソースファイルの相対パスを保持
            Path relativePath = sourceDirectory.relativize(source);
            Path destinationFile = destinationDir.resolve(relativePath);

            // 宛先ファイルの親ディレクトリを作成
            Path parentDir = destinationFile.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // ファイルをコピー
            Files.copy(source, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            logger.fine(String.format("ファイルをコピーしました: %s -> %s", source, destinationFile));

            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "ファイルのコピーに失敗しました: " + source, e);
            return false;
        }
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
     * メインメソッド
     */
    public static void main(String[] args) {
        try {
            // プロパティファイルから設定を読み込み
            Properties properties = new Properties();
            Path configPath = Paths.get("config.properties");

            if (Files.exists(configPath)) {
                properties.load(Files.newInputStream(configPath));
            } else {
                logger.warning("config.propertiesが見つかりません。デフォルト設定を使用します。");
                // デフォルト設定
                properties.setProperty("source.directory", "/mnt/nfs/source");
                properties.setProperty("destination.directories", "/mnt/dest1,/mnt/dest2,/mnt/dest3");
                properties.setProperty("distribution.strategy", "roundrobin");
            }

            // 設定値の取得
            Path sourceDir = Paths.get(properties.getProperty("source.directory"));
            String[] destDirs = properties.getProperty("destination.directories").split(",");
            List<Path> destinationDirs = new ArrayList<>();
            for (String dir : destDirs) {
                destinationDirs.add(Paths.get(dir.trim()));
            }

            // 分散戦略の選択
            DistributionStrategy strategy;
            String strategyType = properties.getProperty("distribution.strategy", "roundrobin");
            switch (strategyType.toLowerCase()) {
                case "hash":
                    strategy = new HashDistributionStrategy();
                    break;
                case "roundrobin":
                default:
                    strategy = new RoundRobinDistributionStrategy();
                    break;
            }

            // ファイル分散処理の実行
            FileDistributor distributor = new FileDistributor(sourceDir, destinationDirs, strategy);
            try {
                distributor.distribute();
            } finally {
                distributor.shutdown();
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "エラーが発生しました", e);
            System.exit(1);
        }
    }
}
