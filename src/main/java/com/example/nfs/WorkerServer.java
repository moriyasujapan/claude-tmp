package com.example.nfs;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ワーカーサーバー
 * マスターサーバーからタスクを受け取り、XML処理を実行する
 */
public class WorkerServer {
    private static final Logger logger = Logger.getLogger(WorkerServer.class.getName());
    private static final Gson gson = new Gson();

    private final int port;
    private final XMLProcessor processor;
    private HttpServer server;

    public WorkerServer(int port) {
        this.port = port;
        this.processor = new XMLProcessor();
    }

    /**
     * サーバーを起動
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // タスク処理エンドポイント
        server.createContext("/process", new ProcessTaskHandler());

        // ヘルスチェックエンドポイント
        server.createContext("/health", new HealthCheckHandler());

        server.setExecutor(null); // デフォルトのエグゼキューターを使用
        server.start();

        logger.info("ワーカーサーバーを起動しました: ポート " + port);
    }

    /**
     * サーバーを停止
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("ワーカーサーバーを停止しました");
        }
    }

    /**
     * タスク処理ハンドラー
     */
    private class ProcessTaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\": \"Method Not Allowed\"}");
                return;
            }

            try {
                // リクエストボディを読み込み
                InputStream is = exchange.getRequestBody();
                String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                is.close();

                // JSONをProcessingTaskにデシリアライズ
                ProcessingTask task = gson.fromJson(requestBody, ProcessingTask.class);

                logger.info(String.format("タスクを受信しました: ID=%s, ファイル=%s",
                        task.getTaskId(), task.getFilePath()));

                // XML処理を実行
                ProcessingResult result = processor.processXMLFile(task);

                // 結果をJSONに変換して返す
                String jsonResponse = gson.toJson(result);
                sendResponse(exchange, 200, jsonResponse);

                logger.info(String.format("タスク処理完了: ID=%s, 成功=%s",
                        task.getTaskId(), result.isSuccess()));

            } catch (Exception e) {
                logger.log(Level.SEVERE, "タスク処理中にエラーが発生しました", e);
                String errorJson = String.format("{\"error\": \"%s\"}", e.getMessage());
                sendResponse(exchange, 500, errorJson);
            }
        }
    }

    /**
     * ヘルスチェックハンドラー
     */
    private class HealthCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\": \"ok\"}";
            sendResponse(exchange, 200, response);
        }
    }

    /**
     * HTTPレスポンスを送信
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    /**
     * メインメソッド
     */
    public static void main(String[] args) {
        try {
            // プロパティファイルから設定を読み込み
            Properties properties = new Properties();
            Path configPath = Paths.get("config-worker.properties");

            if (Files.exists(configPath)) {
                properties.load(Files.newInputStream(configPath));
            } else {
                logger.warning("config-worker.propertiesが見つかりません。デフォルト設定を使用します。");
                // デフォルト設定
                properties.setProperty("worker.port", "8080");
            }

            // ポート番号の取得
            int port = Integer.parseInt(properties.getProperty("worker.port", "8080"));

            // ワーカーサーバーの起動
            WorkerServer worker = new WorkerServer(port);
            worker.start();

            // シャットダウンフック
            Runtime.getRuntime().addShutdownHook(new Thread(worker::stop));

            logger.info("ワーカーサーバーが稼働中です。終了するには Ctrl+C を押してください。");

            // サーバーを無限に実行
            Thread.currentThread().join();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "エラーが発生しました", e);
            System.exit(1);
        }
    }
}
