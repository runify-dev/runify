<template>
  <div class="login-form-container w-full">
    <!-- Logo 和标题 -->
    <div class="logo-bg w-16 h-16 md:w-[70px] md:h-[70px] mx-auto mb-6"></div>
    <h2 class="text-2xl md:text-[28px] font-semibold tracking-wide mb-12 text-center">欢迎登录 Runify</h2>

    <!-- 表单内容插槽 -->
    <slot></slot>

    <!-- 底部信息 -->
    <div class="mt-8 text-xs text-white/60 text-center">
      由 Runify AI 驱动 · 智能知识中枢
    </div>
  </div>
</template>
<script setup lang="ts">
withDefaults(defineProps<
    {
        title?: string,
        subTitle?: string
    }>(), {
    title: "Runify",
    subTitle: "欢迎使用 Runify 系统"
});


</script>
<style lang="scss" scope>
/* Logo 背景图 */
.logo-bg {
  background-image: url('/ui/login_icon.png');
  background-size: contain;
  background-repeat: no-repeat;
}
</style>
