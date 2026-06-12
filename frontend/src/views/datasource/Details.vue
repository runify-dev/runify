<template>
  <div class="p-4">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-lg font-bold">{{ t('datasource.details.title') }}</h2>
      <Button @click="submit">{{ t('common.save') }}</Button>
    </div>
    <div class="flex flex-col gap-4 max-w-2xl">
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">{{ t('datasource.details.name') }}</label>
        <InputText v-model="formData.name" type="text" fluid />
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">{{ t('datasource.details.desc') }}</label>
        <InputText v-model="formData.desc" type="text" fluid />
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">{{ t('datasource.details.type') }}</label>
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
        <label class="text-sm font-semibold text-color">{{ t('datasource.details.vendor') }}</label>
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
    </div>

    <!-- 表信息 - 仅 SQL 类型显示 -->
    <TablePreview
      v-if="isSQLType"
      :datasource-id="resourceId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { t } from '@/locales'
import DynamicsForm from '@/components/dynamics-form-plus/index.vue'
import RadioCard from '@/components/radio-card/index.vue'
import TablePreview from './components/TablePreview.vue'
import databaseConnectionPoolAPI from '@/api/database-connection-pool'
import { TreeCommonAPI } from '@/api/tree'
import { put } from '@/request/admin/index'
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
const loading = ref(false)

const resourceId = ref(route.params.id as string)

// 判断是否为 SQL 类型数据源
const isSQLType = computed(() => {
  const type = formData.value.dataSourceType?.toLowerCase() || ''
  return type.includes('sql') || type.includes('database') || type.includes('db')
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

const loadFormDefinition = (provider: string, meta: Record<string, any>) => {
  databaseConnectionPoolAPI.getProviderForm(provider).then((ok) => {
    if (ok.data) {
      nextTick(() => {
        dynamicsFormRef.value?.render(ok.data, meta)
      })
    }
  })
}

watch(
  () => formData.value.provider,
  (provider, oldProvider) => {
    if (loading.value) return
    if (provider && provider !== oldProvider) {
      loadFormDefinition(provider, {})
    }
  }
)

const loadResource = () => {
  loading.value = true
  treeCommonAPI.getResource(resourceId.value).then((ok) => {
    const data = ok.data
    formData.value = {
      name: data.name || '',
      desc: data.desc || '',
      dataSourceType: data.dataSourceType || '',
      provider: data.provider || ''
    }
    metaData.value = data.meta || {}

    if (data.dataSourceType) {
      loadProviders(data.dataSourceType)
    }
    if (data.provider) {
      loadFormDefinition(data.provider, data.meta || {})
    }
    nextTick(() => {
      loading.value = false
    })
  })
}

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

  put(`/datasource/resources/${resourceId.value}`, payload).then(() => {
    bus.emit('message:success', [t('datasource.modifySuccess'), ''])
  })
}

watch(
  () => route.params.id,
  (newId) => {
    resourceId.value = newId as string
    loadResource()
  }
)

onMounted(() => {
  databaseConnectionPoolAPI.getDataSourceTypes().then((ok) => {
    dataSourceTypeList.value = ok.data
  })
  loadResource()
})
</script>
