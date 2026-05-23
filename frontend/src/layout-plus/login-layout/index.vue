<template>
  <div class="login-layout h-screen flex items-center justify-center">
    <div
      ref="cardRef"
      class="login-card flex backdrop-blur-sm rounded-2xl shadow-2xl overflow-hidden w-[1000px] h-[600px] max-w-[95vw] max-h-[95vh]"
      :style="{ transform: `rotateX(${tiltX}deg) rotateY(${tiltY}deg)` }"
      @mousemove="onMouseMove"
      @mouseleave="onMouseLeave"
    >
      <div
        class="left-bg hidden md:flex flex-1 flex-col items-center justify-center text-center text-white relative"
      >
        <ParticleNetwork />
        <div class="left-overlay absolute inset-0"></div>
        <div class="relative z-10 px-8">
          <h1 class="login-title text-5xl font-bold mb-5 tracking-wider">
            <span
              v-for="(ch, i) in 'Runify'"
              :key="i"
              class="title-letter"
              :data-char="ch"
              :style="{ '--enter-delay': `${0.3 + i * 0.08}s` }"
            >{{ ch }}</span>
          </h1>
          <p class="login-subtitle text-lg leading-relaxed opacity-90 max-w-[300px]">
            {{ t('login.subtitle') }}
          </p>
        </div>
      </div>

      <div class="right-panel flex-1 md:flex-none md:w-[400px] min-h-[600px] p-10 md:p-[60px_45px] backdrop-blur-lg flex flex-col justify-center">
        <slot></slot>
      </div>
    </div>
    <Toast />
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { t } from '@/locales'
import ParticleNetwork from '@/views/login/ParticleNetwork.vue'

defineOptions({ name: 'LoginLayout' })

const cardRef = ref<HTMLElement>()
const tiltX = ref(0)
const tiltY = ref(0)
let rafId = 0

const MAX_TILT = 4
const SPEED = 0.08
const RESET_SPEED = 0.05

let targetX = 0
let targetY = 0
let currentX = 0
let currentY = 0

function update() {
  currentX += (targetX - currentX) * (targetX === 0 ? RESET_SPEED : SPEED)
  currentY += (targetY - currentY) * (targetY === 0 ? RESET_SPEED : SPEED)

  tiltX.value = currentX
  tiltY.value = currentY

  if (Math.abs(currentX) > 0.01 || Math.abs(currentY) > 0.01 || targetX !== 0 || targetY !== 0) {
    rafId = requestAnimationFrame(update)
  }
}

function onMouseMove(e: MouseEvent) {
  const card = cardRef.value
  if (!card) return
  const rect = card.getBoundingClientRect()
  const x = (e.clientX - rect.left) / rect.width - 0.5
  const y = (e.clientY - rect.top) / rect.height - 0.5

  targetX = -y * MAX_TILT
  targetY = x * MAX_TILT

  cancelAnimationFrame(rafId)
  rafId = requestAnimationFrame(update)
}

function onMouseLeave() {
  targetX = 0
  targetY = 0
  cancelAnimationFrame(rafId)
  rafId = requestAnimationFrame(update)
}
</script>
<style lang="scss" scoped>
.login-layout {
  background: linear-gradient(
    135deg,
    var(--login-bg-start) 0%,
    var(--login-bg-end) 100%
  );
  perspective: 1200px;
}

.login-card {
  transform-style: preserve-3d;
  transition: box-shadow 0.3s ease;
  will-change: transform;
  background: color-mix(in srgb, var(--p-surface-0) 82%, transparent);
  border: 1px solid color-mix(in srgb, var(--p-primary-color) 14%, var(--p-surface-0));
}

.left-bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background: url('/ui/login.jpg') no-repeat center center;
  background-size: cover;
  animation: ken-burns 20s ease-in-out infinite alternate;
}

.left-overlay::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    var(--login-overlay-start) 0%,
    var(--login-overlay-end) 100%
  );
}

.right-panel {
  color: var(--p-surface-900);
}

.login-title {
  display: inline-flex;
  overflow: hidden;
}

.title-letter {
  display: inline-block;
  animation: letter-enter 0.6s cubic-bezier(0.16, 1, 0.3, 1) var(--enter-delay) both;
  position: relative;

  &::after {
    content: attr(data-char);
    position: absolute;
    inset: 0;
    background: linear-gradient(
      105deg,
      transparent 30%,
      rgba(255, 255, 255, 0.6) 50%,
      transparent 70%
    );
    background-size: 300% 100%;
    background-position: 200% 0;
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
    animation: shimmer 3s ease-in-out 1.2s infinite;
  }
}

.login-subtitle {
  color: #0f172a;
  animation: text-enter 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.7s both;
}

@keyframes ken-burns {
  0% {
    transform: scale(1) translate(0, 0);
  }
  100% {
    transform: scale(1.1) translate(-2%, -1%);
  }
}

@keyframes text-enter {
  from {
    opacity: 0;
    transform: translateY(20px);
    filter: blur(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

@keyframes letter-enter {
  from {
    opacity: 0;
    transform: translateY(40px) rotateX(-60deg);
    filter: blur(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0) rotateX(0);
    filter: blur(0);
  }
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -100% 0;
  }
}

</style>
