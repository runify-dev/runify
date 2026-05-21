<template>
  <div class="login-form-container w-full">
    <div class="logo-bg w-16 h-16 md:w-[70px] md:h-[70px] mx-auto mb-6"></div>
    <h2 class="login-title text-2xl md:text-[28px] font-semibold tracking-wide mb-12 text-center">
      {{ t('login.title') }}
    </h2>

    <slot></slot>

    <div class="login-footer mt-8 text-xs text-center">
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

.login-title {
  color: var(--p-surface-900);
}

.login-footer {
  color: var(--p-surface-500);
}
</style>
