
<template>
  <div class="h-full flex flex-col overflow-hidden">
    <!-- 顶栏 -->
    <header class="shrink-0 h-14 px-6 flex items-center justify-between bg-surface-0/80 backdrop-blur-md border-b border-surface-200/60 z-10">
      <div class="flex items-center gap-2.5">
        <img src="@/assets/app.svg" class="w-7 h-7" />
        <span class="text-base font-bold text-surface-900 tracking-tight">Run</span>
      </div>

      <div class="flex items-center gap-2">
        <div v-if="store.isLogged && store.isAnonymous" class="flex items-center gap-2">
          <span class="text-xs text-surface-400">匿名访问中</span>
          <Button label="账号登录" size="small" severity="secondary" outlined @click="showLoginDialog = true" />
        </div>
        <div v-else-if="store.isLogged" class="flex items-center gap-2">
          <Avatar
            v-if="store.profile?.user?.icon"
            :image="store.profile.user.icon"
            shape="circle"
            size="normal"
          />
          <Avatar
            v-else
            :label="store.displayName.substring(0, 1)"
            shape="circle"
            size="normal"
            style="background: #3370ff; color: #fff"
          />
          <span class="text-sm text-surface-700">{{ store.displayName }}</span>
          <Button icon="pi pi-sign-out" severity="secondary" variant="text" size="small" class="!w-7 !h-7 !p-0" @click="handleLogout" />
        </div>
        <Button v-if="!store.isLogged" label="请登录" size="small" @click="showLoginDialog = true" />
      </div>
    </header>

    <!-- 主内容 -->
    <div class="flex-1 overflow-y-auto">
      <!-- Hero -->
      <div class="hero-section relative overflow-hidden">
        <!-- 装饰背景 -->
        <div class="absolute inset-0 bg-gradient-to-br from-[#eef4ff] via-[#f5f8ff] to-[#e8f4f0]" />
        <div class="absolute inset-0">
          <div class="absolute top-8 left-[15%] w-72 h-72 bg-primary-200/20 rounded-full blur-[80px] animate-float" />
          <div class="absolute bottom-0 right-[10%] w-80 h-80 bg-[#28E3C4]/10 rounded-full blur-[80px] animate-float-delayed" />
          <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-primary-100/15 rounded-full blur-[100px]" />
        </div>
        <!-- 网格装饰 -->
        <div class="absolute inset-0 opacity-[0.03]" style="background-image: radial-gradient(circle, #3370ff 1px, transparent 1px); background-size: 24px 24px;" />

        <div class="relative px-8 pt-10 pb-8 text-center">
          <div class="inline-flex items-center gap-1.5 px-3 py-1 mb-3 rounded-full bg-surface-0/60 backdrop-blur-sm border border-primary-200/40 text-primary-600 text-xs font-medium shadow-sm">
            <i class="pi pi-sparkles text-[10px]" />
            AI 智能体平台
          </div>
          <h1 class="text-3xl font-extrabold text-surface-900 mb-2 tracking-tight leading-tight">
            探索你的
            <span class="bg-gradient-to-r from-primary-500 to-[#28E3C4] bg-clip-text text-transparent">AI 智能体</span>
          </h1>
          <p class="text-sm text-surface-500 mb-6 max-w-md mx-auto leading-relaxed">
            发现并使用不同能力的智能体，开启高效智能的对话体验
          </p>
          <div class="max-w-3xl mx-auto">
            <div class="relative group">
              <div class="absolute -inset-0.5 bg-gradient-to-r from-primary-400 to-[#28E3C4] rounded-2xl opacity-0 group-hover:opacity-30 blur-sm transition-opacity duration-300" />
              <div class="relative flex items-center bg-surface-0 rounded-xl border border-surface-200 shadow-sm group-hover:shadow-md transition-shadow duration-300">
                <i class="pi pi-search text-surface-400 text-sm ml-4" />
                <input
                  v-model="searchText"
                  type="text"
                  placeholder="搜索你感兴趣的智能体..."
                  class="flex-1 px-3 py-3.5 text-sm bg-transparent outline-none placeholder:text-surface-400"
                />
                <span class="text-xs text-surface-300 mr-4">{{ total }} 个智能体</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 卡片区域 -->
      <div class="px-8 pt-5 pb-10">
        <div v-loading="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5 min-h-[200px]">
          <template v-for="(item, index) in filteredList" :key="item.id">
            <div
              class="agent-card group relative flex flex-col rounded-2xl bg-surface-0 border border-surface-200/60 cursor-pointer overflow-hidden transition-all duration-300"
              :style="{ animationDelay: `${index * 60}ms` }"
              @click="goToAgent(item.id)"
            >
              <!-- 顶部渐变条 -->
              <div class="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-primary-400 via-primary-500 to-[#28E3C4] opacity-0 group-hover:opacity-100 transition-opacity duration-300" />

              <!-- 卡片头部 -->
              <div class="p-5 pb-0">
                <div class="flex items-start justify-between mb-4">
                  <div class="icon-wrapper relative">
                    <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-primary-50 to-primary-100/40 flex items-center justify-center border border-primary-100/60 group-hover:border-primary-200 group-hover:shadow-lg group-hover:shadow-primary-100/40 transition-all duration-300">
                      <Avatar
                        v-if="item.icon"
                        :image="item.icon"
                        shape="square"
                        size="large"
                        class="rounded-xl"
                      />
                      <span v-else class="text-xl font-bold bg-gradient-to-br from-primary-500 to-primary-600 bg-clip-text text-transparent">
                        {{ item.name?.substring(0, 1) }}
                      </span>
                    </div>
                    <div class="absolute -bottom-1 -right-1 w-4 h-4 rounded-full bg-emerald-400 border-2 border-surface-0 shadow-sm" />
                  </div>
                  <div class="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-primary-50 text-primary-600 text-xs font-medium opacity-0 group-hover:opacity-100 translate-x-1 group-hover:translate-x-0 transition-all duration-200">
                    对话
                    <i class="pi pi-arrow-right text-[10px]" />
                  </div>
                </div>
              </div>

              <!-- 卡片内容 -->
              <div class="flex-1 px-5 pb-4">
                <h3 class="text-[15px] font-semibold text-surface-900 mb-1.5 group-hover:text-primary-600 transition-colors duration-200">
                  {{ item.name }}
                </h3>
                <p class="text-xs text-surface-400 leading-relaxed line-clamp-2 min-h-[2rem]">
                  {{ item.desc || '暂无描述' }}
                </p>
              </div>

              <!-- 卡片底部 -->
              <div class="px-5 py-3 flex items-center justify-between border-t border-surface-100 bg-surface-50/30">
                <span class="inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full bg-primary-50 text-primary-600">
                  <i class="pi pi-bolt text-[9px]" />
                  智能体
                </span>
                <span v-if="item.allowAnonymousAccess" class="inline-flex items-center gap-1 text-[11px] text-surface-400">
                  <i class="pi pi-globe text-[9px]" />
                  公开
                </span>
              </div>
            </div>
          </template>

          <!-- 空状态 -->
          <div v-if="!loading && filteredList.length === 0" class="col-span-full flex flex-col items-center justify-center py-20">
            <div class="w-20 h-20 rounded-3xl bg-surface-100 flex items-center justify-center mb-5">
              <i class="pi pi-inbox text-4xl text-surface-300" />
            </div>
            <p class="text-base font-medium text-surface-600 mb-1.5">暂无智能体</p>
            <p class="text-sm text-surface-400">还没有可用的智能体，请稍后再来</p>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="total > pageSize" class="flex justify-center mt-10">
          <Paginator :rows="pageSize" :total-records="total" @page="onPage" />
        </div>
      </div>
    </div>

    <!-- 登录弹框 -->
    <Dialog v-model:visible="showLoginDialog" modal :closable="true" :draggable="false" :style="{ width: '400px' }" :pt="{ content: { class: 'pt-0' } }">
      <template #header><span /></template>
      <template #closeicon>
        <i class="pi pi-times text-surface-400 hover:text-surface-700 transition-colors" />
      </template>
      <!-- 品牌头部 -->
      <div class="flex flex-col items-center text-center mb-6">
        <div class="relative mb-4">
          <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-primary-50 to-primary-100/60 flex items-center justify-center border border-primary-100/80 shadow-lg shadow-primary-100/30">
            <img src="@/assets/app.svg" class="w-9 h-9" />
          </div>
          <div class="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-emerald-400 border-2 border-surface-0 flex items-center justify-center">
            <i class="pi pi-check text-[8px] text-white" />
          </div>
        </div>
        <h2 class="text-lg font-bold text-surface-900 mb-1">欢迎回来</h2>
        <p class="text-xs text-surface-400">登录后开始与智能体对话</p>
      </div>

      <!-- 表单 -->
      <div class="flex flex-col gap-4">
        <div>
          <label class="text-xs font-medium text-surface-600 mb-1.5 block">用户名</label>
          <InputText v-model="loginForm.username" placeholder="请输入用户名" class="w-full !rounded-lg" @keyup.enter="handleLogin" />
        </div>
        <div>
          <label class="text-xs font-medium text-surface-600 mb-1.5 block">密码</label>
          <Password v-model="loginForm.password" placeholder="请输入密码" class="w-full" :input-class="'w-full !rounded-lg'" :feedback="false" toggle-mask @keyup.enter="handleLogin" />
        </div>
        <Button
          label="登 录"
          class="w-full mt-1 !rounded-lg !h-11 !font-semibold !text-sm !bg-gradient-to-r !from-primary-500 !to-primary-600 !border-none hover:!shadow-lg hover:!shadow-primary-200/50 transition-shadow"
          :loading="loginLoading"
          :disabled="!loginForm.username || !loginForm.password"
          @click="handleLogin"
        />
      </div>

      <!-- 底部 -->
      <div class="mt-5 pt-4 border-t border-surface-100 text-center">
        <p class="text-[11px] text-surface-300">登录即表示同意使用条款</p>
      </div>
    </Dialog>
      <Toast />
  </div>
