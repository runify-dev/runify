<template>
    <el-aside class="app-aside">
        <div class="user-contaner">
            <div class="user">
                <el-avatar :size="40" :src="userPojo?.icon" />
            </div>
        </div>
        <div class="menu-contaner">
            <Menu :menu="menu" v-for="(menu, index) in menuList" :key="index">
            </Menu>
        </div>
        <div class="operate-contaner">
            <el-button text>
                <AppIcon name="Setting"></AppIcon>
            </el-button>
        </div>
    </el-aside>
</template>
<script setup lang="ts">
import AppIcon from "@/components/icons/AppIcon.vue";
import { computed, onMounted, ref } from 'vue';
import Menu from "@/layout/components/app-aside/Menu.vue"
import { getChildRouteListByPathAndName } from '@/router/index'
import useStore from '@/stores';
import type { User } from '@/api/type/user';
const { user } = useStore()
const menuList = computed(() => {
    return getChildRouteListByPathAndName('home')
})
const userPojo = ref<User>();

onMounted(() => {
    user.profile().then(user => {
        userPojo.value = user
    })
})
</script>
<style lang="scss"></style>
