<template>
  <div class="flex flex-col h-full">
    <!-- 顶部操作栏 -->
    <div class="flex items-center justify-between px-6 py-3 border-b border-surface-border">
      <span class="text-lg font-bold">模型配置</span>
      <Button label="保存" icon="pi pi-save" @click="edit" />
    </div>

    <!-- 表单内容 -->
    <div class="flex-1 overflow-auto px-6 py-6">
      <DynamicsForm
        v-loading="loading"
        ref="dynamicsFormRef"
        :modelValue="formData"
        @update:modelValue="formData = $event"
        class="flex flex-col gap-6"
      >
        <template #default>
          <!-- 供应商 -->
          <div class="flex flex-col gap-1">
            <label class="text-sm font-semibold text-color">供应商</label>
            <RadioCard
              grid-class="grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5"
              :model-value="formData.provider"
              @update:model-value="setField('provider', $event)"
              :option-list="providerList"
              value-field="provider"
            >
              <template v-slot="item">
                <div class="flex items-center gap-2">
                  <div :innerHTML="item.icon" class="h-5 w-5 shrink-0" />
                  <span class="text-sm">{{ item.name }}</span>
                </div>
              </template>
            </RadioCard>
          </div>

          <!-- 模型类型 -->
          <div class="flex flex-col gap-1">
            <label class="text-sm font-semibold text-color">模型类型</label>
            <RadioCard
              grid-class="grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5"
              :model-value="formData.modelType"
              @update:model-value="setField('modelType', $event)"
              :option-list="modelTypeList"
              value-field="code"
            >
              <template v-slot="item">
                <div class="flex items-center gap-2">
                  <div :innerHTML="item.icon" class="h-5 w-5 shrink-0" />
                  <span class="text-sm">{{ item.message }}</span>
                </div>
              </template>
            </RadioCard>
          </div>

          <!-- 模型名称 -->
          <div class="flex flex-col gap-1">
            <label class="text-sm font-semibold text-color">模型名称</label>
            <Select
              :model-value="formData.modelName"
              @update:model-value="setField('modelName', $event)"
              :options="modelList"
              fluid
              optionLabel="name"
              option-value="name"
              placeholder="输入或选择模型名称"
              editable
            />
          </div>
        </template>

        <template #after>
          <!-- 模型参数 -->
          <div class="flex items-center justify-between mt-2">
            <span class="text-sm font-semibold text-color">模型参数</span>
            <Button label="添加参数" icon="pi pi-plus" size="small" variant="outlined" @click="openAddModelParameterForm()" />
          </div>

          <DataTable
            :value="formData.modelParameterForm || []"
            v-if="formData.modelParameterForm?.length > 0"
            size="small"
          >
            <Column field="field" header="字段" />
            <Column field="label" header="显示名称">
              <template #body="scope">
                {{ scope.data.label.value }}
              </template>
            </Column>
            <Column field="defaultValue" header="默认值" />
            <Column field="type" header="组件类型" />
            <Column field="operate" header="操作" style="width: 100px">
              <template #body="scope">
                <div class="flex gap-1">
                  <Button
                    icon="pi pi-file-edit"
                    variant="text"
                    rounded
                    size="small"
                    @click.stop="openAddModelParameterForm(scope.data, scope.index)"
                  />
                  <Button
                    icon="pi pi-trash"
                    variant="text"
                    severity="danger"
                    rounded
                    size="small"
                    @click="deleteModelParameterForm(scope.index)"
                  />
                </div>
              </template>
            </Column>
          </DataTable>
          <div v-else class="text-sm text-muted-color py-4 text-center border border-dashed border-surface-border rounded">
            暂无模型参数
          </div>

          <ModelParameterForm
            :addParams="addParamsModelParameterForm"
            ref="modelParameterFormRef"
          />
        </template>
      </DynamicsForm>
    </div>
  </div>
</template>
<script setup lang="ts">
import DynamicsForm from '@/components/dynamics-form-plus/index.vue'
import ModelAPI from '@/api/model'
import { computed, onMounted, ref, watch } from 'vue'
import RadioCard from '@/components/radio-card/index.vue'
import { groupBy } from '@/utils/common'
import ModelParameterForm from '@/views/model/components/ModelParameterForm.vue'
import bus from '@/bus'
import { useRoute } from 'vue-router'
import { TreeCommonAPI } from '@/api/tree'

const treeCommonAPI = new TreeCommonAPI('model')
const loading = ref<boolean>(false)
const route = useRoute()

