<template>
  <Dialog v-model:visible="visible" modal header="新建模型" :style="{ width: '36rem' }">
    <div v-loading="loading" class="flex flex-col gap-4">
      <!-- 名称 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">名称</label>
        <InputText
          v-model="formData.name"
          type="text"
          placeholder="请输入名称"
          fluid
          class="!text-sm"
        />
      </div>

      <!-- 供应商 -->
      <div class="flex flex-col gap-1.5">
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
              <div :innerHTML="item.icon" class="h-5 w-5 shrink-0 [&_svg]:w-full [&_svg]:h-full"/>
              <span class="text-sm">{{ item.name }}</span>
            </div>
          </template>
        </RadioCard>
      </div>

      <!-- 模型类型 -->
      <div v-if="formData.provider" class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">模型类型</label>
        <RadioCard
          grid-class="grid-cols-2 sm:grid-cols-3 md:grid-cols-4"
          :model-value="formData.modelType"
          @update:model-value="setField('modelType', $event)"
          :option-list="modelTypeList"
          value-field="code"
        >
          <template v-slot="item">
            <div class="flex items-center gap-2">
              <div :innerHTML="item.icon" class="h-5 w-5 shrink-0 [&_svg]:w-full [&_svg]:h-full"/>
              <span class="text-sm">{{ item.message }}</span>
            </div>
          </template>
        </RadioCard>
      </div>

      <!-- 模型名称 -->
      <div v-if="formData.provider" class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">模型名称</label>
        <Select
          :model-value="formData.modelName"
          @update:model-value="setField('modelName', $event)"
          :options="modelList"
          fluid
          optionLabel="name"
          option-value="name"
          placeholder="输入或选择模型名称"
          editable
          class="!text-sm"
        />
      </div>

      <!-- 动态表单 -->
      <DynamicsForm
        v-if="formData.provider"
        ref="dynamicsFormRef"
        :modelValue="metaData"
        @update:modelValue="metaData = $event"
      />

      <!-- 模型参数 -->
      <div v-if="formData.provider" class="flex flex-col gap-2">
        <div class="flex items-center justify-between">
          <label class="text-sm font-medium text-surface-700">模型参数</label>
          <Button label="添加参数" icon="pi pi-plus" size="small" variant="outlined"
                  @click="openAddModelParameterForm()"/>
        </div>
        <DataTable :value="modelParameterForm" v-if="modelParameterForm.length > 0" size="small">
          <Column field="field" header="字段"/>
          <Column field="label" header="显示名称">
            <template #body="scope">
              {{ scope.data.label.value }}
            </template>
          </Column>
          <Column field="defaultValue" header="默认值"/>
          <Column field="type" header="组件类型"/>
          <Column field="operate" header="操作" style="width: 100px">
            <template #body="scope">
              <div class="flex gap-1">
                <Button icon="pi pi-file-edit" variant="text" rounded size="small"
                        @click.stop="openAddModelParameterForm(scope.data, scope.index)"/>
                <Button icon="pi pi-trash" variant="text" severity="danger" rounded size="small"
                        @click="deleteModelParameterForm(scope.index)"/>
              </div>
            </template>
          </Column>
        </DataTable>
        <div v-else
             class="text-sm text-surface-400 py-4 text-center border border-dashed border-surface-300 rounded">
          暂无模型参数
        </div>
        <ModelParameterForm :addParams="addParamsModelParameterForm" ref="modelParameterFormRef"/>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button label="取消" :loading="loading" severity="secondary" variant="outlined"
                @click="close"/>
        <Button label="创建" :loading="loading" @click="submit"/>
      </div>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import {ref, watch, computed} from 'vue'
import DynamicsForm from '@/components/dynamics-form-plus/index.vue'
import RadioCard from '@/components/radio-card/index.vue'
import ModelParameterForm from './ModelParameterForm.vue'
import ModelAPI from '@/api/model'
import {TreeCommonAPI} from '@/api/tree'
import {groupBy} from '@/utils/common'
import type {TreeNode} from 'primevue/treenode'
import bus from '@/bus'
import {ROOT_FOLDER_ID} from "@/constants/common.ts";

const props = defineProps<{ api: TreeCommonAPI }>()
const emit = defineEmits(['create:success'])

const visible = ref(false)
const current = ref<TreeNode>()
const dynamicsFormRef = ref<InstanceType<typeof DynamicsForm>>()
const modelParameterFormRef = ref<InstanceType<typeof ModelParameterForm>>()
const loading = ref<boolean>(false);
const providerList = ref<Array<any>>([])
const modelList = ref<Array<any>>([])
const modelDict = ref<any>({})
const modelTypeList = ref<Array<any>>([])
const defaultModelDict = ref<any>({})
const metaData = ref<Record<string, any>>({})
const modelParameterForm = ref<Array<any>>([])

const formData = ref<Record<string, any>>({
  name: '',
  provider: '',
  modelType: '',
  modelName: ''
})

const setField = (field: string, value: any) => {
  formData.value = {...formData.value, [field]: value}
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
  if (credentialForm.value.length) {
    dynamicsFormRef.value?.render(
      credentialForm.value.map((c: any) => ({...c, field: 'credential.' + c.field})),
      metaData.value
    )
  }
})

watch(
  () => formData.value.provider,
  (provider) => {
    if (provider) {
      ModelAPI.getProviderModelList(provider).then((ok) => {
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
      ModelAPI.listModelType(provider).then((ok) => {
        modelTypeList.value = ok.data
        if (ok.data && ok.data.length > 0) {
          setField('modelType', ok.data[0].code)
        }
      })
    } else {
      modelList.value = []
      modelTypeList.value = []
      modelDict.value = {}
      defaultModelDict.value = {}
    }
    setField('modelType', '')
    setField('modelName', '')
  }
)

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

const addParamsModelParameterForm = (data: any, index?: number) => {
  const list = modelParameterForm.value
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
  modelParameterForm.value.splice(index, 1)
}

const resetForm = () => {
  formData.value = {name: '', provider: '', modelType: '', modelName: ''}
  metaData.value = {}
  modelParameterForm.value = []
  providerList.value = []
  modelList.value = []
  modelTypeList.value = []
  modelDict.value = {}
  defaultModelDict.value = {}
}

const submit = () => {
  if (!formData.value.name.trim() || !formData.value.provider || !formData.value.modelType || !formData.value.modelName) return

  const {values, errors} = dynamicsFormRef.value?.validate() || {values: {}, errors: {}}
  if (Object.keys(errors).length > 0) return

  const credential = toNestedObject(values)
  const payload = {
    name: formData.value.name,
    provider: formData.value.provider,
    modelType: formData.value.modelType,
    modelName: formData.value.modelName,
    ...credential,
    modelParameterForm: modelParameterForm.value
  }

  ModelAPI.validate(current.value ? current.value.key : ROOT_FOLDER_ID, payload, loading).then(ok => {
    if (ok.code == 500) {
      bus.emit('message:error', ok.message)
      return
    }
    props.api
      .createResource(current.value ? current.value.key : ROOT_FOLDER_ID, payload)
      .then((ok) => {
        emit('create:success', current.value ? current.value.key : undefined, ok.data)
        close()
      })
  })
}

const open = (node?: TreeNode) => {
  current.value = node
  resetForm()
  visible.value = true
  ModelAPI.getProvider().then((ok) => {
    providerList.value = ok.data
  })
}

const close = () => {
  current.value = undefined
  visible.value = false
}

defineExpose({open, close})
</script>
