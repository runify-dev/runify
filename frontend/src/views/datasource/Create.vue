<template>
  <div class="p-4">
    <h2 class="text-lg font-bold mb-4">新建数据源</h2>
    <div class="flex flex-col gap-4 max-w-2xl">
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">名称</label>
        <InputText v-model="formData.name" type="text" fluid />
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">描述</label>
        <InputText v-model="formData.desc" type="text" fluid />
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">数据源类型</label>
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
      <div v-if="formData.dataSourceType" class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">供应商</label>
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
      <DynamicsForm
        v-if="formData.provider"
        ref="dynamicsFormRef"
        :modelValue="metaData"
        @update:modelValue="metaData = $event"
      />
      <div class="flex gap-2">
        <Button @click="submit">保存</Button>
        <Button @click="cancel" severity="secondary">取消</Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DynamicsForm from '@/components/dynamics-form-plus/index.vue'
import RadioCard from '@/components/radio-card/index.vue'
import databaseConnectionPoolAPI from '@/api/database-connection-pool'
import { TreeCommonAPI } from '@/api/tree'
import bus from '@/bus/index'

const route = useRoute()
const router = useRouter()
const treeCommonAPI = new TreeCommonAPI('datasource')

const dynamicsFormRef = ref<InstanceType<typeof DynamicsForm>>()
const dataSourceTypeList = ref<Array<any>>([])
const providerList = ref<Array<any>>([])
const metaData = ref<Record<string, any>>({})
const formData = ref<Record<string, any>>({
  name: '',
  desc: '',
  dataSourceType: '',
  provider: ''
})

const folderId = computed(() => route.params.folderId as string)

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

const submit = () => {
  const { values, errors } = dynamicsFormRef.value?.validate() || { values: {}, errors: {} }
  if (Object.keys(errors).length > 0) return

  const payload = {
    name: formData.value.name,
    desc: formData.value.desc,
    dataSourceType: formData.value.dataSourceType,
    provider: formData.value.provider,
    meta: values || {}
  }

  treeCommonAPI.createResource(folderId.value, payload).then((ok) => {
    bus.emit('datasource:created', { folderId: folderId.value, node: ok.data })
    bus.emit('message:success', ['创建数据源', '成功'])
    router.push({ name: 'datasourceDetails', params: { id: ok.data.id } })
  })
}

const cancel = () => {
  router.back()
}

onMounted(() => {
  databaseConnectionPoolAPI.getDataSourceTypes().then((ok) => {
    dataSourceTypeList.value = ok.data
  })
})
</script>
