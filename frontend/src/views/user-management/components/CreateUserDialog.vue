<template>
  <Drawer v-model:visible="visible" :header="isEdit ? '编辑用户' : '创建用户'" position="right" class="!w-[26rem]">
    <Form ref="formRef" :initial-values="initialValues" :resolver="resolver" @submit="submit">
      <FormField v-slot="$field" name="icon" class="flex flex-col gap-2 mt-4">
        <label>头像</label>
        <div class="flex items-center gap-3">
          <div
            v-if="!$field.value"
            class="w-16 h-16 rounded-xl border-2 border-dashed border-surface-300 flex items-center justify-center cursor-pointer hover:border-primary-400 hover:bg-primary-50 transition-all duration-200"
            @click="triggerFileInput"
          >
            <i class="pi pi-plus text-surface-400 text-lg" />
          </div>
          <div v-else class="relative group">
            <div class="w-16 h-16 rounded-xl overflow-hidden border border-surface-200">
              <Image :src="$field.value" alt="头像" width="64" height="64" preview />
            </div>
            <button
              class="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-red-500 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-150"
              @click="($field as any).onChange({ value: '' })"
            >
              <i class="pi pi-times text-xs" />
            </button>
          </div>
          <span class="text-xs text-surface-400">支持 JPG、PNG 格式，选填</span>
        </div>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          class="hidden"
          @change="handleFileChange"
        />
      </FormField>

      <FormField v-slot="$field" name="username" class="flex flex-col gap-1 mt-4">
        <label>用户名</label>
        <InputText class="mt-1" type="text" placeholder="请输入用户名（3-20位）" :disabled="isEdit" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
          {{ $field.error?.message }}
        </Message>
      </FormField>

      <FormField v-slot="$field" name="nickname" class="flex flex-col gap-1 mt-4">
        <label>昵称</label>
        <InputText class="mt-1" type="text" placeholder="请输入昵称（2-20位）" fluid />
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

      <FormField v-slot="$field" name="password" class="flex flex-col gap-1 mt-4" v-if="!isEdit">
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
import { ref, computed, watch } from 'vue'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import type { FormInstance } from '@primevue/forms'
import UserAPI from '@/api/user'
import fileAPI from '@/api/file'
import type { User } from '@/api/type/user'
import { resetUrl } from '@/utils/common'

const emit = defineEmits(['success'])

const visible = ref<boolean>(false)
const fileInputRef = ref<HTMLInputElement>()
const isEdit = ref<boolean>(false)
const editingUser = ref<User | null>(null)

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    const fd = new FormData()
    fd.append('file', file)
    fileAPI.uploadFile(fd).then((ok) => {
      formRef.value?.setFieldValue('icon', `./api/storage/file/${ok.data.id}`)
    })
    input.value = ''
  }
}

const initialValues = computed(() => {
  if (isEdit.value && editingUser.value) {
    return {
      icon: editingUser.value.icon ? resetUrl(editingUser.value.icon) : '',
      username: editingUser.value.username,
      nickname: editingUser.value.nickname,
      email: editingUser.value.email,
      phone: editingUser.value.phone || ''
    }
  }
  return {
    icon: '',
    username: '',
    nickname: '',
    email: '',
    phone: '',
    password: ''
  }
})

const resolver = computed(() =>
  zodResolver(
    z.object({
      icon: z.string().optional(),
      username: z
        .string()
        .min(3, { message: '用户名长度为 3 - 20 之间' })
        .max(20, { message: '用户名长度为 3 - 20 之间' }),
      nickname: z
        .string()
        .min(2, { message: '昵称长度为 2 - 20 之间' })
        .max(20, { message: '昵称长度为 2 - 20 之间' }),
      email: z
        .string()
        .min(1, { message: '邮箱为必填参数' })
        .email({ message: '请输入正确的邮箱地址' }),
      phone: z.string().optional(),
      password: isEdit.value
        ? z.string().optional()
        : z.string().min(6, { message: '密码长度为 6 - 20 之间' }).max(20, { message: '密码长度为 6 - 20 之间' })
    })
  )
)

const formRef = ref<FormInstance>()

const submit = () => {
  formRef.value?.validate().then(({ errors, values }) => {
    if (Object.keys(errors).length === 0) {
      if (isEdit.value && editingUser.value) {
        const updateData = { ...values }
        delete updateData.password
        UserAPI.updateUser(editingUser.value.id, updateData).then(() => {
          emit('success')
          close()
        })
      } else {
        UserAPI.createUser(values).then(() => {
          emit('success')
          close()
        })
      }
    }
  })
}

const open = (user?: User) => {
  if (user) {
    isEdit.value = true
    editingUser.value = user
  } else {
    isEdit.value = false
    editingUser.value = null
  }
  visible.value = true
}

const close = () => {
  formRef.value?.reset()
  visible.value = false
  isEdit.value = false
  editingUser.value = null
}

watch(isEdit, () => {
  if (formRef.value) {
    formRef.value.reset()
  }
})

defineExpose({ open, close })
</script>
