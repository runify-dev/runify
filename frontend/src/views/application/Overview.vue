<template>
  <div class="p-4 bg-slate-50">
    <div class="flex items-center justify-between mb-4">
      <h4 class="font-bold text-slate-800">应用概览</h4>
    </div>
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <div class="lg:col-span-2 space-y-4">
        <div class="bg-white rounded-lg shadow-sm p-4">
          <div class="flex items-center mb-4">
            <div class="w-8 h-8 rounded-lg  flex items-center justify-center mr-3 overflow-hidden">
              <img :src="resetUrl('./application.png')" alt="应用图标"
                   class="w-full h-full object-contain"/>
            </div>
            <div class="flex-1">
              <div class="flex items-center">
                <span class="text-[14px] font-semibold text-slate-800 mr-2">{{
                    appInfo.name
                  }}</span>
                <span
                  class="inline-flex items-center px-2 py-0.5 bg-green-100 text-green-800 rounded text-[12px]">
                  {{ appInfo.status }}
                </span>
              </div>
              <div class="text-[12px] text-slate-500 mt-1">ID: {{ appInfo.id }} |
                {{ appInfo.updateTime }} 更新
              </div>
            </div>
          </div>
          <div class="flex flex-wrap gap-2">
            <Button label="公开链接" icon="pi pi-link" text class="text-xs"
                    @click="copyPublicLink"/>
            <Button label="嵌入第三方" icon="pi pi-external-link" text class="text-xs" @click="embedDialogRef?.open(applicationId)"/>
          </div>
        </div>
        <div class="bg-white rounded-lg shadow-sm p-4">
          <div class="flex items-center justify-between mb-4">
            <h4 class="text-sm font-semibold text-slate-800">监控统计</h4>
            <div class="flex items-center gap-3">
              <span v-if="selectedTimeRange === 'custom' && customStartTime && customEndTime"
                    class="text-xs text-slate-600">
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
            <div
              class="flex items-center gap-3 bg-white rounded-xl p-3 shadow-sm border border-slate-100">
              <div class="w-9 h-9 rounded-lg bg-blue-100 flex items-center justify-center">
                <i class="pi pi-comments text-blue-600 text-sm"></i>
              </div>
              <div>
                <p class="text-xs text-slate-500">对话数</p>
                <p class="text-base font-bold text-slate-800">{{
                    overviewData.conversationCount
                  }}</p>
              </div>
            </div>
            <div
              class="flex items-center gap-3 bg-white rounded-xl p-3 shadow-sm border border-slate-100">
              <div class="w-9 h-9 rounded-lg bg-orange-100 flex items-center justify-center">
                <i class="pi pi-comment text-orange-600 text-sm"></i>
              </div>
              <div>
                <p class="text-xs text-slate-500">消息总数</p>
                <p class="text-base font-bold text-slate-800">{{ overviewData.messageCount }}</p>
              </div>
            </div>
            <div
              class="flex items-center gap-3 bg-white rounded-xl p-3 shadow-sm border border-slate-100">
              <div class="w-9 h-9 rounded-lg bg-green-100 flex items-center justify-center">
                <i class="pi pi-clock text-green-600 text-sm"></i>
              </div>
              <div>
                <p class="text-xs text-slate-500">平均响应时间</p>
                <p class="text-base font-bold text-slate-800">
                  {{ formatDuration(overviewData.avgDuration) }}</p>
              </div>
            </div>
            <div
              class="flex items-center gap-3 bg-white rounded-xl p-3 shadow-sm border border-slate-100">
              <div class="w-9 h-9 rounded-lg bg-teal-100 flex items-center justify-center">
                <i class="pi pi-key text-teal-600 text-sm"></i>
              </div>
              <div>
                <p class="text-xs text-slate-500">Token 消耗</p>
                <p class="text-base font-bold text-slate-800">
                  {{ formatTokens(overviewData.totalTokens) }}</p>
              </div>
            </div>
          </div>
          <div class="border border-slate-200 rounded-lg p-3 mt-3">
            <p class="text-xs text-slate-500 mb-2">消息量趋势</p>
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
      <div class="order-first lg:order-none space-y-4">
        <div
          class="bg-gradient-to-br from-[#10b981] to-[#059669] rounded-lg p-5 text-white shadow-md">
          <div class="text-[16px] font-bold mb-3">快速开始对话</div>
          <div class="text-sm mb-5 leading-relaxed">通过对话 Agent 帮助您分析数据并导出 Word 报表。
          </div>
          <button
            class="w-full text-center bg-white text-[#10b981] hover:bg-slate-100 text-[14px] py-2 rounded-lg border border-[#10b981] cursor-pointer"
            @click="goToChat">进入聊天界面
          </button>
        </div>

        <div class="bg-white rounded-xl shadow-sm py-[0.5rem] px-4 pb-[1rem]">
          <div class="flex items-center justify-between mb-1">
            <div class="flex items-center">
              <i class="pi pi-flame text-orange-500 mr-2"></i>
              <h4 class="text-sm font-semibold text-slate-800">最近提问</h4>
            </div>
            <button class="p-1 hover:bg-slate-100 rounded-md transition-colors">
              <i class="pi pi-refresh text-slate-400 text-xs"></i>
            </button>
          </div>

          <div v-if="overviewData.recentQuestions.length" class="space-y-2 mb-4">
            <div
              v-for="(item, index) in overviewData.recentQuestions"
              :key="index"
              class="flex items-center justify-between bg-slate-50 rounded-lg px-3 py-2.5"
            >
              <div class="flex items-center">
                <span
                  class="inline-flex items-center justify-center w-5 h-5 rounded-md text-xs font-medium mr-3"
                  :class="index === 0 ? 'bg-orange-100 text-orange-600' : index === 1 ? 'bg-blue-100 text-blue-600' : index === 2 ? 'bg-red-100 text-red-600' : 'bg-slate-200 text-slate-600'"
                >{{ index + 1 }}</span>
                <span class="text-xs text-slate-700">{{ parseQuestionContent(item.content) }}</span>
              </div>
              <span class="text-xs text-slate-400">{{ item.createTime?.slice(5, 16) }}</span>
            </div>
          </div>
          <div v-else class="text-center text-xs text-slate-400 py-6">暂无数据</div>
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
        <Button label="取消" text @click="cancelCustomTime" class="text-xs"/>
        <Button label="确定" @click="confirmCustomTime"
                class="bg-[#10b981] hover:bg-[#059669] text-white text-xs"/>
      </div>
    </div>
  </Dialog>

  <EmbedDialog ref="embedDialogRef"/>
