package com.ruoyi.system.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.system.domain.bo.AiTryOnBo;
import com.ruoyi.system.domain.vo.AiTryOnResultVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 虛擬換裝服務
 *
 * 流程：
 * 1. 接收前端 photoUrl + clothesId
 * 2. 查詢衣服圖片 URL
 * 3. 呼叫 4090 服務器上的 Python AI 接口
 * 4. 把生成的圖片存入 MinIO
 * 5. 把任務記錄寫入資料庫
 * 6. 返回 taskId（前端輪詢用）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTryOnServiceImpl {

    /**
     * 4090 AI 服務地址（可透過內網或 frp 穿透訪問）
     * 示例：http://192.168.1.100:7860/api/tryon
     * 或透過 frp 穿透後：http://51.38.123.49:17860/api/tryon
     */
    @Value("${ai.tryon.endpoint:http://127.0.0.1:7860/api/tryon}")
    private String aiEndpoint;

    /** AI 接口超時時間（毫秒），換裝通常需要 30-120 秒 */
    @Value("${ai.tryon.timeout-ms:120000}")
    private int aiTimeoutMs;

    /**
     * 提交換裝任務（同步等待 AI 結果）
     *
     * @param bo 請求參數（photoUrl + clothesId）
     * @return 換裝結果 VO（含 resultUrl + gpuDurationMs）
     */
    public AiTryOnResultVo submitTryOn(AiTryOnBo bo) {
        long startTime = System.currentTimeMillis();

        // 1. 查詢衣服圖片 URL（實際項目替換為 DB 查詢）
        String clothesUrl = getClothesUrl(bo.getClothesId());

        // 2. 構建 AI 請求參數
        Map<String, Object> params = new HashMap<>();
        params.put("person_image_url", bo.getPhotoUrl());
        params.put("cloth_image_url", clothesUrl);
        params.put("cloth_type", "upper");   // upper/lower/dresses
        params.put("num_steps", 20);          // 推理步驟數（越高質量越好但越慢）
        params.put("guidance_scale", 2.0);

        log.info("[AI TryOn] 開始換裝請求 → clothesId={}, photoUrl={}", bo.getClothesId(), bo.getPhotoUrl());

        // 3. 呼叫 4090 Python AI 接口
        String responseBody;
        try {
            responseBody = HttpUtil.post(
                aiEndpoint,
                JSONUtil.toJsonStr(params),
                aiTimeoutMs
            );
        } catch (Exception e) {
            log.error("[AI TryOn] 呼叫 AI 接口失敗: {}", e.getMessage());
            AiTryOnResultVo vo = new AiTryOnResultVo();
            vo.setStatus(3);
            vo.setErrorMsg("AI 服務暫時不可用：" + e.getMessage());
            return vo;
        }

        long gpuDurationMs = System.currentTimeMillis() - startTime;

        // 4. 解析 AI 響應
        // 預期 Python 返回格式：{"success": true, "result_url": "http://..."}
        JSONObject result = JSONUtil.parseObj(responseBody);
        AiTryOnResultVo vo = new AiTryOnResultVo();

        if (result.getBool("success", false)) {
            String resultUrl = result.getStr("result_url");
            // 5. TODO: 如果 resultUrl 是臨時地址，下載後上傳到 MinIO
            // String minioUrl = minioService.uploadFromUrl(resultUrl, "ai-results/");
            vo.setStatus(2);
            vo.setResultUrl(resultUrl);
            vo.setGpuDurationMs(gpuDurationMs);
            log.info("[AI TryOn] 換裝成功 → resultUrl={}, gpu={}ms", resultUrl, gpuDurationMs);
        } else {
            vo.setStatus(3);
            vo.setErrorMsg(result.getStr("error", "AI 處理失敗"));
            log.warn("[AI TryOn] AI 返回失敗: {}", result.getStr("error"));
        }

        return vo;
    }

    /**
     * 非同步提交：把任務存 DB，讓 MQ/定時任務異步處理
     * 前端用 taskId 輪詢或 SSE 推送結果
     *
     * @return taskId
     */
    public Long submitTryOnAsync(AiTryOnBo bo) {
        // TODO: 把任務存入 ai_tryon_task 表（status=0 排隊中）
        // 然後用 @Async 或 RocketMQ 觸發 AI 調用
        // 這裡返回 taskId，前端用 taskId 輪詢
        log.info("[AI TryOn Async] 任務入隊 → clothesId={}", bo.getClothesId());
        return System.currentTimeMillis(); // 示意，實際替換為 DB insert 後返回的 id
    }

    /**
     * 查詢任務結果（前端輪詢用）
     */
    public AiTryOnResultVo queryResult(Long taskId) {
        // TODO: SELECT * FROM ai_tryon_task WHERE task_id = ?
        AiTryOnResultVo vo = new AiTryOnResultVo();
        vo.setTaskId(taskId);
        vo.setStatus(1); // 示意：處理中
        return vo;
    }

    // ==================== 私有方法 ====================

    private String getClothesUrl(Long clothesId) {
        // TODO: SELECT image_url FROM ai_clothes_item WHERE clothes_id = ?
        // 目前返回示範地址
        return "https://api.deepay.srl/profile/clothes/" + clothesId + ".jpg";
    }
}
