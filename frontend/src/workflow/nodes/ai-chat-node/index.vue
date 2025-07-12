<template>
    <SimpleNodeContainer :model="model" :close="close" :open="open" :validate="validate">
        <el-form ref="formRef" label-position="top" :model="form" label-width="auto" style="max-width: 600px">
            <el-form-item :rules="[
                { required: true, message: '请选择模型', trigger: 'change' },

            ]" label="模型" prop="model_id">
                <el-select v-model="form.model_id" placeholder="Select" style="width: 240px">
                    <el-option v-for="item in model_list" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
            </el-form-item>

        </el-form>
    </SimpleNodeContainer>

</template>
<script setup lang="ts">
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue';
import type { BaseNodeModel } from '@logicflow/core';
import type { FormInstance } from 'element-plus'
import { inject, ref } from "vue"
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const form = ref({})
const model_list = [
    {
        'id': 'xx',
        'name': "测试",

    }
]
const formRef = ref<FormInstance>()
const validate = () => {
    return formRef.value?.validate()
}
const open = () => {
    return Promise.resolve('ok')
}
const close = () => {
    return Promise.resolve("ok")
}
</script>
<style lang="scss" scoped></style>