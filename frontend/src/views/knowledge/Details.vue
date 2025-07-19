<template>
    <div class="preview-contaner p-10 pt-5">
        <div
            class="w-full flex items-center gap-x-4 rounded-xl bg-white p-4 mb-5 shadow-lg outline outline-black/5 dark:bg-slate-800 dark:shadow-none dark:-outline-offset-1 dark:outline-white/10">
            <span>{{ knowledge.name }}</span>
            <div class="flex-auto"></div>
            <el-button type="primary" text bg @click="goEdit">编辑 </el-button>
        </div>
        <div class="preview" style="height: 100%;overflow: auto;">
            <MdPreview style="height: auto;padding: 16px;box-sizing: border-box ;
        box-sizing: border-box;" :modelValue="knowledge.content" previewTheme="default">
            </MdPreview>
        </div>
    </div>
</template>
<script setup lang="ts">
import type { ResourceType } from '@/api/type/common';
import { MdPreview } from 'md-editor-v3';
defineProps<{ resource: ResourceType }>()
import { useRoute, useRouter } from 'vue-router';
const route = useRoute()
const router = useRouter()
import { computed, onMounted, ref, watch } from "vue"
import NodeApi from "@/api/node"
const knowledge = ref<any>({})
const folderId = computed(() => {
    const {
        params: { folderId },
    } = route as any
    return folderId
})
const goEdit = () => {
    router.push({ name: "knowledgeEdit", params: { folderId: folderId.value, id: resourceId.value } })
}
const resourceId = computed(() => {
    const {
        params: { id },
    } = route as any
    return id
})

const get = () => {
    NodeApi.resourceInfo('knowledge', folderId.value, resourceId.value).then(ok => {
        knowledge.value = ok.data
        console.log(ok.data)
    })
}
watch(resourceId, () => {
    get()
})
onMounted(() => {
    get()
})

</script>
<style lang="scss" scoped></style>