</template>

<script setup lang="ts">
import {ref, onMounted, watch, nextTick} from 'vue'
import Button from 'primevue/button'
import Dropdown from 'primevue/dropdown'
import Dialog from 'primevue/dialog'
import DatePicker from 'primevue/datepicker'
import {useRoute, useRouter} from 'vue-router'
import {resetUrl,copyContent} from "@/utils/common"
import applicationApi from '@/api/application'
import {Chart, registerables} from 'chart.js'
import bus from "@/bus"
import EmbedDialog from './component/EmbedDialog.vue'

const route = useRoute()
const router = useRouter()
const applicationId = route.params.id as string

const appInfo = ref({
  name: '',
  id: '',
  status: '',
  updateTime: ''
})

const overviewData = ref({
  conversationCount: 0,
  messageCount: 0,
  totalTokens: 0,
  avgDuration: 0,
  messageTrend: [] as { date: string; count: number }[],
  tokenTrend: [] as { date: string; total: number }[],
  recentQuestions: [] as { content: string; createTime: string }[]
})

const selectedTimeRange = ref('7')

const timeRangeOptions = ref([
  {label: '最近7天', value: '7'},
  {label: '最近30天', value: '30'},
  {label: '最近90天', value: '90'},
  {label: '半年', value: '180'},
  {label: '一年', value: '365'},
  {label: '自定义时间', value: 'custom'}
])

