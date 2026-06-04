<template>
  <div class="flex flex-col h-full">
    <!-- 顶部操作栏 -->
    <div class="flex items-center justify-between px-6 py-3 border-b" style="border-color: var(--p-content-border-color);">
      <span class="text-lg font-bold">技能设置</span>
      <Button label="保存" icon="pi pi-save" @click="save" :loading="saving"/>
    </div>

    <!-- 表单内容 -->
    <div class="flex-1 overflow-auto px-6 py-6 flex flex-col gap-8">
      <!-- 基本信息 -->
      <div class="flex flex-col gap-4">
        <h3 class="text-sm font-semibold text-color">基本信息</h3>
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-surface-700">技能名称</label>
          <InputText v-model="formData.name" type="text" fluid class="!text-sm"/>
        </div>
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-surface-700">技能描述</label>
          <Textarea v-model="formData.desc" rows="3" fluid class="!text-sm !resize-none"/>
        </div>
      </div>

      <!-- 参数表单定义 -->
      <div class="flex flex-col gap-4">
        <div class="flex items-center justify-between">
          <h3 class="text-sm font-semibold text-color">参数定义</h3>
          <Button label="添加参数" icon="pi pi-plus" size="small" variant="outlined" @click="openAddParameterForm()"/>
        </div>
        <p class="text-xs text-surface-400 -mt-2">定义技能运行时需要的输入参数</p>
        <DataTable :value="formData.skillParameterForm || []" v-if="formData.skillParameterForm?.length > 0" size="small">
          <Column field="field" header="字段"/>
          <Column field="label" header="显示名称">
            <template #body="scope">
              {{ scope.data.label?.value || scope.data.label }}
            </template>
          </Column>
          <Column field="defaultValue" header="默认值"/>
          <Column field="type" header="组件类型"/>
          <Column field="operate" header="操作" style="width: 100px">
            <template #body="scope">
              <div class="flex gap-1">
                <Button icon="pi pi-file-edit" variant="text" rounded size="small"
                        @click.stop="openAddParameterForm(scope.data, scope.index)"/>
                <Button icon="pi pi-trash" variant="text" severity="danger" rounded size="small"
                        @click="deleteParameterForm(scope.index)"/>
              </div>
            </template>
          </Column>
        </DataTable>
        <div v-else class="text-sm text-surface-400 py-4 text-center border border-dashed border-surface-300 rounded">
          暂无参数定义
        </div>
        <ModelParameterForm :addParams="addParameterForm" ref="parameterFormRef"/>
      </div>

      <!-- 参数值（根据参数定义自动渲染表单） -->
      <div class="flex flex-col gap-4" v-if="formData.skillParameterForm?.length > 0">
        <h3 class="text-sm font-semibold text-color">参数值</h3>
        <p class="text-xs text-surface-400 -mt-2">设置参数的具体值</p>
        <DynamicsForm
          ref="dynamicsFormRef"
          :modelValue="parameterValues"
          @update:modelValue="parameterValues = $event"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onMounted, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {TreeCommonAPI} from '@/api/tree'
import DynamicsForm from '@/components/dynamics-form-plus/index.vue'
import ModelParameterForm from '@/views/model/components/ModelParameterForm.vue'
import skillApi from '@/api/skill'
import bus from '@/bus'

const route = useRoute()
const treeCommonAPI = new TreeCommonAPI('skill')
const saving = ref(false)

const resourceId = computed(() => (route.params as any).id)

const formData = ref<Record<string, any>>({
  name: '',
  desc: '',
  skillParameterForm: []
})

const parameterValues = ref<Record<string, any>>({})
const dynamicsFormRef = ref<InstanceType<typeof DynamicsForm>>()
const parameterFormRef = ref<InstanceType<typeof ModelParameterForm>>()

// 参数定义
const addParameterForm = (data: any, index?: number) => {
  const list = formData.value.skillParameterForm || []
  const dup = list.find((item: any, i: number) => item.field === data.field && i !== index)
  if (dup) {
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

const openAddParameterForm = (data?: any, index?: number) => {
  parameterFormRef.value?.open(data, index)
}

const deleteParameterForm = (index: number) => {
  formData.value.skillParameterForm?.splice(index, 1)
}

// 渲染参数值表单
const renderParamForm = () => {
  const fields = formData.value.skillParameterForm || []
  if (fields.length > 0 && dynamicsFormRef.value) {
    dynamicsFormRef.value.render(fields, parameterValues.value)
  }
}

// 加载数据
const get = () => {
  treeCommonAPI.getResource(resourceId.value).then(ok => {
    const data = ok.data
    formData.value = {
      name: data.name || '',
      desc: data.desc || '',
      skillParameterForm: data.skillParameterForm || []
    }
    // parameterValue 后端已解密为 JsonObject
    parameterValues.value = data.parameterValue || {}
    nextTick(() => renderParamForm())
  })
}

// 保存
const save = () => {
  saving.value = true
  skillApi.edit(resourceId.value, {
    name: formData.value.name,
    desc: formData.value.desc,
    parameterValue: parameterValues.value,
    skillParameterForm: formData.value.skillParameterForm
  }).then(() => {
    saving.value = false
    bus.emit('message:success', '保存成功')
  }).catch(() => {
    saving.value = false
  })
}

watch(resourceId, () => get())

onMounted(() => {
  get()
})
</script>
