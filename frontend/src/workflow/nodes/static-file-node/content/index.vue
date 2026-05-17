<template>
  <div>
    <Fieldset legend="基本信息">
      <!-- ZIP文件上传 -->
      <div class="mb-3">
        <label class="mb-2 block">上传ZIP文件</label>
        <div v-if="formData.fileId" class="flex items-center gap-2 p-2 border rounded bg-surface-50">
          <i class="pi pi-file-archive text-primary"></i>
          <span class="text-sm flex-1 truncate">{{ formData.fileName || '已上传文件' }}</span>
          <Button icon="pi pi-times" size="small" severity="danger" text @click="removeFile" />
        </div>
        <div v-else>
          <FileUpload
            mode="basic"
            accept=".zip"
            :max-file-size="100 * 1024 * 1024"
            :auto="true"
            :custom-upload="true"
            @uploader="onUpload"
            :loading="uploading"
            choose-label="选择ZIP文件"
            class="w-full"
          />
          <small class="text-muted-color">支持 .zip 格式，最大 100MB</small>
        </div>
        <Message v-if="errors.file" severity="error" size="small" variant="simple">
          {{ errors.file }}
        </Message>
      </div>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { reactive, inject, onMounted, ref } from 'vue'
import FileUpload from 'primevue/fileupload'
import type { BaseNodeModel } from '@logicflow/core'
import { cloneDeep } from 'lodash'
import fileApi from '@/api/file'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()

const uploading = ref(false)

const formData = reactive({
  fileId: '',
  fileName: ''
})

const errors = reactive<Record<string, string>>({})

const onUpload = async (event: any) => {
  const file = event.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    const formDataObj = new FormData()
    formDataObj.append('file', file)
    const res = await fileApi.uploadFile(formDataObj)
    if (res.data) {
      formData.fileId = res.data.id
      formData.fileName = res.data.fileName || file.name
    }
  } catch (e: any) {
    errors.file = '上传失败: ' + (e.message || '未知错误')
  } finally {
    uploading.value = false
  }
}

const removeFile = () => {
  formData.fileId = ''
  formData.fileName = ''
}

const validate = () => {
  Object.keys(errors).forEach((k) => delete errors[k])

  if (!formData.fileId) {
    errors.file = '请上传ZIP文件'
  }

  const valid = Object.keys(errors).length === 0
  const values = cloneDeep({ ...formData })
  return Promise.resolve({ values, errors: valid ? {} : errors })
}

const submit = () => {
  return validate().then(({ values, errors: errs }) => {
    if (Object.keys(errs).length === 0) {
      model.properties.nodeData = values
      return Promise.resolve(values)
    }
    return Promise.resolve(errs)
  })
}

defineExpose({ validate, submit })

onMounted(() => {
  if (model.properties.nodeData) {
    const data = cloneDeep(model.properties.nodeData)
    Object.assign(formData, {
      fileId: data.fileId || '',
      fileName: data.fileName || ''
    })
  } else {
    model.properties.nodeData = {
      fileId: '',
      fileName: ''
    }
  }
  model.properties.field_list = [
    { label: '访问地址', value: 'url' },
    { label: '部署ID', value: 'deployId' }
  ]
})
</script>

<style lang="scss" scoped></style>
