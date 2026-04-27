<template>
  <div class="p-4 bg-slate-50">
    <div class="flex items-center justify-between mb-4">
      <h4 class="font-bold text-slate-800">应用概览</h4>
      <Button label="去对话" icon="pi pi-comments" class="bg-[#10b981] hover:bg-[#059669] text-white text-sm" />
    </div>
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <div class="lg:col-span-2 space-y-4">
        <div class="bg-white rounded-lg shadow-sm p-4">
          <div class="flex items-center mb-4">
            <div class="w-8 h-8 rounded-lg  flex items-center justify-center mr-3 overflow-hidden">
              <img src="/login_icon.png" alt="应用图标" class="w-full h-full object-contain" />
            </div>
            <div class="flex-1">
              <div class="flex items-center">
                <span class="text-[14px] font-semibold text-slate-800 mr-2">{{ appInfo.name }}</span>
                <span class="inline-flex items-center px-2 py-0.5 bg-green-100 text-green-800 rounded text-[12px]">
                  {{ appInfo.status }}
                </span>
              </div>
              <div class="text-[12px] text-slate-500 mt-1">ID: {{ appInfo.id }} | {{ appInfo.updateTime }} 更新</div>
            </div>
          </div>
          <div class="flex flex-wrap gap-2">
            <Button label="公开链接" icon="pi pi-link" text class="text-xs" />
            <Button label="API 凭据" icon="pi pi-key" text class="text-xs" />
            <Button label="显示设置" icon="pi pi-cog" text class="text-xs" />
          </div>
        </div>
        <div class="bg-white rounded-lg shadow-sm p-4">
          <div class="flex items-center justify-between mb-4">
            <h4 class="text-sm font-semibold text-slate-800">监控统计</h4>
            <div class="flex items-center gap-3">
              <span v-if="selectedTimeRange === 'custom' && customStartTime && customEndTime" class="text-xs text-slate-600">
                {{ formatDate(customStartTime) }} - {{ formatDate(customEndTime) }}
              </span>
              <Dropdown
                v-model="selectedTimeRange"
                :options="timeRangeOptions"
                optionLabel="label"
                optionValue="value"
                class="w-auto text-xs"
                placeholder="选择时间范围"
              />
            </div>
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
            <div class="flex items-center gap-3 bg-white rounded-xl p-3 shadow-sm border border-slate-100">
              <div class="w-9 h-9 rounded-lg bg-blue-100 flex items-center justify-center">
                <i class="pi pi-user text-blue-600 text-sm"></i>
              </div>
              <div>
                <p class="text-xs text-slate-500">用户总数</p>
                <p class="text-base font-bold text-slate-800">0 <span class="text-xs font-normal text-green-500">+0</span></p>
              </div>
            </div>
            <div class="flex items-center gap-3 bg-white rounded-xl p-3 shadow-sm border border-slate-100">
              <div class="w-9 h-9 rounded-lg bg-orange-100 flex items-center justify-center">
                <i class="pi pi-comments text-orange-600 text-sm"></i>
              </div>
              <div>
                <p class="text-xs text-slate-500">提问次数</p>
                <p class="text-base font-bold text-slate-800">0</p>
              </div>
            </div>
            <div class="flex items-center gap-3 bg-white rounded-xl p-3 shadow-sm border border-slate-100">
              <div class="w-9 h-9 rounded-lg bg-green-100 flex items-center justify-center">
                <i class="pi pi-clock text-green-600 text-sm"></i>
              </div>
              <div>
                <p class="text-xs text-slate-500">平均响应时间</p>
                <p class="text-base font-bold text-slate-800">0s</p>
              </div>
            </div>
            <div class="flex items-center gap-3 bg-white rounded-xl p-3 shadow-sm border border-slate-100">
              <div class="w-9 h-9 rounded-lg bg-teal-100 flex items-center justify-center">
                <i class="pi pi-key text-teal-600 text-sm"></i>
              </div>
              <div>
                <p class="text-xs text-slate-500">Tokens 总数</p>
                <p class="text-base font-bold text-slate-800">0</p>
              </div>
            </div>
          </div>
          <div class="border border-slate-200 rounded-lg p-3 mt-3">
            <p class="text-xs text-slate-500 mb-2">用户数据趋势</p>
            <div class="h-36">
              <canvas ref="userChartCanvas"></canvas>
            </div>
          </div>
          <div class="border border-slate-200 rounded-lg p-3 mt-3">
            <p class="text-xs text-slate-500 mb-2">Token消耗趋势</p>
            <div class="h-36">
              <canvas ref="tokenChartCanvas"></canvas>
            </div>
          </div>
        </div>
      </div>
      <div class="space-y-4">
        <div class="bg-gradient-to-br from-[#10b981] to-[#059669] rounded-lg p-5 text-white shadow-md">
          <div class="text-[16px] font-bold mb-3">快速开始对话</div>
          <div class="text-sm mb-5 leading-relaxed">通过对话 Agent 帮助您分析数据并导出 Word 报表。</div>
          <button class="w-full bg-white text-[#10b981] hover:bg-slate-100 text-[14px] py-2 rounded-lg border border-[#10b981] cursor-pointer">进入聊天界面</button>
        </div>

        <div class="bg-white rounded-xl shadow-sm py-[0.5rem] px-4 pb-[1rem]">
          <div class="flex items-center justify-between mb-1">
            <div class="flex items-center">
              <i class="pi pi-flame text-orange-500 mr-2"></i>
              <h4 class="text-sm font-semibold text-slate-800">最近高频问题</h4>
            </div>
            <button class="p-1 hover:bg-slate-100 rounded-md transition-colors">
              <i class="pi pi-refresh text-slate-400 text-xs"></i>
            </button>
          </div>

          <div class="space-y-2 mb-4">
            <div class="flex items-center justify-between bg-slate-50 rounded-lg px-3 py-2.5">
              <div class="flex items-center">
                <span class="inline-flex items-center justify-center w-5 h-5 bg-orange-100 text-orange-600 rounded-md text-xs font-medium mr-3">1</span>
                <span class="text-xs text-slate-700">帮我导出上周的工作周报 Word</span>
              </div>
              <span class="text-xs text-slate-400">342</span>
            </div>
            <div class="flex items-center justify-between bg-slate-50 rounded-lg px-3 py-2.5">
              <div class="flex items-center">
                <span class="inline-flex items-center justify-center w-5 h-5 bg-blue-100 text-blue-600 rounded-md text-xs font-medium mr-3">2</span>
                <span class="text-xs text-slate-700">查询 Token 消耗排行前三的应用</span>
              </div>
              <span class="text-xs text-slate-400">215</span>
            </div>
            <div class="flex items-center justify-between bg-slate-50 rounded-lg px-3 py-2.5">
              <div class="flex items-center">
                <span class="inline-flex items-center justify-center w-5 h-5 bg-red-100 text-red-600 rounded-md text-xs font-medium mr-3">3</span>
                <span class="text-xs text-slate-700">如何批量下载项目文档？</span>
              </div>
              <span class="text-xs text-slate-400">189</span>
            </div>
            <div class="flex items-center justify-between bg-slate-50 rounded-lg px-3 py-2.5">
              <div class="flex items-center">
                <span class="inline-flex items-center justify-center w-5 h-5 bg-slate-200 text-slate-600 rounded-md text-xs font-medium mr-3">4</span>
                <span class="text-xs text-slate-700">总结昨日新增用户的主要来源</span>
              </div>
              <span class="text-xs text-slate-400">94</span>
            </div>
            <div class="flex items-center justify-between bg-slate-50 rounded-lg px-3 py-2.5">
              <div class="flex items-center">
                <span class="inline-flex items-center justify-center w-5 h-5 bg-slate-200 text-slate-600 rounded-md text-xs font-medium mr-3">5</span>
                <span class="text-xs text-slate-700">查看当前 API 的并发限制</span>
              </div>
              <span class="text-xs text-slate-400">56</span>
            </div>
          </div>

          <button class="w-full border border-slate-200 text-slate-600 text-xs py-2 rounded-lg hover:bg-slate-50 transition-colors">
            分析更多用户意图
          </button>
        </div>
      </div>
    </div>
  </div>

  <Dialog
    v-model:visible="showCustomTimeModal"
    header="选择时间范围"
    :style="{ width: '400px' }"
    :modal="true"
    class="text-sm"
  >
    <div class="space-y-4">
      <div class="flex items-center gap-4">
        <div class="flex-1">
          <label class="block text-xs text-slate-600 mb-1">开始时间</label>
          <DatePicker
            v-model="customStartTime"
            dateFormat="yyyy-mm-dd"
            class="w-full text-sm"
            :showIcon="true"
          />
        </div>
        <div class="flex-1">
          <label class="block text-xs text-slate-600 mb-1">结束时间</label>
          <DatePicker
            v-model="customEndTime"
            dateFormat="yyyy-mm-dd"
            class="w-full text-sm"
            :showIcon="true"
          />
        </div>
      </div>
      <div class="flex justify-end gap-2">
        <Button label="取消" text @click="cancelCustomTime" class="text-xs" />
        <Button label="确定" @click="confirmCustomTime" class="bg-[#10b981] hover:bg-[#059669] text-white text-xs" />
      </div>
    </div>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed, nextTick } from 'vue'
