package common.campuslife.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import common.cn.kafei.simukraft.SimuKraft;

import java.net.URI;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MiMo-V2.5 异步LLM客户端。
 * 线程安全，所有调用异步执行，不阻塞游戏线程。
 * 30秒超时，3次连续失败触发降级。
 */
public final class LLMClient {

    private static final String API_URL = "https://api.xiaomimimo.com/v1/chat/completions";
    private static final String API_KEY = "sk-c77rfuu2v5v1zlyp0mpjvyx737o093ixhjrcj57j1kaal2eo";
    private static final String MODEL = "mimo-v2.5";
    private static final Duration TIMEOUT = Duration.ofSeconds(150);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 9674)))
            .build();

    private static final Gson GSON = new Gson();
    private static final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private static volatile boolean degradedMode = false;

    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[^{}]*\\}", Pattern.DOTALL);

    private LLMClient() {}

    /**
     * 异步调用LLM，返回CompletableFuture<JsonObject>。
     * 调用方用 .thenAccept() / .exceptionally() 处理结果。
     */
    public static CompletableFuture<JsonObject> chatAsync(String systemPrompt, String userPrompt) {
        if (degradedMode) {
            SimuKraft.LOGGER.debug("LLM: Skipped call (degraded mode)");
            return CompletableFuture.completedFuture(null);
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL);
        requestBody.addProperty("max_tokens", 8192);
        requestBody.addProperty("temperature", 0.8);

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);

        var messages = new com.google.gson.JsonArray();
        messages.add(systemMsg);
        messages.add(userMsg);
        requestBody.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .build();

        SimuKraft.LOGGER.info("LLM: Sending request to MiMo-V2.5 ({} bytes)", GSON.toJson(requestBody).length());

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    SimuKraft.LOGGER.info("LLM: Received response status {} ({} bytes)", response.statusCode(), response.body().length());
                    if (response.statusCode() == 200) {
                        consecutiveFailures.set(0);
                        if (degradedMode) {
                            degradedMode = false;
                            SimuKraft.LOGGER.info("LLM: Connection restored, exiting degraded mode");
                        }
                        JsonObject respJson = JsonParser.parseString(response.body()).getAsJsonObject();
                        JsonObject message = respJson.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message");

                        // MiMo-V2.5: content 是最终答案, reasoning_content 是思维链
                        // 只取 content, 不取 reasoning_content
                        String content = "";
                        if (message.has("content") && !message.get("content").isJsonNull()) {
                            content = message.get("content").getAsString();
                        }

                        if (content == null || content.isBlank()) {
                            // content 为空可能是因为 max_tokens 被 reasoning 消耗了
                            SimuKraft.LOGGER.warn("LLM: content is empty, reasoning_content was used. Need more max_tokens.");
                            throw new RuntimeException("LLM returned empty content (reasoning consumed tokens)");
                        }

                        SimuKraft.LOGGER.info("LLM: Content preview: {}", content.length() > 200 ? content.substring(0, 200) + "..." : content);

                        // 清理并解析JSON
                        String cleaned = cleanJsonResponse(content);
                        JsonObject parsed = parseJsonLenient(cleaned);
                        if (parsed == null) {
                            throw new RuntimeException("LLM content is not valid JSON: " + content.substring(0, Math.min(100, content.length())));
                        }
                        return parsed;
                    } else {
                        throw new RuntimeException("LLM API returned " + response.statusCode() + ": " + response.body().substring(0, Math.min(200, response.body().length())));
                    }
                })
                .exceptionally(e -> {
                    int failures = consecutiveFailures.incrementAndGet();
                    String msg = e.getMessage();
                    if (e.getCause() != null) msg = e.getCause().getMessage();
                    SimuKraft.LOGGER.warn("LLM call failed ({}): {}", failures, msg);
                    if (failures >= 3 && !degradedMode) {
                        degradedMode = true;
                        SimuKraft.LOGGER.warn("LLM: Degraded mode activated after {} consecutive failures", failures);
                    }
                    return null;
                });
    }

    /**
     * 清理LLM返回中可能的markdown包裹。
     */
    private static String cleanJsonResponse(String content) {
        String trimmed = content.trim();
        // 移除 markdown 代码块
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    /**
     * 宽松JSON解析：尝试直接解析，失败则用正则提取第一个JSON对象。
     */
    private static JsonObject parseJsonLenient(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            JsonElement elem = JsonParser.parseString(text);
            if (elem.isJsonObject()) return elem.getAsJsonObject();
        } catch (Exception ignored) {}
        // 尝试从文本中提取 JSON 对象
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return JsonParser.parseString(matcher.group()).getAsJsonObject();
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static boolean isDegraded() {
        return degradedMode;
    }

    public static int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }
}
