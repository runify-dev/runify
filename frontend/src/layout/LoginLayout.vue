<template>
    <div class="login-warp flex-center">
        <div class="login-container w-full h-full">
            <el-row class="container w-full h-full">
                <el-col :xs="0" :sm="0" :md="10" :lg="10" :xl="10" class="left-container">
                    <div class="login-image" :style="{ backgroundImage: `url(/ui/login.jpg)` }"></div>
                </el-col>
                <el-col :xs="24" :sm="24" :md="14" :lg="14" :xl="14" class="right-container flex-center">
                    <slot></slot>
                </el-col>
            </el-row>
        </div>
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
.login-warp {
    height: 100vh;

    .login-image {
        background-repeat: no-repeat;
        background-position: center;
        background-size: cover;
        width: 100%;
        height: 100%;
    }

    .right-container {
        position: relative;

        .lang {
            position: absolute;
            right: 20px;
            top: 20px;
        }
    }
}
</style>
