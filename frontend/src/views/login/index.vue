<template>
    <LoginLayout v-loading="loading">
        <LoginContainer>
            <h2 class="mb-24">普通登录</h2>
            <el-form class="login-form" :rules="rules" :model="loginForm" ref="loginFormRef" @keyup.enter="login">
                <div class="mb-24">
                    <el-form-item prop="username">
                        <el-input size="large" class="input-item" v-model="loginForm.username" placeholder="请输入用户名">
                        </el-input>
                    </el-form-item>
                </div>
                <div class="mb-24">
                    <el-form-item prop="password">
                        <el-input type="password" size="large" class="input-item" v-model="loginForm.password"
                            placeholder="请输入密码" show-password>
                        </el-input>
                    </el-form-item>
                </div>
            </el-form>

            <el-button size="large" type="primary" class="w-full" @click="login">登陆
            </el-button>
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
.login-gradient-divider {
    position: relative;
    text-align: center;
    color: var(--el-color-info);

    ::before {
        content: '';
        width: 25%;
        height: 1px;
        background: linear-gradient(90deg, rgba(222, 224, 227, 0) 0%, #dee0e3 100%);
        position: absolute;
        left: 16px;
        top: 50%;
    }

    ::after {
        content: '';
        width: 25%;
        height: 1px;
        background: linear-gradient(90deg, #dee0e3 0%, rgba(222, 224, 227, 0) 100%);
        position: absolute;
        right: 16px;
        top: 50%;
    }
}

.login-button-circle {
    padding: 20px !important;
    margin: 0 4px;
    width: 32px;
    height: 32px;
    text-align: center;
}
</style>
