<template>
  <div class="flex flex-col h-full">
    <!-- 顶部操作栏 -->
    <div class="flex items-center justify-between px-6 py-3 border-b border-surface-border">
      <span class="text-lg font-bold">{{ t('integration.details.title') }}</span>
      <Button :label="t('common.save')" icon="pi pi-save" @click="save"/>
    </div>

    <!-- 表单内容 -->
    <div v-loading="loading" class="flex-1 overflow-auto px-6 py-6 flex flex-col gap-6 max-w-3xl">
      <!-- 平台类型 -->
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">{{ t('integration.details.type') }}</label>
        <Tag :value="typeMeta?.label || formData.type" severity="info"/>
      </div>

      <!-- 绑定应用(可换) -->
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">{{ t('integration.details.application') }}</label>
        <Select
          v-model="formData.applicationId"
          :options="applicationOptions"
          optionLabel="name"
          optionValue="id"
          filter
          fluid
          :placeholder="t('integration.form.applicationPlaceholder')"
        />
      </div>

      <!-- 微信(个人号/iLink) 扫码登录 -->
      <div v-if="isWeixin" class="flex flex-col gap-2">
        <label class="text-sm font-semibold text-color">微信登录</label>
        <div v-if="connectedAccount" class="text-sm text-green-600">✅ 已连接微信，account={{ connectedAccount }}</div>
        <div class="flex items-start gap-4">
          <img v-if="qrImg" :src="qrImg" class="w-44 h-44 border rounded" alt="QR"/>
          <div class="flex flex-col gap-2">
            <Button :label="qrImg ? '刷新二维码' : '获取登录二维码'" icon="pi pi-qrcode" size="small" @click="fetchQr"/>
            <small v-if="qrImg" class="text-muted-color">用手机微信扫码并确认。状态：{{ weixinStatusText }}</small>
            <small class="text-orange-500">注意：个人号为非官方接口，有封号风险。</small>
          </div>
        </div>
      </div>

      <!-- 回调地址(只读) -->
      <div v-if="!isWeixin" class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">{{ t('integration.details.callbackUrl') }}</label>
        <InputGroup>
          <InputText :model-value="callbackUrl" readonly fluid/>
          <Button icon="pi pi-copy" severity="secondary" @click="copyCallback"/>
        </InputGroup>
        <small class="text-muted-color">{{ t('integration.details.callbackHint') }}</small>
      </div>

      <!-- 凭证 -->
      <div v-for="f in fields" :key="f.field" class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">{{ f.label }}</label>
        <Password
          v-if="f.secret"
          v-model="formData.config[f.field]"
          :feedback="false"
          toggleMask
          fluid
          :placeholder="f.placeholder"
          inputClass="w-full"
        />
        <InputText
          v-else
          v-model="formData.config[f.field]"
          :placeholder="f.placeholder"
          fluid
        />
      </div>

      <!-- 启用 -->
      <div class="flex items-center justify-between">
        <label class="text-sm font-semibold text-color">{{ t('integration.details.enabled') }}</label>
        <ToggleSwitch v-model="formData.enabled"/>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import {computed, onMounted, onBeforeUnmount, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {TreeCommonAPI} from '@/api/tree'
import IntegrationAPI, {getTypeMeta} from '@/api/integration'
import {ROOT_FOLDER_ID} from '@/constants/common'
import bus from '@/bus'
import { t } from '@/locales'

const treeCommonAPI = new TreeCommonAPI('integration')
const loading = ref<boolean>(false)
const route = useRoute()

const resourceId = computed(() => {
  const {params: {id}} = route as any
  return id
})

const applicationOptions = ref<Array<{id: string; name: string}>>([])
const formData = ref<Record<string, any>>({type: '', applicationId: '', enabled: true, config: {}})

const typeMeta = computed(() => getTypeMeta(formData.value.type))
const fields = computed(() => typeMeta.value?.fields || [])
const isWeixin = computed(() => formData.value.type === 'WEIXIN')
const connectedAccount = computed(() => formData.value.config?.accountId || '')

// ============ 微信(个人号) 扫码登录 ============
const qrImg = ref<string>('')
const weixinStatus = ref<string>('')
let qrcodeValue = ''
let qrBaseUrl = ''
let pollTimer: any = null

const weixinStatusText = computed(() => ({
  wait: '等待扫码', scaned: '已扫码，请在手机确认', scaned_but_redirect: '连接中…',
  expired: '二维码已过期', confirmed: '已确认'
} as Record<string, string>)[weixinStatus.value] || weixinStatus.value)

const stopPoll = () => { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }

const fetchQr = () => {
  stopPoll()
  IntegrationAPI.weixinQrcode(resourceId.value).then((ok) => {
    qrcodeValue = ok.data.qrcode
    const scan = ok.data.qrcode_img_content || ok.data.qrcode
    qrImg.value = `https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=${encodeURIComponent(scan)}`
    qrBaseUrl = ''
    weixinStatus.value = 'wait'
    pollTimer = setInterval(pollStatus, 2000)
  })
}

const pollStatus = () => {
  IntegrationAPI.weixinQrcodeStatus(resourceId.value, qrcodeValue, qrBaseUrl).then((ok) => {
    const s = ok.data || {}
    weixinStatus.value = s.status || ''
    if (s.status === 'scaned_but_redirect' && s.redirect_host) {
      qrBaseUrl = 'https://' + s.redirect_host
    } else if (s.status === 'expired') {
      fetchQr()
    } else if (s.status === 'confirmed') {
      stopPoll()
      qrImg.value = ''
      bus.emit('message:success', '微信已连接')
      get()
    }
  }).catch(() => {})
}
const callbackUrl = computed(() => {
  if (!typeMeta.value) return ''
  return window.location.origin + typeMeta.value.callbackPath.replace('{id}', resourceId.value)
})

const copyCallback = () => {
  navigator.clipboard?.writeText(callbackUrl.value).then(() => {
    bus.emit('message:success', t('integration.details.copied'))
  })
}

const save = () => {
  const payload = {
    type: formData.value.type,
    applicationId: formData.value.applicationId,
    enabled: formData.value.enabled,
    config: formData.value.config
  }
  IntegrationAPI.edit(resourceId.value, payload, loading).then(() => {
    bus.emit('message:success', t('integration.details.saveSuccess'))
  })
}

const get = () => {
  treeCommonAPI.getResource(resourceId.value, loading).then((ok) => {
    formData.value = {
      type: ok.data.type,
      applicationId: ok.data.applicationId,
      enabled: ok.data.enabled ?? true,
      config: ok.data.config || {}
    }
  })
}

watch(resourceId, () => get())

onMounted(() => {
  IntegrationAPI.listAllApplications(ROOT_FOLDER_ID, loading).then((list) => {
    applicationOptions.value = list
  })
  get()
})

onBeforeUnmount(() => stopPoll())
</script>
<style lang="scss" scoped></style>
