package com.ruoyi.system.domain.vo;

import lombok.Data;

/**
 * AI 換裝任務響應 VO
 */
@Data
public class AiTryOnResultVo {

    /** 任務ID */
    private Long taskId;

    /** 狀態: 0=排隊 1=處理中 2=完成 3=失敗 */
    private Integer status;

    /** 狀態描述 */
    private String statusDesc;

    /** AI 生成後的圖片 URL */
    private String resultUrl;

    /** GPU 消耗時長（毫秒） */
    private Long gpuDurationMs;

    /** 失敗原因 */
    private String errorMsg;

    public String getStatusDesc() {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "排隊中";
            case 1 -> "處理中";
            case 2 -> "完成";
            case 3 -> "失敗";
            default -> "未知";
        };
    }
}
