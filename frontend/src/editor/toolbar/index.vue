<template>
  <div role="toolbar" aria-label="toolbar" class="tiptap-toolbar" data-variant="fixed">
    <div style="flex: 1"></div>
    <Undo :editor="editor"></Undo>
    <Redo :editor="editor"></Redo>
    <Separatror></Separatror>
    <Heading :editor="editor"></Heading>
    <List :editor="editor"></List>
    <Blockquote :editor="editor"></Blockquote>
    <CodeBlock :editor="editor"></CodeBlock>
    <Separatror></Separatror>
    <Blod :editor="editor"></Blod>
    <Italic :editor="editor"></Italic>
    <Code :editor="editor"></Code>
    <MathematicsBlock :editor="editor"></MathematicsBlock>
    <Table :editor="editor"></Table>
    <Separatror></Separatror>
    <Image :editor="editor"></Image>
    <Audio :editor="editor"></Audio>
    <Video :editor="editor"></Video>
    <File :editor="editor"></File>
    <div style="flex: 1"></div>
  </div>
</template>
<script setup lang="ts">
import { type Editor } from '@tiptap/vue-3'
import Undo from './operate/undo.vue'
import Redo from './operate/redo.vue'
import Separatror from './operate/separator.vue'
import Heading from './operate/heading/index.vue'
import List from './operate/list/index.vue'
import Blockquote from './operate/blockquote.vue'
import CodeBlock from './operate/CodeBlock.vue'
import Blod from './operate/Blod.vue'
import Italic from './operate/Italic.vue'
import Code from './operate/Code.vue'
import MathematicsBlock from './operate/mathematics-block/index.vue'
import Table from './operate/table/index.vue'
import Image from './operate/image/index.vue'
import Audio from './operate/audio/index.vue'
import Video from './operate/video/index.vue'
import File from './operate/file/index.vue'
import './style/index.scss'

const props = defineProps<{ editor: Editor }>()
</script>
<style lang="scss"></style>
