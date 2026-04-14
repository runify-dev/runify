<template>
  <Dialog v-model:visible="visible" modal header="创建项目" :style="{ width: '25rem' }">
    <Form ref="formRef" :initial-values="{ name: '' }" :resolver="resolver" @submit="submit">
      <FormField v-slot="$field" name="name" class="mt-2">
        <label>项目名称</label>
        <InputText class="mt-2" type="text" :placeholder="`请输入项目名称`" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
      <FormField v-slot="$field" name="desc" class="mt-2">
        <label>项目描述</label>
        <InputText class="mt-2" type="text" :placeholder="`请输入项目描述`" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
      <FormField v-slot="$field" name="path" class="mt-2">
        <label>项目路径</label>
        <InputText type="text" class="mt-2" :placeholder="`请输入项目前缀路径`" fluid />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>

      <FormField v-slot="$field: any" name="icon" class="mt-2">
        <div class="flex flex-col gap-2">
          <label>项目icon</label>
          <div class="icon-upload">
            <!-- 未上传时显示上传按钮 -->
            <FileUpload
              v-if="!$field.value"
              ref="fileupload"
              mode="basic"
              name="file"
              accept="image/*"
              :fileLimit="1"
              :multiple="false"
              :maxFileSize="9999999"
              :auto="true"
              chooseLabel="上传图标"
              @select="uploadIcon"
            />

            <!-- 已上传时显示图片 -->
            <div v-else class="preview-wrapper">
              <Image :src="$field.value" alt="icon" width="200" preview />

              <Button
                icon="pi pi-times"
                severity="danger"
                rounded
                text
                class="delete-btn"
                @click="
                  () => {
                    $field.onChange({ value: '' })
                  }
                "
              />
            </div>
          </div>
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
            $field.error?.message
          }}</Message>
        </div>
      </FormField>
    </Form>
    <template #footer>
      <Button type="button" label="取消" severity="secondary" @click="close"></Button>
      <Button type="button" label="创建" @click="submit"></Button
    ></template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import fileAPI from '@/api/file'
import { TreeCommonAPI } from '@/api/tree'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import type { TreeNode } from 'primevue/treenode'
import type { FileUploadSelectEvent } from 'primevue/fileupload'
import type { FormInstance } from '@primevue/forms'
const emit = defineEmits(['create:resource:success'])
const resolver = ref(
  zodResolver(
    z.object({
      name: z.string().min(1, { message: `项目名称必填` }),
      path: z
        .string()
        .startsWith('/', { message: '必须已/开头' })
        .regex(/^\/[a-zA-Z0-9_\-]+$/, { message: '必须字母或者数字组合' }),
      desc: z.string(),
      icon: z.string().min(1, { message: `图标必选` })
    })
  )
)
const treeCommonAPI = new TreeCommonAPI('project')

const formRef = ref<FormInstance>()

const uploadIcon = (uploadFile: FileUploadSelectEvent) => {
  if (uploadFile.files) {
    let file
    if (Array.isArray(uploadFile.files)) {
      file = uploadFile.files[0]
    } else {
      file = uploadFile.files
    }
    const fd = new FormData()
    fd.append('file', file)
    fileAPI.uploadFile(fd).then((ok) => {
      formRef.value?.setFieldValue('icon', `./api/storage/file/${ok.data.id}`)
    })
  }
}
const visible = ref<boolean>()
const submit = () => {
  formRef.value?.validate().then(({ errors, values }) => {
    if (Object.keys(errors).length == 0) {
      treeCommonAPI
        .createResource(current.value ? current.value.key : 'root', values)
        .then((ok) => {
          emit('create:resource:success', current.value ? current.value.key : undefined, ok.data)
          close()
        })
    }
  })
}
const current = ref<TreeNode>()
const open = (node: any) => {
  current.value = node
  visible.value = true
}
const close = () => {
  current.value = undefined
  visible.value = false
}
defineExpose({ open, close })
</script>
<style lang="scss" scoped>
.icon-upload {
  display: inline-flex;
  align-items: center;
}

.preview-wrapper {
  position: relative;
  display: inline-block;
}

.delete-btn {
  position: absolute;
  top: -10px;
  right: -10px;
}
</style>
