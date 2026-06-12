<template>
  <div class="h-full flex flex-col overflow-hidden">
    <!-- 顶栏 -->
    <header class="shrink-0 h-14 px-6 flex items-center justify-between bg-[var(--p-content-background)]/80 backdrop-blur-md border-b border-[var(--p-content-border-color)] z-10">
      <div class="flex items-center gap-2.5">
        <img src="@/assets/app.svg" class="w-7 h-7" />
        <span class="text-base font-bold text-[var(--p-text-color)] tracking-tight">Run</span>
      </div>

      <div class="flex items-center gap-2">
        <div v-if="store.isLogged && store.isAnonymous" class="flex items-center gap-2">
          <span class="text-xs text-[var(--p-text-muted-color)]">{{ t('conversation.anonymousAccess') }}</span>
          <Button :label="t('login.login')" size="small" severity="secondary" outlined @click="showLoginDialog = true" />
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
            style="background: var(--p-primary-color); color: var(--p-surface-0)"
          />
          <span class="text-sm text-[var(--p-text-color)]">{{ store.displayName }}</span>
          <Button icon="pi pi-sign-out" severity="secondary" variant="text" size="small" class="!w-7 !h-7 !p-0" @click="handleLogout" />
        </div>
        <Button v-if="!store.isLogged" :label="t('login.login')" size="small" @click="showLoginDialog = true" />
      </div>
    </header>

    <!-- 主内容 -->
    <div class="page-scroll flex-1 overflow-y-auto">
      <!-- Hero -->
      <div class="hero-section relative overflow-hidden">
        <div class="absolute inset-0 hero-bg" />
        <div class="absolute inset-0 hero-grid" />
        <!-- 顶部边缘高光，柔化与卡片区的衔接 -->
        <div class="absolute inset-x-0 bottom-0 h-px bg-gradient-to-r from-transparent via-[var(--p-primary-color)]/25 to-transparent" />

        <div class="relative px-8 pt-12 pb-10 text-center">
          <div class="inline-flex items-center gap-1.5 px-3 py-1 mb-4 rounded-full bg-[var(--p-content-background)]/50 backdrop-blur-sm border border-[var(--p-primary-color)]/25 text-[var(--p-primary-color)] text-xs font-medium shadow-sm">
            <i class="pi pi-sparkles text-[10px]" />
            AI 智能体平台
          </div>
          <h1 class="text-3xl font-extrabold text-[var(--p-text-color)] mb-2.5 tracking-tight leading-tight">
            探索你的
            <span class="bg-gradient-to-r from-[var(--p-primary-500)] to-[#28E3C4] bg-clip-text text-transparent">AI 智能体</span>
          </h1>
          <p class="text-sm text-[var(--p-text-muted-color)] mb-7 max-w-md mx-auto leading-relaxed">
            发现并使用不同能力的智能体，开启高效智能的对话体验
          </p>
          <div class="max-w-2xl mx-auto">
            <div class="relative group">
              <div class="absolute -inset-0.5 bg-gradient-to-r from-primary-400 to-[#28E3C4] rounded-2xl opacity-0 group-hover:opacity-25 blur-sm transition-opacity duration-300" />
              <div class="relative flex items-center bg-[var(--p-content-background)] rounded-xl border border-[var(--p-content-border-color)] shadow-sm group-hover:border-[var(--p-primary-color)]/40 transition-colors duration-300">
                <i class="pi pi-search text-[var(--p-text-muted-color)] text-sm ml-4" />
                <input
                  v-model="searchText"
                  type="text"
                  placeholder="搜索你感兴趣的智能体..."
                  class="flex-1 px-3 py-3.5 text-sm bg-transparent outline-none text-[var(--p-text-color)] placeholder:text-[var(--p-text-muted-color)]"
                />
                <span class="text-xs text-[var(--p-text-muted-color)] mr-4 whitespace-nowrap">{{ total }} 个智能体</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 卡片区域 -->
      <div class="px-8 pt-7 pb-10">
        <div v-loading="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5 min-h-[200px]">
          <template v-for="(item, index) in filteredList" :key="item.id">
            <div
              class="agent-card group relative flex flex-col rounded-2xl cursor-pointer overflow-hidden"
              :style="{ animationDelay: `${index * 60}ms` }"
              @click="goToAgent(item.id)"
            >
              <!-- 顶部渐变条 -->
              <div class="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-primary-400 via-primary-500 to-[#28E3C4] opacity-0 group-hover:opacity-100 transition-opacity duration-300" />

              <!-- 卡片头部 -->
              <div class="px-5 pt-5">
                <div class="flex items-start justify-between">
                  <div class="relative">
                    <div class="icon-tile w-12 h-12 rounded-xl flex items-center justify-center overflow-hidden transition-all duration-300 group-hover:shadow-lg">
                      <Avatar
                        v-if="item.icon"
                        :image="item.icon"
                        shape="square"
                        size="large"
                        class="!w-full !h-full !rounded-xl object-cover"
                      />
                      <span v-else class="text-lg font-bold bg-gradient-to-br from-primary-500 to-primary-600 bg-clip-text text-transparent">
                        {{ item.name?.substring(0, 1) }}
                      </span>
                    </div>
                    <div class="absolute -bottom-1 -right-1 w-3.5 h-3.5 rounded-full bg-emerald-400 border-2 border-[var(--p-content-background)] shadow-sm" />
                  </div>
                  <div class="chip flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium opacity-0 group-hover:opacity-100 translate-x-1 group-hover:translate-x-0 transition-all duration-200">
                    对话
                    <i class="pi pi-arrow-right text-[10px]" />
                  </div>
                </div>
              </div>

              <!-- 卡片内容 -->
              <div class="flex-1 px-5 pt-4 pb-5">
                <h3 class="text-[15px] font-semibold text-[var(--p-text-color)] mb-1.5 group-hover:text-[var(--p-primary-color)] transition-colors duration-200 line-clamp-1">
                  {{ item.name }}
                </h3>
                <p class="text-xs text-[var(--p-text-muted-color)] leading-relaxed line-clamp-2 min-h-[2rem]">
                  {{ item.desc || '暂无描述' }}
                </p>
              </div>

              <!-- 卡片底部 -->
              <div class="card-footer px-5 py-3 flex items-center justify-between">
                <span class="chip inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full">
                  <i class="pi pi-bolt text-[9px]" />
                  智能体
                </span>
                <span v-if="item.allowAnonymousAccess" class="inline-flex items-center gap-1 text-[11px] text-[var(--p-text-muted-color)]">
                  <i class="pi pi-globe text-[9px]" />
                  公开
                </span>
              </div>
            </div>
          </template>

          <!-- 空状态 -->
          <div v-if="!loading && filteredList.length === 0" class="col-span-full flex flex-col items-center justify-center py-20">
            <div class="empty-icon w-20 h-20 rounded-3xl flex items-center justify-center mb-5">
              <i class="pi pi-inbox text-4xl text-[var(--p-text-muted-color)]" />
            </div>
            <p class="text-base font-medium text-[var(--p-text-color)] mb-1.5">暂无智能体</p>
            <p class="text-sm text-[var(--p-text-muted-color)]">还没有可用的智能体，请稍后再来</p>
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
        <i class="pi pi-times text-[var(--p-text-muted-color)] hover:text-[var(--p-text-color)] transition-colors" />
      </template>
      <div class="flex flex-col items-center text-center mb-6">
        <div class="relative mb-4">
          <div class="login-logo w-16 h-16 rounded-2xl flex items-center justify-center shadow-lg">
            <img src="@/assets/app.svg" class="w-9 h-9" />
          </div>
          <div class="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-emerald-400 border-2 border-[var(--p-content-background)] flex items-center justify-center">
            <i class="pi pi-check text-[8px] text-white" />
          </div>
        </div>
        <h2 class="text-lg font-bold text-[var(--p-text-color)] mb-1">欢迎回来</h2>
        <p class="text-xs text-[var(--p-text-muted-color)]">登录后开始与智能体对话</p>
      </div>

      <div class="flex flex-col gap-4">
        <div>
          <label class="text-xs font-medium text-[var(--p-text-muted-color)] mb-1.5 block">用户名</label>
          <InputText v-model="loginForm.username" placeholder="请输入用户名" class="w-full !rounded-lg" @keyup.enter="handleLogin" />
        </div>
        <div>
          <label class="text-xs font-medium text-[var(--p-text-muted-color)] mb-1.5 block">密码</label>
          <Password v-model="loginForm.password" placeholder="请输入密码" class="w-full" :input-class="'w-full !rounded-lg'" :feedback="false" toggle-mask @keyup.enter="handleLogin" />
        </div>
        <Button
          label="登 录"
          class="w-full mt-1 !rounded-lg !h-11 !font-semibold !text-sm !bg-gradient-to-r !from-primary-500 !to-primary-600 !border-none hover:!shadow-lg hover:!shadow-primary-500/30 transition-shadow"
          :loading="loginLoading"
          :disabled="!loginForm.username || !loginForm.password"
          @click="handleLogin"
        />
      </div>

      <div class="mt-5 pt-4 border-t border-[var(--p-content-border-color)] text-center">
        <p class="text-[11px] text-[var(--p-text-muted-color)]">登录即表示同意使用条款</p>
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
import { t } from '@/locales'

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
/* ============================================================
 * 全部基于会随主题翻转的语义 token + color-mix，亮暗自适应。
 * ============================================================ */

