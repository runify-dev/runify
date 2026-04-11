<template>
  <component v-if="authenticationType" :is="kw[authenticationType]"></component>
</template>
<script setup lang="ts">
import conversationAPI from '@/api/conversation'
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import anonymous from './impl/anonymous.vue'
const route = useRoute()
const applicationId = route.params.applicationId as string
const authenticationType = ref<string>()
const kw: any = {
  anonymous
}
onMounted(() => {
  conversationAPI.config(applicationId).then((ok) => {
    authenticationType.value = ok.data.authenticationType
  })
})
</script>
<style lang="scss" scoped></style>