import Button from 'primevue/button'
import Dropdown from 'primevue/dropdown'
import Dialog from 'primevue/dialog'
import DatePicker from 'primevue/datepicker'
import { useRoute } from 'vue-router'
import applicationApi from '@/api/application'
import { Chart, registerables } from 'chart.js'
const route = useRoute()
const appInfo = ref({
  name: '',
  id: '',
  status: '',
  updateTime: ''
})

const selectedTimeRange = ref('7')

const timeRangeOptions = ref([
  { label: '最近7天', value: '7' },
  { label: '最近30天', value: '30' },
  { label: '最近90天', value: '90' },
  { label: '半年', value: '180' },
  { label: '一年', value: '365' },
  { label: '自定义时间', value: 'custom' }
])

const showCustomTimeModal = ref(false)
const customStartTime = ref<Date | null>(null)
const customEndTime = ref<Date | null>(null)

watch(selectedTimeRange, (newVal) => {
  if (newVal === 'custom') {
    const endDate = new Date()
    const startDate = new Date()
    startDate.setDate(startDate.getDate() - 7)
    customStartTime.value = startDate
    customEndTime.value = endDate
    showCustomTimeModal.value = true
  }
})

const cancelCustomTime = () => {
  showCustomTimeModal.value = false
  selectedTimeRange.value = '7'
}

