<template>
  <div class="p-4">
    <div class="flex items-center justify-between mb-4">
      <h4 class="font-bold" style="color: var(--p-text-color)">应用概览</h4>
    </div>
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <div class="lg:col-span-2 space-y-4">
        <Card :pt="{ root: { style: 'border: 1px solid var(--p-content-border-color) !important; box-shadow: var(--p-shadow-1); background: var(--p-content-background)' } }">
          <template #content>
            <div class="flex items-center mb-4">
              <div class="w-8 h-8 rounded-lg flex items-center justify-center mr-3 overflow-hidden">
                <img :src="resetUrl('./application.png')" alt="应用图标" class="w-full h-full object-contain"/>
              </div>
              <div class="flex-1">
                <div class="flex items-center">
                  <span class="text-[14px] font-semibold mr-2" style="color: var(--p-text-color)">{{ appInfo.name }}</span>
                  <span class="inline-flex items-center px-2 py-0.5 bg-green-50 text-green-700 rounded text-[12px]">{{ appInfo.status }}</span>
                </div>
                <div class="text-[12px] mt-1" style="color: var(--p-text-muted-color)">ID: {{ appInfo.id }} | {{ appInfo.updateTime }} 更新</div>
              </div>
            </div>
            <div class="flex flex-wrap gap-2">
              <Button label="公开链接" icon="pi pi-link" text class="text-xs" @click="copyPublicLink"/>
              <Button label="嵌入第三方" icon="pi pi-external-link" text class="text-xs" @click="embedDialogRef?.open(applicationId)"/>
            </div>
          </template>
        </Card>
        <Card :pt="{ root: { style: 'border: 1px solid var(--p-content-border-color) !important; box-shadow: var(--p-shadow-1); background: var(--p-content-background)' } }">
          <template #content>
            <div class="flex items-center justify-between mb-4">
              <h4 class="text-sm font-semibold" style="color: var(--p-text-color)">监控统计</h4>
              <div class="flex items-center gap-3">
                <span v-if="selectedTimeRange === 'custom' && customStartTime && customEndTime" class="text-xs" style="color: var(--p-text-muted-color)">
                  {{ formatDate(customStartTime) }} - {{ formatDate(customEndTime) }}
                </span>
                <Dropdown v-model="selectedTimeRange" :options="timeRangeOptions" optionLabel="label" optionValue="value" class="w-auto text-xs" placeholder="选择时间范围"/>
              </div>
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              <div v-for="stat in [
                { icon: 'pi-comments', bg: 'var(--p-primary-100)', fg: 'var(--p-primary-color)', label: '对话数', value: overviewData.conversationCount },
                { icon: 'pi-comment', bg: 'var(--p-orange-100)', fg: 'var(--p-orange-500)', label: '消息总数', value: overviewData.messageCount },
                { icon: 'pi-clock', bg: 'var(--p-green-100)', fg: 'var(--p-green-500)', label: '平均响应时间', value: formatDuration(overviewData.avgDuration) },
                { icon: 'pi-key', bg: 'var(--p-cyan-100)', fg: 'var(--p-cyan-500)', label: 'Token 消耗', value: formatTokens(overviewData.totalTokens) }
              ]" :key="stat.label"
                class="flex items-center gap-3 rounded-xl p-3"
                :style="{ background: 'var(--p-content-background)', border: '1px solid var(--p-content-border-color)', boxShadow: 'var(--p-shadow-1)' }">
                <div class="w-9 h-9 rounded-lg flex items-center justify-center" :style="{ background: stat.bg }">
                  <i class="pi text-sm" :class="stat.icon" :style="{ color: stat.fg }"></i>
                </div>
                <div>
                  <p class="text-xs" style="color: var(--p-text-muted-color)">{{ stat.label }}</p>
                  <p class="text-base font-bold" style="color: var(--p-text-color)">{{ stat.value }}</p>
                </div>
              </div>
            </div>
            <div class="rounded-lg p-3 mt-3" style="border: 1px solid var(--p-content-border-color)">
              <p class="text-xs mb-2" style="color: var(--p-text-muted-color)">消息量趋势</p>
              <div class="h-36"><canvas ref="userChartCanvas"></canvas></div>
            </div>
            <div class="rounded-lg p-3 mt-3" style="border: 1px solid var(--p-content-border-color)">
              <p class="text-xs mb-2" style="color: var(--p-text-muted-color)">Token消耗趋势</p>
              <div class="h-36"><canvas ref="tokenChartCanvas"></canvas></div>
            </div>
          </template>
        </Card>
      </div>
      <div class="order-first lg:order-none space-y-4">
        <div
          class="rounded-lg p-5 text-white shadow-md"
          style="background: linear-gradient(135deg, var(--p-primary-color), var(--p-primary-600));">
          <div class="text-[16px] font-bold mb-3">快速开始对话</div>
          <div class="text-sm mb-5 leading-relaxed opacity-90">通过对话 Agent 帮助您分析数据并导出 Word 报表。
          </div>
          <Button label="进入聊天界面" severity="primary" outlined class="w-full !border-white/50 !text-white hover:!bg-white/15" @click="goToChat"/>
        </div>

        <Card :pt="{ root: { style: 'border: 1px solid var(--p-content-border-color) !important; box-shadow: var(--p-shadow-1); background: var(--p-content-background)' }, body: { style: 'padding: 0.5rem 1rem 1rem' } }">
          <template #content>
            <div class="flex items-center justify-between mb-1">
              <div class="flex items-center">
                <i class="pi pi-flame text-orange-500 mr-2"></i>
                <h4 class="text-sm font-semibold" style="color: var(--p-text-color)">最近提问</h4>
              </div>
              <Button icon="pi pi-refresh" text size="small" class="!w-7 !h-7 !p-0"/>
            </div>
            <div v-if="overviewData.recentQuestions.length" class="space-y-2 mb-4">
              <div v-for="(item, index) in overviewData.recentQuestions" :key="index"
                class="flex items-center justify-between rounded-lg px-3 py-2.5 bg-surface-100 dark:bg-surface-800">
                <div class="flex items-center">
                  <span class="inline-flex items-center justify-center w-5 h-5 rounded-md text-xs font-medium mr-3"
                    :class="index === 0 ? 'bg-orange-50 text-orange-500 dark:bg-orange-50 dark:text-orange-400' : index === 1 ? 'bg-primary-100 text-primary-600 dark:bg-primary-50 dark:text-primary-400' : index === 2 ? 'bg-red-50 text-red-500 dark:bg-red-50 dark:text-red-400' : 'bg-surface-200 text-surface-600 dark:bg-surface-700 dark:text-surface-400'"
                  >{{ index + 1 }}</span>
                  <span class="text-xs text-surface-700 dark:text-surface-200">{{ parseQuestionContent(item.content) }}</span>
                </div>
                <span class="text-xs text-surface-400 dark:text-surface-500">{{ item.createTime?.slice(5, 16) }}</span>
              </div>
            </div>
            <div v-else class="text-center text-xs py-6" style="color: var(--p-text-muted-color)">暂无数据</div>
          </template>
        </Card>
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
          <label class="block text-xs mb-1" style="color: var(--p-text-muted-color)">开始时间</label>
          <DatePicker
            v-model="customStartTime"
            dateFormat="yyyy-mm-dd"
            class="w-full text-sm"
            :showIcon="true"
          />
        </div>
        <div class="flex-1">
          <label class="block text-xs mb-1" style="color: var(--p-text-muted-color)">结束时间</label>
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
        <Button label="确定" severity="success" @click="confirmCustomTime" class="text-xs"/>
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

const parseQuestionContent = (content: any): string => {
  try {
    const items = typeof content === 'string' ? JSON.parse(content) : content
    if (!Array.isArray(items) || items.length === 0) return '暂无内容'
    const item = items[0]
    if (item.content) return truncate(item.content)
    if (item.texts?.length) return truncate(item.texts[0].text || item.texts[0].content || '')
    if (item.images?.length) return '[图片]'
    if (item.files?.length) return `[文件] ${item.files[0].name || ''}`
    if (item.videos?.length) return '[视频]'
    return truncate(JSON.stringify(item))
  } catch {
    return typeof content === 'string' ? truncate(content) : '暂无内容'
  }
}

const truncate = (s: string) => s.length > 30 ? s.slice(0, 30) + '...' : s

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
