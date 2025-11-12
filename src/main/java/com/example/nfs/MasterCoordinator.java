package com.example.nfs;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * マスターコーディネーター
 * XMLファイルをスキャンし、複数のワーカーサーバーにタスクを配分して処理を実行
 */
public class MasterCoordinator {
    private static final Logger logger = Logger.getLogger(MasterCoordinator.class.getName());
    private static final Gson gson = new Gson();

    private final Path sourceDirectory;
    private final List<String> workerUrls;
    private final ExecutorService executorService;
    private final HttpClient httpClient;
    private final XMLProcessor localProcessor; // ローカル処理用

    public MasterCoordinator(Path sourceDirectory, List<String> workerUrls) {
        this.sourceDirectory = sourceDirectory;
        this.workerUrls = workerUrls;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.localProcessor = new XMLProcessor();
    }

    /**
     * 分散処理を実行
     */
    public void execute() throws IOException {
        logger.info("分散処理を開始します: " + sourceDirectory);
        long startTime = System.currentTimeMillis();

        // XMLファイルをスキャン
        List<Path> xmlFiles = listXMLFiles(sourceDirectory);
        logger.info(String.format("検出されたXMLファイル数: %d", xmlFiles.size()));

        if (xmlFiles.isEmpty()) {
            logger.warning("処理対象のXMLファイルが見つかりませんでした");
            return;
        }

        // タスクを作成
        List<ProcessingTask> tasks = new ArrayList<>();
        for (Path file : xmlFiles) {
            String taskId = UUID.randomUUID().toString();
            tasks.add(new ProcessingTask(taskId, file.toString()));
        }

        // タスクをワーカーに配分して処理
        List<Future<ProcessingResult>> futures = new ArrayList<>();
        int totalWorkers = workerUrls.size() + 1; // ワーカー数 + ローカル処理

        for (int i = 0; i < tasks.size(); i++) {
            ProcessingTask task = tasks.get(i);
            int workerIndex = i % totalWorkers;

            Future<ProcessingResult> future;
            if (workerIndex < workerUrls.size()) {
                // リモートワーカーに送信
                String workerUrl = workerUrls.get(workerIndex);
                future = executorService.submit(() -> sendTaskToWorker(workerUrl, task));
            } else {
                // ローカルで処理（マスターサーバーも処理に参加）
                future = executorService.submit(() -> localProcessor.processXMLFile(task));
            }
            futures.add(future);
        }

        // すべてのタスクの完了を待機して結果を集計
        int successCount = 0;
        int failureCount = 0;
        long totalProcessingTime = 0;
        int totalElementCount = 0;

        logger.info("すべてのタスクの完了を待機しています...");

        for (int i = 0; i < futures.size(); i++) {
            try {
                ProcessingResult result = futures.get(i).get();

                if (result.isSuccess()) {
                    successCount++;
                    totalElementCount += result.getElementCount();
                } else {
                    failureCount++;
                    logger.warning(String.format("タスク失敗: %s - %s",
                            result.getFilePath(), result.getErrorMessage()));
                }
                totalProcessingTime += result.getProcessingTimeMs();

            } catch (InterruptedException | ExecutionException e) {
                logger.log(Level.SEVERE, "タスク実行中にエラーが発生しました", e);
                failureCount++;
            }
        }

        long endTime = System.currentTimeMillis();
        long totalElapsedTime = endTime - startTime;

        // 結果をレポート
        logger.info("========================================");
        logger.info("分散処理が完了しました");
        logger.info("========================================");
        logger.info(String.format("処理ファイル数: %d", xmlFiles.size()));
        logger.info(String.format("成功: %d", successCount));
        logger.info(String.format("失敗: %d", failureCount));
        logger.info(String.format("合計XML要素数: %d", totalElementCount));
        logger.info(String.format("合計処理時間: %d ms", totalProcessingTime));
        logger.info(String.format("全体経過時間: %d ms", totalElapsedTime));
        logger.info(String.format("使用ワーカー数: %d (リモート) + 1 (ローカル)", workerUrls.size()));
        logger.info("========================================");
    }

    /**
     * XMLファイルをリストアップ
     */
    private List<Path> listXMLFiles(Path directory) throws IOException {
        List<Path> xmlFiles = new ArrayList<>();

        if (!Files.exists(directory)) {
            logger.warning("ディレクトリが存在しません: " + directory);
            return xmlFiles;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry) && entry.toString().toLowerCase().endsWith(".xml")) {
                    xmlFiles.add(entry);
                } else if (Files.isDirectory(entry)) {
                    // サブディレクトリも再帰的に処理
                    xmlFiles.addAll(listXMLFiles(entry));
                }
            }
        }

        return xmlFiles;
    }

    /**
     * タスクをワーカーサーバーに送信
     */
    private ProcessingResult sendTaskToWorker(String workerUrl, ProcessingTask task) {
        ProcessingResult result = new ProcessingResult(task.getTaskId(), task.getFilePath());

        try {
            // タスクをJSONに変換
            String jsonTask = gson.toJson(task);

            // HTTPリクエストを作成
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(workerUrl + "/process"))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonTask, StandardCharsets.UTF_8))
                    .timeout(Duration.ofMinutes(5))
                    .build();

            logger.fine(String.format("タスクをワーカーに送信: %s -> %s", task.getTaskId(), workerUrl));

            // リクエストを送信
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 結果をパース
                result = gson.fromJson(response.body(), ProcessingResult.class);
                logger.fine(String.format("タスク完了: %s (ワーカー: %s)", task.getTaskId(), workerUrl));
            } else {
                result.setSuccess(false);
                result.setErrorMessage(String.format("HTTPエラー: %d - %s",
                        response.statusCode(), response.body()));
                logger.warning(String.format("ワーカーからエラーレスポンス: %s - %s",
                        workerUrl, result.getErrorMessage()));
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "ワーカーへのタスク送信に失敗しました: " + workerUrl, e);
            result.setSuccess(false);
            result.setErrorMessage("通信エラー: " + e.getMessage());
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
     * メインメソッド
     */
    public static void main(String[] args) {
        try {
            // プロパティファイルから設定を読み込み
            Properties properties = new Properties();
            Path configPath = Paths.get("config-master.properties");

            if (Files.exists(configPath)) {
                properties.load(Files.newInputStream(configPath));
            } else {
                logger.warning("config-master.propertiesが見つかりません。デフォルト設定を使用します。");
                // デフォルト設定
                properties.setProperty("source.directory", "/mnt/nfs/xml-files");
                properties.setProperty("worker.urls", "http://server2:8080,http://server3:8080");
            }

            // 設定値の取得
            Path sourceDir = Paths.get(properties.getProperty("source.directory"));
            String[] workerUrlArray = properties.getProperty("worker.urls").split(",");
            List<String> workerUrls = new ArrayList<>();
            for (String url : workerUrlArray) {
                workerUrls.add(url.trim());
            }

            logger.info("マスターコーディネーター設定:");
            logger.info("  ソースディレクトリ: " + sourceDir);
            logger.info("  ワーカーURL: " + workerUrls);

            // 分散処理の実行
            MasterCoordinator coordinator = new MasterCoordinator(sourceDir, workerUrls);
            try {
                coordinator.execute();
            } finally {
                coordinator.shutdown();
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "エラーが発生しました", e);
            System.exit(1);
        }
    }
}
