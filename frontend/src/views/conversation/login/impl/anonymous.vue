<template>
  <div></div>
</template>
<script setup lang="ts">
import { onMounted } from 'vue'
import conversationAPI from '@/api/conversation'
import { useRoute, useRouter } from 'vue-router'
import FingerprintJS from '@fingerprintjs/fingerprintjs'
import useStore from '@/stores/converstaion/index'
const router = useRouter()
const route = useRoute()
const applicationId = route.params.applicationId as string
onMounted(() => {
  // 初始化
  FingerprintJS.load()
    .then((fp) => {
      return fp.get()
    })
    .then((result) => {
      conversationAPI.anonymousLogin(applicationId, result.visitorId).then((ok) => {
        const { conversationToken } = useStore()
        conversationToken.setToken(applicationId, ok.data)
        router.push({ name: 'conversation' })
      })
    })
})
</script>
<style lang="scss" scoped></style>
