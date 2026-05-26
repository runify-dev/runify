<template>
  <Dialog
    v-model:visible="visible"
    modal
    header="新建数据源"
    :style="{ width: '36rem' }"
  >
    <div class="flex flex-col gap-4">
      <!-- 名称 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">名称</label>
        <InputText
          v-model="formData.name"
          type="text"
          placeholder="请输入数据源名称"
          fluid
          class="!text-sm"
        />
      </div>

      <!-- 描述 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">描述</label>
        <Textarea
          v-model="formData.desc"
          placeholder="请输入描述（选填）"
          rows="3"
          fluid
          class="!text-sm !resize-none"
        />
      </div>

      <!-- 数据源类型 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">数据源类型</label>
        <RadioCard
          grid-class="grid-cols-2 sm:grid-cols-3 md:grid-cols-4"
          :model-value="formData.dataSourceType"
          @update:model-value="setDataSourceType"
          :option-list="dataSourceTypeList"
          value-field="code"
        >
          <template v-slot="item">
            <div class="flex items-center gap-2">
              <div :innerHTML="item.icon" class="h-5 w-5 shrink-0 [&_svg]:w-full [&_svg]:h-full" />
              <span class="text-sm">{{ item.message }}</span>
            </div>
          </template>
        </RadioCard>
      </div>

      <!-- 供应商 -->
      <div v-if="formData.dataSourceType" class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">供应商</label>
        <RadioCard
          grid-class="grid-cols-2 sm:grid-cols-3 md:grid-cols-4"
          :model-value="formData.provider"
          @update:model-value="setField('provider', $event)"
          :option-list="providerList"
          value-field="provider"
        >
          <template v-slot="item">
            <div class="flex items-center gap-2">
              <div :innerHTML="item.icon" class="h-5 w-5 shrink-0 [&_svg]:w-full [&_svg]:h-full" />
              <span class="text-sm">{{ item.name }}</span>
            </div>
          </template>
        </RadioCard>
      </div>

      <!-- 动态表单 -->
      <DynamicsForm
        v-if="formData.provider"
        ref="dynamicsFormRef"
        :modelValue="metaData"
        @update:modelValue="metaData = $event"
      />
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button label="取消" severity="secondary" variant="outlined" @click="close" />
        <Button label="创建" @click="submit" />
      </div>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import DynamicsForm from '@/components/dynamics-form-plus/index.vue'
import RadioCard from '@/components/radio-card/index.vue'
import databaseConnectionPoolAPI from '@/api/database-connection-pool'
import { TreeCommonAPI } from '@/api/tree'
import type { TreeNode } from 'primevue/treenode'

const props = defineProps<{ api: TreeCommonAPI }>()
const emit = defineEmits(['create:success'])

const dynamicsFormRef = ref<InstanceType<typeof DynamicsForm>>()
const visible = ref(false)
const current = ref<TreeNode>()

const dataSourceTypeList = ref<Array<any>>([])
const providerList = ref<Array<any>>([])
const metaData = ref<Record<string, any>>({})
const formData = ref<Record<string, any>>({
  name: '',
  desc: '',
  dataSourceType: '',
  provider: ''
})

const setField = (field: string, value: any) => {
  formData.value = { ...formData.value, [field]: value }
}

const setDataSourceType = (value: string) => {
  formData.value = { ...formData.value, dataSourceType: value, provider: '' }
  providerList.value = []
  loadProviders(value)
}

const loadProviders = (type: string) => {
  databaseConnectionPoolAPI.getProviders(type).then((ok) => {
    providerList.value = ok.data
  })
}

watch(
  () => formData.value.provider,
  (provider) => {
    if (provider) {
      databaseConnectionPoolAPI.getProviderForm(provider).then((ok) => {
        if (ok.data) {
          dynamicsFormRef.value?.render(ok.data, {})
        }
      })
    }
  }
)

const resetForm = () => {
  formData.value = { name: '', desc: '', dataSourceType: '', provider: '' }
  providerList.value = []
  metaData.value = {}
}

const submit = () => {
  if (!formData.value.name.trim()) return

  const { values, errors } = dynamicsFormRef.value?.validate() || { values: {}, errors: {} }
  if (Object.keys(errors).length > 0) return

  const payload = {
    name: formData.value.name,
    desc: formData.value.desc,
    dataSourceType: formData.value.dataSourceType,
    provider: formData.value.provider,
    meta: values || {}
  }

  props.api
    .createResource(current.value ? current.value.key : 'root', payload)
    .then((ok) => {
      emit('create:success', current.value ? current.value.key : undefined, ok.data)
      close()
    })
}

const open = (node?: TreeNode) => {
  current.value = node
  resetForm()
  visible.value = true
  databaseConnectionPoolAPI.getDataSourceTypes().then((ok) => {
    dataSourceTypeList.value = ok.data
  })
}

const close = () => {
  current.value = undefined
  visible.value = false
}

defineExpose({ open, close })
</script>