const confirmCustomTime = () => {
  if (customStartTime.value && customEndTime.value) {
    showCustomTimeModal.value = false
    if (customStartTime.value.getTime() > customEndTime.value.getTime()) {
      alert('开始时间不能大于结束时间')
      selectedTimeRange.value = '7'
    }
  } else {
    alert('请选择开始时间和结束时间')
  }
}

const formatDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
watch(selectedTimeRange, () => {
  nextTick(() => {
    initCharts()
  })
})

Chart.register(...registerables)
const userChartCanvas = ref<HTMLCanvasElement | null>(null)
const tokenChartCanvas = ref<HTMLCanvasElement | null>(null)
let userChartInstance: Chart | null = null
let tokenChartInstance: Chart | null = null

const generateChartData = () => {
  const range = selectedTimeRange.value
  let labels: string[] = []
  let userData: number[] = []
  let newUserData: number[] = []
  let inputTokenData: number[] = []
  let outputTokenData: number[] = []

  switch (range) {
    case '7':
      for (let i = 6; i >= 0; i--) {
        const date = new Date()
        date.setDate(date.getDate() - i)
        labels.push(`${date.getMonth() + 1}/${date.getDate()}`)
        userData.push(300 + Math.floor(Math.random() * 50))
        newUserData.push(10 + Math.floor(Math.random() * 15))
        inputTokenData.push(5000 + Math.floor(Math.random() * 1000))
        outputTokenData.push(8000 + Math.floor(Math.random() * 1500))
      }
      break
    case '30':
      for (let i = 29; i >= 0; i -= 5) {
        const date = new Date()
        date.setDate(date.getDate() - i)
        labels.push(`${date.getMonth() + 1}/${date.getDate()}`)
        userData.push(320 + Math.floor(Math.random() * 80))
        newUserData.push(15 + Math.floor(Math.random() * 20))
        inputTokenData.push(6000 + Math.floor(Math.random() * 1500))
        outputTokenData.push(9000 + Math.floor(Math.random() * 2000))
      }
      break
    case '90':
      for (let i = 89; i >= 0; i -= 15) {
        const date = new Date()
        date.setDate(date.getDate() - i)
        labels.push(`${date.getMonth() + 1}月${date.getDate()}日`)
        userData.push(350 + Math.floor(Math.random() * 100))
        newUserData.push(20 + Math.floor(Math.random() * 25))
        inputTokenData.push(8000 + Math.floor(Math.random() * 2000))
        outputTokenData.push(12000 + Math.floor(Math.random() * 3000))
      }
      break
    case '180':
      for (let i = 5; i >= 0; i--) {
        const date = new Date()
        date.setMonth(date.getMonth() - i)
        labels.push(`${date.getMonth() + 1}月`)
        userData.push(400 + Math.floor(Math.random() * 120))
        newUserData.push(25 + Math.floor(Math.random() * 30))
        inputTokenData.push(15000 + Math.floor(Math.random() * 5000))
        outputTokenData.push(20000 + Math.floor(Math.random() * 6000))
      }
      break
    case '365':
      for (let i = 11; i >= 0; i--) {
        const date = new Date()
        date.setMonth(date.getMonth() - i)
        labels.push(`${date.getMonth() + 1}月`)
        userData.push(300 + Math.floor(Math.random() * 150))
        newUserData.push(15 + Math.floor(Math.random() * 35))
        inputTokenData.push(20000 + Math.floor(Math.random() * 8000))
        outputTokenData.push(28000 + Math.floor(Math.random() * 10000))
      }
      break
    case 'custom':
      if (customStartTime.value && customEndTime.value) {
        const start = customStartTime.value
        const end = customEndTime.value
        const daysDiff = Math.floor((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24))
        const step = Math.max(1, Math.floor(daysDiff / 6))
        for (let i = 0; i <= daysDiff; i += step) {
          const date = new Date(start)
          date.setDate(date.getDate() + i)
          labels.push(`${date.getMonth() + 1}/${date.getDate()}`)
          userData.push(300 + Math.floor(Math.random() * 100))
          newUserData.push(10 + Math.floor(Math.random() * 25))
          inputTokenData.push(5000 + Math.floor(Math.random() * 2000))
          outputTokenData.push(8000 + Math.floor(Math.random() * 3000))
        }
      } else {
        labels = ['1月', '2月', '3月', '4月', '5月', '6月']
        userData = [300, 350, 380, 420, 450, 480]
        newUserData = [20, 25, 30, 35, 40, 45]
        inputTokenData = [10000, 12000, 15000, 18000, 20000, 22000]
        outputTokenData = [15000, 18000, 22000, 26000, 30000, 35000]
      }
      break
    default:
      labels = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
      userData = [120, 150, 180, 220, 280, 320, 380, 420, 450, 480, 520, 580]
      newUserData = [10, 15, 12, 20, 25, 30, 35, 40, 38, 45, 50, 55]
      inputTokenData = [8000, 10000, 12000, 15000, 18000, 22000, 25000, 28000, 30000, 32000, 35000, 38000]
      outputTokenData = [12000, 15000, 18000, 22000, 26000, 32000, 38000, 42000, 45000, 48000, 52000, 58000]
  }

  return { labels, userData, newUserData, inputTokenData, outputTokenData }
}

