package com.ruoyi.system.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 換裝任務請求 BO
 * 前端傳入：用戶照片 URL + 衣服 ID
 */
@Data
public class AiTryOnBo {

    /** 用戶上傳的照片 URL（已上傳到 MinIO 後的地址） */
    @NotBlank(message = "照片URL不能為空")
    private String photoUrl;

    /** 選中的衣服 ID */
    @NotNull(message = "衣服ID不能為空")
    private Long clothesId;
}
