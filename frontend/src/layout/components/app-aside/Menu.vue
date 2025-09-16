<template>
    <div class="card" @click="router.push({ name: menu.name })" :class="isActive ? 'card-active' : ''">
        <div class="particles">
            <span></span>
            <span></span>
            <span></span>
        </div>
        <div class="content">
            <AppIcon v-if="typeof menu.meta?.icon === 'string'" :name="menu.meta.icon" />
            <span>{{ menu.meta?.title }}</span>
        </div>
    </div>
</template>
<script setup lang="ts">
import AppIcon from "@/components/icons/AppIcon.vue";
import { useRouter, useRoute, type RouteRecordRaw } from 'vue-router'
import { computed } from 'vue'
const router = useRouter()
const route = useRoute()

const props = defineProps<{
    menu: RouteRecordRaw
}>()

const isActive = computed(() => {
    const { name, path, meta } = route
    return (name == props.menu.name && path == props.menu.path) || meta.activeMenu == props.menu.meta?.activeMenu
})
</script>
<style lang="scss" scoped>
.card {
    width: 46px;
    height: 46px;
    border-radius: 7px;
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
    cursor: pointer;
    transition: all 0.5s;

    .content {
        display: flex;
        flex-wrap: wrap;
        justify-content: center;

        :deep {
            span {
                white-space: nowrap;
                font-size: 10px;

            }
        }
    }
}

.card-active {

    transform: scale(1.05);
    box-shadow: 0 0 3px rgba(0, 255, 255, 0.6);

    &::before {
        content: '';
        position: absolute;
        width: 150%;
        height: 140%;
        background: linear-gradient(#00fffc, #ff00ff);
        animation: rotate 4s linear infinite;
    }

    &::after {
        content: '';
        position: absolute;
        inset: 3px;
        background: var(--app-background-color, #f1f3f5);
        border-radius: 5px;
    }

    .content {
        position: relative;
        z-index: 1;
        color: var(--el-menu-active-color);
        padding: 20px;
        text-align: center;

    }
}

.card:hover {
    transform: scale(1.05);
    box-shadow: 0 0 3px rgba(0, 255, 255, 0.6);

    &::before {
        content: '';
        position: absolute;
        width: 150%;
        height: 140%;
        background: linear-gradient(#00fffc, #ff00ff);
        animation: rotate 4s linear infinite;
    }

    &::after {
        content: '';
        position: absolute;
        inset: 3px;
        background: var(--app-background-color, #f1f3f5);
        border-radius: 5px;
    }

    .content {
        position: relative;
        z-index: 1;
        color: var(--el-menu-active-color);
        padding: 20px;
        text-align: center;

    }
}

@keyframes rotate {
    0% {
        transform: rotate(0deg);
    }

    100% {
        transform: rotate(360deg);
    }
}




.glowing-text {
    font-size: 2em;
    font-weight: bold;
    background: linear-gradient(45deg, #ff00ff, #00fffc, #ffeb3b);
    -webkit-background-clip: text;
    color: transparent;
    animation: gradient 3s ease infinite;
}

@keyframes gradient {
    0% {
        background-position: 0% 50%;
    }

    50% {
        background-position: 100% 50%;
    }

    100% {
        background-position: 0% 50%;
    }
}

.particles span {
    position: absolute;
    width: 4px;
    height: 4px;
    background: #fff;
    border-radius: 50%;
    animation: particle 2s linear infinite;
    opacity: 0;
}

@keyframes particle {
    0% {
        transform: translateY(0) translateX(0);
        opacity: 1;
    }

    100% {
        transform: translateY(-100px) translateX(50px);
        opacity: 0;
    }
}

/* 生成随机粒子位置 */
.particles span:nth-child(1) {
    left: 20%;
    animation-delay: 0s;
}

.particles span:nth-child(2) {
    left: 50%;
    animation-delay: 0.5s;
}

.particles span:nth-child(3) {
    left: 70%;
    animation-delay: 1s;
}
</style>
