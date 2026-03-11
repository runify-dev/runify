import { ref, type Ref } from 'vue'
import type { Editor } from '@tiptap/vue-3'

export interface GridCell {
  r: number
  c: number
}

export interface UseTableGridOptions {
  maxRows?: number
  maxCols?: number
  onInserted?: (rows: number, cols: number) => void
}

export interface UseTableGridReturn {
  isOpen: Ref<boolean>
  hoveredRow: Ref<number>
  hoveredCol: Ref<number>
  hover: (r: number, c: number) => void
  pick: (r: number, c: number) => void
  open: () => void
  close: () => void
  toggle: () => void
  grid: () => GridCell[][]
  maxRows: number
  maxCols: number
}

export function useTableGrid(
  editorRef: { readonly value: Editor | undefined | null },
  options: UseTableGridOptions = {}
): UseTableGridReturn {
  const { maxRows = 8, maxCols = 8, onInserted } = options

  const hoveredRow = ref(0)
  const hoveredCol = ref(0)
  const isOpen = ref(false)

  function hover(r: number, c: number): void {
    hoveredRow.value = r
    hoveredCol.value = c
  }

  function pick(r: number, c: number): void {
    if (!editorRef.value) return
    editorRef.value.chain().focus().insertTable({ rows: r, cols: c, withHeaderRow: true }).run()
    isOpen.value = false
    onInserted?.(r, c)
  }

  function open(): void { isOpen.value = true }
  function close(): void { isOpen.value = false; hoveredRow.value = 0; hoveredCol.value = 0 }
  function toggle(): void { isOpen.value ? close() : open() }

  function grid(): GridCell[][] {
    return Array.from({ length: maxRows }, (_, r) =>
      Array.from({ length: maxCols }, (_, c) => ({ r: r + 1, c: c + 1 }))
    )
  }

  return { isOpen, hoveredRow, hoveredCol, hover, pick, open, close, toggle, grid, maxRows, maxCols }
}
