#!/usr/bin/env python3
"""
4090 AI 虛擬換裝服務（FastAPI）
====================================================
支援：IDM-VTON / CatVTON / 任何本地 diffusion model
啟動：uvicorn main:app --host 0.0.0.0 --port 5000 --workers 1

接口：
  POST /predict     → 同步換裝（等待結果）
  POST /predict/async → 異步換裝（返回 taskId）
  GET  /task/{id}   → 查詢任務狀態
  GET  /health      → 健康檢查
"""

import time
import uuid
import asyncio
import threading
from datetime import datetime
from pathlib import Path
from typing import Optional

import torch
import requests
from fastapi import FastAPI, BackgroundTasks, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel

app = FastAPI(title="DeepAI TryOn Service", version="1.0.0")

# ================== 資料模型 ==================

class TryOnRequest(BaseModel):
    photoUrl: str           # 用戶照片 URL
    clothesUrl: str         # 衣服圖片 URL（由 Java 後端傳入）
    clothType: str = "upper"  # upper / lower / dresses
    numSteps: int = 20        # 推理步驟數（越高越慢）
    guidanceScale: float = 2.0

class TryOnResponse(BaseModel):
    success: bool
    resultUrl: Optional[str] = None
    gpuDurationMs: Optional[int] = None
    taskId: Optional[str] = None
    error: Optional[str] = None

# ================== 任務狀態（生產環境改成 Redis）==================

task_store: dict = {}  # taskId -> {status, resultUrl, gpuDurationMs, error}

# ================== 工具函數 ==================

def download_image(url: str, save_path: str) -> bool:
    """從 URL 下載圖片到本地"""
    try:
        resp = requests.get(url, timeout=30)
        resp.raise_for_status()
        with open(save_path, "wb") as f:
            f.write(resp.content)
        return True
    except Exception as e:
        print(f"[ERROR] 下載圖片失敗: {url} -> {e}")
        return False

def run_tryon_model(person_path: str, cloth_path: str, output_path: str, cloth_type: str) -> float:
    """
    調用本地 AI 模型進行換裝
    
    替換此函數以對接不同模型：
    - IDM-VTON: 使用 diffusers pipeline
    - CatVTON:  使用 catVTON inference script  
    - ComfyUI:  HTTP 調用本地 ComfyUI API
    
    返回 GPU 耗時（秒）
    """
    gpu_start = time.time()
    
    # ====================================================
    # TODO: 替換為你實際的模型調用代碼
    # ====================================================
    # 示例（IDM-VTON）：
    #   from src.tryon_pipeline import StableDiffusionXLInpaintPipeline
    #   pipe = StableDiffusionXLInpaintPipeline.from_pretrained(...)
    #   result = pipe(image=person_img, mask_image=mask, ...)
    #   result.images[0].save(output_path)
    # ====================================================
    
    # 示意：直接複製 person_path 作為 output（開發用）
    import shutil
    shutil.copy(person_path, output_path)
    
    return time.time() - gpu_start

def upload_to_storage(file_path: str, filename: str) -> str:
    """
    上傳結果圖片到存儲（MinIO / 本地靜態文件）
    返回可公開訪問的 URL
    """
    # 方式1：直接返回本地靜態文件 URL（配合 nginx 靜態目錄）
    output_dir = Path("/home/ai-results")
    output_dir.mkdir(exist_ok=True)
    dest = output_dir / filename
    Path(file_path).rename(dest)
    return f"https://api.deepay.srl/ai-results/{filename}"
    
    # 方式2：上傳到 MinIO（生產推薦）
    # from minio import Minio
    # client = Minio("127.0.0.1:9000", access_key="...", secret_key="...", secure=False)
    # client.fput_object("ai-results", filename, file_path)
    # return f"https://oss.deepay.srl/ai-results/{filename}"

# ================== 核心 AI 處理邏輯 ==================

