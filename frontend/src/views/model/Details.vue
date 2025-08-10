<template>
  <header class="sticky top-0 left-0 z-50 bg-white right-0 pr-10 pl-10">
    <div
      class="w-full flex items-center gap-x-4 rounded-xl p-4 mb-5 shadow-lg outline outline-black/5 dark:bg-slate-800 dark:shadow-none dark:-outline-offset-1 dark:outline-white/10"
    >
      <span>{{ name }} </span>
      <div class="flex-auto"></div>
      <el-button type="primary" text bg @click="goEdit">{{ $t('common.edit') }} </el-button>
    </div>
  </header>

  <DynamicsForm
    :disabled="true"
    class="pr-10 pl-10 pb-10"
    :model="modelForm"
    v-model="dynamicsFormValue"
    ref="dynamicsFormRef"
    label-position="top"
    require-asterisk-position="right"
    :otherParams="{}"
  >
    <template #default>
      <el-form-item prop="provider" required>
        <template #label> 供应商 </template>
        <RadioCard
          style="--el-card-padding: 5px"
          body-class="p-0"
          v-model="baseModelForm.provider"
          :option-list="providerList"
          value-field="provider"
        >
          <template v-slot="item">
            <div class="w-full h-full flex items-center justify-center content-center">
              <div :innerHTML="item.icon" style="height: 20px; width: 20px" class="mr-4" />
              {{ item.name }}
            </div>
          </template>
        </RadioCard>
      </el-form-item>
      <el-form-item prop="modelType" required>
        <template #label> 模型类型 </template>
        <RadioCard
          style="--el-card-padding: 5px"
          body-class="p-0"
          v-model="baseModelForm.modelType"
          :option-list="modelTypeList"
          value-field="code"
        >
          <template v-slot="item">
            <div class="w-full h-full flex items-center justify-center content-center">
              <div :innerHTML="item.icon" style="height: 20px; width: 20px" class="mr-4" />
              {{ item.message }}
            </div>
          </template>
        </RadioCard>
      </el-form-item>
      <el-form-item prop="modelName" required>
        <template #label>
          {{ $t('model.form.modelName.label') }}
        </template>
        <el-select
          v-model="baseModelForm.modelName"
          :placeholder="$t('model.form.modelName.placeholder')"
          style="width: 100%"
        >
          <el-option
            v-for="model in modelList"
            :key="model.name"
            :label="model.name"
            :value="model.name"
          />
        </el-select>
      </el-form-item>
    </template>
    <template #after>
      <el-table
        :data="baseModelForm.modelParameterForm"
        v-if="baseModelForm.modelParameterForm?.length > 0"
        class="mb-16"
      >
        <el-table-column
          prop="label"
          :label="$t('dynamicsForm.paramForm.name.label')"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span v-if="row.label && row.label.input_type === 'TooltipLabel'">{{
              row.label.label
            }}</span>
            <span v-else>{{ row.label }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="field"
          :label="$t('dynamicsForm.paramForm.field.label')"
          show-overflow-tooltip
          width="95px"
        />
        <el-table-column :label="$t('dynamicsForm.paramForm.input_type.label')" width="110px">
          <template #default="{ row }">
            <el-tag type="info" class="info-tag">{{
              input_type_list.find((item) => item.value === row.input_type)?.label
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="default_value"
          :label="$t('dynamicsForm.default.label')"
          show-overflow-tooltip
        />
        <el-table-column :label="$t('common.required')">
          <template #default="{ row }">
            <div @click.stop>
              <el-switch disabled size="small" v-model="row.required" />
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="$t('common.operation')" align="left" width="100">
          <template #default="{ row, $index }">
            <span class="flex items-center content-center justify-center">
              <el-tooltip effect="dark" :content="$t('common.modify')" placement="top">
                <el-button type="primary" text @click.stop="openAddmodelParameterForm(row, $index)">
                  <el-icon><EditPen /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip effect="dark" :content="$t('common.delete')" placement="top">
                <el-button type="primary" text @click="deletemodelParameterForm($index)">
                  <el-icon>
                    <Delete />
                  </el-icon>
                </el-button>
              </el-tooltip>
            </span>
          </template>
        </el-table-column>
      </el-table>
      <modelParameterForm
        :addParams="addParamsmodelParameterForm"
        ref="modelParameterFormRef"
      ></modelParameterForm>
    </template>
  </DynamicsForm>
</template>
<script setup lang="ts">
import DynamicsForm from '@/components/dynamics-form/index.vue'
import ModelAPI from '@/api/model'
import NodeAPI from '@/api/node'
import { computed, onMounted, ref, watch, inject } from 'vue'
import RadioCard from '@/components/radio-card/index.vue'
import { groupBy } from '@/utils/common'
import ModelParameterForm from '@/views/model/components/ModelParameterForm.vue'
import { input_type_list } from '@/components/dynamics-form/constructor/data'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
const router = useRouter()
const route = useRoute()
const folderId = computed(() => {
  const {
    params: { folderId }
  } = route as any
  return folderId
})
const resourceId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})
const getNode = inject('getNode') as any
const providerList = ref<Array<any>>([])
const modelList = ref<Array<any>>()
const modelDict = ref<any>({})
const modelTypeList = ref<Array<any>>([])
const defaultModelDict = ref<any>({})
const dynamicsFormRef = ref<InstanceType<typeof DynamicsForm>>()
const dynamicsFormValue = ref<any>({})
const modelParameterFormRef = ref<InstanceType<typeof ModelParameterForm>>()
const name = computed(() => {
  if (getNode && getNode()) {
    return getNode().name
  }
  return ''
})
const addParamsmodelParameterForm = (data: any, index?: number) => {
  const fin = baseModelForm.value.modelParameterForm.find(
    (item: any, i: number) => item.field == data.field && i !== index
  )
  if (fin) {
    ElMessage.error('字段:' + data.field + '已存在')
    return false
  }

  if (index !== undefined) {
    baseModelForm.value.modelParameterForm.splice(index, 1, data)
  } else {
    baseModelForm.value.modelParameterForm.push(data)
  }
  return true
}
const openAddmodelParameterForm = (data?: any, index?: number) => {
  modelParameterFormRef.value?.open(data, index)
}
const deletemodelParameterForm = (index: number) => {
  baseModelForm.value.modelParameterForm.splice(index, 1)
}
const edit = () => {
  ModelAPI.edit(folderId.value, resourceId.value, {
    ...baseModelForm.value,
    credential: dynamicsFormValue.value
  })
}
const baseModelForm = ref<any>({
  name: '',
  provider: 'openai_provider',
  modelType: '',
  modelName: '',
  modelParameterForm: []
})

const modelForm = computed(() => {
  return {
    ...dynamicsFormValue.value,
    ...baseModelForm.value
  }
})

const credentialForm = computed(() => {
  if (baseModelForm.value.modelName) {
    const v = modelDict.value[baseModelForm.value.modelType][baseModelForm.value.modelName]
    if (v && v.length > 0) {
      return v[0].credential
    }
    return defaultModelDict.value[baseModelForm.value.modelType][0].credential
  }
  return []
})

watch(credentialForm, () => {
  dynamicsFormRef.value?.render(credentialForm.value, dynamicsFormValue.value)
})

watch(
  () => baseModelForm.value.provider,
  () => {
    if (baseModelForm.value.provider) {
      ModelAPI.getProviderModelList(baseModelForm.value.provider).then((ok) => {
        modelList.value = ok.data
        const _modelDict = groupBy(ok.data, 'modelType')
        for (const key in _modelDict) {
          const v = _modelDict[key]
          _modelDict[key] = groupBy(v, 'name')
        }

        modelDict.value = _modelDict
        defaultModelDict.value = groupBy(
          ok.data.filter((item: any) => item.isDefault),
          'modelType'
        )
      })
      ModelAPI.listModelType(baseModelForm.value.provider).then((ok) => {
        modelTypeList.value = ok.data
        if (ok.data) {
          baseModelForm.value.modelType = ok.data[0].code
        }
      })
    }
  },
  { immediate: true }
)
const goEdit = () => {
  router.push({ name: 'modelEdit', params: { folderId: folderId.value, id: resourceId.value } })
}

watch(resourceId, () => {
  get()
})
const get = () => {
  NodeAPI.resourceInfo('model', folderId.value, resourceId.value).then((ok) => {
    baseModelForm.value = ok.data
    dynamicsFormValue.value = ok.data.credential
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
