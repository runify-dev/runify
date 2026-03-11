<template>
  <div>
    <button @click="toggle" class="tiptap-button" type="button" data-style="ghost" role="button">
      <span class="pi pi-volume-up"></span>
    </button>

    <!-- Panel -->
    <OverlayPanel ref="panel">
      <div class="flex flex-col gap-3 w-64">
        <!-- 上传 -->
        <FileUpload
          mode="basic"
          chooseLabel="从本地上传"
          accept="audio/*"
          customUpload
          :auto="true"
          @uploader="onUpload"
        />

        <!-- URL -->
        <div class="flex flex-col gap-2">
          <InputText
            v-model="urlDraft"
            placeholder="https://example.com/audio.mp3"
            @keydown.enter="insertFromUrl"
          />

          <Button label="插入" size="small" :disabled="!urlDraft" @click="insertFromUrl" />
        </div>
      </div>
    </OverlayPanel>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import type { Editor } from '@tiptap/core'
import FileAPI from '@/api/file'
const props = defineProps<{ editor: Editor }>()

const panel = ref()
const urlDraft = ref('')

function toggle(event: Event) {
  panel.value.toggle(event)
}

function insertFromUrl() {
  const url = urlDraft.value.trim()
  if (!url) return

  props.editor
    .chain()
    .focus()
    .setAudioBlock({
      src: url,
      title: ''
    })
    .run()

  urlDraft.value = ''
  panel.value.hide()
}

function onUpload(e: any) {
  const files = e.files

  files.forEach((file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return FileAPI.uploadFile(fd).then((ok) => {
      props.editor
        .chain()
        .focus()
        .setAudioBlock({ src: `./api/storage/file/${ok.data.id}` })
        .run()
    })
  })

  panel.value.hide()
}
</script>
<style>
@keyframes eqBounce {
  0% {
    height: 3px;
  }
  100% {
    height: 11px;
  }
}
</style>
