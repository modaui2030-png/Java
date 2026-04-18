<template>
  <div class="cyber-tryon-root">
    <!-- ───── 頂部狀態列 ───── -->
    <div class="cyber-stats-bar">
      <div class="stat-card">
        <p class="stat-label">SYSTEM</p>
        <p class="stat-value text-cyan">DEEPAY AI v2.0</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">ENGINE</p>
        <p class="stat-value text-cyan">IDM-VTON ACTIVE</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">GPU STATUS</p>
        <p class="stat-value" :class="step === 2 ? 'text-amber blink' : 'text-green'">
          {{ step === 2 ? 'PROCESSING...' : 'READY' }}
        </p>
      </div>
      <div class="stat-card">
        <p class="stat-label">SESSION</p>
        <p class="stat-value text-slate">{{ sessionId }}</p>
      </div>
    </div>

    <!-- ───── 主卡片容器 ───── -->
    <div class="cyber-main-card">
      <!-- 流光邊框裝飾層 -->
      <div class="glow-border" />

      <div class="cyber-inner">
        <!-- 標題列 -->
        <div class="cyber-header">
          <div>
            <h2 class="cyber-title">數字人換裝試驗場</h2>
            <p class="cyber-subtitle">IDM-VTON 高精度渲染引擎 · Deepay AI Platform</p>
          </div>
          <div class="header-actions" v-if="step === 1">
            <button class="cyber-btn-primary" @click="triggerFileInput">⬆ 上傳照片</button>
          </div>
          <button v-if="step === 3" class="cyber-btn-ghost" @click="reset">↩ 重新換裝</button>
        </div>

        <!-- ── STEP 1：上傳 + 選衣服 ── -->
        <div v-if="step === 1" class="step-upload">
          <div class="upload-preview-grid">
            <!-- 人像上傳區 -->
            <div class="preview-slot">
              <div class="slot-label">SOURCE PHOTO</div>
              <input
                ref="fileInputRef"
                type="file"
                accept="image/*"
                style="display:none"
                @change="handleFileInputChange"
              />
              <div
                class="dropzone"
                :class="{ 'dropzone-drag': isDragging, 'dropzone-filled': !!photoUrl }"
                @click="triggerFileInput"
                @dragover.prevent="isDragging = true"
                @dragleave.prevent="isDragging = false"
                @drop.prevent="handleDrop"
              >
                <img v-if="photoUrl" :src="photoUrl" class="slot-img" />
                <div v-else class="dropzone-hint">
                  <span class="dropzone-icon">📸</span>
                  <p class="dropzone-main">拖拽模特照片至此，或 <span class="text-cyan">點擊上傳</span></p>
                  <p class="dropzone-sub">SUPPORT: JPG · PNG · WEBP · MAX 5MB</p>
                </div>
              </div>
            </div>

            <!-- 衣服選擇列表 -->
            <div class="clothes-panel">
              <div class="slot-label">SELECT GARMENT</div>
              <div class="clothes-grid">
                <div
                  v-for="item in clothesList"
                  :key="item.clothesId"
                  :class="['clothes-card', { 'selected': selectedClothesId === item.clothesId }]"
                  @click="selectedClothesId = item.clothesId"
                >
                  <img :src="item.imageUrl" :alt="item.name" />
                  <span>{{ item.name }}</span>
                </div>
              </div>
              <button
                class="cyber-btn-primary full-width"
                :disabled="!photoUrl || !selectedClothesId"
                @click="startTryOn"
              >
                ▶ 開始 AI 換裝
              </button>
            </div>
          </div>
        </div>

        <!-- ── STEP 2：處理中 ── -->
        <div v-if="step === 2" class="step-processing">
          <!-- SVG 進度環包裹掃描球 -->
          <div class="scan-orb-wrapper">
            <svg class="progress-ring-svg" viewBox="0 0 120 120">
              <circle class="ring-track" cx="60" cy="60" r="54" />
              <circle
                class="ring-progress"
                cx="60" cy="60" r="54"
                :stroke-dasharray="`${2 * Math.PI * 54}`"
                :stroke-dashoffset="2 * Math.PI * 54 * (1 - progress / 100)"
              />
            </svg>
            <div class="scan-orb">
              <div class="orb-ring ring-1" />
              <div class="orb-ring ring-2" />
              <div class="orb-ring ring-3" />
              <span class="orb-text">{{ progress }}%</span>
            </div>
          </div>
          <p class="processing-label">AI 正在分析像素矩陣...</p>
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: progress + '%' }" />
          </div>
          <p class="processing-sub">GPU 已運算 {{ gpuSeconds }}s · IDM-VTON 渲染中</p>
          <!-- GPU 折線圖 -->
          <div class="gpu-chart">
            <div class="gpu-chart-header">
              <span class="pro-hud-label">GPU LOAD</span>
              <span class="gpu-chart-val">{{ Math.round(gpuValues[gpuValues.length - 1]) }}%</span>
            </div>
            <svg viewBox="0 0 200 60" class="sparkline-svg" preserveAspectRatio="none">
              <defs>
                <linearGradient id="gpuGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="rgba(34,211,238,0.3)" />
                  <stop offset="100%" stop-color="rgba(34,211,238,0)" />
                </linearGradient>
              </defs>
              <path :d="gpuFillPath" fill="url(#gpuGrad)" />
              <path :d="gpuSparklinePath" fill="none" stroke="#22d3ee" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </div>
        </div>

        <!-- ── STEP 3：對比結果 ── -->
        <div v-if="step === 3" class="step-result">
          <div class="compare-container" ref="compareRef">
            <!-- 原圖（左） -->
            <div class="compare-pane pane-before" :style="{ width: sliderPos + '%' }">
              <img :src="photoUrl" alt="原圖" />
              <span class="pane-badge">SOURCE</span>
            </div>
            <!-- AI 結果（右），帶掃描線 -->
            <div class="compare-pane pane-after">
              <img :src="resultUrl" alt="換裝效果" />
              <span class="pane-badge badge-ai">AI RESULT</span>
              <div class="scan-line-overlay">
                <div class="scan-line" />
              </div>
            </div>
            <!-- 拖動把手 -->
            <div
              class="compare-handle"
              :style="{ left: sliderPos + '%' }"
              @mousedown="startDrag"
            >
              <div class="handle-bar" />
              <div class="handle-knob">⟺</div>
            </div>
          </div>

          <!-- 結果資訊列 -->
          <div class="result-footer">
            <div class="result-stat">
              <span class="rstat-label">GPU 耗時</span>
              <span class="rstat-val text-cyan">{{ (gpuDurationMs / 1000).toFixed(1) }}s</span>
            </div>
            <div class="result-stat">
              <span class="rstat-label">引擎</span>
              <span class="rstat-val text-slate">IDM-VTON</span>
            </div>
            <div class="result-actions">
              <button class="cyber-btn-ghost" @click="reset">↩ 重新換裝</button>
              <button class="cyber-btn-primary" @click="downloadResult">⬇ 下載圖片</button>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  generateDigitalHuman,
  uploadUserPhoto,
  getClothesItems,
  type ClothesItem
} from '@/api/ai/tryon'

