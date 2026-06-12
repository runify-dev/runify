<template>
  <div>
    <FormField v-slot="$field" name="meta.collection.host" class="mt-4" :resolver="resolvers.host">
      <IftaLabel>
        <label>{{ t('database.host') }}</label>
        <InputText type="text" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </IftaLabel>
    </FormField>
    <FormField v-slot="$field" name="meta.collection.port" class="mt-4" :resolver="resolvers.port">
      <IftaLabel>
        <label>{{ t('database.port') }}</label>
        <InputNumber type="text" :useGrouping="false" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </IftaLabel>
    </FormField>
    <FormField
      v-slot="$field"
      name="meta.collection.database"
      class="mt-4"
      :resolver="resolvers.database"
    >
      <IftaLabel>
        <label>{{ t('database.database') }}</label>
        <InputText type="text" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </IftaLabel>
    </FormField>
    <FormField v-slot="$field" name="meta.collection.user" class="mt-4" :resolver="resolvers.user">
      <IftaLabel>
        <label>{{ t('database.username') }}</label>
        <InputText type="text" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </IftaLabel>
    </FormField>
    <FormField
      v-slot="$field"
      name="meta.collection.password"
      class="mt-4"
      :resolver="resolvers.password"
    >
      <IftaLabel>
        <Password type="text" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
        <label>{{ t('database.password') }}</label>
      </IftaLabel>
    </FormField>
    <FormField v-slot="$field" name="meta.pool.maxSize" class="mt-4" :resolver="resolvers.maxSize">
      <IftaLabel>
        <InputNumber inputId="withoutgrouping" :useGrouping="false" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
        <label>{{ t('database.maxConnections') }}</label>
      </IftaLabel>
    </FormField>
  </div>
</template>
<script setup lang="ts">
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import { t } from '@/locales'

const resolvers = {
  host: zodResolver(z.string().min(1, { error: t('database.validation.hostRequired') })),
  database: zodResolver(z.string().min(1, { error: t('database.validation.databaseRequired') })),
  user: zodResolver(z.string().min(1, { error: t('database.validation.userRequired') })),
  password: zodResolver(z.string().min(1, { error: t('database.validation.passwordRequired') })),
  port: zodResolver(
    z
      .int()
      .min(1, { error: t('database.validation.portRange') })
      .max(65536, { error: t('database.validation.portRange') })
  ),
  maxSize: zodResolver(z.int().min(1, { error: t('database.validation.maxConnectionsRequired') }))
}
const props = defineProps<{
  setFieldValue: (field: string, value: any) => void
}>()

const setValues = (values: any) => {
  props.setFieldValue('meta.collection.host', values.collection.host)
  props.setFieldValue('meta.collection.database', values.collection.database)
  props.setFieldValue('meta.collection.user', values.collection.user)
  props.setFieldValue('meta.collection.password', values.collection.password)
  props.setFieldValue('meta.collection.port', values.collection.port)
  props.setFieldValue('meta.pool.maxSize', values.pool.maxSize)
}
defineExpose({ setValues })
</script>
<style lang="scss" scoped></style>
