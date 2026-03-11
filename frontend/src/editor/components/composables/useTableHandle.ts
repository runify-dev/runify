import { ref, onMounted, onUnmounted, type Ref } from 'vue'
import type { Editor } from '@tiptap/vue-3'

export interface HandlePosition {
  visible: boolean
  x: number
  y: number
}

export interface UseTableHandleReturn {
  rowHandle: Ref<HandlePosition>
  colHandle: Ref<HandlePosition>
  openMenuRef: Ref<'row' | 'col' | null>
  setHandleHovered: (v: boolean) => void
  cancelHide: () => void
  scheduleHide: () => void
  addRowBefore: () => void
  addRowAfter: () => void
  deleteRow: () => void
  addColumnBefore: () => void
  addColumnAfter: () => void
  deleteColumn: () => void
}

export function useTableHandle(
  editorRef: { readonly value: Editor | undefined | null },
  editableRef: Ref<boolean>
): UseTableHandleReturn {
  const rowHandle = ref<HandlePosition>({ visible: false, x: 0, y: 0 })
  const colHandle = ref<HandlePosition>({ visible: false, x: 0, y: 0 })
  const openMenuRef = ref<'row' | 'col' | null>(null)
  const isHandleHovered = ref(false)

  let hoveredCellEl: HTMLElement | null = null
  let hideTimer: ReturnType<typeof setTimeout> | null = null

  // ── DOM helpers ────────────────────────────────────────────────────────
  function getCellFromEvent(e: MouseEvent): HTMLElement | null {
    let el = e.target as HTMLElement | null
    while (el && el !== document.body) {
      if (el.tagName === 'TD' || el.tagName === 'TH') return el
      el = el.parentElement
    }
    return null
  }

  function getTableFromCell(cellEl: HTMLElement): HTMLElement | null {
    let el = cellEl.parentElement
    while (el) {
      if (el.tagName === 'TABLE') return el as HTMLElement
      el = el.parentElement
    }
    return null
  }

  // ── Resolve DOM cell → ProseMirror position ────────────────────────────
  function getCellPos(cellEl: HTMLElement | null): number | null {
    const ed = editorRef.value
    if (!ed || !cellEl) return null
    try {
      const pos = ed.view.posAtDOM(cellEl, 0)
      const $pos = ed.state.doc.resolve(pos)
      for (let d = $pos.depth; d >= 0; d--) {
        const node = $pos.node(d)
        if (node.type.name === 'tableCell' || node.type.name === 'tableHeader') {
          return $pos.before(d)
        }
      }
    } catch (_) { /* element outside doc */ }
    return null
  }

  // ── Run command at hovered cell ────────────────────────────────────────
  function cmd(name: string): void {
    const ed = editorRef.value
    if (!ed) return
    const cellPos = getCellPos(hoveredCellEl)
    if (cellPos !== null) {
      ed.chain().focus().setTextSelection(cellPos + 1)[name as keyof typeof ed.chain]?.()
        // Use a type-safe workaround for dynamic command names
        ; (ed.chain().focus().setTextSelection(cellPos + 1) as any)[name]().run()
    } else {
      ; (ed.chain().focus() as any)[name]().run()
    }
  }

  // ── Handle positions (fixed coords) ───────────────────────────────────
  function updateHandles(cellEl: HTMLElement): void {
    const tableEl = getTableFromCell(cellEl)
    if (!tableEl) return
    const tr = tableEl.getBoundingClientRect()
    const cr = cellEl.getBoundingClientRect()
    rowHandle.value = { visible: true, x: tr.left - 28, y: cr.top + cr.height / 2 - 11 }
    colHandle.value = { visible: true, x: cr.left + cr.width / 2 - 11, y: tr.top - 28 }
  }

  function hideHandles(): void {
    rowHandle.value = { ...rowHandle.value, visible: false }
    colHandle.value = { ...colHandle.value, visible: false }
  }

  // ── Mouse tracking ─────────────────────────────────────────────────────
  function onMouseMove(e: MouseEvent): void {
    // In read-only mode, never show handles
    if (!editableRef.value) {
      hideHandles()
      return
    }
    const cellEl = getCellFromEvent(e)
    if (!cellEl) {
      if (!isHandleHovered.value) scheduleHide()
      return
    }
    clearHideTimer()
    hoveredCellEl = cellEl
    updateHandles(cellEl)
  }

  function scheduleHide(): void {
    clearHideTimer()
    hideTimer = setTimeout(() => {
      if (openMenuRef.value) return
      if (isHandleHovered.value) return
      hideHandles()
    }, 200)
  }

  function clearHideTimer(): void {
    if (hideTimer !== null) { clearTimeout(hideTimer); hideTimer = null }
  }

  function cancelHide(): void { clearHideTimer() }

  function setHandleHovered(v: boolean): void {
    isHandleHovered.value = v
    if (v) cancelHide()
  }
  // 在它上面加
  function onScroll(): void {
    if (!editableRef.value) return
    if (hoveredCellEl && rowHandle.value.visible) {
      updateHandles(hoveredCellEl)
    }
  }
  onMounted(() => {
    document.addEventListener('mousemove', onMouseMove);
    window.addEventListener('scroll', onScroll, { capture: true, passive: true })
  })
  onUnmounted(() => {
    document.removeEventListener('mousemove', onMouseMove); clearHideTimer();
    window.removeEventListener('scroll', onScroll)
  })

  return {
    rowHandle,
    colHandle,
    openMenuRef,
    cancelHide,
    scheduleHide,
    setHandleHovered,
    addRowBefore: () => cmd('addRowBefore'),
    addRowAfter: () => cmd('addRowAfter'),
    deleteRow: () => cmd('deleteRow'),
    addColumnBefore: () => cmd('addColumnBefore'),
    addColumnAfter: () => cmd('addColumnAfter'),
    deleteColumn: () => cmd('deleteColumn'),
  }
}