// ============ 基礎狀態 ============
const sessionId = ref('DP-' + Math.random().toString(36).substr(2, 8).toUpperCase())
const step = ref<1 | 2 | 3>(1)
const photoUrl = ref('')
const selectedClothesId = ref<number | null>(null)
const clothesList = ref<ClothesItem[]>([])
const resultUrl = ref('')
const gpuDurationMs = ref(0)
const progress = ref(0)
const gpuSeconds = ref(0)
const sliderPos = ref(50)
const isDragging = ref(false)

// ============ 自訂 Dropzone ============
const fileInputRef = ref<HTMLInputElement | null>(null)

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileInputChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file) processFile(file)
}

function handleDrop(e: DragEvent) {
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) processFile(file)
}

async function processFile(file: File) {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('檔案超過 5MB 限制')
    return
  }
  try {
    const res = await uploadUserPhoto(file)
    photoUrl.value = res.url
    ElMessage.success('照片上傳成功')
  } catch {
    ElMessage.error('上傳失敗，請重試')
  }
}

// ============ GPU 折線圖資料 ============
const GPU_POINTS = 20
const gpuValues = ref<number[]>(Array.from({ length: GPU_POINTS }, () => 40 + Math.random() * 20))
let gpuTimer: ReturnType<typeof setInterval> | null = null

