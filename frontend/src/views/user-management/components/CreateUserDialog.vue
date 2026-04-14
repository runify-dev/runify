<template>
  <Drawer v-model:visible="visible" header="创建用户" position="right" :style="{ width: '26rem' }">
    <Form ref="formRef" :initial-values="initialValues" :resolver="resolver" @submit="submit">
      <FormField v-slot="$field" name="username" class="flex flex-col gap-1 mt-4">
        <label>用户名</label>
        <InputText class="mt-1" type="text" placeholder="请输入用户名（3-20位）" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
          {{ $field.error?.message }}
        </Message>
      </FormField>

      <FormField v-slot="$field" name="nickname" class="flex flex-col gap-1 mt-4">
        <label>昵称</label>
        <InputText class="mt-1" type="text" placeholder="请输入昵称（3-20位）" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
          {{ $field.error?.message }}
        </Message>
      </FormField>

      <FormField v-slot="$field" name="email" class="flex flex-col gap-1 mt-4">
        <label>邮箱</label>
        <InputText class="mt-1" type="email" placeholder="请输入邮箱地址" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
          {{ $field.error?.message }}
        </Message>
      </FormField>

      <FormField v-slot="$field" name="phone" class="flex flex-col gap-1 mt-4">
        <label>手机号</label>
        <InputText class="mt-1" type="text" placeholder="请输入手机号" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
          {{ $field.error?.message }}
        </Message>
      </FormField>

      <FormField v-slot="$field" name="password" class="flex flex-col gap-1 mt-4">
        <label>密码</label>
        <Password
          class="mt-1"
          placeholder="请输入密码（6-20位）"
          :feedback="false"
          toggleMask
          fluid
        />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
          {{ $field.error?.message }}
        </Message>
      </FormField>
    </Form>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button label="取消" severity="secondary" @click="close" />
        <Button label="创建" @click="submit" />
      </div>
    </template>
  </Drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import type { FormInstance } from '@primevue/forms'
import UserAPI from '@/api/user'

const emit = defineEmits(['success'])

const visible = ref<boolean>(false)

const initialValues = {
  username: '',
  nickname: '',
  email: '',
  phone: '',
  password: ''
}

const resolver = ref(
  zodResolver(
    z.object({
      username: z
        .string()
        .min(3, { message: '用户名长度为 3 - 20 之间' })
        .max(20, { message: '用户名长度为 3 - 20 之间' }),
      nickname: z
        .string()
        .min(3, { message: '昵称长度为 3 - 20 之间' })
        .max(20, { message: '昵称长度为 3 - 20 之间' }),
      email: z
        .string()
        .min(1, { message: '邮箱为必填参数' })
        .email({ message: '请输入正确的邮箱地址' }),
      phone: z.string().optional(),
      password: z
        .string()
        .min(6, { message: '密码长度为 6 - 20 之间' })
        .max(20, { message: '密码长度为 6 - 20 之间' })
    })
  )
)

const formRef = ref<FormInstance>()

const submit = () => {
  formRef.value?.validate().then(({ errors, values }) => {
    if (Object.keys(errors).length === 0) {
      UserAPI.createUser(values).then(() => {
        emit('success')
        close()
      })
    }
  })
}

const open = () => {
  visible.value = true
}

const close = () => {
  formRef.value?.reset()
  visible.value = false
}

defineExpose({ open, close })
</script>
