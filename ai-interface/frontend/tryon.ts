import request from '@/utils/request'

// ============================================================
// AI 虛擬換裝接口
// 用法示例：
//   const res = await generateDigitalHuman({ photoUrl, clothesId })
//   console.log(res.resultUrl) // 換裝後的圖片 URL
// ============================================================

export interface TryOnParams {
  /** 用戶照片 URL（上傳到 MinIO 後的地址） */
  photoUrl: string
  /** 選中的衣服 ID */
  clothesId: number
}

export interface TryOnResult {
  taskId?: number
  /** 0=排隊 1=處理中 2=完成 3=失敗 */
  status: 0 | 1 | 2 | 3
  statusDesc: string
  /** AI 生成的換裝圖片 URL */
  resultUrl?: string
  /** GPU 耗時（毫秒） */
  gpuDurationMs?: number
  errorMsg?: string
}

export interface ClothesItem {
  clothesId: number
  name: string
  category: 'top' | 'bottom' | 'dress'
  imageUrl: string
  brand?: string
}

/**
 * 提交換裝任務（同步，等待 AI 返回完整結果）
 * 適合快速展示場景，後端最長等待 120s
 *
 * @example
 *   const result = await generateDigitalHuman({ photoUrl, clothesId })
 *   if (result.status === 2) {
 *     showCompareSlider(result.resultUrl)
 *   }
 */
export function generateDigitalHuman(params: TryOnParams): Promise<TryOnResult> {
  return request({
    url: '/ai/tryon/submit',
    method: 'post',
    data: params,
    timeout: 130000 // 比後端超時多 10s
  })
}

/**
 * 非同步提交換裝任務（立即返回 taskId，適合進度條場景）
 *
 * @example
 *   const taskId = await submitTryOnAsync({ photoUrl, clothesId })
 *   // 然後輪詢：
 *   const timer = setInterval(async () => {
 *     const result = await getTryOnResult(taskId)
 *     if (result.status === 2 || result.status === 3) clearInterval(timer)
 *   }, 3000)
 */
export function submitTryOnAsync(params: TryOnParams): Promise<number> {
  return request({
    url: '/ai/tryon/submitAsync',
    method: 'post',
    data: params
  })
}

/**
 * 查詢換裝任務結果
 * 前端輪詢或 SSE 觸發後用此接口驗證最終結果
 */
export function getTryOnResult(taskId: number): Promise<TryOnResult> {
  return request({
    url: `/ai/tryon/result/${taskId}`,
    method: 'get'
  })
}

/**
 * 獲取衣服列表
 */
export function getClothesItems(params?: { category?: string }): Promise<ClothesItem[]> {
  return request({
    url: '/ai/clothes/list',
    method: 'get',
    params
  })
}

/**
 * 上傳用戶照片到 MinIO，返回 URL
 * 上傳後把 URL 傳給 generateDigitalHuman
 */
export function uploadUserPhoto(file: File): Promise<{ url: string }> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('module', 'ai-tryon') // MinIO 存儲路徑前綴
  return request({
    url: '/common/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
