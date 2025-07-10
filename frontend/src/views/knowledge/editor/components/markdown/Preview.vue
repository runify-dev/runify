<template>
    <div class="preview-contaner">
        <span @click="change_edit">
            <AppIcon name="Edit"></AppIcon>
        </span>
        <div class="preview" style="height: 100%;overflow: auto;">
            <MdPreview style="height: auto;padding: 16px;box-sizing: border-box ;
        box-sizing: border-box;" :modelValue="text" :previewTheme="theme">
            </MdPreview>
        </div>
    </div>

</template>
<script setup lang="ts">
import { MdPreview } from 'md-editor-v3';
import AppIcon from '@/components/icons/AppIcon.vue';
defineProps<{ text: string, theme: string }>()
const emit = defineEmits(['change-operate'])
const change_edit = () => {
    emit('change-operate', 'editor')
}

</script>
<style lang="scss" scoped>
.preview-contaner {
    position: relative;
    width: 100%;
    height: 100%;

    span {
        height: 14px;
        width: 14px;
        display: inline-block;
        position: absolute;
        right: 20px;
        top: 10px;
        z-index: 999;

        &:hover {
            background-color: var(--el-color-primary-light-9);
            cursor: pointer;
        }
    }
}
</style>