<template>
  <div class="simple-editor-content">
    <editor-content :editor="editor" />
  </div>
</template>
<script setup lang="ts">
import './nodes/index.scss'
import { EditorContent, Editor } from '@tiptap/vue-3'
import {
  StarterKit,
  CodeBlockLowlight,
  HorizontalRule,
  TextAlign,
  TaskList,
  TaskItem,
  Highlight,
  Typography,
  Image,
  Superscript,
  Subscript,
  Selection,
  Markdown,
  Mathematics,
  TableKit
} from './nodes/index'

const editor = new Editor({
  editorProps: {
    attributes: {
      'aria-label': 'Main content area, start typing to enter text.',
      class: 'simple-editor'
    }
  },
  extensions: [
    CodeBlockLowlight,
    StarterKit.configure({
      codeBlock: false
    }),
    HorizontalRule,
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
    TaskList,
    TaskItem.configure({ nested: true }),
    Highlight.configure({ multicolor: true }),
    Image,
    Typography,
    Superscript,
    Subscript,
    Selection,
    Markdown,
    TableKit,
    Mathematics.configure({
      inlineOptions: {
        // optional options for the inline math node
      },
      blockOptions: {
        onClick: (node: any, pos: number) => {
          console.log('sss')
          const newCalculation = prompt('Enter new calculation:', node.attrs.latex)
          if (newCalculation) {
            editor
              .chain()
              .setNodeSelection(pos)
              .updateBlockMath({ latex: newCalculation })
              .focus()
              .run()
          }
        }
      },
      katexOptions: {
        // optional options for the KaTeX renderer
      }
    })
  ],

  content: `# Hello World\n\nStart typing...\nH~2~O and E = mc^2^ \n$$
\sub(3*5=15)
$$


|你好|sss|
|-|-|
|ssss|ssss|

\`\`\`javascript
package org.jadestudio.message.impl.impl;

import org.jadestudio.message.struct.chunk.TextContentChunk;

public class TextContentChunkImpl {
    private TextContentChunk textContentChunk;

    public TextContentChunkImpl(TextContentChunk textContentChunk) {
        this.textContentChunk = textContentChunk;
    }

}
\`\`\`
`,
  contentType: 'markdown' // parse initial content as Markdown
})
</script>
<style lang="scss"></style>
