<template>
  <div class="login-form-container w-full">
    <div class="logo-bg w-16 h-16 md:w-[70px] md:h-[70px] mx-auto mb-6"></div>
    <h2 class="text-2xl md:text-[28px] font-semibold tracking-wide mb-12 text-center">
      {{ t('login.title') }}
    </h2>

    <slot></slot>

    <div class="mt-8 text-xs text-white/60 text-center">
      {{ t('login.footer') }}
    </div>
  </div>
</template>
<script setup lang="ts">
import { t } from '@/locales'

defineOptions({ name: 'LoginContainer' })
</script>
<style lang="scss" scoped>
.logo-bg {
  background-image: url('/ui/login_icon.png');
  background-size: contain;
  background-repeat: no-repeat;
}
</style>
