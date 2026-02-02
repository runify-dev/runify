<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="current ? '修改连接池' : '创建连接池'"
    :style="{ width: '50rem' }"
  >
    <Form ref="formRef" v-slot="$form" @submit="submit">
      <FormField v-slot="$field" name="name" :resolver="resolvers.name">
        <IftaLabel>
          <label>名称</label>
          <InputText type="text" fluid />
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
            $field.error?.message
          }}</Message>
        </IftaLabel>
      </FormField>
      <FormField v-slot="$field" name="desc" class="mt-4" :resolver="resolvers.desc">
        <IftaLabel>
          <label>描述</label>
          <InputText type="text" fluid />
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
            $field.error?.message
          }}</Message>
        </IftaLabel>
      </FormField>
      <FormField v-slot="$field: any" name="protocol" class="mt-4" :resolver="resolvers.protocol">
        <label>协议</label>
        <RadioCard
          style="--el-card-padding: 5px"
          body-class="p-0"
          v-bind:model-value="$field.value"
          @update:model-value="(v) => $field.onChange({ value: v })"
          :option-list="protocolOptions"
          value-field="value"
        >
          <template v-slot="item">
            <div class="w-full h-full flex items-center justify-center content-center">
              <component :is="item.icon" style="height: 20px; width: 20px" class="mr-4"></component>
              {{ item.label }}
            </div>
          </template>
        </RadioCard>
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
      <component
        v-if="$form.protocol?.value"
        :is="kw[$form.protocol.value]"
        ref="protocolInstanceRef"
        :setFieldValue="setFieldValue"
      ></component>
    </Form>
    <template #footer>
      <Button @click="formRef?.submit()">保存</Button>
      <Button>取消</Button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { nextTick, ref } from 'vue'
import PostgreSql from './postgresql/index.vue'
import RadioCard from '@/components/radio-card/index.vue'
import PostgreSQLIcon from '@/views/project/database-collection-pool/icons/postgres-icon.vue'
import type { CreateDatabaseCollectionPoolVO } from '@/api/type/database-connection-pool'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import dtabaseCollectionPoolAPI from '@/api/database-connection-pool'
import type { FormInstance } from '@primevue/forms'
import bus from '@/bus'
const emit = defineEmits(['refresh'])
const protocolInstanceRef = ref<any>()
const props = defineProps<{
  projectId: string
}>()
const formRef = ref<FormInstance>()
const protocolOptions = [
  {
    icon: PostgreSQLIcon,
    label: 'PostgreSQL',
    value: 'POSTGRESQL'
  }
]

const resolvers = {
  name: zodResolver(z.string().min(1, { error: '请输入名称' })),
  desc: zodResolver(z.string().min(1, { error: '请输入描述' })),
  protocol: zodResolver(z.string().min(1, { error: '请选择连接池协议' }))
}
const kw: any = {
  POSTGRESQL: PostgreSql
}
const visible = ref<boolean>(false)
const close = () => {
  visible.value = false
  current.value = undefined
}
const current = ref<any>()
const open = (databaseCollectionPool?: any) => {
  visible.value = true
  if (databaseCollectionPool) {
    nextTick(() => {
      formRef.value?.setValues({
        name: databaseCollectionPool.name,
        desc: databaseCollectionPool.desc,
        protocol: databaseCollectionPool.protocol
      })

      nextTick(() => {
        protocolInstanceRef.value.setValues(databaseCollectionPool.meta)
      })
    })

    current.value = databaseCollectionPool
  }
}
const setFieldValue = (field: string, value: any) => {
  formRef.value?.setFieldValue(field, value)
}

const submit = ({ valid, values }: any) => {
  if (valid) {
    if (current.value) {
      dtabaseCollectionPoolAPI
        .edit(props.projectId, current.value.id, {
          name: values.name,
          desc: values.desc,
          protocol: values.protocol,
          meta: values.meta
        })
        .then((ok) => {
          bus.emit('message:success', ['修改连接池', '成功'])
          emit('refresh')
          close()
        })
    } else {
      return dtabaseCollectionPoolAPI
        .create(props.projectId, {
          name: values.name,
          desc: values.desc,
          protocol: values.protocol,
          meta: values.meta
        })
        .then((ok) => {
          emit('refresh')
          bus.emit('message:success', ['创建连接池', '成功'])
          close()
        })
    }
  }
}

defineExpose({ open, close })
</script>
<style lang="scss"></style>
