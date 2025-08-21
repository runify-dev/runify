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
        <div class="operate-container">
          <el-popover trigger="click" popper-class="system-person" placement="right">
            <template #reference>
              <el-button text>
                <AppIcon name="Setting"></AppIcon>
              </el-button>
            </template>
            <div class="w-[200px] shadow-md border border-[#dee0e3] bg-white rounded-md">
              <!-- 上部分用户信息 -->
              <div class="flex items-center h-[62px] px-2 mb-1 border-b border-[#dee0e3]">
                <el-avatar src="/ui/user.jpeg" class="mr-4" />
                <div class="ml-2 font-medium text-[16px] leading-6 truncate">Admin</div>
              </div>

              <!-- 菜单列表 -->
              <div class="flex flex-col">
                <div class="flex items-center h-8 px-2 cursor-pointer rounded hover:bg-gray-100 active:bg-gray-200" @click="toSystem">
                  <AppIcon name="app-profile" style="font-size: 16px"></AppIcon>
                  <div class="ml-2">个人信息</div>
                </div>

                <div class="flex items-center h-8 px-2 cursor-pointer rounded hover:bg-gray-100 active:bg-gray-200" @click="toSystem">
                  <AppIcon name="app-setting" style="font-size: 16px"></AppIcon>
                  <div class="ml-2">系统设置</div>
                </div>

                <div class="flex items-center h-8 px-2 cursor-pointer rounded hover:bg-gray-100 active:bg-gray-200" @click="toAbout">
                  <AppIcon name="app-about" style="font-size: 16px"></AppIcon>
                  <div class="ml-2">关于</div>
                </div>

                <div class="flex items-center h-8 px-2 cursor-pointer rounded hover:bg-gray-100 active:bg-gray-200" @click="openHelp">
                 <AppIcon name="app-help" style="font-size: 16px"></AppIcon>
                  <div class="ml-2">帮助</div>
                </div>

                <div class="flex items-center h-8 px-2 my-1 cursor-pointer rounded hover:bg-gray-100 active:bg-gray-200 border-t border-[#dee0e3]" @click="logout">
                  <AppIcon name="app-logout"></AppIcon>
                  <div class="ml-2">退出登录</div>
                </div>
              </div>
            </div>
          </el-popover>
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
import router from '@/router'
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
const logout = () => {
    user.logout();
    router.push('/')
};

</script>
<style lang="scss">
.el-popover.system-person {
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
}
</style>
