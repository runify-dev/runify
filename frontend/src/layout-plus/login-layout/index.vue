<template>
  <!-- 整体背景和居中容器 -->
  <div class="h-screen gradient-bg text-white flex items-center justify-center font-inter">
    <!-- 登录主容器 -->
    <div
      class="flex bg-white/5 backdrop-blur-sm rounded-2xl shadow-2xl overflow-hidden w-[1000px] h-[600px] max-w-[95vw] max-h-[95vh]"
    >
      <!-- 左侧背景区域 (仅在中等屏幕及以上显示) -->
      <div
        class="left-bg hidden md:flex flex-1 flex-col items-center justify-center text-center relative"
      >
        <div class="left-overlay absolute inset-0"></div>
        <div class="relative z-10 px-8">
          <h1 class="text-5xl font-bold mb-5 tracking-wider">Runify</h1>
          <p class="text-lg leading-relaxed opacity-90 max-w-[300px]">
            让每一次知识检索<br />都丝滑如流
          </p>
        </div>
      </div>

      <!-- 右侧内容区域 -->
      <div
        class="flex-1 md:flex-none md:w-[400px] min-h-[600px] p-10 md:p-[60px_45px] bg-white/10 backdrop-blur-lg border-l border-white/10 flex flex-col justify-center"
      >
        <slot></slot>
      </div>
    </div>
    <Toast />
  </div>
</template>
<script setup lang="ts">
defineProps({
  lang: {
    type: Boolean,
    default: true
  }
})
defineOptions({ name: 'LoginLayout' })
</script>
<style lang="scss" scope>
.gradient-bg {
  background: linear-gradient(135deg, rgb(29 43 100 / 79%), rgb(248, 205, 218));
  //另一种背景变色#f8cdda  #feedf3
}

.left-bg {
  background: url('/ui/login.jpg') no-repeat center center;
  background-size: cover;
}

.left-overlay::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(29, 43, 100, 0.6) 0%, rgba(29, 43, 100, 0.3) 100%);
}
</style>
