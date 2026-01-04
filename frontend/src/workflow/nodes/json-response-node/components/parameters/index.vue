<template>
  <div class="w-full">
    <div class="flex w-full justify-between content-center items-center">
      <h5 class="break-all lighter">响应数据</h5>
      <span class="ml-4" style="margin-top: -4px">
        <el-button link type="primary" @click="open()">
          <el-icon><Plus /></el-icon>
        </el-button>
      </span>
    </div>
    <el-card shadow="never" style="--el-card-padding: 12px" class="w-full">
      <el-form
        @submit.prevent
        ref="parameterFormRef"
        :model="{ parameters: data }"
        label-position="top"
        require-asterisk-position="right"
      >
        <template v-for="(value, index) in data" :key="value.field">
          <el-form-item
            :label="value.field"
            :prop="'parameters.' + index + '.value'"
            :rules="[
              {
                required: value.required,
                message: value.location === 'reference' ? '请选择引用参数' : '请输入参数',
                trigger: 'blur'
              }
            ]"
          >
            <template #label>
              {{ value.field }}
              <el-tooltip
                class="box-item"
                effect="dark"
                :content="value.description"
                placement="top-start"
              >
                <el-icon><Warning /></el-icon>
              </el-tooltip>
            </template>
            <div class="flex justify-between w-full">
              <el-cascader
                v-if="value.location === 'reference'"
                placeholder="请选择参数"
                :options="options"
                v-model="value.reference"
                :show-all-levels="false"
                class="w-full"
              />
              <el-input v-else v-model="value.value" placeholder="请输入参数"></el-input>
              <el-tooltip effect="dark" content="删除" placement="top">
                <el-button class="p-0 m-0" type="primary" text @click="deleteParameter(value)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </el-form-item>
        </template>
      </el-form>
    </el-card>

    <CreateParameter ref="createParameterRef" @submit="submit"></CreateParameter>
  </div>
</template>

<script setup lang="ts">
import { ref, inject } from 'vue'
import CreateParameter from './CreateParameter.vue'
import { ElMessage } from 'element-plus'
import { computed } from 'vue'
const props = defineProps<{
  parameters: Array<any>
}>()

const createParameterRef = ref<InstanceType<typeof CreateParameter>>()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const options = getNodeFieldOptions()
console.log('ssss', options)
const emit = defineEmits(['update:parameters'])
const data = computed({
  get: () => {
    if (!props.parameters) {
      emit('update:parameters', [])
    }
    return props.parameters
  },
  set: (e) => {
    emit('update:parameters', e)
  }
})
const open = (row?: any) => {
  createParameterRef.value?.open(row)
}
const submit = (event: any) => {
  if (event.edit) {
    for (const index in data.value) {
      if (data.value[index].field == event.row.field) {
        data.value[index] = event.row
        break
      }
    }
  } else {
    if (data.value.some((row: any) => row.field == event.row.field)) {
      ElMessage.warning('字段已存在')
      return
    }
    data.value.push(event.row)
  }
  createParameterRef.value?.close()
}

const deleteParameter = (row: any) => {
  data.value = data.value.filter((item: any) => item.field != row.field)
}
const parameterFormRef = ref()
const validate = () => {
  return parameterFormRef.value.validate()
}
defineExpose({ validate })
</script>
<style lang="scss" scoped></style>
