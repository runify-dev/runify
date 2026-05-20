  <template>
    <node-view-wrapper
      as="span"
      class="variable-node inline-flex items-center rounded-md px-1.5 py-0.5 text-[12px] font-medium leading-4 cursor-default select-none"
      :class="selected ? 'ring-2 ring-primary' : ''"
      style="
        background: color-mix(in srgb, var(--p-primary-color) 12%, transparent);
        color: var(--p-primary-color);
        border: 1px solid color-mix(in srgb, var(--p-primary-color) 25%, transparent);
      "
    >
      <span class="whitespace-nowrap">【{{ decodeURIComponent(node.attrs.label) }}】</span>
    </node-view-wrapper>
  </template>

  <script setup lang="ts">
  import { NodeViewWrapper } from '@tiptap/vue-3'
  import type { NodeViewProps } from '@tiptap/vue-3'

  const props = defineProps<NodeViewProps>()
  </script>
