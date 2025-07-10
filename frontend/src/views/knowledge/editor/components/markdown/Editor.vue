<template>
    <MdEditor style="height: 100%;width: 100%;" :editorId="state.id" @onUploadImg="onUploadImg"
        :onGetCatalog="onGetCatalog" ref="mdEditorRef" v-model="text" :toolbars="toolbars" :previewTheme="theme">
        <template #defToolbars>
            <NormalToolbar title="保存" @on-click="customHandler">
                <template #trigger>
                    <AppIcon style="height: 24px;width: 24px;" name="app-save"></AppIcon>
                </template>
            </NormalToolbar>
        </template>
    </MdEditor>

</template>
<script setup lang="ts">
import { MdEditor, NormalToolbar, type ToolbarNames, type HeadList } from 'md-editor-v3';
import { computed, ref, reactive } from 'vue';
import AppIcon from '@/components/icons/AppIcon.vue';
import { type MarkdownNode } from "@/api/type/knowledge"
import FileAPI from "@/api/file"
import MarkdownAPI from "@/api/knowledge/markdown"
const mdEditorRef = ref<InstanceType<typeof MdEditor>>();
const props = defineProps<{ node: MarkdownNode, modelValue: string, theme: string }>()
console.log(MdEditor)
const onGetCatalog = (list: HeadList[]) => {
    console.log(list)
}
const state = reactive({
    theme: 'dark',
    text: '标题',
    id: 'my-editor',
});


const emit = defineEmits(['update:modelValue', 'change-operate'])
const text = computed({
    get: () => {
        return props.modelValue
    },
    set: (value: string) => {
        emit('update:modelValue', value)
    }
})
const onUploadImg = (files: any, callback: any) => {

    const uploadFileList = files.map((file: any) => {
        const fd = new FormData()
        fd.append('file', file)
        return FileAPI
            .uploadFile(fd).then(ok => ok.data.id)
    })
    Promise.all(uploadFileList).then(id_list => {
        callback(id_list)
    })

}

const customHandler = () => {
    update_note().then(() => {
        emit('change-operate', 'preview')
    })

}
// 工具栏配置
const toolbars = ref<ToolbarNames[]>([
    'bold', 'underline', 'italic', 'strikeThrough', '-', 'title', 'sub',
    'sup', 'quote', 'unorderedList', 'orderedList', 'task', 'codeRow', 'code', '-', 'link',
    'image', 'table', 'mermaid', 'katex', '-', 'prettier', 'pageFullscreen',
    'catalog', 'preview', 'previewOnly', '=', 0, 1, 2,
]);
const update_note = () => {
    return MarkdownAPI.edit(props.node.id, text.value)
}
</script>