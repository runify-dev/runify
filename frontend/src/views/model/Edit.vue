<template>
  <div class="flex flex-col">
    <div class="card overflow-auto" style="height: calc(100dvh - 8rem)">
      <Button class="float-right" label="保存" @click="edit" />
      <DynamicsForm
        class="pr-10 pl-10 pb-10"
        v-loading="loading"
        ref="dynamicsFormRef"
        :otherParams="{}"
      >
        <template #default>
          <FormField
            v-slot="$field: any"
            asChild
            name="provider"
            :resolver="resolver.provider"
            initialValue="openai_provider"
          >
            <label>供应商</label>
            <RadioCard
              style="--el-card-padding: 5px"
              body-class="p-0"
              :model-value="$field.value"
              @update:model-value="(v) => $field.onChange({ value: v })"
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
          </FormField>
          <FormField
            v-slot="$field: any"
            asChild
            name="modelType"
            :resolver="resolver.modelType"
            initialValue="LLM"
          >
            <label>模型类型</label>
            <RadioCard
              style="--el-card-padding: 5px"
              body-class="p-0"
              :model-value="$field.value"
              @update:model-value="(v) => $field.onChange({ value: v })"
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
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
              $field.error?.message
            }}</Message>
          </FormField>
          <FormField
            v-slot="$field"
            asChild
            name="modelName"
            initialValue=""
            :resolver="resolver.modelName"
          >
            <label>模型名称</label>
            <Select
              :options="modelList"
              fluid
              optionLabel="name"
              option-value="name"
              placeholder="请选择模型"
              class="w-full md:w-56"
            />
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
              $field.error?.message
            }}</Message>
          </FormField>
        </template>
        <template #after>
          <FormField asChild v-slot="$field" name="modelParameterForm" :initialValue="[]">
            <DataTable :value="$field.value" v-if="$field.value?.length > 0">
              <Column field="field" header="字段"></Column>
              <Column field="label" header="显示名称">
                <template #body="scope">
                  {{ scope.data.label.value }}
                </template>
              </Column>
              <Column field="defaultValue" header="默认值"></Column>
              <Column field="type" header="组件类型"></Column>
              <Column field="operate" header="操作">
                <template #body="scope">
                  <Button
                    icon="pi pi-file-edit"
                    variant="text"
                    rounded
                    aria-label="Cancel"
                    size="normal"
                    @click.stop="openAddmodelParameterForm(scope.data, scope.index)"
                  />
                  <Button
                    icon="pi pi-times-circle"
                    variant="text"
                    rounded
                    aria-label="Cancel"
                    size="normal"
                    @click="deletemodelParameterForm(scope.index)"
                  />
                </template>
              </Column>
            </DataTable>
          </FormField>

          <Button label="添加模型参数" variant="text" @click="openAddmodelParameterForm()" />
          <ModelParameterForm
            :addParams="addParamsmodelParameterForm"
            ref="modelParameterFormRef"
          ></ModelParameterForm>
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
import { useRoute, useRouter } from 'vue-router'
import { TreeCommonAPI } from '@/api/tree'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
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
const addParamsmodelParameterForm = (data: any, index?: number) => {
  const modelParameterForm =
    dynamicsFormRef.value?.formRef?.getFieldState('modelParameterForm')?.value
  const fin = modelParameterForm.find(
    (item: any, i: number) => item.field == data.field && i !== index
  )
  if (fin) {
    bus.emit('message:error', '字段:' + data.field + '已存在')
    return false
  }

  if (index !== undefined) {
    modelParameterForm.splice(index, 1, data)
  } else {
    modelParameterForm.push(data)
  }
  return true
}
const openAddmodelParameterForm = (data?: any, index?: number) => {
  modelParameterFormRef.value?.open(data, index)
}
const deletemodelParameterForm = (index: number) => {
  const modelParameterForm =
    dynamicsFormRef.value?.formRef?.getFieldState('modelParameterForm')?.value
  modelParameterForm.splice(index, 1)
}

const edit = () => {
  dynamicsFormRef.value?.formRef?.validate().then(({ values, errors }) => {
    if (Object.keys(errors).length === 0) {
      ModelAPI.edit(resourceId.value, values, loading).then(() => {
        bus.emit('message:success', '模型保存成功')
      })
    }
  })
}
const resolver = computed(() => {
  return {
    provider: zodResolver(z.string().min(1, { error: '供应商必填' })),
    modelType: zodResolver(z.string().min(1, { error: '模型类型必填' })),
    modelName: zodResolver(z.string().min(1, { error: '模型名称必填' }))
  }
})
const getCredentialForm = (type: string, name: string) => {
  const v = modelDict.value[type][name]
  if (v && v.length > 0) {
    return v[0].credential
  }
  return defaultModelDict.value[type][0].credential
}

const credentialForm = computed(() => {
  const modelType = dynamicsFormRef.value?.formRef?.getFieldState('modelType')
  const modelName = dynamicsFormRef.value?.formRef?.getFieldState('modelName')
  if (modelName && modelType && modelType.value && modelName.value) {
    return getCredentialForm(modelType.value, modelName.value)
  }
  return []
})

watch(credentialForm, () => {
  if (credentialForm.value) {
    dynamicsFormRef.value?.render(
      credentialForm.value.map((c: any) => ({ ...c, field: 'credential.' + c.field })),
      {}
    )
  }
})

const provider = computed(() => {
  return dynamicsFormRef.value?.formRef?.getFieldState('provider')?.value
})

watch(
  provider,
  () => {
    if (provider.value) {
      ModelAPI.getProviderModelList(provider.value).then((ok) => {
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
      ModelAPI.listModelType(provider.value).then((ok) => {
        modelTypeList.value = ok.data
        if (ok.data) {
          dynamicsFormRef.value?.formRef?.setFieldValue('modelType', ok.data[0].code)
        }
      })
    }
  },
  { immediate: true }
)

watch(resourceId, () => {
  get()
})
const get = () => {
  treeCommonAPI.getResource(resourceId.value).then((ok) => {
    model.value = ok.data
    const credentialForm = getCredentialForm(ok.data.modelType, ok.data.modelName)
    dynamicsFormRef.value?.render(
      credentialForm.map((c: any) => ({ ...c, field: 'credential.' + c.field })),
      ok.data
    )
    dynamicsFormRef.value?.formRef?.setFieldValue('modelName', ok.data.modelName)
    dynamicsFormRef.value?.formRef?.setFieldValue('modelParameterForm', ok.data.modelParameterForm)
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