function buildSparklinePath(values: number[], fill = false): string {
  const w = 200, h = 60
  const pts = values.map((v, i) => {
    const x = (i / (values.length - 1)) * w
    const y = h - (v / 100) * h
    return `${x},${y}`
  })
  if (fill) return `M0,${h} L${pts.join(' L')} L${w},${h} Z`
  return `M${pts.join(' L')}`
}

const gpuSparklinePath = ref(buildSparklinePath(gpuValues.value))
const gpuFillPath = ref(buildSparklinePath(gpuValues.value, true))

function startGpuAnimation() {
  gpuTimer = setInterval(() => {
    gpuValues.value.shift()
    gpuValues.value.push(60 + Math.random() * 35)
    gpuSparklinePath.value = buildSparklinePath(gpuValues.value)
    gpuFillPath.value = buildSparklinePath(gpuValues.value, true)
  }, 600)
}

function stopGpuAnimation() {
  if (gpuTimer) { clearInterval(gpuTimer); gpuTimer = null }
}

// ============ 動態控制台日誌 ============
const consoleLogs = ref<string[]>([])
const LOG_MESSAGES = [
  '[INFO] Initializing IDM-VTON engine...',
  '[INFO] Loading DensePose weights (2.1GB)...',
  '[DEBUG] Preprocessing input image 768x1024...',
  '[INFO] 4090 tensor cores: 16384 active',
  '[DEBUG] Denoising step 10/50... loss=0.342',
  '[DEBUG] Denoising step 25/50... loss=0.187',
  '[INFO] Warping garment geometry...',
  '[DEBUG] Denoising step 40/50... loss=0.091',
  '[INFO] Blending textures at 4K resolution...',
  '[SUCCESS] Render complete! SSIM=0.94',
]
let logTimer: ReturnType<typeof setInterval> | null = null

function startConsoleLogs() {
  consoleLogs.value = []
  let idx = 0
  logTimer = setInterval(() => {
    if (idx < LOG_MESSAGES.length) {
      consoleLogs.value.push(LOG_MESSAGES[idx++])
      nextTick(() => {
        const el = document.querySelector('.console-output') as HTMLElement
        if (el) el.scrollTop = el.scrollHeight
      })
    } else {
      if (logTimer) clearInterval(logTimer)
    }
  }, 800)
}

// ============ 初始化 ============
getClothesItems().then(list => { clothesList.value = list })

// ============ 核心：AI 換裝 ============
async function startTryOn() {
  if (!photoUrl.value || !selectedClothesId.value) return
  step.value = 2
  progress.value = 0
  gpuSeconds.value = 0
  startGpuAnimation()
  startConsoleLogs()

  const timer = setInterval(() => {
    if (progress.value < 90) progress.value += 3
    gpuSeconds.value++
  }, 1000)

  try {
    const result = await generateDigitalHuman({
      photoUrl: photoUrl.value,
      clothesId: selectedClothesId.value
    })
    clearInterval(timer)
    stopGpuAnimation()

    if (result.status === 2 && result.resultUrl) {
      progress.value = 100
      resultUrl.value = result.resultUrl
      gpuDurationMs.value = result.gpuDurationMs ?? 0
      setTimeout(() => { step.value = 3 }, 500)
    } else {
      ElMessage.error(result.errorMsg || 'AI 換裝失敗')
      step.value = 1
    }
  } catch {
    clearInterval(timer)
    stopGpuAnimation()
    ElMessage.error('服務異常，請稍後再試')
    step.value = 1
  }
}

