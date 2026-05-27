<template>
  <div class="editor-root flex h-full min-h-0 min-w-0 w-full flex-col">
    <div
      class="flex h-full min-h-0 min-w-0 w-full flex-col overflow-hidden rounded-lg border border-[var(--p-content-border-color)] bg-[var(--p-content-background)] shadow-sm"
    >
      <div class="relative flex min-h-0 min-w-0 w-full flex-1 flex-col" @click.self="focusEditor">
        <EditorContent
          :editor="editor"
          class="flex min-h-0 min-w-0 flex-1 flex-col overflow-auto"
        />
        <div class="pointer-events-none absolute bottom-2 right-2 z-10">
          <AppIcon
            class="pointer-events-auto cursor-pointer text-base text-[color:var(--p-text-muted-color)] hover:opacity-70"
            name="app-magnify"
            @click.stop="openDialog"
          />
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div
        v-if="pop.show"
        ref="popEl"
        class="fixed z-[9999] overflow-hidden rounded-xl border border-slate-200 bg-white shadow-[0_8px_24px_rgba(0,0,0,0.12)]"
        :style="{
          left: `${pop.x}px`,
          top: `${pop.y}px`,
          width: `${pop.width}px`
        }"
      >
        <div class="flex items-center gap-1.5 border-b border-slate-200 bg-slate-50 px-2 py-1.5">
          <i class="pi pi-search shrink-0 text-[11px] text-slate-400" />
          <input
            ref="popInputRef"
            v-model="pop.query"
            class="min-w-0 flex-1 border-none bg-transparent text-[12px] leading-5 text-slate-800 outline-none placeholder:text-slate-400"
            placeholder="搜索"
            @keydown="onPopKeydown"
          />
        </div>

        <div class="var-picker-tree overflow-y-auto p-1" style="height: 200px">
          <Tree
            v-if="filteredTreeNodes.length"
            v-model:expandedKeys="expandedKeys"
            :value="filteredTreeNodes"
          >
            <template #default="{ node }">
              <div
                class="flex min-w-0 items-center gap-1 rounded-md px-1.5 py-1 transition-colors"
                :class="selectedNodeKey === node.key ? 'bg-slate-100' : 'hover:bg-slate-50'"
                @click.stop="handleNodeClick(node)"
                @dblclick.stop="handleNodeDblClick(node)"
              >
                <div class="min-w-0 flex-1">
                  <div
                    class="truncate text-[12px] leading-4"
                    :class="node.data?.disabled ? 'text-slate-400' : 'font-medium text-slate-700'"
                  >
                    {{ node.label }}
                  </div>
                  <div
                    v-if="node.data?.fullValue"
                    class="mt-0.5 truncate text-[10px] leading-4 text-slate-400"
                  >
                    {{ node.data.fullValue }}
                  </div>
                </div>
              </div>
            </template>
          </Tree>

          <div v-else class="px-2 py-3 text-center text-[12px] text-slate-400">无匹配变量</div>
        </div>
      </div>
    </Teleport>

    <Dialog
      v-model:visible="dialogVisible"
      :header="title"
      :style="{ width: '90vw', height: '80vh' }"
      :modal="false"
      :draggable="false"
      append-to-body
      class="[&_.p-dialog]:flex [&_.p-dialog]:flex-col [&_.p-dialog-content]:flex [&_.p-dialog-content]:min-h-0 [&_.p-dialog-content]:flex-1 [&_.p-dialog-content]:flex-col [&_.p-dialog-content]:overflow-hidden [&_.p-dialog-content]:p-3"
      @show="onDialogShow"
      @hide="onDialogClose"
    >
      <EditorContent
        :editor="dialogEditorInstance"
        class="flex min-h-0 flex-1 flex-col overflow-auto"
      />
      <template #footer>
        <div class="flex justify-end gap-2">
          <Button label="取消" severity="secondary" @click="cancelDialog" />
          <Button label="提交" @click="submitDialog" />
        </div>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch, shallowRef } from 'vue'
import { Editor, EditorContent } from '@tiptap/vue-3'
import { StarterKit } from '@tiptap/starter-kit'
import { Markdown } from '@tiptap/markdown'
import AppIcon from '@/components/icons/AppIcon.vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Tree from 'primevue/tree'
import type { TreeNode } from 'primevue/treenode'
import { Variable } from '@/editor/editor/nodes/variable-node'

defineOptions({ name: 'TemplateEditor' })

export interface VariableItem {
  label: string
  value: string
  disabled?: boolean
  children?: VariableItem[]
}

interface VariableTreeNode extends TreeNode {
  key: string
  label: string
  data: {
    value: string
    label: string
    disabled?: boolean
    fullValue: string
    pathValues: string[]
  }
  children?: VariableTreeNode[]
  leaf?: boolean
}

