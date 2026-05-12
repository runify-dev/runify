<template>
  <canvas ref="canvasRef" class="absolute inset-0 z-0"></canvas>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

defineOptions({ name: 'ParticleNetwork' })

const canvasRef = ref<HTMLCanvasElement>()

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  radius: number
  opacity: number
}

let animationId = 0
let particles: Particle[] = []
let mouseX = -1000
let mouseY = -1000
let canvasW = 0
let canvasH = 0

const PARTICLE_COUNT = 60
const CONNECT_DIST = 120
const MOUSE_RADIUS = 150
const MOUSE_FORCE = 0.03

let cachedColor: [number, number, number] | null = null

function getPrimaryColor(): [number, number, number] {
  if (cachedColor) return cachedColor
  const style = getComputedStyle(document.documentElement)
  const color = style.getPropertyValue('--p-primary-color').trim()
  const temp = document.createElement('div')
  temp.style.color = color
  document.body.appendChild(temp)
  const computed = getComputedStyle(temp).color
  document.body.removeChild(temp)
  const match = computed.match(/(\d+)/g)
  cachedColor = match ? [+match[0], +match[1], +match[2]] : [16, 185, 129]
  return cachedColor
}

function initParticles(w: number, h: number) {
  particles = Array.from({ length: PARTICLE_COUNT }, () => ({
    x: Math.random() * w,
    y: Math.random() * h,
    vx: (Math.random() - 0.5) * 0.4,
    vy: (Math.random() - 0.5) * 0.4,
    radius: Math.random() * 1.5 + 0.5,
    opacity: Math.random() * 0.5 + 0.2
  }))
}

function draw(ctx: CanvasRenderingContext2D, w: number, h: number) {
  const [r, g, b] = getPrimaryColor()
  ctx.clearRect(0, 0, w, h)

  for (const p of particles) {
    const dx = mouseX - p.x
    const dy = mouseY - p.y
    const dist = Math.sqrt(dx * dx + dy * dy)

    if (dist < MOUSE_RADIUS && dist > 0) {
      const force = (1 - dist / MOUSE_RADIUS) * MOUSE_FORCE
      p.vx -= (dx / dist) * force
      p.vy -= (dy / dist) * force
    }

    p.vx *= 0.98
    p.vy *= 0.98
    p.x += p.vx
    p.y += p.vy

    if (p.x < 0) p.x = w
    if (p.x > w) p.x = 0
    if (p.y < 0) p.y = h
    if (p.y > h) p.y = 0
  }

  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const dist = Math.sqrt(dx * dx + dy * dy)

      if (dist < CONNECT_DIST) {
        const alpha = (1 - dist / CONNECT_DIST) * 0.2
        ctx.strokeStyle = `rgba(${r}, ${g}, ${b}, ${alpha})`
        ctx.lineWidth = 0.5
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.stroke()
      }
    }

    const mdx = mouseX - particles[i].x
    const mdy = mouseY - particles[i].y
    const mDist = Math.sqrt(mdx * mdx + mdy * mdy)
    if (mDist < MOUSE_RADIUS) {
      const alpha = (1 - mDist / MOUSE_RADIUS) * 0.3
      ctx.strokeStyle = `rgba(${r}, ${g}, ${b}, ${alpha})`
      ctx.lineWidth = 0.5
      ctx.beginPath()
      ctx.moveTo(particles[i].x, particles[i].y)
      ctx.lineTo(mouseX, mouseY)
      ctx.stroke()
    }

    const pAlpha = particles[i].opacity
    ctx.fillStyle = `rgba(${r}, ${g}, ${b}, ${pAlpha})`
    ctx.beginPath()
    ctx.arc(particles[i].x, particles[i].y, particles[i].radius, 0, Math.PI * 2)
    ctx.fill()
  }
}

function animate(ctx: CanvasRenderingContext2D, w: number, h: number) {
  draw(ctx, w, h)
  animationId = requestAnimationFrame(() => animate(ctx, w, h))
}

function handleResize(canvas: HTMLCanvasElement, ctx: CanvasRenderingContext2D) {
  const rect = canvas.parentElement!.getBoundingClientRect()
  canvasW = rect.width
  canvasH = rect.height
  canvas.width = canvasW
  canvas.height = canvasH
  initParticles(canvasW, canvasH)
}

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')!

  handleResize(canvas, ctx)

  const resizeObserver = new ResizeObserver(() => handleResize(canvas, ctx))
  resizeObserver.observe(canvas.parentElement!)

  const parent = canvas.parentElement!
  parent.addEventListener('mousemove', (e: Event) => {
    const me = e as MouseEvent
    const rect = parent.getBoundingClientRect()
    mouseX = me.clientX - rect.left
    mouseY = me.clientY - rect.top
  })
  parent.addEventListener('mouseleave', () => {
    mouseX = -1000
    mouseY = -1000
  })

  animate(ctx, canvasW, canvasH)

  onUnmounted(() => {
    cancelAnimationFrame(animationId)
    resizeObserver.disconnect()
  })
})
</script>
