-- =============================================
-- AI 虛擬換裝任務表
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_tryon_task` (
    `task_id`        BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '任務ID',
    `user_id`        BIGINT       NOT NULL                 COMMENT '用戶ID',
    `photo_url`      VARCHAR(500) NOT NULL                 COMMENT '用戶上傳原圖 URL (MinIO)',
    `clothes_id`     BIGINT       NOT NULL                 COMMENT '選中的衣服ID',
    `clothes_url`    VARCHAR(500) NOT NULL                 COMMENT '衣服圖片 URL',
    `status`         TINYINT      NOT NULL DEFAULT 0       COMMENT '狀態: 0=排隊 1=處理中 2=完成 3=失敗',
    `result_url`     VARCHAR(500)                          COMMENT 'AI 生成後圖片 URL (MinIO)',
    `gpu_duration_ms` BIGINT                               COMMENT 'GPU 消耗時長(毫秒)',
    `error_msg`      VARCHAR(1000)                         COMMENT '失敗原因',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`       CHAR(1)      NOT NULL DEFAULT '0'     COMMENT '刪除標誌(0存在 2刪除)',
    PRIMARY KEY (`task_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 虛擬換裝任務';

-- =============================================
-- 衣服/商品表（Demo 資料）
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_clothes_item` (
    `clothes_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '衣服ID',
    `name`         VARCHAR(200) NOT NULL                COMMENT '衣服名稱',
    `category`     VARCHAR(50)                          COMMENT '類型: top/bottom/dress',
    `image_url`    VARCHAR(500) NOT NULL                COMMENT '衣服圖片 URL',
    `brand`        VARCHAR(100)                         COMMENT '品牌',
    `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '1=上架 0=下架',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`clothes_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 換裝衣服庫';

-- 示範衣服資料
INSERT INTO `ai_clothes_item` (`name`, `category`, `image_url`, `brand`) VALUES
('白色棉質T恤', 'top', 'https://api.deepay.srl/profile/clothes/white-tshirt.jpg', 'Deepay'),
('黑色連身裙', 'dress', 'https://api.deepay.srl/profile/clothes/black-dress.jpg', 'Deepay'),
('藍色牛仔褲', 'bottom', 'https://api.deepay.srl/profile/clothes/blue-jeans.jpg', 'Deepay');
