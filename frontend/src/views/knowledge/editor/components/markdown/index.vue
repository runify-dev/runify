<template>
    <Preview style="height: 100%;" :text="node ? node.content : ''" :theme="theme" v-if="operate == 'preview'"
        @change-operate="changeOperate">
    </Preview>
    <Editor :node="node" :theme="theme" style="height: 100%;" v-else-if="node" v-model="node.content"
        @change-operate="changeOperate">
    </Editor>
</template>
<script setup lang="ts">
import { onMounted, ref, watch } from "vue"
import Preview from './Preview.vue';
import Editor from './Editor.vue';
import { type MarkdownNode } from "@/api/type/knowledge"
import KnowledgeApi from "@/api/knowledge"
const props = defineProps<{ id: string }>()
const operate = ref<'preview' | 'editor'>('preview')
const theme = ref<string>('default');
const changeOperate = (operate_name: 'preview' | 'editor') => {
    operate.value = operate_name
}
const node = ref<MarkdownNode>()
watch(() => props.id, () => {
    get()
})
const get = () => {
    KnowledgeApi.oneMarkdown(props.id).then(ok => {
        node.value = ok.data
    })
}
onMounted(() => {
    get()
})
</script>
<style lang="scss"></style>