def process_tryon_task(task_id: str, req: TryOnRequest):
    """後台執行的換裝任務"""
    task_store[task_id] = {"status": "processing"}
    
    tmp_dir = Path(f"/tmp/tryon-{task_id}")
    tmp_dir.mkdir(exist_ok=True)
    
    try:
        # 1. 下載圖片
        person_path = str(tmp_dir / "person.jpg")
        cloth_path  = str(tmp_dir / "cloth.jpg")
        if not download_image(req.photoUrl, person_path):
            raise Exception(f"無法下載用戶照片: {req.photoUrl}")
        if not download_image(req.clothesUrl, cloth_path):
            raise Exception(f"無法下載衣服圖片: {req.clothesUrl}")
        
        # 2. 運行模型
        output_path = str(tmp_dir / "result.jpg")
        gpu_seconds = run_tryon_model(person_path, cloth_path, output_path, req.clothType)
        gpu_ms = int(gpu_seconds * 1000)
        
        # 3. 上傳結果
        filename = f"{task_id}-result.jpg"
        result_url = upload_to_storage(output_path, filename)
        
        task_store[task_id] = {
            "status": "success",
            "resultUrl": result_url,
            "gpuDurationMs": gpu_ms
        }
        print(f"[OK] Task {task_id} 完成，GPU={gpu_ms}ms, url={result_url}")
        
    except Exception as e:
        task_store[task_id] = {"status": "failed", "error": str(e)}
        print(f"[ERROR] Task {task_id} 失敗: {e}")
    finally:
        import shutil
        shutil.rmtree(tmp_dir, ignore_errors=True)

# ================== API 路由 ==================

@app.get("/health")
def health():
    """健康檢查"""
    cuda_available = torch.cuda.is_available()
    device_name = torch.cuda.get_device_name(0) if cuda_available else "CPU"
    return {
        "status": "ok",
        "cuda": cuda_available,
        "device": device_name,
        "time": datetime.now().isoformat()
    }

@app.post("/predict", response_model=TryOnResponse)
async def predict_sync(req: TryOnRequest):
    """
    同步換裝接口（Java 後端直接調用）
    適合超時設定 60-120s 的場景
    """
    task_id = str(uuid.uuid4())
    
    # 在線程池中跑模型（避免阻塞事件循環）
    loop = asyncio.get_event_loop()
    await loop.run_in_executor(None, process_tryon_task, task_id, req)
    
    result = task_store.get(task_id, {})
    if result.get("status") == "success":
        return TryOnResponse(
            success=True,
            resultUrl=result["resultUrl"],
            gpuDurationMs=result["gpuDurationMs"],
            taskId=task_id
        )
    else:
        return TryOnResponse(
            success=False,
            error=result.get("error", "Unknown error"),
            taskId=task_id
        )

@app.post("/predict/async", response_model=TryOnResponse)
async def predict_async(req: TryOnRequest, background_tasks: BackgroundTasks):
    """
    異步換裝接口（立即返回 taskId，前端輪詢）
    """
    task_id = str(uuid.uuid4())
    task_store[task_id] = {"status": "queued"}
    background_tasks.add_task(process_tryon_task, task_id, req)
    return TryOnResponse(success=True, taskId=task_id)

@app.get("/task/{task_id}", response_model=TryOnResponse)
def get_task(task_id: str):
    """查詢任務狀態（前端輪詢用）"""
    result = task_store.get(task_id)
    if not result:
        raise HTTPException(status_code=404, detail="Task not found")
    
    status = result.get("status")
    if status == "success":
        return TryOnResponse(
            success=True,
            resultUrl=result["resultUrl"],
            gpuDurationMs=result["gpuDurationMs"],
            taskId=task_id
        )
    elif status == "failed":
        return TryOnResponse(success=False, error=result.get("error"), taskId=task_id)
    else:
        # queued / processing
        return TryOnResponse(success=False, taskId=task_id, error=status)

# ================== 啟動 ==================
# uvicorn main:app --host 0.0.0.0 --port 5000 --workers 1
# 注意：--workers 必須是 1，因為 GPU 不能並行多進程共用