/* 页面底：作为卡片的「衬底」，比卡片略暗一档，制造层次 */
.page-scroll {
  background: var(--p-content-background);
}

/* ---------- Hero：一束干净的顶部光晕 ---------- */
.hero-section {
  position: relative;
  border-bottom: 1px solid color-mix(in srgb, var(--p-content-border-color) 60%, transparent);
}

.hero-bg {
  background:
    radial-gradient(
      120% 90% at 50% -10%,
      color-mix(in srgb, var(--p-primary-color) 16%, transparent) 0%,
      color-mix(in srgb, var(--p-primary-color) 6%, transparent) 35%,
      transparent 65%
    ),
    var(--p-content-background);
}

.hero-grid {
  opacity: 0.04;
  background-image: radial-gradient(circle, var(--p-primary-color) 1px, transparent 1px);
  background-size: 26px 26px;
  -webkit-mask-image: radial-gradient(120% 80% at 50% 0%, #000 30%, transparent 75%);
  mask-image: radial-gradient(120% 80% at 50% 0%, #000 30%, transparent 75%);
}

/* ---------- 复用：主色 chip ---------- */
.chip {
  background: color-mix(in srgb, var(--p-primary-color) 12%, transparent);
  color: var(--p-primary-color);
}

/* ---------- 卡片：从背景里「浮」出来 ---------- */
.agent-card {
  /* 比页面底色亮/暗一档，形成可见层次 */
  background: color-mix(in srgb, var(--p-text-color) 5%, var(--p-content-background));
  border: 1px solid color-mix(in srgb, var(--p-text-color) 9%, transparent);
  box-shadow: 0 1px 2px color-mix(in srgb, var(--p-text-color) 6%, transparent);
  opacity: 0;
  animation: cardIn 0.4s ease-out forwards;
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease, background 0.3s ease;
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.agent-card:hover {
  transform: translateY(-4px);
  background: color-mix(in srgb, var(--p-text-color) 7%, var(--p-content-background));
  border-color: color-mix(in srgb, var(--p-primary-color) 35%, transparent);
  box-shadow:
    0 16px 36px -12px color-mix(in srgb, var(--p-primary-color) 22%, transparent),
    0 4px 12px -6px color-mix(in srgb, var(--p-text-color) 14%, transparent);
}

/* 图标底座 */
.icon-tile {
  background: linear-gradient(
    to bottom right,
    color-mix(in srgb, var(--p-primary-color) 14%, transparent),
    color-mix(in srgb, var(--p-primary-color) 5%, transparent)
  );
  border: 1px solid color-mix(in srgb, var(--p-primary-color) 22%, transparent);
}
.group:hover .icon-tile {
  border-color: color-mix(in srgb, var(--p-primary-color) 45%, transparent);
  box-shadow: 0 8px 20px -6px color-mix(in srgb, var(--p-primary-color) 24%, transparent);
}

/* 卡片底部：仅一条分隔线，不再填浅色块 */
.card-footer {
  border-top: 1px solid color-mix(in srgb, var(--p-text-color) 8%, transparent);
}

/* 空状态图标 */
.empty-icon {
  background: color-mix(in srgb, var(--p-text-color) 8%, transparent);
}

/* 登录弹框 logo 底座 */
.login-logo {
  background: linear-gradient(
    to bottom right,
    color-mix(in srgb, var(--p-primary-color) 14%, var(--p-content-background)),
    color-mix(in srgb, var(--p-primary-color) 6%, var(--p-content-background))
  );
  border: 1px solid color-mix(in srgb, var(--p-primary-color) 22%, transparent);
  box-shadow: 0 10px 24px -10px color-mix(in srgb, var(--p-primary-color) 30%, transparent);
}
</style>