const showCustomTimeModal = ref(false)
const customStartTime = ref<Date | null>(null)
const customEndTime = ref<Date | null>(null)
const embedDialogRef = ref<InstanceType<typeof EmbedDialog>>()

watch(selectedTimeRange, (newVal) => {
  if (newVal === 'custom') {
    const endDate = new Date()
    const startDate = new Date()
    startDate.setDate(startDate.getDate() - 7)
    customStartTime.value = startDate
    customEndTime.value = endDate
    showCustomTimeModal.value = true
  } else {
    fetchOverview(Number(newVal))
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
    } else {
      const daysDiff = Math.floor((customEndTime.value.getTime() - customStartTime.value.getTime()) / (1000 * 60 * 60 * 24))
      fetchOverview(daysDiff)
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

const formatDuration = (ms: number) => {
  if (!ms) return '0s'
  const seconds = (ms / 1000).toFixed(1)
  return `${seconds}s`
}

const formatTokens = (tokens: number) => {
  if (!tokens) return '0'
  if (tokens >= 10000) return `${(tokens / 10000).toFixed(1)}万`
  if (tokens >= 1000) return `${(tokens / 1000).toFixed(1)}k`
  return String(tokens)
}

const parseQuestionContent = (content: string) => {
  try {
    const parsed = JSON.parse(content)
    if (Array.isArray(parsed) && parsed.length > 0) {
      const text = parsed[0]?.text || parsed[0]?.content || ''
      return text.length > 30 ? text.slice(0, 30) + '...' : text
    }
    return content.length > 30 ? content.slice(0, 30) + '...' : content
  } catch {
    return content.length > 30 ? content.slice(0, 30) + '...' : content
  }
}

Chart.register(...registerables)
const userChartCanvas = ref<HTMLCanvasElement | null>(null)
const tokenChartCanvas = ref<HTMLCanvasElement | null>(null)
let userChartInstance: Chart | null = null
let tokenChartInstance: Chart | null = null

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

  const trend = overviewData.value.messageTrend
  const labels = trend.map(item => item.date.slice(5))
  const data = trend.map(item => item.count)

  userChartInstance = new Chart(userChartCanvas.value, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: '消息数',
          data,
          borderColor: '#10b981',
          backgroundColor: 'rgba(16, 185, 129, 0.1)',
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

  const trend = overviewData.value.tokenTrend
  const labels = trend.map(item => item.date.slice(5))
  const data = trend.map(item => item.total)

  tokenChartInstance = new Chart(tokenChartCanvas.value, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: 'Token 消耗',
          data,
          borderColor: '#f59e0b',
          backgroundColor: 'rgba(245, 158, 11, 0.1)',
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

const fetchOverview = async (days = 7) => {
  try {
    const response = await applicationApi.getOverview(applicationId, days, loading)
    if (response && response.data) {
      overviewData.value = response.data
      nextTick(() => {
        initCharts()
      })
    }
  } catch (error) {
    console.error('获取概览数据失败:', error)
  }
}

const fetchApplicationInfo = async () => {
  try {
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
  }
}

const copyPublicLink = () => {
  const host = window.location.origin === 'http://localhost:3000' ? 'http://localhost:3001' : window.location.origin
  const url = `${host}/conversation/a/${applicationId}`
  copyContent(url).then(() => {
    bus.emit("message:success", "复制成功")
  }).catch(() => {
    bus.emit("message:success", url)
  })
}

const goToChat = () => {
  router.push({name: 'applicationChat', params: {applicationId}})
}

onMounted(() => {
  fetchApplicationInfo()
  fetchOverview()
})
</script>

<style scoped>
</style>