const getBaseChartOptions = () => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom' as const,
      labels: {
        usePointStyle: true,
        padding: 10,
        font: {
          size: 9
        }
      }
    },
    tooltip: {
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      padding: 10,
      titleFont: {
        size: 12
      },
      bodyFont: {
        size: 11
      }
    }
  },
  scales: {
    x: {
      grid: {
        display: false
      },
      ticks: {
        font: {
          size: 11
        }
      }
    },
    y: {
      grid: {
        color: 'rgba(0, 0, 0, 0.05)'
      },
      ticks: {
        font: {
          size: 11
        }
      }
    }
  }
})

const initUserChart = () => {
  if (!userChartCanvas.value) return
  if (userChartInstance) userChartInstance.destroy()

  const { labels, userData, newUserData } = generateChartData()
  userChartInstance = new Chart(userChartCanvas.value, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: '总用户数',
          data: userData,
          borderColor: '#10b981',
          backgroundColor: 'rgba(16, 185, 129, 0.1)',
          fill: true,
          tension: 0.4,
          pointRadius: 4,
          pointHoverRadius: 6
        },
        {
          label: '新增用户',
          data: newUserData,
          borderColor: '#3b82f6',
          backgroundColor: 'rgba(59, 130, 246, 0.1)',
          fill: true,
          tension: 0.4,
          pointRadius: 4,
          pointHoverRadius: 6
        }
      ]
    },
    options: getBaseChartOptions()
  })
}

const initTokenChart = () => {
  if (!tokenChartCanvas.value) return
  if (tokenChartInstance) tokenChartInstance.destroy()

  const { labels, inputTokenData, outputTokenData } = generateChartData()
  tokenChartInstance = new Chart(tokenChartCanvas.value, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: '输入Token',
          data: inputTokenData,
          borderColor: '#f59e0b',
          backgroundColor: 'rgba(245, 158, 11, 0.1)',
          fill: true,
          tension: 0.4,
          pointRadius: 4,
          pointHoverRadius: 6
        },
        {
          label: '输出Token',
          data: outputTokenData,
          borderColor: '#8b5cf6',
          backgroundColor: 'rgba(139, 92, 246, 0.1)',
          fill: true,
          tension: 0.4,
          pointRadius: 4,
          pointHoverRadius: 6
        }
      ]
    },
    options: getBaseChartOptions()
  })
}

const initCharts = () => {
  initUserChart()
  initTokenChart()
}

const loading = ref(false)

const fetchApplicationInfo = async () => {
  try {
    const applicationId = route.params.id as string || '5258c9c6-a8db-4d8a-9117-dcaf58215457'
    loading.value = true
    const response = await applicationApi.getApplicationInfo(applicationId, loading)
    if (response && response.data) {
      appInfo.value = {
        name: response.data.name || '应用名称',
        id: response.data.id || applicationId,
        status: response.data.status || '运行中',
        updateTime: response.data.updateTime || new Date().toISOString().split('T')[0]
      }
    }
  } catch (error) {
    console.error('获取应用信息失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchApplicationInfo()
  nextTick(() => {
    initCharts()
  })
})
</script>

<style scoped>
</style>
