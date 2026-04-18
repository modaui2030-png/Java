# 4090 AI 服務部署指南

## 1. 安裝依賴

```bash
cd /path/to/ai-service
pip install -r requirements.txt
```

## 2. 啟動服務

```bash
# 生產模式（workers=1，避免多進程搶 GPU）
uvicorn main:app --host 0.0.0.0 --port 5000 --workers 1

# 開發模式（自動 reload）
uvicorn main:app --host 0.0.0.0 --port 5000 --reload
```

## 3. frp 穿透配置（讓伺服器 51.38.123.49 訪問到 4090）

### 在 4090 本機安裝 frpc：
```bash
# 下載 frp
wget https://github.com/fatedier/frp/releases/download/v0.57.0/frp_0.57.0_linux_amd64.tar.gz
tar -xzf frp_0.57.0_linux_amd64.tar.gz
cd frp_0.57.0_linux_amd64
```

### frpc.toml（4090 本機）：
```toml
serverAddr = "51.38.123.49"
serverPort = 7000

[[proxies]]
name = "ai-tryon"
type = "tcp"
localIP = "127.0.0.1"
localPort = 5000      # FastAPI 本地端口
remotePort = 15000    # 伺服器上暴露的端口
```

### frps.toml（伺服器 51.38.123.49）：
```toml
bindPort = 7000
```

### 啟動：
```bash
# 伺服器端
nohup ./frps -c frps.toml > /tmp/frps.log 2>&1 &

# 4090 本機
nohup ./frpc -c frpc.toml > /tmp/frpc.log 2>&1 &
```

### 驗證：
```bash
curl http://51.38.123.49:15000/health
# 應返回 {"status":"ok","cuda":true,"device":"NVIDIA GeForce RTX 4090",...}
```

## 4. 修改 application-prod.yml

```yaml
# 在檔案末尾添加：
ai:
  tryon:
    endpoint: http://51.38.123.49:15000/predict
    timeout-ms: 60000
```

## 5. 測試 Java → 4090 連通性

```bash
curl -X POST http://51.38.123.49:15000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "photoUrl": "https://api.deepay.srl/profile/test-user.jpg",
    "clothesUrl": "https://api.deepay.srl/profile/test-clothes.jpg",
    "clothType": "upper"
  }'
```

## 6. 整合真實模型（IDM-VTON 示例）

修改 `main.py` 中的 `run_tryon_model` 函數：

```python
from diffusers import AutoPipelineForInpainting
from PIL import Image

# 全局加載模型（只加載一次）
pipe = AutoPipelineForInpainting.from_pretrained(
    "yisol/IDM-VTON",
    torch_dtype=torch.float16
).to("cuda")

def run_tryon_model(person_path, cloth_path, output_path, cloth_type):
    gpu_start = time.time()
    person_img = Image.open(person_path).convert("RGB")
    cloth_img  = Image.open(cloth_path).convert("RGB")
    
    result = pipe(
        prompt="a photo of a person wearing the clothes",
        image=person_img,
        mask_image=...,   # 需要分割遮罩
        num_inference_steps=20,
    ).images[0]
    
    result.save(output_path)
    return time.time() - gpu_start
```
