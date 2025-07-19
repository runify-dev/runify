<template>
    <SimpleNodeContainer :model="model" :validate="validate">
        <el-form ref="formRef" label-position="top" :model="form" label-width="auto" style="max-width: 600px">
            <el-form-item :rules="[
                { required: true, message: '请选择模型', trigger: 'change' },

            ]" label="模型" prop="modelId">
                <el-select v-model="form.modelId" placeholder="Select" style="width: 240px">
                    <el-option v-for="item in model_list" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
            </el-form-item>
            <el-form-item label="系统提示词" prop="system">
                <MdInput title="系统提示词" v-model="form.system" style="height: 100px" />
            </el-form-item>
            <el-form-item label="提示词" prop="user" :rules="[
                { required: true, message: '请选择输出用户提示词', trigger: 'blur' },

            ]">
                <MdInput title="用户提示词" v-model="form.user" style="height: 100px" />
            </el-form-item>
            <el-form-item label="上下文次数" prop="contextNumber">
                <el-input-number style="width: 100%" v-model="form.contextNumber" :min="0" :value-on-clear="0"
                    controls-position="right" class="w-full" :step="1" :step-strictly="true" />
            </el-form-item>
        </el-form>
    </SimpleNodeContainer>

</template>
<script setup lang="ts">
import MdInput from '@/components/md/MdInput.vue';
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue';
import type { BaseNodeModel } from '@logicflow/core';
import type { FormInstance } from 'element-plus'
import { inject, ref } from "vue"
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const form = ref({
    modelId: "",
    user: "",
    system: "",
    contextNumber: 0
})
const model_list = [
    {
        'id': 'xx',
        'name': "测试",

    }
]
const formRef = ref<FormInstance>()
const validate = () => {
    if (formRef.value) {
        return formRef.value?.validate()
    }
    return Promise.reject(false)
}

</script>
<style lang="scss" scoped>
:deep(.el-form-item__content) {
    width: 100%
}
</style>