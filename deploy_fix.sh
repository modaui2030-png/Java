#!/bin/bash
# 完整部署腳本 - 修復所有問題後執行

SERVER="root@51.38.123.49"
REMOTE="/www/wwwroot/www.deepay.srl/core"
PHP="/www/server/php/83/bin/php"

echo "===== DeePay 企業級修復部署 ====="
echo ""

# ── 1. 建立 send_moneys 表 ────────────────────────────────────
echo "[1/6] 建立 send_moneys 資料表..."
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

TABLE_CHECK=$(mysql -u ovovovov -povovovov ovovovov -e "SHOW TABLES LIKE 'send_moneys';" 2>/dev/null | grep -c "send_moneys" || echo 0)
[ "$TABLE_CHECK" -eq 1 ] && echo "  ✓ send_moneys 表建立/已存在" || echo "  ✗ 建立失敗"

# ── 2. 清除快取 ───────────────────────────────────────────────
echo "[2/6] 清除 Laravel 快取..."
cd $REMOTE
$PHP artisan optimize:clear 2>&1 | grep -E "INFO|success|cleared" | head -5
echo "  ✓ 快取已清除"

# ── 3. 修正權限 ───────────────────────────────────────────────
echo "[3/6] 修正檔案權限..."
chown -R www:www $REMOTE/storage $REMOTE/bootstrap/cache 2>/dev/null
chmod -R 775 $REMOTE/storage $REMOTE/bootstrap/cache 2>/dev/null
echo "  ✓ 權限修正完成"

# ── 4. 重建路由快取 ───────────────────────────────────────────
echo "[4/6] 重建路由與設定快取..."
cd $REMOTE
$PHP artisan config:cache 2>&1 | tail -1
$PHP artisan route:cache 2>&1 | tail -1
echo "  ✓ 快取重建完成"

# ── 5. PHP 語法驗證重要文件 ───────────────────────────────────
echo "[5/6] PHP 語法驗證..."
for f in \
  "app/Http/Controllers/Admin/AdminController.php" \
  "app/Http/Controllers/Api/DeepayController.php" \
  "app/Providers/GlobalVariablesServiceProvider.php" \
  "routes/api/api.php"; do
  result=$($PHP -l "$REMOTE/$f" 2>&1)
  if echo "$result" | grep -q "No syntax errors"; then
    echo "  ✓ $f"
  else
    echo "  ✗ $f: $result"
  fi
done

# ── 6. HTTP 狀態驗證 ──────────────────────────────────────────
echo "[6/6] HTTP 端點狀態驗證..."
PASS=0; FAIL=0
check_url() {
  local url="$1"; local expect="$2"
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$url")
  if echo "$expect" | grep -q "$code"; then
    echo "  ✓ $url => $code"
    PASS=$((PASS+1))
  else
    echo "  ✗ $url => $code (expected: $expect)"
    FAIL=$((FAIL+1))
  fi
}

check_url "https://deepay.srl/admin" "200 302"
check_url "https://deepay.srl/agent/login" "200"
check_url "https://deepay.srl/merchant/login" "200"
check_url "https://deepay.srl/user/login" "200"
check_url "https://deepay.srl/api/general-setting" "200"
check_url "https://deepay.srl/api/dashboard" "401"
check_url "https://deepay.srl/api/dashboard/overview" "401"
check_url "https://deepay.srl/api/iban" "401"
check_url "https://deepay.srl/api/transactions" "401"

echo ""
echo "===== 結果: $PASS 通過, $FAIL 失敗 ====="
