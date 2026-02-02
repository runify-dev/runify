<template>
  <div>
    <FormField v-slot="$field" name="meta.collection.host" class="mt-4" :resolver="resolvers.host">
      <IftaLabel>
        <label>主机</label>
        <InputText type="text" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </IftaLabel>
    </FormField>
    <FormField v-slot="$field" name="meta.collection.port" class="mt-4" :resolver="resolvers.port">
      <IftaLabel>
        <label>端口</label>
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
        <label>数据库</label>
        <InputText type="text" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </IftaLabel>
    </FormField>
    <FormField v-slot="$field" name="meta.collection.user" class="mt-4" :resolver="resolvers.user">
      <IftaLabel>
        <label>用户名</label>
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
        <label>密码</label>
      </IftaLabel>
    </FormField>
    <FormField v-slot="$field" name="meta.pool.maxSize" class="mt-4" :resolver="resolvers.maxSize">
      <IftaLabel>
        <InputNumber inputId="withoutgrouping" :useGrouping="false" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
        <label>最大连接数</label>
      </IftaLabel>
    </FormField>
  </div>
</template>
<script setup lang="ts">
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'

const resolvers = {
  host: zodResolver(z.string().min(1, { error: '请输入Host' })),
  database: zodResolver(z.string().min(1, { error: '请输入数据库' })),
  user: zodResolver(z.string().min(1, { error: '请输入用户' })),
  password: zodResolver(z.string().min(1, { error: '请输入密码' })),
  port: zodResolver(
    z
      .int()
      .min(1, { error: '端口必须在1-65536之间' })
      .max(65536, { error: '端口必须在1-65536之间' })
  ),
  maxSize: zodResolver(z.int().min(1, { error: '请输入最大连接数量' }))
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
