<template>
    <div
        class="w-full flex items-center gap-x-4 rounded-xl bg-white shadow-lg outline outline-black/5 dark:bg-slate-800 dark:shadow-none dark:-outline-offset-1 dark:outline-white/10">
        <span>{{ t('application.debug') }}</span>
        <div class="flex-auto"></div>
        <el-button type="primary" text bg @click="edit">{{ t('common.save') }} </el-button>
    </div>
    <Workflow ref="workflowRef"></Workflow>
</template>
<script setup lang="ts">

import { onMounted, ref } from 'vue';
import Workflow from '@/workflow/index.vue';
import { t } from '@/locales';
const edit = () => {

}
const workflowRef = ref<InstanceType<typeof Workflow>>()

onMounted(() => {
    workflowRef.value?.render({
        nodes:
            Array.from({ length: 1 }).map(item => ({
                type: 'start-node',
                text: "",
                x: 0,
                y: 0,
                label: t('application.editNode.startNode'),
                properties: {
                    width: 250,
                    height: 50,
                    name: t('application.editNode.startNode'),
                    isHovered: false,
                    field_list: [
                        {
                            label: t('application.editNode.userQuestion'),
                            value: 'question'
                        }
                    ]
                }
            }))

        ,
        edges: []
    })

})
</script>
<style lang="scss" scoped></style>