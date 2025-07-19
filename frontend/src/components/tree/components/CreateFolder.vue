<template>

    <el-dialog v-model="dialogVisible" title="创建文件夹" width="500" :before-close="close">
        <el-form ref="ruleFormRef" style="max-width: 600px" :model="folderForm" :rules="rules" label-width="auto">
            <el-form-item label="文件夹目录" prop="name">
                <el-input v-model="folderForm.name" />
            </el-form-item>
        </el-form>
        <template #footer>
            <div class="dialog-footer">
                <el-button @click="close">取消</el-button>
                <el-button type="primary" @click="create">
                    确定
                </el-button>
            </div>
        </template>
    </el-dialog>
</template>
<script setup lang="ts">
import { ref, reactive } from "vue"
import type { FormInstance, FormRules, } from 'element-plus'
import NodeApi from "@/api/node"
import type { ResourceType } from '@/api/type/common'
import type { CreateNodePojo } from '@/api/type/node'
const ruleFormRef = ref<FormInstance>()
const dialogVisible = ref<boolean>()
const folderId = ref<string>()
const resourceType = ref<ResourceType>('knowledge')

const folderForm = ref<CreateNodePojo>({
    name: '',
    type: 'folder'
})
const emit = defineEmits(['refresh'])
const open = (resource: ResourceType, parentId?: string) => {
    folderId.value = parentId
    resourceType.value = resource
    dialogVisible.value = true

}
const close = () => {
    folderForm.value.name = ""
    dialogVisible.value = false
}

const rules = reactive<FormRules<{ name: string }>>({
    name: [
        { required: true, message: 'Please input Activity name', trigger: 'blur' },
        { min: 3, max: 5, message: 'Length should be 3 to 5', trigger: 'blur' },
    ],
})
const create = () => {
    NodeApi.create(resourceType.value, folderId.value ? folderId.value : 'root', folderForm.value)
        .then((ok: any) => {
            emit('refresh', ok.data)
            close()
        })
}

defineExpose({ open, close })
</script>
<style lang="scss" scoped></style>