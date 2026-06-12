<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="current ? t('database.editPool') : t('database.createPool')"
    :style="{ width: '50rem' }"
  >
    <div class="flex flex-col gap-4">
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">{{ t('datasource.form.name') }}</label>
        <InputText v-model="formData.name" type="text" fluid />
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">{{ t('datasource.form.desc') }}</label>
        <InputText v-model="formData.desc" type="text" fluid />
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold text-color">{{ t('datasource.form.type') }}</label>
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
        <label class="text-sm font-semibold text-color">{{ t('datasource.form.vendor') }}</label>
        <RadioCard
          grid-class="grid-cols-2 sm:grid-cols-3 md:grid-cols-4"
          :model-value="formData.protocol"
          @update:model-value="setField('protocol', $event)"
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
        v-if="formData.protocol"
        ref="dynamicsFormRef"
        :modelValue="metaData"
        @update:modelValue="metaData = $event"
      />
    </div>
    <template #footer>
      <Button @click="submit">{{ t('common.save') }}</Button>
      <Button @click="close">{{ t('common.cancel') }}</Button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { nextTick, ref, watch, computed, onMounted } from 'vue'
import { t } from '@/locales'
import DynamicsForm from '@/components/dynamics-form-plus/index.vue'
import RadioCard from '@/components/radio-card/index.vue'
import databaseConnectionPoolAPI from '@/api/database-connection-pool'
import bus from '@/bus'

const emit = defineEmits(['refresh'])
const props = defineProps<{
  projectId: string
}>()

const dynamicsFormRef = ref<InstanceType<typeof DynamicsForm>>()
const visible = ref<boolean>(false)
const current = ref<any>()
const dataSourceTypeList = ref<Array<any>>([])
const providerList = ref<Array<any>>([])
const formData = ref<Record<string, any>>({
  name: '',
  desc: '',
  dataSourceType: '',
  protocol: ''
})
const metaData = ref<Record<string, any>>({})

const setField = (field: string, value: any) => {
  formData.value = { ...formData.value, [field]: value }
}

const setDataSourceType = (value: string) => {
  formData.value = { ...formData.value, dataSourceType: value, protocol: '' }
  providerList.value = []
  loadProviders(value)
}

const dataSourceType = computed(() => formData.value.dataSourceType)
const protocol = computed(() => formData.value.protocol)

watch(protocol, () => {
  if (protocol.value) {
    loadFormDefinition(protocol.value)
  }
})

const loadProviders = (type: string) => {
  databaseConnectionPoolAPI.getProviders(type).then((ok) => {
    providerList.value = ok.data
  })
}

const loadFormDefinition = (provider: string) => {
  databaseConnectionPoolAPI.getProviderForm(provider).then((ok) => {
    if (ok.data) {
      const fields = ok.data.map((item: any) => ({
        ...item,
        field: item.field
      }))
      nextTick(() => {
        dynamicsFormRef.value?.render(fields, current.value?.meta || {})
      })
    }
  })
}

const open = (databaseCollectionPool?: any) => {
  visible.value = true
  current.value = databaseCollectionPool
  metaData.value = {}
  providerList.value = []

  const loadData = () => {
    if (databaseCollectionPool) {
      const dsType = databaseCollectionPool.dataSourceType || ''
      const provider = databaseCollectionPool.provider || ''
      formData.value = {
        name: databaseCollectionPool.name,
        desc: databaseCollectionPool.desc,
        dataSourceType: dsType,
        protocol: provider
      }
      // 如果有 dataSourceType，加载对应的供应商列表
      if (dsType) {
        loadProviders(dsType)
      }
    } else {
      formData.value = {
        name: '',
        desc: '',
        dataSourceType: '',
        protocol: ''
      }
    }
  }

  if (dataSourceTypeList.value.length === 0) {
    databaseConnectionPoolAPI.getDataSourceTypes().then((ok) => {
      dataSourceTypeList.value = ok.data
      nextTick(loadData)
    })
  } else {
    loadData()
  }
}

const close = () => {
  visible.value = false
  current.value = undefined
  formData.value = { name: '', desc: '', dataSourceType: '', protocol: '' }
  metaData.value = {}
  providerList.value = []
}

const submit = () => {
  const { values, errors } = dynamicsFormRef.value?.validate() || { values: {}, errors: {} }
  if (Object.keys(errors).length > 0) {
    return
  }

  const payload = {
    name: formData.value.name,
    desc: formData.value.desc,
    dataSourceType: formData.value.dataSourceType,
    provider: formData.value.protocol,
    meta: values || {}
  }

  if (current.value) {
    databaseConnectionPoolAPI
      .edit(props.projectId, current.value.id, payload)
      .then(() => {
        bus.emit('message:success', [t('database.editSuccess'), t('common.confirm')])
        emit('refresh')
        close()
      })
  } else {
    databaseConnectionPoolAPI
      .create(props.projectId, payload)
      .then(() => {
        emit('refresh')
        bus.emit('message:success', [t('database.createSuccess'), t('common.confirm')])
        close()
      })
  }
}

onMounted(() => {
  databaseConnectionPoolAPI.getDataSourceTypes().then((ok) => {
    dataSourceTypeList.value = ok.data
  })
})

defineExpose({ open, close })
</script>
<style lang="scss"></style>
