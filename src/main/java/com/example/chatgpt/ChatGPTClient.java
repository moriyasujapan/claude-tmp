package com.example.chatgpt;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * OpenAI ChatGPT APIクライアント
 * Chat Completions APIを使用してChatGPTと対話します
 */
public class ChatGPTClient {

    private static final String API_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;
    private final String model;

    /**
     * ChatGPTClientのコンストラクタ
     *
     * @param apiKey OpenAI APIキー
     */
    public ChatGPTClient(String apiKey) {
        this(apiKey, "gpt-3.5-turbo");
    }

    /**
     * ChatGPTClientのコンストラクタ（モデル指定版）
     *
     * @param apiKey OpenAI APIキー
     * @param model 使用するモデル（例: "gpt-3.5-turbo", "gpt-4"）
     */
    public ChatGPTClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
    }

    /**
     * ChatGPTに質問を送信して回答を取得します
     *
     * @param message ユーザーからの質問
     * @return ChatGPTからの回答
     * @throws Exception API呼び出しでエラーが発生した場合
     */
    public String sendMessage(String message) throws Exception {
        // リクエストボディの作成
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);

        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", message);
        messages.add(userMessage);

        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 2000);

        // HTTPリクエストの作成
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        // APIリクエストの送信
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        // レスポンスの処理
        if (response.statusCode() != 200) {
            throw new RuntimeException("API Error: " + response.statusCode() +
                    " - " + response.body());
        }

        // レスポンスから回答を抽出
        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        return responseJson.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }

    /**
     * システムプロンプトを指定して質問を送信します
     *
     * @param systemPrompt システムプロンプト（ChatGPTの役割や振る舞いを定義）
     * @param userMessage ユーザーからの質問
     * @return ChatGPTからの回答
     * @throws Exception API呼び出しでエラーが発生した場合
     */
    public String sendMessageWithSystem(String systemPrompt, String userMessage) throws Exception {
        // リクエストボディの作成
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);

        JsonArray messages = new JsonArray();

        // システムメッセージ
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);

        // ユーザーメッセージ
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 2000);

        // HTTPリクエストの作成
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        // APIリクエストの送信
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        // レスポンスの処理
        if (response.statusCode() != 200) {
            throw new RuntimeException("API Error: " + response.statusCode() +
                    " - " + response.body());
        }

        // レスポンスから回答を抽出
        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        return responseJson.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }
}