const props = defineProps<{
  title: string
  modelValue: string
  variables?: VariableItem[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  submitDialog: [value: string]
}>()

const popEl = ref<HTMLElement | null>(null)
const popInputRef = ref<HTMLInputElement | null>(null)

const editor = shallowRef<Editor>()
const dialogEditorInstance = shallowRef<Editor>()
let removePopListeners: (() => void) | null = null

const dialogVisible = ref(false)
const isSubmitting = ref(false)

const pop = reactive({
  show: false,
  x: 0,
  y: 0,
  width: 240,
  query: '',
  from: 0,
  to: 0
})

const expandedKeys = ref<Record<string, boolean>>({})
const selectedNodeKey = ref<string | null>(null)

function getMarkdown(ed: any): string {
  return (ed as any).getMarkdown?.() ?? ed.getText()
}

function createEditor(
  content: string,
  editable = true,
  onUpdate?: (text: string) => void
): Editor {
  const ed = new Editor({
    editable,
    extensions: [
      StarterKit,
      Markdown.configure({}),
      Variable,
    ],
    editorProps: {
      attributes: {
        class: 'prose prose-sm max-w-none focus:outline-none min-h-full p-3',
      },
      handlePaste(view, event) {
        const text = event.clipboardData?.getData('text/plain')?.trim()
        if (!text) return false

        const varType = view.state.schema.nodes.variable
        if (!varType) return false

        const RE = /^:::variable\s+\{value="([^"]*)"\s+label="([^"]*)"\}\s*:::$/
        const lines = text.split(/\n/).filter(l => l.trim())
        const nodes: any[] = []

        for (const line of lines) {
          const match = RE.exec(line.trim())
          if (match) {
            nodes.push(varType.create({ value: match[1], label: match[2] }))
          }
        }

        if (nodes.length === 0) return false

        const { from, to } = view.state.selection
        const tr = view.state.tr.delete(from, to)

        let pos = from
        for (const node of nodes) {
          tr.insert(pos, node)
          pos += node.nodeSize
        }

        view.dispatch(tr)
        return true
      },
    },
    onUpdate: ({ editor: updated }) => {
      if (onUpdate) onUpdate(getMarkdown(updated))
    },
    content,
    contentType: 'markdown',
  })

  return ed
}

// ── Detect `$` input ──
let lastContent = ''

function handleEditorUpdate(ed: Editor) {
  const newContent = getMarkdown(ed)

  if (!pop.show) {
    const cursor = ed.state.selection.head
    const ch = cursor > 0 ? ed.state.doc.textBetween(cursor - 1, cursor) : ''
    if (ch === '$' && newContent.length > lastContent.length) {
      openPop(cursor - 1, cursor)
    }
  } else {
    const cursor = ed.state.selection.head
    pop.to = cursor

    const before = ed.state.doc.textBetween(0, cursor)
    if (
      before.length < pop.from + 1 ||
      before[pop.from] !== '$' ||
      before.slice(pop.from + 1).includes('\n')
    ) {
      closePop()
    } else {
      requestAnimationFrame(() => updatePopPosition())
    }
  }

  lastContent = newContent
}

// ── Tree ──
function buildTreeNodes(
  list: VariableItem[] = [],
  parentKey = 'root',
  parentValues: string[] = [],
  parentLabels: string[] = []
): VariableTreeNode[] {
  return list.map((item, index) => {
    const key = `${parentKey}-${index}`
    const currentValues = item.value ? [...parentValues, item.value] : [...parentValues]
    const currentLabels = item.label ? [...parentLabels, item.label] : [...parentLabels]
    const fullValue = `context${currentValues.map((v: string) => `['${v}']`).join('')}`
    const fullLabel = currentLabels.join(' / ')
    const children = buildTreeNodes(item.children ?? [], key, currentValues, currentLabels)

    return {
      key,
      label: item.label,
      data: {
        value: item.value,
        label: fullLabel,
        disabled: item.disabled,
        fullValue,
        pathValues: currentValues
      },
      children,
      leaf: children.length === 0
    }
  })
}

const treeNodes = computed<VariableTreeNode[]>(() => buildTreeNodes(props.variables ?? []))

function filterTree(nodes: VariableTreeNode[], keyword: string): VariableTreeNode[] {
  const q = keyword.trim().toLowerCase()
  if (!q) return nodes

  const result: VariableTreeNode[] = []

  for (const node of nodes) {
    const selfMatched =
      node.label.toLowerCase().includes(q) ||
      String(node.data?.value ?? '').toLowerCase().includes(q) ||
      String(node.data?.fullValue ?? '').toLowerCase().includes(q)

    const sourceChildren = (node.children as VariableTreeNode[]) ?? []
    const filteredChildren = filterTree(sourceChildren, q)

    if (selfMatched) {
      result.push({ ...node, children: sourceChildren, expanded: true })
      continue
    }

    if (filteredChildren.length > 0) {
      result.push({ ...node, children: filteredChildren, expanded: true })
    }
  }

  return result
}

const filteredTreeNodes = computed<VariableTreeNode[]>(() => filterTree(treeNodes.value, pop.query))

function findNodeByKey(nodes: VariableTreeNode[], key: string | null): VariableTreeNode | null {
  if (!key) return null
  for (const node of nodes) {
    if (node.key === key) return node
    const found = findNodeByKey((node.children as VariableTreeNode[]) ?? [], key)
    if (found) return found
  }
  return null
}

function findFirstSelectableNode(nodes: VariableTreeNode[]): VariableTreeNode | null {
  for (const node of nodes) {
    if (!node.data?.disabled) return node
    const found = findFirstSelectableNode((node.children as VariableTreeNode[]) ?? [])
    if (found) return found
  }
  return null
}

function collectExpandedKeys(nodes: VariableTreeNode[]) {
  const nextExpanded: Record<string, boolean> = {}
  function walk(list: VariableTreeNode[]) {
    for (const node of list) {
      if (node.children?.length) {
        nextExpanded[node.key] = true
        walk(node.children as VariableTreeNode[])
      }
    }
  }
  walk(nodes)
  return nextExpanded
}

const selectedNode = computed<VariableTreeNode | null>(() =>
  findNodeByKey(filteredTreeNodes.value, selectedNodeKey.value)
)

watch(
  () => filteredTreeNodes.value,
  (nodes) => {
    if (!pop.show) return
    expandedKeys.value = collectExpandedKeys(nodes)
    const current = findNodeByKey(nodes, selectedNodeKey.value)
    if (current && !current.data?.disabled) return
    const first = findFirstSelectableNode(nodes)
    selectedNodeKey.value = first?.key ?? null
  },
  { deep: true }
)

watch(
  () => props.modelValue,
  (val) => {
    if (!editor.value) return
    const cur = getMarkdown(editor.value)
    if (val !== cur) {
      editor.value.commands.setContent(val ?? '', { contentType: 'markdown' })
      lastContent = getMarkdown(editor.value)
    }
  }
)

// ── Popover positioning ──
function clamp(n: number, min: number, max: number) {
  return Math.min(Math.max(n, min), max)
}

function getViewportSize() {
  const vv = window.visualViewport
  return {
    width: vv?.width ?? window.innerWidth,
    height: vv?.height ?? window.innerHeight
  }
}

function updatePopPosition() {
  if (!pop.show || !editor.value) return

  try {
    const coords = editor.value.view.coordsAtPos(pop.from + 1)
    if (!coords) return

    const { width: vw, height: vh } = getViewportSize()
    const margin = 8
    const width = Math.min(280, Math.max(220, vw - margin * 2))
    const popupHeight = 235

    const x = clamp(coords.left, margin, Math.max(margin, vw - width - margin))
    let y = coords.bottom + 6
    if (y + popupHeight > vh - margin) {
      y = Math.max(margin, coords.top - popupHeight - 6)
    }

    pop.width = width
    pop.x = x
    pop.y = y
  } catch {
    // coordsAtPos can throw if position is invalid
  }
}

function bindPopPositionListeners() {
  removePopListeners?.()

  const onReposition = () => {
    requestAnimationFrame(() => updatePopPosition())
  }

  window.addEventListener('resize', onReposition)
  document.addEventListener('scroll', onReposition, true)

  if (window.visualViewport) {
    window.visualViewport.addEventListener('resize', onReposition)
    window.visualViewport.addEventListener('scroll', onReposition)
  }

  removePopListeners = () => {
    window.removeEventListener('resize', onReposition)
    document.removeEventListener('scroll', onReposition, true)
    if (window.visualViewport) {
      window.visualViewport.removeEventListener('resize', onReposition)
      window.visualViewport.removeEventListener('scroll', onReposition)
    }
  }
}

function openPop(from: number, to: number) {
  pop.show = true
  pop.from = from
  pop.to = to
  pop.query = ''
  selectedNodeKey.value = null
  expandedKeys.value = {}

  bindPopPositionListeners()

  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      expandedKeys.value = collectExpandedKeys(filteredTreeNodes.value)
      const first = findFirstSelectableNode(filteredTreeNodes.value)
      selectedNodeKey.value = first?.key ?? null
      updatePopPosition()
      popInputRef.value?.focus()
    })
  })
}

