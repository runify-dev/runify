<template>
  <div class="editor-root flex h-full min-h-0 w-full flex-col">
    <div
      class="flex h-full min-h-0 w-full flex-col overflow-hidden rounded-lg border border-[var(--p-content-border-color)] bg-[var(--p-content-background)] shadow-sm"
    >
      <div class="relative flex min-h-0 w-full flex-1 flex-col" @click.self="focusEditor">
        <div ref="cmContainer" class="flex min-h-0 flex-1 flex-col overflow-hidden" />
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
      :modal="true"
      :draggable="false"
      append-to-body
      class="[&_.p-dialog]:flex [&_.p-dialog]:flex-col [&_.p-dialog-content]:flex [&_.p-dialog-content]:min-h-0 [&_.p-dialog-content]:flex-1 [&_.p-dialog-content]:flex-col [&_.p-dialog-content]:overflow-hidden [&_.p-dialog-content]:p-3"
      @show="onDialogShow"
      @hide="onDialogClose"
    >
      <div ref="cmDialogContainer" class="flex min-h-0 flex-1 flex-col overflow-hidden" />
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
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Tree from 'primevue/tree'
import type { TreeNode } from 'primevue/treenode'

import { basicSetup } from 'codemirror'
import {
  Decoration,
  EditorView,
  type DecorationSet,
  ViewPlugin,
  type ViewUpdate
} from '@codemirror/view'
import { EditorState, RangeSetBuilder } from '@codemirror/state'
import { markdown } from '@codemirror/lang-markdown'
import { oneDark } from '@codemirror/theme-one-dark'

defineOptions({ name: 'MdEditorMagnify' })

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

const cmContainer = ref<HTMLElement | null>(null)
const cmDialogContainer = ref<HTMLElement | null>(null)
const popEl = ref<HTMLElement | null>(null)
const popInputRef = ref<HTMLInputElement | null>(null)

let mainView: EditorView | null = null
let dialogView: EditorView | null = null
let activeView: EditorView | null = null
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

