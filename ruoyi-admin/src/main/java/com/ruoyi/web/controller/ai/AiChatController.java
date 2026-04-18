package com.ruoyi.web.controller.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI 智能助手控制器
 * 接入 Deepseek API（OpenAI 兼容格式）
 */
@RestController
@RequestMapping("/ai")
public class AiChatController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            你是 DeepPay 智能管理系統的 AI 助手。
            DeepPay 是一個基於 RuoYi 框架的企業管理系統，整合了 Swan 歐洲銀行 API（SEPA 支付），
            並包含：用戶管理、角色權限、菜單配置、操作日誌、服務監控等模組。
            
            你的職責：
            1. 解答用戶關於系統操作和功能的問題
            2. 根據用戶職位/需求推薦合適的角色和權限配置
            3. 分析操作模式，給出安全建議
            4. 協助配置 Swan 銀行相關功能
            5. 用繁體中文回答，保持簡潔專業
            
            當前系統菜單包括：系統管理（用戶/角色/菜單/部門/崗位/字典/參數/通知/日誌）、
            系統監控（在線用戶/任務調度/服務監控/緩存監控）、Swan銀行（賬戶/交易/IBAN/入網）。
            """;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @PostMapping("/chat")
    public AjaxResult chat(@RequestBody Map<String, Object> body) {
        String message = (String) body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return AjaxResult.error("消息不能為空");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return AjaxResult.error("AI 功能尚未配置，請在 application.yml 中設置 deepseek.api-key");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) body.get("history");

        try {
            String reply = callDeepseek(message, history);
            return AjaxResult.success(reply);
        } catch (Exception e) {
            log.error("AI chat error: {}", e.getMessage(), e);
            return AjaxResult.error("AI 服務暫時不可用：" + e.getMessage());
        }
    }

    private String callDeepseek(String userMessage, List<Map<String, String>> history) throws IOException, InterruptedException {
        JSONArray messages = new JSONArray();

        // 系統提示
        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", SYSTEM_PROMPT);
        messages.add(sysMsg);

        // 歷史消息（最多保留最近10條）
        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                Map<String, String> h = history.get(i);
                JSONObject msg = new JSONObject();
                msg.put("role", h.get("role"));
                msg.put("content", h.get("content"));
                messages.add(msg);
            }
        }

        // 用戶當前消息
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        // 構建請求體
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 1024);
        requestBody.put("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Deepseek API error {}: {}", response.statusCode(), response.body());
            throw new IOException("API 返回錯誤碼: " + response.statusCode());
        }

        JSONObject result = JSON.parseObject(response.body());
        return result.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }
}