// ============ Compare 滑塊 ============
function startDrag(e: MouseEvent) {
  const container = document.querySelector('.compare-container') as HTMLElement
  if (!container) return
  const onMove = (ev: MouseEvent) => {
    const rect = container.getBoundingClientRect()
    const pos = ((ev.clientX - rect.left) / rect.width) * 100
    sliderPos.value = Math.min(Math.max(pos, 5), 95)
  }
  const onUp = () => {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

// ============ 重置 & 下載 ============
function reset() {
  step.value = 1
  resultUrl.value = ''
  progress.value = 0
  consoleLogs.value = []
  stopGpuAnimation()
}

function downloadResult() {
  const a = document.createElement('a')
  a.href = resultUrl.value
  a.download = 'ai-tryon-result.jpg'
  a.click()
}

onUnmounted(() => {
  stopGpuAnimation()
  if (logTimer) clearInterval(logTimer)
})
</script>

<style scoped>
/* ============ 根容器 ============ */
.cyber-tryon-root {
  min-height: 100vh;
  background: #020617;
  padding: 24px;
  font-family: 'JetBrains Mono', 'Cascadia Code', monospace;
  color: #e2e8f0;
}

/* ============ 頂部狀態列 ============ */
.cyber-stats-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.stat-card {
  flex: 1;
  min-width: 140px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 10px;
  padding: 10px 16px;
}
.stat-label { font-size: 9px; letter-spacing: 0.15em; color: rgba(255,255,255,0.3); margin-bottom: 4px; }
.stat-value { font-size: 13px; font-weight: 600; }
.text-cyan { color: #22d3ee; }
.text-green { color: #4ade80; }
.text-amber { color: #fbbf24; }
.text-slate { color: #94a3b8; }

/* ============ 主卡片 ============ */
.cyber-main-card {
  position: relative;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 20px;
  overflow: hidden;
  backdrop-filter: blur(20px);
}
.glow-border {
  position: absolute; inset: 0; border-radius: 20px; pointer-events: none;
  background: linear-gradient(135deg, rgba(34,211,238,0.06) 0%, transparent 50%, rgba(139,92,246,0.04) 100%);
}
.cyber-inner { padding: 28px; position: relative; z-index: 1; }

/* ============ 標題列 ============ */
.cyber-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; }
.cyber-title { font-size: 22px; font-weight: 700; color: #f1f5f9; letter-spacing: -0.02em; }
.cyber-subtitle { font-size: 11px; color: rgba(255,255,255,0.3); margin-top: 4px; letter-spacing: 0.05em; }

/* ============ 按鈕 ============ */
.cyber-btn-primary {
  background: rgba(34,211,238,0.1);
  border: 1px solid rgba(34,211,238,0.3);
  color: #22d3ee;
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s;
  position: relative; overflow: hidden;
}
.cyber-btn-primary:hover { background: rgba(34,211,238,0.2); box-shadow: 0 0 16px rgba(34,211,238,0.2); }
.cyber-btn-primary:disabled { opacity: 0.35; cursor: not-allowed; }
.cyber-btn-ghost {
  background: transparent;
  border: 1px solid rgba(255,255,255,0.15);
  color: rgba(255,255,255,0.6);
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s;
}
.cyber-btn-ghost:hover { border-color: rgba(255,255,255,0.3); color: #fff; }
.full-width { width: 100%; margin-top: 16px; }

/* ============ STEP 1 ============ */
.upload-preview-grid { display: grid; grid-template-columns: 1fr 2fr; gap: 24px; }
.slot-label { font-size: 9px; letter-spacing: 0.15em; color: rgba(255,255,255,0.3); margin-bottom: 10px; }

/* 自訂 Dropzone */
.dropzone {
  border: 2px dashed rgba(255,255,255,0.1);
  border-radius: 20px;
  min-height: 280px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  background: rgba(255,255,255,0.02);
  transition: all 0.3s;
  overflow: hidden;
}
.dropzone:hover, .dropzone-drag {
  border-color: rgba(34,211,238,0.5);
  background: rgba(34,211,238,0.04);
  box-shadow: 0 0 20px rgba(34,211,238,0.08);
}
.dropzone-filled { border-style: solid; border-color: rgba(34,211,238,0.2); padding: 0; }
.dropzone-hint { text-align: center; padding: 24px; }
.dropzone-icon { font-size: 40px; display: block; margin-bottom: 12px; transition: transform 0.3s; }
.dropzone:hover .dropzone-icon { transform: scale(1.1); }
.dropzone-main { font-size: 13px; color: #94a3b8; }
.dropzone-main .text-cyan { color: #22d3ee; }
.dropzone-sub { font-size: 9px; color: rgba(255,255,255,0.2); margin-top: 8px; letter-spacing: 0.12em; }
.slot-img { width: 100%; height: 100%; object-fit: cover; border-radius: 18px; }

/* 衣服列表 */
.clothes-panel { display: flex; flex-direction: column; }
.clothes-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; flex: 1; }
.clothes-card {
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  background: rgba(255,255,255,0.02);
  text-align: center;
}
.clothes-card:hover { border-color: rgba(34,211,238,0.3); }
.clothes-card.selected { border-color: #22d3ee; box-shadow: 0 0 12px rgba(34,211,238,0.2); }
.clothes-card img { width: 100%; aspect-ratio: 3/4; object-fit: cover; }
.clothes-card span { font-size: 10px; color: rgba(255,255,255,0.5); padding: 4px; display: block; }

/* ============ STEP 2 ============ */
.step-processing { text-align: center; padding: 40px 20px; }
.scan-orb-wrapper { position: relative; display: inline-block; margin-bottom: 24px; }
.progress-ring-svg { width: 140px; height: 140px; transform: rotate(-90deg); }
.ring-track { fill: none; stroke: rgba(255,255,255,0.05); stroke-width: 3; }
.ring-progress { fill: none; stroke: #22d3ee; stroke-width: 3; stroke-linecap: round; transition: stroke-dashoffset 0.8s ease; filter: drop-shadow(0 0 6px rgba(34,211,238,0.6)); }
.scan-orb {
  position: absolute; inset: 10px;
  display: flex; align-items: center; justify-content: center;
}
.orb-ring {
  position: absolute; border-radius: 50%; border: 1px solid rgba(34,211,238,0.3);
}
.ring-1 { inset: 20%; animation: spin 3s linear infinite; }
.ring-2 { inset: 35%; animation: spin 2s linear infinite reverse; border-color: rgba(139,92,246,0.4); }
.ring-3 { inset: 50%; animation: spin 1.5s linear infinite; border-color: rgba(34,211,238,0.5); }
.orb-text { font-size: 18px; font-weight: 700; color: #22d3ee; z-index: 1; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.processing-label { font-size: 14px; color: #94a3b8; margin-bottom: 16px; }
.processing-sub { font-size: 11px; color: rgba(255,255,255,0.25); margin-top: 8px; }
.progress-track {
  width: 300px; height: 3px; background: rgba(255,255,255,0.05);
  border-radius: 99px; margin: 0 auto; overflow: hidden;
}
.progress-fill {
  height: 100%; background: linear-gradient(90deg, #22d3ee, #818cf8);
  border-radius: 99px; transition: width 0.8s ease;
  box-shadow: 0 0 8px rgba(34,211,238,0.5);
}

/* GPU 折線圖 */
.gpu-chart {
  margin: 24px auto 0;
  max-width: 320px;
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 12px;
  padding: 12px 16px;
}
.gpu-chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.gpu-chart-val { font-size: 16px; font-weight: 700; color: #22d3ee; }
.sparkline-svg { width: 100%; height: 60px; display: block; }

/* ============ STEP 3 ============ */
.step-result {}
.compare-container {
  position: relative; overflow: hidden; border-radius: 14px;
  user-select: none; height: 500px;
}
.compare-pane { position: absolute; top: 0; height: 100%; overflow: hidden; }
.pane-before { left: 0; }
.pane-after { left: 0; width: 100%; }
.compare-pane img { width: 100%; height: 100%; object-fit: cover; }
.pane-badge {
  position: absolute; bottom: 12px; right: 12px;
  background: rgba(0,0,0,0.6); color: rgba(255,255,255,0.7);
  padding: 4px 10px; border-radius: 20px; font-size: 10px; letter-spacing: 0.1em;
}
.badge-ai { right: auto; left: 12px; color: #22d3ee; border: 1px solid rgba(34,211,238,0.3); }
.scan-line-overlay { position: absolute; inset: 0; overflow: hidden; pointer-events: none; }
.scan-line {
  position: absolute; left: 0; right: 0; height: 2px;
  background: linear-gradient(90deg, transparent, rgba(34,211,238,0.8), transparent);
  animation: scandown 2s linear infinite;
}
@keyframes scandown { from { top: 0; } to { top: 100%; } }
.compare-handle {
  position: absolute; top: 0; height: 100%;
  transform: translateX(-50%); cursor: ew-resize;
  display: flex; flex-direction: column; align-items: center;
}
.handle-bar { width: 2px; flex: 1; background: rgba(255,255,255,0.8); }
.handle-knob {
  background: white; border-radius: 50%;
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.4);
}
.result-footer { display: flex; align-items: center; gap: 24px; margin-top: 16px; padding: 12px 0; }
.result-stat { display: flex; flex-direction: column; gap: 2px; }
.rstat-label { font-size: 9px; color: rgba(255,255,255,0.3); letter-spacing: 0.12em; }
.rstat-val { font-size: 15px; font-weight: 600; }
.result-actions { margin-left: auto; display: flex; gap: 10px; }

/* blink 動畫 */
.blink { animation: blink 1s step-end infinite; }
@keyframes blink { 50% { opacity: 0; } }

/* ============ 設計師級覆蓋 ============ */
::-webkit-scrollbar { width: 4px; height: 4px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 10px; transition: all 0.3s; }
::-webkit-scrollbar-thumb:hover { background: rgba(34,211,238,0.4); }

:deep(.el-card), :deep(.el-input__wrapper), :deep(.el-select__wrapper) {
  background-color: rgba(15,23,42,0.6) !important;
  border: 1px solid rgba(255,255,255,0.08) !important;
  box-shadow: inset 0 1px 1px rgba(255,255,255,0.05) !important;
  border-radius: 12px !important;
  transition: all 0.4s cubic-bezier(0.4,0,0.2,1) !important;
}
:deep(.el-input__wrapper.is-focus) {
  border-color: rgba(34,211,238,0.6) !important;
  box-shadow: 0 0 15px rgba(34,211,238,0.2), inset 0 0 5px rgba(34,211,238,0.1) !important;
}

.model-preview-container {
  position: relative; overflow: hidden; border-radius: 24px;
  background: linear-gradient(135deg, rgba(255,255,255,0.05) 0%, rgba(0,0,0,0) 100%);
}
.model-preview-container::after {
  content: ""; position: absolute; inset: 0; pointer-events: none;
  border: 1px solid rgba(255,255,255,0.1);
  mask-image: linear-gradient(to bottom, black, transparent);
  border-radius: 24px;
}

.pro-hud-label {
  font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.1em;
  text-transform: uppercase; color: rgba(255,255,255,0.4);
  border-left: 2px solid #22d3ee; padding-left: 8px; margin-bottom: 4px;
}

.btn-designer {
  background: rgba(255,255,255,0.03) !important; border: 1px solid rgba(255,255,255,0.1) !important;
  color: #fff !important; backdrop-filter: blur(10px); overflow: hidden; position: relative;
}
.btn-designer::before {
  content: ""; position: absolute; top: -50%; left: -50%; width: 200%; height: 200%;
  background: radial-gradient(circle, rgba(34,211,238,0.15) 0%, transparent 70%);
  opacity: 0; transition: opacity 0.5s;
}
.btn-designer:hover::before { opacity: 1; }
</style>
