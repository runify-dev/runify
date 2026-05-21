<template>
  <LoginLayout v-loading="loading">
    <LoginContainer>
      <Form ref="loginFormRef" :resolver @submit="onFormSubmit" class="flex flex-col gap-8 w-full">
        <div class="form-field-enter" style="animation-delay: 0.9s">
          <FormField v-slot="$field" name="username">
            <FloatLabel>
              <InputText type="text" fluid />
              <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
                $field.error?.message
              }}</Message>
              <label>{{ t('login.username') }}</label>
            </FloatLabel>
          </FormField>
        </div>
        <div class="form-field-enter" style="animation-delay: 1.1s">
          <FormField v-slot="$field" asChild name="password" initialValue="">
            <FloatLabel>
              <Password type="text" :feedback="false" toggleMask fluid />
              <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
                $field.error?.message
              }}</Message>
              <label>{{ t('login.password') }}</label>
            </FloatLabel>
          </FormField>
        </div>
        <div class="form-field-enter" style="animation-delay: 1.3s">
          <Button
            class="login-btn w-full py-4 font-semibold rounded-xl cursor-pointer transition-all duration-300 hover:-translate-y-0.5 hover:shadow-lg active:translate-y-0"
            type="submit"
            severity="secondary"
            :label="t('login.login')"
          />
        </div>
      </Form>
    </LoginContainer>
  </LoginLayout>
</template>
<script setup lang="ts">
import LoginContainer from '@/views/login/LoginContainer.vue'
import LoginLayout from '@/layout-plus/login-layout/index.vue'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import FloatLabel from 'primevue/floatlabel'
import useStore from '@/stores'
import { useRouter } from 'vue-router'
import { ref } from 'vue'
import { t } from '@/locales'

const router = useRouter()
const { user } = useStore()
const resolver = zodResolver(
  z.object({
    username: z.string().min(1, { message: t('login.usernameRequired') }),
    password: z.string().min(1, { message: t('login.passwordRequired') })
  })
)

const onFormSubmit = ({ valid, values }: any) => {
  if (valid) {
    user.login(values.username, values.password).then(() => {
      router.push('/application')
    })
  }
}
const loading = ref<boolean>(false)
</script>
<style lang="scss" scoped>
.form-field-enter {
  animation: text-enter 0.8s cubic-bezier(0.16, 1, 0.3, 1) both;
}

:deep(.p-inputtext),
:deep(.p-password-input) {
  transition: box-shadow 0.3s ease, border-color 0.3s ease;
  background: color-mix(in srgb, var(--p-surface-0) 94%, var(--p-primary-50));
  border-color: color-mix(in srgb, var(--p-primary-color) 20%, var(--p-surface-300));
  color: var(--p-surface-900);

  &:focus {
    border-color: var(--p-primary-color);
    box-shadow:
      0 0 0 1px var(--p-primary-color),
      0 0 20px color-mix(in srgb, var(--p-primary-color) 20%, transparent);
  }
}

:deep(.p-floatlabel label) {
  color: var(--p-surface-600);
}

.login-btn {
  background: linear-gradient(135deg, var(--p-primary-500), var(--p-primary-400));
  color: var(--p-primary-contrast-color);
  border: none;
  box-shadow: 0 6px 18px color-mix(in srgb, var(--p-primary-color) 22%, transparent);

  &:hover {
    box-shadow: 0 8px 24px color-mix(in srgb, var(--p-primary-color) 40%, transparent);
    filter: brightness(1.05);
  }

  &:active {
    filter: brightness(0.95);
  }
}

@keyframes text-enter {
  from {
    opacity: 0;
    transform: translateY(20px);
    filter: blur(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}
</style>
