<template>
  <LoginLayout v-loading="loading">
    <LoginContainer>
      <Form ref="loginFormRef" :resolver @submit="onFormSubmit" class="flex flex-col gap-8 w-full">
        <FormField v-slot="$field" name="username">
          <FloatLabel>
            <label>用户名</label>
            <InputText type="text" fluid />
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
              $field.error?.message
            }}</Message>
          </FloatLabel>
        </FormField>
        <FormField v-slot="$field" asChild name="password" initialValue="">
          <FloatLabel>
            <label>密码</label>
            <Password type="text" :feedback="false" toggleMask fluid />
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
              $field.error?.message
            }}</Message>
          </FloatLabel>
        </FormField>
        <Button
          class="w-full py-4 bg-gradient-to-r from-cyan-400 to-cyan-500 text-black font-semibold rounded-xl cursor-pointer transition-all duration-300 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-cyan-400/40 active:translate-y-0"
          type="submit"
          severity="secondary"
          label="登录"
        />
      </Form>
    </LoginContainer>
  </LoginLayout>
</template>
<script setup lang="ts">
import LoginContainer from '@/views/login/LoginContainer.vue'
import LoginLayout from '@/layout/LoginLayout.vue'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import FloatLabel from 'primevue/floatlabel'
import { z } from 'zod'
import useStore from '@/stores'
import { useRouter } from 'vue-router'
import { ref } from 'vue'
const router = useRouter()
const { user } = useStore()
const resolver = zodResolver(
  z.object({
    username: z.string().min(1, { message: 'Username is required.' }),
    password: z.string().min(1, { message: 'Password is required.' })
  })
)

const onFormSubmit = ({ valid, values }: any) => {
  if (valid) {
    console.log(values)
    user.login(values.username, values.password).then(() => {
      router.push('/application')
    })
  }
}
const loading = ref<boolean>(false)
</script>
<style lang="scss" scope></style>