function buildTreeNodes(
  list: VariableItem[] = [],
  parentKey = 'root',
  parentValues: string[] = []
): VariableTreeNode[] {
  return list.map((item, index) => {
    const key = `${parentKey}-${index}`
    const currentValues = item.value ? [...parentValues, item.value] : [...parentValues]
    const fullValue = currentValues.join('.')
    const children = buildTreeNodes(item.children ?? [], key, currentValues)

    return {
      key,
      label: item.label,
      data: {
        value: item.value,
        label: item.label,
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
      String(node.data?.value ?? '')
        .toLowerCase()
        .includes(q) ||
      String(node.data?.fullValue ?? '')
        .toLowerCase()
        .includes(q)

    const sourceChildren = (node.children as VariableTreeNode[]) ?? []
    const filteredChildren = filterTree(sourceChildren, q)

    if (selfMatched) {
      result.push({
        ...node,
        children: sourceChildren,
        expanded: true
      })
      continue
    }

    if (filteredChildren.length > 0) {
      result.push({
        ...node,
        children: filteredChildren,
        expanded: true
      })
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
  if (!pop.show || !activeView) return

  const anchor = activeView.coordsAtPos(pop.to)
  if (!anchor) return

  const { width: vw, height: vh } = getViewportSize()
  const margin = 8
  const width = Math.min(280, Math.max(220, vw - margin * 2))
  const popupHeight = 235

  const x = clamp(anchor.left, margin, Math.max(margin, vw - width - margin))

  let y = anchor.bottom + 6
  if (y + popupHeight > vh - margin) {
    y = Math.max(margin, anchor.top - popupHeight - 6)
  }

  pop.width = width
  pop.x = x
  pop.y = y
}

function bindPopPositionListeners() {
  removePopListeners?.()

  const onReposition = () => {
    requestAnimationFrame(() => updatePopPosition())
  }

  const scroller = activeView?.scrollDOM ?? null

  window.addEventListener('resize', onReposition)
  document.addEventListener('scroll', onReposition, true)

  if (window.visualViewport) {
    window.visualViewport.addEventListener('resize', onReposition)
    window.visualViewport.addEventListener('scroll', onReposition)
  }

  scroller?.addEventListener('scroll', onReposition, { passive: true })

  removePopListeners = () => {
    window.removeEventListener('resize', onReposition)
    document.removeEventListener('scroll', onReposition, true)

    if (window.visualViewport) {
      window.visualViewport.removeEventListener('resize', onReposition)
      window.visualViewport.removeEventListener('scroll', onReposition)
    }

    scroller?.removeEventListener('scroll', onReposition)
  }
}

function openPop(from: number, to: number, view: EditorView) {
  activeView = view
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

  const rawValue = target.data?.fullValue
  if (!rawValue) return

  const view = activeView
  if (!view) return

  const insert = `\${${rawValue}}`

  view.dispatch({
    changes: { from: pop.from, to: pop.to, insert },
    selection: { anchor: pop.from + insert.length }
  })

  if (view === mainView) {
    emit('update:modelValue', view.state.doc.toString())
  }

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
    nextTick(() => activeView?.focus())
  }
}

function onDocPointerDown(e: PointerEvent) {
  if (!pop.show) return
  if (popEl.value?.contains(e.target as Node)) return
  closePop()
}

const VAR_RE = /\$\{[^}\n]*\}/g

const varHighlightPlugin = ViewPlugin.fromClass(
  class {
    decorations: DecorationSet

    constructor(view: EditorView) {
      this.decorations = this.build(view)
    }

    update(u: ViewUpdate) {
      if (u.docChanged || u.viewportChanged) {
        this.decorations = this.build(u.view)
      }
    }

    build(view: EditorView): DecorationSet {
      const builder = new RangeSetBuilder<Decoration>()

      for (const { from, to } of view.visibleRanges) {
        const text = view.state.doc.sliceString(from, to)
        VAR_RE.lastIndex = 0

        let m: RegExpExecArray | null
        while ((m = VAR_RE.exec(text)) !== null) {
          builder.add(
            from + m.index,
            from + m.index + m[0].length,
            Decoration.mark({ class: 'cm-var-token' })
          )
        }
      }

      return builder.finish()
    }
  },
  { decorations: (v) => v.decorations }
)

function buildExtensions(onChange?: (v: string) => void) {
  return [
    basicSetup,
    markdown(),
    oneDark,
    varHighlightPlugin,
    EditorView.theme({
      '&': { height: '100%', display: 'flex', flexDirection: 'column' },
      '.cm-scroller': { flex: '1', overflow: 'auto' },
      '.cm-var-token': {
        background: 'rgba(56, 189, 248, 0.15)',
        color: '#38bdf8 !important',
        borderRadius: '4px',
        padding: '1px 3px',
        fontWeight: '500',
        fontFamily: 'monospace'
      }
    }),
    EditorView.domEventHandlers({
      keydown(e) {
        if (e.key === 'Escape' && pop.show) {
          closePop()
          return true
        }
        return false
      }
    }),
    EditorView.updateListener.of((update) => {
      if (update.docChanged && onChange) {
        onChange(update.state.doc.toString())
      }

      if (!pop.show && update.docChanged) {
        const isUserInput = update.transactions.some(
          (tr) =>
            tr.isUserEvent('input') ||
            tr.isUserEvent('input.type') ||
            tr.isUserEvent('input.complete') ||
            tr.isUserEvent('input.paste')
        )

        if (isUserInput) {
          const cursor = update.state.selection.main.head
          const ch = cursor > 0 ? update.state.doc.sliceString(cursor - 1, cursor) : ''
          if (ch === '$') {
            openPop(cursor - 1, cursor, update.view)
            return
          }
        }
      }

      if (pop.show) {
        const cursor = update.state.selection.main.head
        pop.to = cursor

        const before = update.state.doc.sliceString(0, cursor)
        if (
          before.length < pop.from + 1 ||
          before[pop.from] !== '$' ||
          before.slice(pop.from + 1).includes('\n')
        ) {
          closePop()
          return
        }

        requestAnimationFrame(() => updatePopPosition())
      }
    })
  ]
}

function createView(container: HTMLElement, content: string, onChange?: (v: string) => void) {
  return new EditorView({
    state: EditorState.create({
      doc: content,
      extensions: buildExtensions(onChange)
    }),
    parent: container
  })
}

function focusEditor() {
  activeView = mainView
  mainView?.focus()
}

function openDialog() {
  closePop()
  isSubmitting.value = false
  dialogVisible.value = true
}

async function onDialogShow() {
  await nextTick()

  if (cmDialogContainer.value) {
    dialogView?.destroy()
    dialogView = createView(cmDialogContainer.value, props.modelValue ?? '')
    activeView = dialogView
    dialogView.focus()
  }
}

function submitDialog() {
  if (!dialogView || !mainView) return

  const content = dialogView.state.doc.toString()
  isSubmitting.value = true

  emit('update:modelValue', content)
  emit('submitDialog', content)

  mainView.dispatch({
    changes: { from: 0, to: mainView.state.doc.length, insert: content }
  })

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

  dialogView?.destroy()
  dialogView = null
  activeView = mainView
  isSubmitting.value = false
  closePop()
}

watch(
  () => props.modelValue,
  (val) => {
    if (!mainView) return
    const cur = mainView.state.doc.toString()

    if (val !== cur) {
      mainView.dispatch({
        changes: { from: 0, to: cur.length, insert: val ?? '' }
      })
    }
  }
)

onMounted(() => {
  if (cmContainer.value) {
    mainView = createView(cmContainer.value, props.modelValue ?? '', (v) =>
      emit('update:modelValue', v)
    )
    activeView = mainView
  }

  document.addEventListener('pointerdown', onDocPointerDown)
})

onUnmounted(() => {
  mainView?.destroy()
  dialogView?.destroy()
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
</style>
