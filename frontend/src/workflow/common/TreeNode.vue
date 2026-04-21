<template>
  <div>
    <div
      class="flex items-center justify-between px-2 py-1.5 rounded hover:bg-gray-100"
      :style="{ paddingLeft: `${depth * 16 + 8}px` }"
    >
      <div
        class="flex items-center gap-1.5 flex-1 min-w-0 cursor-pointer"
        @click="hasChildren && (open = !open)"
      >
        <span
          v-if="hasChildren"
          class="text-[10px] text-gray-400 transition-transform flex-shrink-0"
          :class="open ? 'rotate-90' : ''"
          >▶</span
        >
        <span v-else class="w-3 text-[10px] text-gray-300 flex-shrink-0">─</span>
        <span class="text-sm flex-shrink-0">{{ node?.label }}</span>
        <span class="text-[11px] text-gray-400 truncate">
          {{ [name, ...currentPath].join('.') }}
        </span>
      </div>

      <!-- 父节点和子节点都显示复制按钮 -->
      <Button
        v-tooltip="'复制'"
        icon="pi pi-copy"
        variant="text"
        severity="secondary"
        size="small"
        @click.stop="handleCopy"
      />
    </div>

    <template v-if="hasChildren && open">
      <TreeNode
        v-for="child in node?.children"
        :key="child.value"
        :node="child"
        :name="name"
        :parent-path="currentPath"
        :depth="depth + 1"
        @copy="$emit('copy', $event)"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps({
  node: Object,
  name: String,
  parentPath: { type: Array, default: () => [] }, // 父级路径
  depth: { type: Number, default: 0 }
})

const emit = defineEmits(['copy'])
const open = ref(true)
const hasChildren = computed(() => props.node?.children?.length > 0)

// 当前节点完整路径
const currentPath = computed(() => [...props.parentPath, props.node?.value])

function handleCopy() {
  // 复制格式: ${name.user.name}
  const fullPath = [props.name, ...currentPath.value].join('.')
  emit('copy', `\${${fullPath}}`)
}
</script>
