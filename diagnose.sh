#!/bin/bash
CD=/www/wwwroot/www.deepay.srl/core
cd $CD

echo "=== 1. LARAVEL LOG (last 30 lines) ==="
tail -30 storage/logs/laravel.log 2>/dev/null || echo "Log empty or missing"

echo ""
echo "=== 2. ROUTE LIST (admin + api) ==="
/www/server/php/83/bin/php artisan route:list --path=admin 2>&1 | head -50
echo "---API routes---"
/www/server/php/83/bin/php artisan route:list --path=api 2>&1 | head -80

echo ""
echo "=== 3. HTTP STATUS CHECKS ==="
for url in "https://deepay.srl/admin/login" "https://deepay.srl/agent/login" "https://deepay.srl/merchant/login" "https://deepay.srl/user/login" "https://deepay.srl/api/iban" "https://deepay.srl/api/transactions" "https://deepay.srl/api/dashboard/overview"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 8 "$url")
  echo "$url => $code"
done

echo ""
echo "=== 4. DB TABLES CHECK ==="
mysql -u ovovovov -povovovov ovovovov -e "SHOW TABLES;" 2>&1 | head -60

echo ""
echo "=== 5. MISSING MODELS ON DB ==="
for table in users agents merchants deposits withdrawals transactions bank_transfers cash_outs send_moneys make_payments education_fees microfinances utility_bills; do
  exists=$(mysql -u ovovovov -povovovov ovovovov -e "SHOW TABLES LIKE '$table';" 2>/dev/null | grep -c "$table")
  if [ "$exists" -eq 0 ]; then
    echo "MISSING TABLE: $table"
  fi
done

echo ""
echo "=== 6. PHP SYNTAX CHECK (controllers) ==="
find $CD/app/Http/Controllers -name "*.php" | head -20 | while read f; do
  result=$(/www/server/php/83/bin/php -l "$f" 2>&1)
  if echo "$result" | grep -q "Parse error\|Fatal error"; then
    echo "ERROR in $f: $result"
  fi
done

echo ""
echo "=== 7. ASSET PATH CHECK ==="
grep -rn "assets/assets" $CD/resources/views 2>/dev/null | head -10 || echo "No double assets path found"

echo ""
echo "=== 8. GlobalVariablesServiceProvider check ==="
grep -n "getCurrentLang" $CD/app/Providers/GlobalVariablesServiceProvider.php 2>/dev/null | head -5

echo "=== DONE ==="