const resourceId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})

const providerList = ref<Array<any>>([])
const modelList = ref<Array<any>>()
const modelDict = ref<any>({})
const modelTypeList = ref<Array<any>>([])
const defaultModelDict = ref<any>({})
const dynamicsFormRef = ref<InstanceType<typeof DynamicsForm>>()
const modelParameterFormRef = ref<InstanceType<typeof ModelParameterForm>>()
const model = ref<any>()
const formData = ref<Record<string, any>>({})

const setField = (field: string, value: any) => {
  dynamicsFormRef.value?.setFieldValue(field, value)
}

const addParamsModelParameterForm = (data: any, index?: number) => {
  const list = formData.value.modelParameterForm || []
  const fin = list.find((item: any, i: number) => item.field === data.field && i !== index)
  if (fin) {
    bus.emit('message:error', '字段:' + data.field + '已存在')
    return false
  }
  if (index !== undefined) {
    list.splice(index, 1, data)
  } else {
    list.push(data)
  }
  return true
}

const openAddModelParameterForm = (data?: any, index?: number) => {
  modelParameterFormRef.value?.open(data, index)
}

const deleteModelParameterForm = (index: number) => {
  formData.value.modelParameterForm?.splice(index, 1)
}

const toNestedObject = (flat: Record<string, any>) => {
  const result: Record<string, any> = {}
  for (const [key, value] of Object.entries(flat)) {
    const parts = key.split('.')
    let cur = result
    for (let i = 0; i < parts.length - 1; i++) {
      cur[parts[i]] = cur[parts[i]] || {}
      cur = cur[parts[i]]
    }
    cur[parts[parts.length - 1]] = value
  }
  return result
}

const edit = () => {
  const { values, errors } = dynamicsFormRef.value?.validate() || { values: {}, errors: {} }
  if (Object.keys(errors).length > 0) {
    return
  }
  const payload = toNestedObject(values)
  ModelAPI.edit(resourceId.value, payload, loading).then(() => {
    bus.emit('message:success', '模型保存成功')
  })
}

const credentialForm = computed(() => {
  const mt = formData.value.modelType
  const mn = formData.value.modelName
  if (mt && mn) {
    const typeDict = modelDict.value[mt]
    if (typeDict) {
      const v = typeDict[mn]
      if (v && v.length > 0) return v[0].credential
    }
    const defaults = defaultModelDict.value[mt]
    if (defaults && defaults.length > 0) return defaults[0].credential
  }
  return []
})

watch(credentialForm, () => {
  if (model.value && credentialForm.value.length) {
    dynamicsFormRef.value?.render(
      credentialForm.value.map((c: any) => ({ ...c, field: 'credential.' + c.field })),
      model.value
    )
  }
})

watch(model, () => {
  if (model.value && credentialForm.value.length) {
    dynamicsFormRef.value?.render(
      credentialForm.value.map((c: any) => ({ ...c, field: 'credential.' + c.field })),
      model.value
    )
  }
})

const provider = computed(() => formData.value.provider)

watch(
  provider,
  () => {
    if (provider.value) {
      ModelAPI.getProviderModelList(provider.value).then((ok) => {
        modelList.value = ok.data
        const _modelDict = groupBy(ok.data, 'modelType')
        for (const key in _modelDict) {
          _modelDict[key] = groupBy(_modelDict[key], 'name')
        }
        modelDict.value = _modelDict
        defaultModelDict.value = groupBy(
          ok.data.filter((item: any) => item.isDefault),
          'modelType'
        )
      })
      ModelAPI.listModelType(provider.value).then((ok) => {
        modelTypeList.value = ok.data
        if (ok.data) {
          const current = formData.value.modelType
          if (!current || !ok.data.some((t: any) => t.code === current)) {
            setField('modelType', ok.data[0].code)
          }
        }
      })
    }
  },
  { immediate: true }
)

watch(resourceId, () => get())

const get = () => {
  treeCommonAPI.getResource(resourceId.value).then((ok) => {
    model.value = ok.data
    if (ok.data.provider) setField('provider', ok.data.provider)
    if (ok.data.modelType) setField('modelType', ok.data.modelType)
    if (ok.data.modelName) setField('modelName', ok.data.modelName)
    if (ok.data.modelParameterForm) setField('modelParameterForm', ok.data.modelParameterForm)
  })
}

onMounted(() => {
  ModelAPI.getProvider().then((ok) => {
    providerList.value = ok.data
  })
  get()
})
</script>
<style lang="scss" scoped></style>