function closePop() {
  pop.show = false
  pop.query = ''
  selectedNodeKey.value = null
  expandedKeys.value = {}
  removePopListeners?.()
  removePopListeners = null
}

function commitTreeNode(node?: VariableTreeNode | null) {
  const target = node ?? selectedNode.value
  if (!target || target.data?.disabled) return

  const rawValue = target.data?.pathValues?.join('.')
  const rawLabel = target.data?.label
  console.log('[commitTreeNode] target:', JSON.stringify(target.data, null, 2))
  if (!rawValue || !editor.value) return

  const view = editor.value.view
  const { state } = view

  console.log('[commitTreeNode]', { from: pop.from, to: pop.to, rawValue, rawLabel })
  console.log('[commitTreeNode] doc before:', state.doc.toString())
  console.log('[commitTreeNode] node at from:', state.doc.nodeAt(pop.from)?.type.name)

  const varNode = state.schema.nodes.variable.create({ value: rawValue, label: rawLabel })
  console.log('[commitTreeNode] varNode:', varNode.type.name, 'inline:', varNode.isInline)

  const tr = state.tr
  tr.delete(pop.from, pop.to)
  tr.insert(pop.from, varNode)
  console.log('[commitTreeNode] doc after:', tr.doc.toString())

  view.dispatch(tr)

  closePop()
  nextTick(() => view.focus())
}

