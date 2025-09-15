<template>
  <el-drawer v-model="dialogVisible" title="创建用户" direction="rtl" :before-close="close">
    <el-form
      ref="createUserFormRef"
      :rules="rules"
      label-position="top"
      :model="userFrom"
      label-width="auto"
      style="max-width: 600px"
    >
      <el-form-item label="用户名" prop="username">
        <el-input v-model="userFrom.username" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="userFrom.nickname" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="userFrom.email" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="userFrom.phone" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="userFrom.password" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="createUser"> 确定 </el-button>
      </div>
    </template>
  </el-drawer>
</template>
<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ref, reactive } from 'vue'
import { t } from '@/locales'
import UserAPI from '@/api/user'
const dialogVisible = ref<boolean>()
const userFrom = ref<any>({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  password: '',
  rePassword: ''
})
const rules = reactive<FormRules<any>>({
  username: [
    {
      required: true,
      message: t('user.form.username.requiredMessage', '用户名为必填参数'),
      trigger: 'blur'
    },
    {
      min: 3,
      max: 20,
      message: t('user.form.username.ruleMessage', '用户名长度为 3 - 20 之间'),
      trigger: 'blur'
    }
  ],
  nickname: [
    {
      required: true,
      message: t('user.form.nickname.requiredMessage', '昵称为必填参数'),
      trigger: 'blur'
    },
    {
      min: 3,
      max: 20,
      message: t('user.form.nickname.ruleMessage', '用户名长度为 3 - 20 之间'),
      trigger: 'blur'
    }
  ],
  email: [
    {
      required: true,
      message: t('user.form.email.requiredMessage', '邮箱为必填参数'),
      trigger: 'blur'
    },
    {
      type: 'email',
      message: t('user.form.email.ruleMessage', '请输入正确的邮箱地址'),
      trigger: ['blur', 'change']
    }
  ],
  password: [
    {
      required: true,
      message: t('user.form.password.requiredMessage', '密码为必填参数'),
      trigger: 'blur'
    },
    {
      min: 3,
      max: 20,
      message: t('user.form.password.ruleMessage', '密码长度为 6 - 20 之间'),
      trigger: 'blur'
    }
  ]
})
const createUserFormRef = ref<FormInstance>()
const createUser = () => {
  createUserFormRef.value?.validate().then(() => {
    UserAPI.createUser(userFrom.value).then(() => {
      close()
    })
  })
}
const open = () => {
  dialogVisible.value = true
}
const close = () => {
  dialogVisible.value = false
}
defineExpose({ open, close })
</script>
<style lang="scss"></style>
