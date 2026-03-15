<template>
  <div class="editor-root h-full w-full flex flex-col min-h-0">
    <div
      class="h-full w-full flex flex-col min-h-0 rounded-lg border border-[var(--p-content-border-color)] bg-[var(--p-content-background)] shadow-sm overflow-hidden"
    >
      <div class="relative flex flex-col flex-1 min-h-0 w-full" @click.self="focusEditor">
        <div ref="cmContainer" class="flex flex-col flex-1 min-h-0 overflow-hidden" />
        <div class="absolute bottom-2 right-2 z-10 pointer-events-none">
          <AppIcon
            class="pointer-events-auto text-[color:var(--p-text-muted-color)] hover:opacity-70 cursor-pointer text-base"
            name="app-magnify"
            @click.stop="openDialog"
          />
        </div>
      </div>
    </div>

    <!-- 变量选择浮层 -->
    <Teleport to="body">
      <div
        v-if="pop.show"
        ref="popEl"
        class="fixed z-[9999] w-[240px] rounded-lg overflow-hidden bg-white border border-slate-200 shadow-[0_8px_24px_rgba(0,0,0,0.12)]"
        :style="{ left: pop.x + 'px', top: pop.y + 'px' }"
      >
        <!-- 搜索框 -->
        <div class="flex items-center gap-2 px-3 py-2 bg-slate-50 border-b border-slate-200">
          <i class="pi pi-search text-xs text-slate-400 shrink-0" />
          <input
            ref="popInputRef"
            v-model="pop.query"
            class="flex-1 bg-transparent border-none outline-none text-[13px] text-slate-800 placeholder:text-slate-400"
            placeholder="搜索变量..."
            @keydown="onPopKeydown"
          />
        </div>

        <!-- 列表 -->
        <ul class="m-0 p-1 list-none max-h-[200px] overflow-y-auto">
          <li
            v-for="(v, i) in filteredVars"
            :key="v.name"
            class="flex flex-col px-2.5 py-1.5 rounded-md cursor-pointer transition-colors duration-100"
            :class="i === pop.idx ? 'bg-slate-100' : 'hover:bg-slate-50'"
            @mousedown.prevent="commitVariable(v)"
            @mouseenter="pop.idx = i"
          >
            <span class="text-[13px] font-medium text-slate-700 truncate leading-snug">{{
              v.label || v.name
            }}</span>
            <span v-if="v.label" class="text-[11px] font-mono text-slate-400 truncate mt-0.5">{{
              v.name
            }}</span>
          </li>
          <li
            v-if="!filteredVars.length"
            class="px-2.5 py-3 text-[13px] text-slate-400 text-center"
          >
            无匹配变量
          </li>
        </ul>
      </div>
    </Teleport>

    <!-- 全屏弹窗 -->
    <Dialog
      v-model:visible="dialogVisible"
      :header="title"
      :style="{ width: '90vw', height: '80vh' }"
      :modal="true"
      :draggable="false"
      append-to-body
      class="[&_.p-dialog]:flex [&_.p-dialog]:flex-col [&_.p-dialog-content]:flex [&_.p-dialog-content]:flex-col [&_.p-dialog-content]:flex-1 [&_.p-dialog-content]:min-h-0 [&_.p-dialog-content]:overflow-hidden [&_.p-dialog-content]:p-3"
      @show="onDialogShow"
      @hide="onDialogClose"
    >
      <div ref="cmDialogContainer" class="flex flex-col flex-1 min-h-0 overflow-hidden" />
      <template #footer>
        <div class="flex gap-2 justify-end">
          <Button label="取消" severity="secondary" @click="cancelDialog" />
          <Button label="提交" @click="submitDialog" />
        </div>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'

import { basicSetup } from 'codemirror'
import { EditorView, Decoration, DecorationSet, ViewPlugin, ViewUpdate } from '@codemirror/view'
import { EditorState, RangeSetBuilder } from '@codemirror/state'
import { markdown } from '@codemirror/lang-markdown'
import { oneDark } from '@codemirror/theme-one-dark'

defineOptions({ name: 'MdEditorMagnify' })

