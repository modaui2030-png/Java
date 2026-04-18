#!/bin/bash
# 企業級全面修復腳本 v1.0
CD=/www/wwwroot/www.deepay.srl/core
PHP=/www/server/php/83/bin/php

echo "===== DEEPAY 企業級修復開始 ====="
echo ""

# ============================================================
# FIX 1: 建立 send_moneys 表 (修復 admin/dashboard 500)
# ============================================================
echo "[1/5] 建立 send_moneys 表..."
mysql -u ovovovov -povovovov ovovovov 2>/dev/null <<'SQL'
CREATE TABLE IF NOT EXISTS `send_moneys` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `sender_id` bigint(20) unsigned NOT NULL DEFAULT 0,
  `receiver_id` bigint(20) unsigned NOT NULL DEFAULT 0,
  `trx` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` decimal(28,8) NOT NULL DEFAULT 0.00000000,
  `charge` decimal(28,8) NOT NULL DEFAULT 0.00000000,
  `total_amount` decimal(28,8) NOT NULL DEFAULT 0.00000000,
  `sender_post_balance` decimal(28,8) NOT NULL DEFAULT 0.00000000,
  `receiver_post_balance` decimal(28,8) NOT NULL DEFAULT 0.00000000,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `send_moneys_sender_id_index` (`sender_id`),
  KEY `send_moneys_receiver_id_index` (`receiver_id`),
  KEY `send_moneys_trx_index` (`trx`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SQL

if [ $? -eq 0 ]; then
  echo "  ✓ send_moneys 表建立成功"
else
  echo "  ✗ send_moneys 表建立失敗"
fi

# 驗證表已建立
COUNT=$(mysql -u ovovovov -povovovov ovovovov -e "SHOW TABLES LIKE 'send_moneys';" 2>/dev/null | grep -c "send_moneys")
if [ "$COUNT" -eq 1 ]; then
  echo "  ✓ 驗證通過: send_moneys 表存在"
else
  echo "  ✗ 驗證失敗: send_moneys 表不存在"
fi

echo ""

# ============================================================
# FIX 2: 修復 GlobalVariablesServiceProvider getCurrentLang
# ============================================================
echo "[2/5] 修復 GlobalVariablesServiceProvider..."
FILE="$CD/app/Providers/GlobalVariablesServiceProvider.php"
if grep -q "strtolower(getCurrentLang())" "$FILE" 2>/dev/null; then
  sed -i "s/strtolower(getCurrentLang())/strtolower(\\\\getCurrentLang())/g" "$FILE"
  echo "  ✓ getCurrentLang() 已修復為 \\getCurrentLang()"
else
  if grep -q "strtolower(\\\\getCurrentLang())" "$FILE" 2>/dev/null; then
    echo "  ✓ 已是正確的 \\getCurrentLang()，無需修復"
  else
    echo "  - 未找到 getCurrentLang() 呼叫"
  fi
fi
echo ""

# ============================================================
# FIX 3: 清除 Laravel 快取
# ============================================================
echo "[3/5] 清除所有快取..."
cd $CD
$PHP artisan config:clear 2>&1 | tail -1
$PHP artisan route:clear 2>&1 | tail -1
$PHP artisan view:clear 2>&1 | tail -1
$PHP artisan cache:clear 2>&1 | tail -1
$PHP artisan optimize:clear 2>&1 | tail -1
echo "  ✓ 快取清除完成"
echo ""

# ============================================================
# FIX 4: 重新生成快取 (優化性能)
# ============================================================
echo "[4/5] 重新生成優化快取..."
$PHP artisan config:cache 2>&1 | tail -1
$PHP artisan route:cache 2>&1 | tail -1
echo "  ✓ 快取重建完成"
echo ""

# ============================================================
# FIX 5: 最終驗證
# ============================================================
echo "[5/5] 最終狀態驗證..."
for url in "https://deepay.srl/admin/login" "https://deepay.srl/admin" "https://deepay.srl/agent/login" "https://deepay.srl/merchant/login" "https://deepay.srl/user/login" "https://deepay.srl/api/transactions" "https://deepay.srl/api/dashboard"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$url")
  if [ "$code" = "200" ] || [ "$code" = "401" ] || [ "$code" = "302" ]; then
    echo "  ✓ $url => $code"
  else
    echo "  ✗ $url => $code"
  fi
done

echo ""
echo "===== 修復完成 ====="