function handleNodeClick(node: any) {
  if (node.data?.disabled) return
  selectedNodeKey.value = node.key
}

function handleNodeDblClick(node: any) {
  if (node.data?.disabled) return
  selectedNodeKey.value = node.key
  commitTreeNode(node)
}

function onPopKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    e.preventDefault()
    commitTreeNode()
    return
  }

  if (e.key === 'Escape') {
    e.preventDefault()
    closePop()
    nextTick(() => editor.value?.commands.focus())
  }
}

function onDocPointerDown(e: PointerEvent) {
  if (!pop.show) return
  if (popEl.value?.contains(e.target as Node)) return
  closePop()
}

function focusEditor() {
  editor.value?.commands.focus()
}

function openDialog() {
  closePop()
  isSubmitting.value = false
  dialogVisible.value = true
}

async function onDialogShow() {
  await nextTick()
  const ed = createEditor(props.modelValue ?? '')
  dialogEditorInstance.value = ed
  ed.commands.focus()
}

function submitDialog() {
  if (!dialogEditorInstance.value || !editor.value) return

  const content = getMarkdown(dialogEditorInstance.value)
  isSubmitting.value = true

  emit('update:modelValue', content)
  emit('submitDialog', content)

  editor.value.commands.setContent(content, { contentType: 'markdown' })
  lastContent = getMarkdown(editor.value)
  dialogVisible.value = false
}

function cancelDialog() {
  isSubmitting.value = false
  dialogVisible.value = false
}

function onDialogClose() {
  if (!isSubmitting.value) {
    emit('submitDialog', props.modelValue)
  }

  dialogEditorInstance.value?.destroy()
  dialogEditorInstance.value = undefined
  isSubmitting.value = false
  closePop()
}

onMounted(() => {
  const ed = createEditor(props.modelValue ?? '', true, (text) => {
    emit('update:modelValue', text)
    handleEditorUpdate(ed)
  })
  editor.value = ed
  lastContent = getMarkdown(ed)

  document.addEventListener('pointerdown', onDocPointerDown)
})

onUnmounted(() => {
  editor.value?.destroy()
  dialogEditorInstance.value?.destroy()
  document.removeEventListener('pointerdown', onDocPointerDown)
  removePopListeners?.()
})
</script>

<style scoped>
.var-picker-tree :deep(.p-tree) {
  border: none;
  background: transparent;
  padding: 0;
}

.var-picker-tree :deep(.p-tree-root-children) {
  gap: 0;
}

.var-picker-tree :deep(.p-tree-node) {
  margin: 0;
}

.var-picker-tree :deep(.p-tree-node-content) {
  padding: 0;
  background: transparent !important;
  border-radius: 0;
}

.var-picker-tree :deep(.p-tree-node-icon) {
  display: none;
}

.var-picker-tree :deep(.p-tree-node-label) {
  width: 100%;
}

.var-picker-tree :deep(.p-tree-node-toggle-button) {
  width: 1rem;
  height: 1rem;
  margin-right: 0.125rem;
  color: rgb(148 163 184);
}

.var-picker-tree :deep(.p-tree-node-children) {
  padding-left: 0.5rem;
}
/* In your <style scoped> section, add: */
.editor-root :deep(.ProseMirror) {
  word-break: break-word;
  overflow-wrap: break-word;
  white-space: pre-wrap;
  max-width: 100%;
}
</style>
