
<template>
  <div class="h-full">
    <Conversation type="CONVERSATION"></Conversation>

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
import { provide, ref, onMounted, onUnmounted } from 'vue'
import _conversationAPI from '@/api/conversation'
import { v4 as uuidv4 } from 'uuid'
import { useChatStore } from '@/components/conversation-plus/common/use-chat-store/index'
import Conversation from '@/components/conversation-plus/index.vue'
import useConversationTokenStore from '@/stores/converstaion/modules/conversation-token'
import bus from '@/bus/index'

const { conversationId, applicationId } = useChatStore('CONVERSATION')
const store = useConversationTokenStore()

const showLoginDialog = ref(false)
const loginLoading = ref(false)
const loginForm = ref({ username: '', password: '' })
const allowAnonymousAccess = ref(false)

const handleLogin = () => {
  if (!loginForm.value.username || !loginForm.value.password) return
  loginLoading.value = true
  store
    .login(loginForm.value.username, loginForm.value.password)
    .then(() => {
      showLoginDialog.value = false
      window.location.reload()
    })
    .catch((msg) => {
      bus.emit('message:error', msg)
    })
    .finally(() => {
      loginLoading.value = false
    })
}

// 根据 allowAnonymousAccess 决定匿名登录还是弹登录框
const evaluateAuth = (anonymousAccess: boolean) => {
  if (anonymousAccess) {
    store.tryAnonymousLogin().catch(() => {
      showLoginDialog.value = true
    })
  } else {
    showLoginDialog.value = true
  }
}

// 401 时根据身份决定策略
const on401 = () => {
  if (store.lastAuthWasAnonymous) {
    store.tryAnonymousLogin().catch(() => {
      showLoginDialog.value = true
    })
  } else {
    showLoginDialog.value = true
  }
}

// 退出登录
const onLogout = () => {
  store.clearToken()
  showLoginDialog.value = true
}

// 403 时重新验证权限
const on403 = () => {
  bus.emit('message:info', '权限变更，正在重新验证')
  _conversationAPI.authProfile(applicationId.value).then((res) => {
    allowAnonymousAccess.value = res.data.allowAnonymousAccess
    evaluateAuth(res.data.allowAnonymousAccess)
  }).catch(() => {
    showLoginDialog.value = true
  })
}

provide('conversationAPI', (question: any) => {
  if (conversationId.value) {
    return _conversationAPI.conversation(applicationId.value, conversationId.value, {
      content: question,
      workflowRunId: uuidv4()
    })
  }
  return Promise.reject()
})

onMounted(() => {
  bus.on('auth:401', on401)
  bus.on('auth:403', on403)
  bus.on('auth:logout', onLogout)

  // 先查应用权限配置，再根据用户身份决定策略
  _conversationAPI.authProfile(applicationId.value).then((res) => {
    allowAnonymousAccess.value = res.data.allowAnonymousAccess

    if (store.isLogged) {
      // 已登录，先拉取 profile 确认身份
      store.fetchProfile().then(() => {
        if (store.isAnonymous && !allowAnonymousAccess.value) {
          showLoginDialog.value = true
        }
      }).catch(() => {
        // token 失效，按未登录处理
        evaluateAuth(allowAnonymousAccess.value)
      })
    } else {
      evaluateAuth(allowAnonymousAccess.value)
    }
  })
})

onUnmounted(() => {
  bus.off('auth:401', on401)
  bus.off('auth:403', on403)
  bus.off('auth:logout', onLogout)
})
</script>
<style lang="scss">
#app {
  height: 100dvh;
}
</style>