export interface VariableItem {
  name: string
  label?: string
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

const pop = reactive({
  show: false,
  x: 0,
  y: 0,
  query: '',
  idx: 0,
  from: 0,
  to: 0
})

const filteredVars = computed<VariableItem[]>(() => {
  const list = props.variables ?? []
  const q = pop.query.trim().toLowerCase()
  if (!q) return list
  return list.filter(
    (v) =>
      v.name.toLowerCase().includes(q) ||
      (v.label ?? '').toLowerCase().includes(q) ||
      v.name.split('.').some((s) => s.toLowerCase().includes(q))
  )
})

function openPop(x: number, y: number, from: number, to: number) {
  pop.show = true
  pop.from = from
  pop.to = to
  pop.query = ''
  pop.idx = 0

  const W = 240,
    H = 280
  pop.x = x + W > window.innerWidth ? window.innerWidth - W - 8 : x
  pop.y = y + H > window.innerHeight ? y - H - 4 : y + 6

  nextTick(() => popInputRef.value?.focus())
}

function closePop() {
  pop.show = false
  pop.query = ''
}

function onPopKeydown(e: KeyboardEvent) {
  const len = filteredVars.value.length
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    pop.idx = len ? (pop.idx + 1) % len : 0
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    pop.idx = len ? (pop.idx - 1 + len) % len : 0
  } else if (e.key === 'Enter') {
    e.preventDefault()
    const v = filteredVars.value[pop.idx]
    if (v) commitVariable(v)
  } else if (e.key === 'Escape') {
    e.preventDefault()
    closePop()
    nextTick(() => activeView?.focus())
  }
}

function commitVariable(v: VariableItem) {
  const view = activeView
  if (!view) return
  // 插入格式仍保持 {{ var }} ，@ 只是触发字符，会被替换掉
  const insert = `{{ ${v.name} }}`
  view.dispatch({
    changes: { from: pop.from, to: pop.to, insert },
    selection: { anchor: pop.from + insert.length }
  })
  if (view === mainView) emit('update:modelValue', view.state.doc.toString())
  closePop()
  nextTick(() => view.focus())
}

function onDocPointerDown(e: PointerEvent) {
  if (!pop.show) return
  if (popEl.value?.contains(e.target as Node)) return
  closePop()
}

// ── 变量高亮 ──────────────────────────────────────────────
const VAR_RE = /\{\{[^}\n]*\}\}/g

const varHighlightPlugin = ViewPlugin.fromClass(
  class {
    decorations: DecorationSet
    constructor(view: EditorView) {
      this.decorations = this.build(view)
    }
    update(u: ViewUpdate) {
      if (u.docChanged || u.viewportChanged) this.decorations = this.build(u.view)
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

// ── CodeMirror extensions ─────────────────────────────────
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
      keydown(e, view) {
        if (e.key === 'Escape' && pop.show) {
          closePop()
          return true
        }
        // 触发字符改为 @
        if (e.key !== '@') return false
        setTimeout(() => {
          const cursor = view.state.selection.main.head
          // 确认刚插入的字符是 @
          const ch = view.state.doc.sliceString(cursor - 1, cursor)
          if (ch !== '@') return
          const c = view.coordsAtPos(cursor)
          if (!c) return
          activeView = view
          // from = @ 的位置（cursor-1），to = cursor
          openPop(c.left, c.bottom, cursor - 1, cursor)
        }, 0)
        return false
      }
    }),
    EditorView.updateListener.of((update) => {
      if (!update.docChanged) return
      if (onChange) onChange(update.state.doc.toString())
      // 如果 @ 被删掉了就关闭浮层
      if (pop.show) {
        const cursor = update.state.selection.main.head
        pop.to = cursor
        const before = update.state.doc.sliceString(0, cursor)
        // @ 还在且没有换行就继续，否则关闭
        if (
          before.length < pop.from + 1 ||
          before[pop.from] !== '@' ||
          before.slice(pop.from + 1).includes('\n')
        ) {
          closePop()
        }
      }
    })
  ]
}

function createView(container: HTMLElement, content: string, onChange?: (v: string) => void) {
  return new EditorView({
    state: EditorState.create({ doc: content, extensions: buildExtensions(onChange) }),
    parent: container
  })
}

function focusEditor() {
  mainView?.focus()
}

const dialogVisible = ref(false)
const isSubmitting = ref(false)

function openDialog() {
  isSubmitting.value = false
  dialogVisible.value = true
}

async function onDialogShow() {
  await nextTick()
  if (cmDialogContainer.value) {
    dialogView?.destroy()
    dialogView = createView(cmDialogContainer.value, props.modelValue ?? '')
    activeView = dialogView
  }
}

function submitDialog() {
  if (!dialogView) return
  const content = dialogView.state.doc.toString()
  isSubmitting.value = true
  emit('update:modelValue', content)
  emit('submitDialog', content)
  mainView?.dispatch({ changes: { from: 0, to: mainView.state.doc.length, insert: content } })
  dialogVisible.value = false
}

function cancelDialog() {
  isSubmitting.value = false
  dialogVisible.value = false
}

function onDialogClose() {
  // called by @hide
  if (!isSubmitting.value) emit('submitDialog', props.modelValue)
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
    if (val !== cur) mainView.dispatch({ changes: { from: 0, to: cur.length, insert: val ?? '' } })
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
})
</script>
