<template>
  <div class="flip-card-container" :style="{ '--flip-duration': `${duration}ms` }">
    <div class="flip-card" :class="{ 'is-flipped': isFlipped }">
      <!-- 正面 -->
      <div class="flip-card-face flip-card-front">
        <slot name="front">
          <div class="default-front">
            <h3>正面内容</h3>
            <p>点击卡片翻转查看背面</p>
          </div>
        </slot>
      </div>

      <!-- 背面 -->
      <div class="flip-card-face flip-card-back">
        <slot name="back">
          <div class="default-back">
            <h3>背面内容</h3>
            <p>再次点击翻转回正面</p>
          </div>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  modelValue: boolean
  // 翻转动画时长（毫秒）
  duration?: number
  // 是否禁用点击翻转
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  duration: 600,
  initialFlipped: false,
  disabled: false
})
const isFlipped = computed({
  get: () => {
    return props.modelValue
  },
  set: (event: boolean) => {
    emit('update:modelValue', event)
  }
})
const emit = defineEmits<{
  'update:modelValue': [isFlipped: boolean]
}>()

// 提供手动控制方法
const flip = () => {
  isFlipped.value = true
  emit('update:modelValue', true)
}

const unflip = () => {
  isFlipped.value = false
  emit('update:modelValue', false)
}

const toggle = () => {
  isFlipped.value = !isFlipped.value
  emit('update:modelValue', isFlipped.value)
}

// 暴露方法给父组件使用
defineExpose({
  flip,
  unflip,
  toggle
})
</script>

<style scoped>
.flip-card-container {
  perspective: 1000px;
  width: 100%;
  height: 100%;
}

.flip-card {
  position: relative;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
  transition: transform var(--flip-duration) cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.flip-card.is-flipped {
  transform: rotateY(180deg);
}

.flip-card-face {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  display: flex;
  flex-direction: column;
}

.flip-card-front {
  color: white;
}

.flip-card-back {
  color: white;
  transform: rotateY(180deg);
}

/* 默认样式 */
.default-front,
.default-back {
  text-align: center;
}

.default-front h3,
.default-back h3 {
  font-size: 24px;
}

.default-front p,
.default-back p {
  margin: 0;
  opacity: 0.9;
  font-size: 16px;
}

/* 禁用状态 */
.flip-card.disabled {
  cursor: not-allowed;
  opacity: 0.7;
}
</style>
