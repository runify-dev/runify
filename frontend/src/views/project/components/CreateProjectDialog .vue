<template>
  <el-dialog v-model="visible" title="创建项目" width="500" height="100%" :before-close="close">
    <el-form
      ref="createProjectFormRef"
      style="max-width: 600px"
      :model="project"
      label-width="auto"
      label-position="top"
      :rules="rules"
    >
      <el-form-item label="项目名称" prop="name">
        <el-input v-model="project.name" placeholder="请输入项目名称" />
      </el-form-item>
      <el-form-item label="项目描述" prop="desc">
        <el-input v-model="project.desc" placeholder="请输入项目描述" />
      </el-form-item>
      <el-form-item label="项目路径" prop="path">
        <el-input v-model="project.path" placeholder="请输入项目器" />
      </el-form-item>
      <el-form-item label="项目icon" prop="icon">
        <el-upload
          class="avatar-uploader"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="uploadIcon"
        >
          <el-avatar
            shape="square"
            :size="100"
            fit="fill"
            v-if="project.icon"
            :src="project.icon"
          />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="submit"> 创建 </el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import type { CreateProjectVO } from '@/api/type/project'
import fileAPI from '@/api/file'
import { TreeCommonAPI } from '@/api/tree'
import { useRouter } from 'vue-router'
const router = useRouter()
const go = (id: string, data?: any) => {
  if (['star', 'share', 'root'].includes(id)) {
    router.push({ name: 'projectFolders', params: { id: id } })
    return
  }
  if (data) {
    if (data.type == 'folder') {
      router.push({ name: 'projectFolders', params: { id: id } })
    } else {
      router.push({ name: 'projectDetails', params: { id: id } })
    }
  }
}
const treeCommonAPI = new TreeCommonAPI('project')
const defaultValue = {
  name: '',
  desc: '',
  path: '',
  icon: ''
}
const project = ref<CreateProjectVO>({ ...defaultValue })
const validatePath = (rule: any, value: any, callback: any) => {
  if (!value) {
    return callback(new Error('路径不能为空'))
  }
  if (!value.startsWith('/')) {
    return callback(new Error('路径必须以/开头'))
  }
  // 可选：添加更多路径格式校验
  if (!/^\/[a-zA-Z0-9_\-/]+$/.test(value)) {
    return callback(new Error('路径格式不正确'))
  }
  callback()
}
const rules = reactive<FormRules<CreateProjectVO>>({
  name: [{ required: true, message: '项目名称必填', trigger: 'blur' }],
  desc: [{ required: true, message: '项目描述必填', trigger: 'blur' }],
  path: [{ validator: validatePath, required: true, trigger: 'blur' }],
  icon: [{ required: true, message: '项目图像必填', trigger: 'blur' }]
})

const uploadIcon = (uploadFile: UploadFile) => {
  if (uploadFile.raw) {
    const fd = new FormData()
    fd.append('file', uploadFile.raw)
    fileAPI.uploadFile(fd).then((ok) => {
      project.value.icon = `./api/storage/file/${ok.data.id}`
    })
  }
}
const createProjectFormRef = ref<FormInstance>()
const visible = ref<boolean>()
const submit = () => {
  createProjectFormRef.value?.validate().then(() => {
    treeCommonAPI.createResource(current.value.data.id, project.value).then((ok) => {
      if (current.value.data.id === 'root') {
        currentData.value.push({ ...ok.data, type: 'project' })
        go(ok.data.id, { ...ok.data, type: 'project' })
      } else {
        current.value.node.insertAfter(
          { data: { ...ok.data, type: 'project' } },
          current.value.node
        )
        go(ok.data.id, { ...ok.data, type: 'project' })
      }
      close()
    })
  })
}
const current = ref<any>()
const currentData = ref<any>()
const open = (event: any, data: any) => {
  current.value = event
  currentData.value = data
  visible.value = true
}
const close = () => {
  current.value = undefined
  currentData.value = undefined
  project.value = { ...defaultValue }
  visible.value = false
}
defineExpose({ open, close })
</script>
<style lang="scss" scoped>
.avatar-uploader {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
}
</style>