</template>
<script setup lang="ts">
import conversationAPI from '@/api/conversation'
import useConversationTokenStore from '@/stores/converstaion/modules/conversation-token'
import bus from '@/bus/index'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

interface Application {
  id: string
  name: string
  desc: string
  icon: string
  allowAnonymousAccess: boolean
}

const router = useRouter()
const store = useConversationTokenStore()

const loading = ref(false)
const records = ref<Application[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10
const searchText = ref('')

const showLoginDialog = ref(false)
const loginLoading = ref(false)
const loginForm = ref({ username: '', password: '' })

const filteredList = computed(() => records.value)

let searchTimer: ReturnType<typeof setTimeout> | null = null
const onSearch = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    fetchApplications()
  }, 300)
}

const fetchApplications = () => {
  loading.value = true
  conversationAPI
    .queryApplication({ currentPage: currentPage.value, pageSize, name: searchText.value || undefined })
    .then((res) => {
      records.value = res.data.records || []
      total.value = res.data.total || 0
    })
    .catch(() => {})
    .finally(() => {
      loading.value = false
    })
}

const goToAgent = (id: string) => {
  router.push({ name: 'conversation-new', params: { applicationId: id } })
}

const onPage = (event: { page: number }) => {
  currentPage.value = event.page + 1
  fetchApplications()
}

