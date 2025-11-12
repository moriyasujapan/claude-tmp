package com.example.nfs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;

/**
 * LiveUtils - 17LIVE API interaction utility class
 * Ported from PHP to Java
 */
public class LiveUtils {
    private static final String VERSION = "3.315.0";

    private String liveID;
    private String loginID;
    private String deviceID;
    private Map<String, String> headers;
    private Map<String, String> streamerInfo;
    private HikariDataSource dataSource;
    private ProxySettings proxy;
    private String cookiePath;
    private CookieManager cookieManager;
    private HttpClient httpClient;
    private Gson gson;

    /**
     * Constructor
     * @param liveID The live stream ID
     */
    public LiveUtils(String liveID) {
        this.liveID = liveID;
        this.headers = new HashMap<>();
        this.streamerInfo = new HashMap<>();
        this.gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();

        getProxy();
        pdoSetup();
        initializeHttpClient();
    }

    /**
     * Initialize HTTP client with cookie management and proxy
     */
    private void initializeHttpClient() {
        this.cookieManager = new CookieManager();
        this.cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .cookieHandler(cookieManager);

        // Add proxy if configured
        if (proxy != null && proxy.host != null && !proxy.host.isEmpty()) {
            InetSocketAddress proxyAddress = new InetSocketAddress(proxy.host, proxy.port);
            builder.proxy(ProxySelector.of(proxyAddress));

            // Add proxy authentication if needed
            if (proxy.auth != null && !proxy.auth.isEmpty()) {
                String[] authParts = proxy.auth.split(":");
                if (authParts.length == 2) {
                    final String username = authParts[0];
                    final String password = authParts[1];

                    builder.authenticator(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password.toCharArray());
                        }
                    });
                }
            }
        }

        this.httpClient = builder.build();
    }

    /**
     * Setup database connection pool
     */
    private void pdoSetup() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/bot?useSSL=false&serverTimezone=UTC");
        config.setUsername("bot");
        config.setPassword("bot");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        try {
            this.dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Get database connection from pool
     */
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Load proxy settings from proxy.txt
     */
    private void getProxy() {
        try {
            Path proxyFile = Paths.get(System.getProperty("user.dir"), "proxy.txt");
            if (Files.exists(proxyFile)) {
                List<String> lines = Files.readAllLines(proxyFile);
                if (lines.size() >= 3) {
                    proxy = new ProxySettings();
                    proxy.host = lines.get(0).trim();
                    proxy.port = Integer.parseInt(lines.get(1).trim());
                    proxy.auth = lines.get(2).trim();
                    System.out.println("Proxy loaded: " + proxy);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load proxy settings: " + e.getMessage());
            proxy = new ProxySettings(); // Use empty proxy settings
        }
    }

    /**
     * Refresh authentication token
     */
    public void refresh(String loginID) throws Exception {
        this.loginID = loginID;
        this.deviceID = generateDeviceID();

        // Load stored headers
        Path headerFile = Paths.get(System.getProperty("user.dir"), "headers", loginID + ".headers");
        if (Files.exists(headerFile)) {
            loadHeaders(headerFile);
        } else {
            throw new FileNotFoundException("Header file not found for loginID: " + loginID);
        }

        String refreshToken = headers.get("refreshToken");
        String url = "https://wap-api.17app.co/api/v1/auth/refresh";

        Map<String, String> params = new HashMap<>();
        params.put("refreshToken", refreshToken);

        String response = postWithJson(url, headers, params);
        System.out.println(response);

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        String jwtAccessToken = json.get("jwtAccessToken").getAsString();
        String newRefreshToken = json.get("refreshToken").getAsString();

        headers.put("jwtAccessToken", jwtAccessToken);
        headers.put("refreshToken", newRefreshToken);
        headers.put("authorization", "Bearer " + jwtAccessToken);

        selfInfo();
        saveHeaders(headerFile);
    }

    /**
     * Login to 17LIVE
     */
    public void login(String loginID, String password, String deviceID) throws Exception {
        this.loginID = loginID;
        this.deviceID = deviceID;

        Map<String, Object> params = new HashMap<>();
        params.put("openID", loginID);
        params.put("password", password);
        params.put("twitterAccessTokenSecret", "");
        params.put("appsflyerID", "1759687482909-8748127");

        String response = postWithJson("https://api-dsa.17app.co/api/v1/auth/loginAction2", new HashMap<>(), params);
        System.out.println(response);

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonObject data = JsonParser.parseString(json.get("data").getAsString()).getAsJsonObject();
        JsonObject userInfo = data.getAsJsonObject("userInfo");

        headers.put("userID", userInfo.get("userID").getAsString());
        headers.put("deviceID", deviceID);
        headers.put("jwtAccessToken", data.get("jwtAccessToken").getAsString());
        headers.put("refreshToken", data.get("refreshToken").getAsString());
        headers.put("accessToken", data.get("accessToken").getAsString());
        headers.put("user-agent", "Story17/" + VERSION + "(iPhone; iOS 14.4.2; Scale/3.00)");
        headers.put("deviceType", "IOS");
        headers.put("deviceName", "iPhone");
        headers.put("packageName", "com.machipopo.story17");
        headers.put("authorization", "Bearer " + data.get("jwtAccessToken").getAsString());

        selfInfo();
        getCookie();

        Path headerFile = Paths.get(System.getProperty("user.dir"), "headers", loginID + ".headers");
        Files.createDirectories(headerFile.getParent());
        saveHeaders(headerFile);

        getStreamerInfo();
    }

    /**
     * Get cookie by making initial requests
     */
    private void getCookie() throws Exception {
        get("https://wap-api.17app.co/api/v1/sections?&count=6", headers);
        get("https://api-dsa.17app.co/api/v1/sections?&count=6", headers);
    }

    /**
     * Get self user information
     */
    private void selfInfo() throws Exception {
        long time = (System.currentTimeMillis() - 300000) / 100;
        String timestamp = time + ".000";

        String dataTemplate = "{\"userID\":\"%s\",\"version\":\"%s\",\"accessToken\":\"%s\"," +
                "\"nonce\":\"1AF44A7E-6A90-46AE-8B6D-FC7919721442\",\"profileDisplayInfo\":1," +
                "\"ipCountry\":\"JP\",\"deviceType\":\"IOS\",\"packageName\":\"com.machipopo.story17\"," +
                "\"action\":\"getSelfInfo\",\"Authorization\":\"Bearer %s\",\"language\":\"EN\"," +
                "\"OSVersion\":\"14.4.2\",\"deviceID\":\"%s\",\"hardware\":\"iPhone12,5\"," +
                "\"prevTimeStamp\":%s}";

        String data = String.format(dataTemplate,
                headers.get("userID"),
                VERSION,
                headers.get("accessToken"),
                headers.get("jwtAccessToken"),
                deviceID,
                timestamp);

        Map<String, Object> params = new HashMap<>();
        params.put("key", "");
        params.put("cypher", "0_v2");
        params.put("data", data);

        postWithJson("https://wap-api.17app.co/apiGateWay", headers, params);
    }

    /**
     * Get streamer information
     */
    private void getStreamerInfo() throws Exception {
        String url = String.format("https://wap-api.17app.co/api/v1/lives/%s", liveID);
        String response = get(url, headers);

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonObject userInfo = json.getAsJsonObject("userInfo");

        streamerInfo.put("openID", userInfo.get("openID").getAsString());
        streamerInfo.put("roomID", userInfo.get("roomID").getAsString());
        streamerInfo.put("userID", userInfo.get("userID").getAsString());
        streamerInfo.put("level", userInfo.get("level").getAsString());
        streamerInfo.put("deviceType", userInfo.get("deviceType").getAsString());
        streamerInfo.put("age", userInfo.get("age").getAsString());
        streamerInfo.put("bio", userInfo.get("bio").getAsString());
    }

    /**
     * Check follow status
     */
    private boolean getFollowStatus() throws Exception {
        getStreamerInfo();

        Map<String, Object> params = new HashMap<>();
        params.put("targetUserIDs", Arrays.asList(streamerInfo.get("userID")));

        String url = String.format("https://wap-api.17app.co/api/v1/users/%s/getFollowStatus", headers.get("userID"));
        String response = postWithJson(url, headers, params);

        JsonObject[] statuses = gson.fromJson(response, JsonObject[].class);
        return statuses.length > 0 && statuses[0].get("status").getAsInt() == 1;
    }

    /**
     * Follow a streamer
     */
    public void follow() {
        try {
            if (getFollowStatus()) {
                return;
            }

            String dataTemplate = "{\"userID\":\"%s\",\"version\":\"%s\",\"accessToken\":\"%s\"," +
                    "\"nonce\":\"4D9B2F52-49FB-4F19-B65B-CB7CD0F7CABD\",\"ipCountry\":\"JP\"," +
                    "\"deviceType\":\"IOS\",\"packageName\":\"com.machipopo.story17\"," +
                    "\"action\":\"followUserAction\",\"Authorization\":\"Bearer %s\"," +
                    "\"language\":\"EN\",\"OSVersion\":\"14.4.2\",\"deviceID\":\"%s\"," +
                    "\"targetUserID\":\"%s\",\"hardware\":\"iPhone12,5\"}";

            String data = String.format(dataTemplate,
                    headers.get("userID"),
                    VERSION,
                    headers.get("accessToken"),
                    headers.get("jwtAccessToken"),
                    deviceID,
                    streamerInfo.get("userID"));

            Map<String, Object> params = new HashMap<>();
            params.put("key", "");
            params.put("cypher", "0_v2");
            params.put("data", data);

            postWithJson("https://wap-api.17app.co/apiGateWay", headers, params);
            reacts(3);
        } catch (Exception e) {
            System.out.println("Failed to follow: " + e.getMessage());
        }
    }

    /**
     * Send like/heart
     */
    public void like() {
        try {
            reacts(2);

            String dataTemplate = "{\"userID\":\"%s\",\"version\":\"%s\",\"accessToken\":\"%s\"," +
                    "\"nonce\":\"2996DA1F-40E3-4C77-8325-1E5BB4902E13\",\"ipCountry\":\"JP\"," +
                    "\"deviceType\":\"IOS\",\"packageName\":\"com.machipopo.story17\"," +
                    "\"likeCount\":\"1\",\"action\":\"likeLivestreamBatchUpdate\"," +
                    "\"Authorization\":\"Bearer %s\",\"language\":\"EN\",\"OSVersion\":\"15.6.1\"," +
                    "\"liveStreamID\":\"%s\",\"absTimestamp\":\"0\",\"deviceID\":\"%s\"," +
                    "\"hardware\":\"iPhone14,3\"}";

            String data = String.format(dataTemplate,
                    headers.get("userID"),
                    VERSION,
                    headers.get("accessToken"),
                    headers.get("jwtAccessToken"),
                    liveID,
                    deviceID);

            Map<String, Object> params = new HashMap<>();
            params.put("key", "");
            params.put("cypher", "0_v2");
            params.put("data", data);

            postWithJson("https://api-dsa.17app.co/apiGateWay", headers, params);
        } catch (Exception e) {
            System.out.println("Failed to like: " + e.getMessage());
        }
    }

    /**
     * Enter live stream
     */
    public void enter() throws Exception {
        String url = String.format("https://wap-api.17app.co/api/v1/lives/%s/enter", liveID);
        postWithJson(url, headers, null);
    }

    /**
     * Quit live stream
     */
    public void quit() throws Exception {
        String url = String.format("https://wap-api.17app.co/api/v1/lives/%s/quit", liveID);
        Map<String, Object> params = new HashMap<>();
        params.put("duration", 345);
        postWithJson(url, headers, params);
    }

    /**
     * Send labor reward (ALE)
     */
    public void ale() throws Exception {
        String url = String.format("https://wap-api.17app.co/api/v1/lives/%s/laborReward", liveID);
        Map<String, Object> params = new HashMap<>();
        params.put("type", 1);

        String result = postWithJson(url, headers, params);

        if (result.contains("block")) {
            getStreamerInfo();
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO ale_err_log(openID) VALUES(?)")) {
                stmt.setString(1, streamerInfo.get("openID"));
                stmt.execute();
            }
        }

        System.out.println("----------- ale result ----------");
        System.out.println(headers);
        System.out.println(result);
        System.out.println("----------- ale result ----------");
    }

    /**
     * Send reaction
     */
    public void reacts(int type) throws Exception {
        String url = String.format("https://wap-api.17app.co/api/v1/lives/%s/reacts", liveID);
        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        postWithJson(url, headers, params);
    }

    /**
     * Share
     */
    public void share() throws Exception {
        reacts(1);
    }

    /**
     * Facebook share
     */
    public void fbShare() throws Exception {
        reacts(0);
    }

    /**
     * Twitter share
     */
    public void twShare() throws Exception {
        reacts(4);
    }

    /**
     * Send comment
     */
    public void comment(String comment) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("userID", headers.get("userID"));
        params.put("version", VERSION);
        params.put("accessToken", headers.get("accessToken"));
        params.put("nonce", "60E92200-81D2-46D4-A5AD-572068A955A1");
        params.put("commentType", 0);
        params.put("ipCountry", "JP");
        params.put("deviceType", "IOS");
        params.put("packageName", "com.machipopo.story17");
        params.put("comment", comment);
        params.put("Authorization", "Bearer " + headers.get("jwtAccessToken"));
        params.put("language", "EN");
        params.put("OSVersion", "14.4.2");
        params.put("absTimestamp", "0");
        params.put("deviceID", deviceID);
        params.put("hardware", "iPhone12,5");

        String url = String.format("https://wap-api.17app.co/api/v1/lives/%s/comments", liveID);
        postWithJson(url, headers, params);
    }

    /**
     * Like a post
     */
    public void likePost(String postID) throws Exception {
        getPostInfo(postID);
        String url = String.format("https://wap-api.17app.co/api/v1/posts/%s/like", postID);
        postWithJson(url, headers, new HashMap<>());
    }

    /**
     * Get post information
     */
    public void getPostInfo(String postID) throws Exception {
        String url = String.format("https://wap-api.17app.co/api/v1/posts/%s/view", postID);
        postWithJson(url, headers, new HashMap<>());
    }

    /**
     * Share a post
     */
    public void sharePost(String postID) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("postID", postID);
        params.put("picture", "");
        params.put("caption", "シェア");
        params.put("type", "image");
        params.put("userTags", new ArrayList<>());
        params.put("platform", "MEDIA17");

        System.out.println("\n" + headers.get("userID") + " has shared the post at " + postID + "!!");
        System.out.println(gson.toJson(params));

        String result = postWithJson("https://wap-api.17app.co/api/v1/post/sharePost", headers, params);
        System.out.println(result);
    }

    /**
     * Comment on a post
     */
    public void commentPost(String postID, String comment) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("userID", headers.get("userID"));
        data.put("version", VERSION);
        data.put("accessToken", headers.get("accessToken"));
        data.put("nonce", "6E01EEDD-2BE8-48B3-ACE4-4F527ABFF9F9");
        data.put("postID", postID);
        data.put("ipCountry", "JP");
        data.put("deviceType", "IOS");
        data.put("packageName", "com.machipopo.story17");
        data.put("comment", comment);
        data.put("userTags", "[]");
        data.put("Authorization", "Bearer " + headers.get("jwtAccessToken"));
        data.put("language", "EN");
        data.put("action", "commentPost");
        data.put("OSVersion", "26.1");
        data.put("deviceID", deviceID);
        data.put("hardware", "iPhone16,2");

        Map<String, Object> params = new HashMap<>();
        params.put("key", "");
        params.put("data", gson.toJson(data));
        params.put("cypher", "0_v2");

        String result = postWithJson("https://wap-api.17app.co/apiGateWay", headers, params);
        System.out.println(result);
    }

    /**
     * Get post comments
     */
    public String getPostComments(String postID) throws Exception {
        String url = String.format("https://wap-api.17app.co/api/v1/posts/%s/comments?orderBy=2", postID);
        return get(url, headers);
    }

    /**
     * Delete post comments
     */
    public void deletePostComment(String postID, List<String> commentIDs) throws Exception {
        String url = String.format("https://wap-api.17app.co/api/v1/posts/%s/comments", postID);
        Map<String, Object> params = new HashMap<>();
        params.put("commentIDs", commentIDs);
        curlWithJson("DELETE", url, headers, params);
    }

    /**
     * Send gift
     */
    public void sendGift(String itemID, String roomID) throws Exception {
        String url = String.format("https://wap-api.17app.co/api/v1/lives/%s/gift", roomID);
        Map<String, Object> params = new HashMap<>();
        params.put("srcID", roomID);
        params.put("giftID", itemID);
        params.put("srcType", 1);
        postWithJson(url, headers, params);
    }

    /**
     * Send free gift
     */
    public void sendFreeGift() throws Exception {
        String itemID = selectFreeGift();
        if (itemID == null) {
            return;
        }

        String url = String.format("https://wap-api.17app.co/api/v1/baggage/item/%s", itemID);
        Map<String, Object> params = new HashMap<>();
        params.put("livestreamid", liveID);
        postWithJson(url, headers, params);
    }

    /**
     * Select a free gift from baggage
     */
    private String selectFreeGift() throws Exception {
        String response = get("https://wap-api.17app.co/api/v1/baggage", headers);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (json.has("baggage") && json.get("baggage").isJsonArray()) {
            for (var bag : json.getAsJsonArray("baggage")) {
                JsonObject bagObj = bag.getAsJsonObject();
                if (bagObj.has("items") && bagObj.get("items").isJsonArray()) {
                    for (var item : bagObj.getAsJsonArray("items")) {
                        return item.getAsJsonObject().get("itemID").getAsString();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Find free gift missions
     */
    public void findFreeGift() throws Exception {
        String response = get("https://wap-api.17app.co/api/v1/missions/overview?entranceType=1", headers);
        JsonObject[] missions = gson.fromJson(response, JsonObject[].class);

        for (JsonObject mission : missions) {
            Map<String, Object> params = new HashMap<>();
            params.put("ID", mission.get("ID").getAsString());
            postWithJson("https://wap-api.17app.co/api/v1/missions", headers, params);
        }
    }

    /**
     * Poke back
     */
    public void pokeback() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("isPokeBack", true);
        params.put("srcID", liveID);
        params.put("userID", headers.get("userID"));

        System.out.println(params);
        String result = postWithJson("https://wap-api.17app.co/api/v1/pokes", headers, params);
        System.out.println(result);
        System.out.println("--");
    }

    /**
     * Add reminder
     */
    public void addReminder(long timestamp) throws Exception {
        get("https://wap-api.17app.co/api/v1/config/streamer", headers);

        Map<String, Object> params = new HashMap<>();
        params.put("armyOnly", false);
        params.put("timestamp", timestamp);
        params.put("scheduleID", "");

        System.out.println(params);
        String url = String.format("https://wap-api.17app.co/api/v1/users/%s/reminder", headers.get("userID"));
        String result = putWithJson(url, headers, params);
        System.out.println(result);
    }

    /**
     * HTTP GET request
     */
    private String get(String url, Map<String, String> headers) throws Exception {
        return get(url, headers, false);
    }

    private String get(String url, Map<String, String> headers, boolean debug) throws Exception {
        Map<String, String> allHeaders = addDefaultHeaders(headers);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(30));

        for (Map.Entry<String, String> entry : allHeaders.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (debug) {
            System.out.println("------- " + url + " start ---------");
            System.out.println("Request headers: " + allHeaders);
            System.out.println("Result: " + response.body());
            System.out.println("------- " + url + " end ---------");
        }

        return response.body();
    }

    /**
     * HTTP POST request with JSON body
     */
    private String postWithJson(String url, Map<String, String> headers, Object params) throws Exception {
        return postWithJson(url, headers, params, false);
    }

    private String postWithJson(String url, Map<String, String> headers, Object params, boolean debug) throws Exception {
        return curlWithJson("POST", url, headers, params, debug);
    }

    /**
     * HTTP PUT request with JSON body
     */
    private String putWithJson(String url, Map<String, String> headers, Object params) throws Exception {
        return putWithJson(url, headers, params, false);
    }

    private String putWithJson(String url, Map<String, String> headers, Object params, boolean debug) throws Exception {
        return curlWithJson("PUT", url, headers, params, debug);
    }

    /**
     * HTTP request with JSON body (generic method)
     */
    private String curlWithJson(String method, String url, Map<String, String> headers, Object params) throws Exception {
        return curlWithJson(method, url, headers, params, false);
    }

    private String curlWithJson(String method, String url, Map<String, String> headers, Object params, boolean debug) throws Exception {
        Map<String, String> allHeaders = addDefaultHeaders(headers);
        allHeaders.put("Content-Type", "application/json");
        allHeaders.put("Accept", "application/json");

        String jsonBody = params != null ? gson.toJson(params) : "{}";

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30));

        // Set method and body
        switch (method.toUpperCase()) {
            case "POST":
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
                break;
            case "PUT":
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
                break;
            case "DELETE":
                requestBuilder.method("DELETE", HttpRequest.BodyPublishers.ofString(jsonBody));
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        // Add headers
        for (Map.Entry<String, String> entry : allHeaders.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (debug) {
            System.out.println("------- " + url + " start ---------");
            System.out.println("Request headers: " + allHeaders);
            System.out.println("Request body: " + jsonBody);
            System.out.println("Result: " + response.body());
            System.out.println("------- " + url + " end ---------");
        }

        return response.body();
    }

    /**
     * Download file
     */
    public void download(String url, Map<String, String> headers, String savePath) throws Exception {
        Map<String, String> allHeaders = addDefaultHeaders(headers);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofMinutes(5));

        for (Map.Entry<String, String> entry : allHeaders.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<Path> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofFile(Paths.get(savePath)));

        System.out.println("Downloaded to: " + response.body());
    }

    /**
     * Add default headers to request
     */
    private Map<String, String> addDefaultHeaders(Map<String, String> headers) {
        Map<String, String> allHeaders = new HashMap<>(headers);

        allHeaders.putIfAbsent("User-Agent", "17LIVE/4 CFNetwork/1335.0.3 Darwin/21.6.0");
        allHeaders.putIfAbsent("DeviceType", "IOS");
        allHeaders.putIfAbsent("DeviceName", "iPhone");
        allHeaders.putIfAbsent("packageName", "com.machipopo.story17");
        allHeaders.putIfAbsent("debug-level", "0");
        allHeaders.putIfAbsent("version", VERSION);
        allHeaders.putIfAbsent("Cache-Control", "no-cache");
        allHeaders.putIfAbsent("hardware", "iPhone14,3");
        allHeaders.putIfAbsent("Accept-Encoding", "gzip, deflate, br");
        allHeaders.putIfAbsent("guest-uuid", "00000000-0000-0000-0000-000000000000");
        allHeaders.putIfAbsent("language", "EN");
        allHeaders.putIfAbsent("advertisementID", "00000000-0000-0000-0000-000000000000");
        allHeaders.putIfAbsent("userIPRegion", "JP");
        allHeaders.putIfAbsent("deviceID", deviceID != null ? deviceID : "");
        allHeaders.putIfAbsent("accessToken", "");
        allHeaders.putIfAbsent("authorization", "Bearer ");
        allHeaders.putIfAbsent("userSelectedRegion", "JP");
        allHeaders.putIfAbsent("OSVersion", "15.6.1");

        return allHeaders;
    }

    /**
     * Generate random device ID (MD5 hash)
     */
    private String generateDeviceID() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(8);
        Random random = ThreadLocalRandom.current();

        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate device ID", e);
        }
    }

    /**
     * Save headers to file
     */
    private void saveHeaders(Path file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file.toFile()))) {
            oos.writeObject(headers);
        }
    }

    /**
     * Load headers from file
     */
    @SuppressWarnings("unchecked")
    private void loadHeaders(Path file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file.toFile()))) {
            headers = (Map<String, String>) ois.readObject();
        }
    }

    // Getters
    public Map<String, String> getStreamerInfos() {
        return streamerInfo;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getLoginID() {
        return loginID;
    }

    /**
     * Close resources
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Proxy settings holder
     */
    private static class ProxySettings {
        String host;
        int port;
        String auth;

        @Override
        public String toString() {
            return "ProxySettings{host='" + host + "', port=" + port + ", auth='" + auth + "'}";
        }
    }
}
