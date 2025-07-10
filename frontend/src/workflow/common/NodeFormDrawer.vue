<template>
    <el-drawer v-model="drawer" direction="rtl" append-to-body>
        <template #header>
            <h4 class="font-medium">{{ name }}</h4>
        </template>
        <template #default>
            <h5 class="relative
                before:block 
                before:absolute before:translate-x-[-50%] 
                before:translate-y-[-50%] 
                before:top-1/2
                before:left-[2px]
                before:w-[2px] before:h-[80%] before:bg-[var(--el-color-primary)] pl-3
                font-medium">
                节点输出</h5>
            <div class="flex p-4 rounded-xs h-[20px] bg-gray-100 font-normal text-[14px] items-center justify-between"
                v-for="field in fieldList" :key="field.value">
                <span>{{ `${field.label}\{\{ ${field.value} \}\}` }} </span>
                <div @click="copy(`{{${name}.${field.value}}}`)"
                    class="w-5 h-5 hover:bg-gray-100 hover:text-gray-900 hover:cursor-pointer flex rounded-xs justify-center items-center">
                    <app-icon name="CopyDocument"></app-icon>
                </div>

            </div>
        </template>
        <template #footer>
            <div style="flex: auto">
                <el-button @click="close">取消</el-button>
                <el-button type="primary" @click="confirm">确认</el-button>
            </div>
        </template>
    </el-drawer>
</template>
<script setup lang="ts">
import { ref } from "vue"
import type { BaseNodeModel } from '@logicflow/core';
import type { Field } from '@/workflow/common/type';
import AppIcon from '@/components/icons/AppIcon.vue';
import Clipboard from 'vue-clipboard3'
import { ElMessage } from 'element-plus';
const props = defineProps<{
    updateFormData: (formData: any) => void
}>()
const drawer = ref<boolean>(false)
const form_data = ref<any>({});
const fieldList = ref<Array<Field>>([])
const name = ref<string>('');


const copy = (text: string) => {
    const { toClipboard } = Clipboard()
    toClipboard(text).then(() => {
        ElMessage.success({ message: '复制成功' })
    }).catch(() => {
        ElMessage.error({ message: "复制失败" })
    })
}
const confirm = () => {
    props.updateFormData(form_data)
    close()
}
const close = () => {
    drawer.value = false
}

const open = (model: BaseNodeModel) => {
    form_data.value = model.properties.form_data
    name.value = model.properties.name
    fieldList.value = model.properties.field_list
    drawer.value = true
    return Promise.resolve("ok")
}
defineExpose({ open, close })
</script>
<style lang="scss" scoped></style>