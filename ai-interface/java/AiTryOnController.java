package com.ruoyi.system.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.domain.bo.AiTryOnBo;
import com.ruoyi.system.domain.vo.AiTryOnResultVo;
import com.ruoyi.system.service.impl.AiTryOnServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * AI 虛擬換裝接口
 *
 * API 協議：
 *   POST /ai/tryon/submit        → 提交換裝（同步，等待結果）
 *   POST /ai/tryon/submitAsync   → 提交換裝（異步，返回 taskId）
 *   GET  /ai/tryon/result/{id}   → 查詢任務結果（前端輪詢）
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/tryon")
public class AiTryOnController {

    private final AiTryOnServiceImpl aiTryOnService;

    /**
     * 同步換裝 —— 直接等 AI 返回結果
     *
     * 請求體:
     * {
     *   "photoUrl": "https://api.deepay.srl/profile/upload/user-photo.jpg",
     *   "clothesId": 1
     * }
     *
     * 響應:
     * {
     *   "code": 200,
     *   "data": {
     *     "status": 2,
     *     "statusDesc": "完成",
     *     "resultUrl": "https://api.deepay.srl/profile/ai-results/xxx.jpg",
     *     "gpuDurationMs": 45000
     *   }
     * }
     */
    @PostMapping("/submit")
    public R<AiTryOnResultVo> submit(@Valid @RequestBody AiTryOnBo bo) {
        AiTryOnResultVo result = aiTryOnService.submitTryOn(bo);
        return R.ok(result);
    }

    /**
     * 異步換裝 —— 立即返回 taskId，前端自行輪詢
     *
     * 適合耗時 >30s 的場景，配合進度條 / SSE 推送使用
     */
    @PostMapping("/submitAsync")
    public R<Long> submitAsync(@Valid @RequestBody AiTryOnBo bo) {
        Long taskId = aiTryOnService.submitTryOnAsync(bo);
        return R.ok(taskId);
    }

    /**
     * 查詢換裝結果（前端輪詢 or SSE 觸發後驗證）
     */
    @GetMapping("/result/{taskId}")
    public R<AiTryOnResultVo> queryResult(@PathVariable Long taskId) {
        AiTryOnResultVo result = aiTryOnService.queryResult(taskId);
        return R.ok(result);
    }
}