watch(searchText, onSearch)

const afterLogin = () => {
  fetchApplications()
}

const handleLogin = () => {
  if (!loginForm.value.username || !loginForm.value.password) return
  loginLoading.value = true
  store
    .login(loginForm.value.username, loginForm.value.password)
    .then(() => {
      showLoginDialog.value = false
      afterLogin()
    })
    .catch((msg) => {
      bus.emit('message:error', msg)
    })
    .finally(() => {
      loginLoading.value = false
    })
}

const handleLogout = () => {
  store.clearToken()
  records.value = []
  total.value = 0
}

const on401 = () => {
  loading.value = false
  store.handle401().then(() => {
    afterLogin()
  }).catch(() => {
    showLoginDialog.value = true
  })
}

onMounted(() => {
  bus.on('auth:401', on401)
  if (store.isLogged) {
    store.fetchProfile().then(() => afterLogin()).catch(() => {
      showLoginDialog.value = true
    })
  } else {
    store.tryAnonymousLogin().then(() => {
      afterLogin()
    }).catch(() => {
      showLoginDialog.value = true
    })
  }
})

onUnmounted(() => {
  bus.off('auth:401', on401)
})
</script>
<style scoped>
.hero-section {
  position: relative;
}

.animate-float {
  animation: float 8s ease-in-out infinite;
}

.animate-float-delayed {
  animation: float 8s ease-in-out infinite;
  animation-delay: 3s;
}

@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-20px) scale(1.05); }
}

.agent-card {
  opacity: 0;
  animation: cardIn 0.4s ease-out forwards;
}

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.agent-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px -8px rgba(51, 112, 255, 0.12), 0 4px 12px -4px rgba(0, 0, 0, 0.05);
  border-color: rgba(51, 112, 255, 0.25);
}
</style>
