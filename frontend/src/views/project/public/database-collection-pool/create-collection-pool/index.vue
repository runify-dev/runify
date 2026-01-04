<template>
  <el-dialog v-model="visible" title="Tips" width="60%" :before-close="close">
    <el-form :model="instance" ref="formRef" :rules="rules" label-width="auto" label-position="top">
      <el-row :gutter="10">
        <el-col :span="12">
          <el-form-item label="名称"> <el-input v-model="instance.name" /> </el-form-item
        ></el-col>
        <el-col :span="12">
          <el-form-item label="描述">
            <el-input v-model="instance.desc" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="协议">
        <RadioCard
          style="--el-card-padding: 5px"
          body-class="p-0"
          v-model="instance.protocol"
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
      </el-form-item>
    </el-form>
    <component :is="kw[instance.protocol]" ref="protocolInstanceRef"></component>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">Cancel</el-button>
        <el-button type="primary" @click="submit"> Confirm </el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import PostgreSql from './postgresql/index.vue'
import RadioCard from '@/components/radio-card/index.vue'
import PostgreSQLIcon from '@/views/project/public/database-collection-pool/icons/postgres-icon.vue'
import type { CreateDatabaseCollectionPoolVO } from '@/api/type/database-connection-pool'
import dtabaseCollectionPoolAPI from '@/api/database-connection-pool'
const props = defineProps<{
  projectId: string
}>()
const formRef = ref<FormInstance>()
const instance = ref<CreateDatabaseCollectionPoolVO>({
  name: '',
  desc: '',
  protocol: 'POSTGRESQL',
  meta: {}
})
const protocolOptions = [
  {
    icon: PostgreSQLIcon,
    label: 'PostgreSQL',
    value: 'POSTGRESQL'
  }
]
const rules = ref<FormRules<any>>({
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  desc: [{ required: true, message: '请输入描述', trigger: 'blur' }],
  protocol: [{ required: true, message: '请选择连接池协议', trigger: 'blur' }]
})
const kw: any = {
  POSTGRESQL: PostgreSql
}
const visible = ref<boolean>(false)
const close = () => {
  visible.value = false
}
const open = () => {
  visible.value = true
}
const protocolInstanceRef = ref()
const submit = () => {
  Promise.all([formRef.value?.validate(), protocolInstanceRef.value.validate()])
    .then(() => {
      instance.value.meta = protocolInstanceRef.value.getData()
      return dtabaseCollectionPoolAPI.create(props.projectId, instance.value)
    })
    .then((ok) => {
      close()
    })
}

defineExpose({ open, close })
</script>
<style lang="scss"></style>
