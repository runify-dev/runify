<template>
    <LoginLayout v-loading="loading">
        <LoginContainer>
            <el-form class="login-form" :rules="rules" :model="loginForm" ref="loginFormRef" @keyup.enter="login">
                <div class="mb-12">
                    <el-form-item prop="username" label-position="top">
                      <label class="block text-sm mb-2 text-white font-medium">用户名</label>
                      <el-input  size="large" class="input-item" v-model="loginForm.username" placeholder="请输入用户名">
                      </el-input>
                    </el-form-item>
                </div>
                <div class="mb-12">
                    <el-form-item prop="password">
                        <label class="block text-sm mb-2 text-white font-medium">密码</label>
                        <el-input type="password" size="large" class="input-item" v-model="loginForm.password"
                            placeholder="请输入密码" show-password>
                        </el-input>
                    </el-form-item>
                </div>
            </el-form>
            <el-button size="large" type="primary" @click="login"
                       class="w-full py-4 bg-gradient-to-r from-cyan-400 to-cyan-500 text-black font-semibold rounded-xl cursor-pointer transition-all duration-300 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-cyan-400/40 active:translate-y-0"
            >登录</el-button>
        </LoginContainer>
    </LoginLayout>
</template>
<script setup lang="ts">
import LoginContainer from "@/views/login/LoginContainer.vue"
import LoginLayout from '@/layout/LoginLayout.vue';
import type { FormInstance, FormRules } from 'element-plus'
import useStore from '@/stores';
import { useRouter } from 'vue-router';
import { ref } from "vue"
const router = useRouter()
const { user } = useStore();
const loginFormRef = ref<FormInstance>()
const loginForm = ref<
    { username: string, password: string }>(
        {
            username: "",
            password: ""
        }
    );
const rules = ref<FormRules<any>>({
    username: [
        {
            required: true,
            message: "用户名必填",
            trigger: 'blur'
        }
    ],
    password: [
        {
            required: true,
            message: "密码必填",
            trigger: 'blur'
        }
    ]
})
const loading = ref<boolean>(false);
const login = () => {
    loginFormRef.value?.validate().then(() => {
        user.login(loginForm.value.username, loginForm.value.password).then(() => {
            router.push('/knowledge')
        })
    })

}
</script>
<style lang="scss" scope>

</style>
