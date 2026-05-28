<script setup lang="ts">
import { computed } from 'vue'

import type { PatchFileInfo, PatchFileStatus } from './patchTypes'
import { parsePatch } from './parsePatch'

const props = withDefaults(
  defineProps<{
    diff: string
    showSummary?: boolean
    showActions?: boolean
    showMeta?: boolean
    showHunk?: boolean
    maxHeight?: string
  }>(),
  {
    showSummary: true,
    showActions: false,
    showMeta: false,
    showHunk: false,
    maxHeight: ''
  }
)

const emit = defineEmits<{
  apply: []
  reject: []
}>()

const files = computed(() => parsePatch(props.diff))

const displayFiles = computed(() => {
  return files.value.map(file => ({
    ...file,
    lines: file.lines.filter(line => {
      if (!props.showMeta && line.type === 'meta') return false
      if (!props.showHunk && line.type === 'hunk') return false
      return true
    })
  }))
})

const summary = computed(() => {
  return files.value.reduce(
    (acc, file) => {
      acc.additions += file.additions
      acc.deletions += file.deletions
      acc.files += 1
      return acc
    },
    { files: 0, additions: 0, deletions: 0 }
  )
})

const contentStyle = computed(() => {
  if (!props.maxHeight) return {}
  return { maxHeight: props.maxHeight }
})

function getFilePath(file: PatchFileInfo) {
  if (file.status === 'rename' || file.status === 'copy') {
    return `${file.oldPath} → ${file.newPath}`
  }
  return file.newPath || file.oldPath || 'unknown'
}

function getStatusText(status: PatchFileStatus) {
  const map: Record<PatchFileStatus, string> = {
    add: '新增',
    modify: '修改',
    delete: '删除',
    rename: '重命名',
    copy: '复制',
    mode: '权限',
    binary: '二进制',
    unknown: '未知'
  }
  return map[status] ?? '未知'
}

async function copyText(text: string) {
  await navigator.clipboard?.writeText(text)
}
</script>

<template>
  <div class="pv">
    <!-- ── 汇总 ── -->
    <div v-if="showSummary" class="pv-summary">
      <div class="pv-summary-head">
        <span class="pv-summary-title">Patch 预览</span>
        <span class="pv-summary-stats">
          <span>{{ summary.files }} file{{ summary.files > 1 ? 's' : '' }}</span>
          <span class="pv-add">+{{ summary.additions }}</span>
          <span class="pv-del">-{{ summary.deletions }}</span>
        </span>
      </div>

      <div class="pv-file-list">
        <div
          v-for="file in files"
          :key="`${file.status}:${file.oldPath}:${file.newPath}`"
          class="pv-file-row"
        >
          <span class="pv-status" :class="`pv-status-${file.status}`">
            {{ getStatusText(file.status) }}
          </span>
          <span class="pv-path">{{ getFilePath(file) }}</span>
          <span v-if="file.additions || file.deletions" class="pv-count">
            <span class="pv-add">+{{ file.additions }}</span>
            <span class="pv-del">-{{ file.deletions }}</span>
          </span>
        </div>
      </div>

      <div v-if="showActions" class="pv-actions">
        <button class="pv-btn pv-btn-sec" type="button" @click="emit('reject')">拒绝</button>
        <button class="pv-btn pv-btn-pri" type="button" @click="emit('apply')">应用补丁</button>
      </div>
    </div>

    <!-- ── Diff 内容 ── -->
    <div v-if="displayFiles.length" class="pv-content" :style="contentStyle">
      <section
        v-for="file in displayFiles"
        :key="`${file.status}:${file.oldPath}:${file.newPath}`"
        class="pv-file"
      >
        <div class="pv-file-head">
          <span class="pv-file-path">{{ getFilePath(file) }}</span>
          <button class="pv-copy" type="button" title="复制路径" @click="copyText(getFilePath(file))">
            <svg viewBox="0 0 16 16" width="13" height="13" fill="none">
              <rect x="5" y="5" width="8" height="8" rx="1.5" stroke="currentColor" stroke-width="1.3" />
              <path
                d="M3 11V3.5A1.5 1.5 0 0 1 4.5 2H11"
                stroke="currentColor"
                stroke-width="1.3"
                stroke-linecap="round"
              />
            </svg>
          </button>
        </div>

        <div v-if="file.lines.length" class="pv-code-wrap">
          <table class="pv-table">
            <tbody>
              <tr
                v-for="(line, index) in file.lines"
                :key="index"
                class="pv-row"
                :class="`pv-row-${line.type}`"
              >
                <template v-if="line.type === 'hunk'">
                  <td class="pv-hunk" colspan="4">{{ line.content }}</td>
                </template>
                <template v-else-if="line.type === 'meta'">
                  <td class="pv-meta" colspan="4">{{ line.content }}</td>
                </template>
                <template v-else>
                  <td class="pv-ln">{{ line.displayLineNumber ?? '' }}</td>
                  <td class="pv-prefix">{{ line.prefix }}</td>
                  <td class="pv-code"><code>{{ line.content || ' ' }}</code></td>
                </template>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else class="pv-empty">仅文件状态变化，无代码内容变更。</div>
      </section>
    </div>

    <div v-else class="pv-empty">暂无 patch 内容</div>
  </div>
</template>

<style scoped>
.pv {
  --pv-radius: 8px;
  --pv-mono: 'JetBrains Mono NL', monospace;

  width: 100%;
  overflow: hidden;
  border-radius: var(--pv-radius);
  color: var(--t1);
  font-size: 13px;
  line-height: 1.5;
}

/* ── 汇总卡片 ── */
.pv-summary {
  border: 1px solid var(--bd);
  border-radius: var(--pv-radius);
  overflow: hidden;
  margin-bottom: 10px;
}

.pv-summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--bg2);
}

.pv-summary-title {
  font-weight: 600;
  font-size: 13px;
  color: var(--t1);
}

.pv-summary-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--t2);
}

.pv-file-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 12px;
}

.pv-file-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.pv-status {
  flex: none;
  min-width: 44px;
  padding: 1px 6px;
  border-radius: 4px;
  text-align: center;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.6;
}

.pv-status-add {
  color: #16a34a;
  background: rgba(34, 197, 94, 0.1);
}
.pv-status-modify {
  color: #3370ff;
  background: rgba(51, 112, 255, 0.1);
}
.pv-status-delete {
  color: var(--danger-text);
  background: var(--danger-bg);
}
.pv-status-rename,
.pv-status-copy {
  color: #3370ff;
  background: rgba(51, 112, 255, 0.1);
}
.pv-status-mode,
.pv-status-binary,
.pv-status-unknown {
  color: var(--t2);
  background: var(--hv);
}

.pv-path {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--pv-mono);
  font-size: 12px;
  color: var(--t1);
}

.pv-count {
  display: inline-flex;
  gap: 6px;
  flex: none;
  font-size: 11px;
  font-family: var(--pv-mono);
}

.pv-add {
  color: #16a34a;
}
.pv-del {
  color: var(--danger-text);
}

.pv-actions {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  padding: 8px 12px;
}

.pv-btn {
  border: 0;
  border-radius: 6px;
  padding: 5px 12px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
}
.pv-btn-pri {
  color: #ffffff;
  background: #3370ff;
}
.pv-btn-sec {
  color: var(--t1);
  background: var(--hv);
}

/* ── Diff 文件 ── */
.pv-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
}

.pv-file {
  border: 1px solid var(--bd);
  border-radius: var(--pv-radius);
  overflow: hidden;
}

.pv-file-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: var(--bg2);
  border-bottom: 1px solid var(--bd);
}

.pv-file-path {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--pv-mono);
  font-size: 12px;
  color: var(--t2);
}

.pv-copy {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--t3);
  cursor: pointer;
  transition: background var(--p-transition-duration), color var(--p-transition-duration);
}
.pv-copy:hover {
  background: var(--hv);
  color: var(--t1);
}

/* ── 代码表格 ── */
.pv-code-wrap {
  overflow-x: auto;
  background: var(--bg);
}

.pv-table {
  width: 100%;
  min-width: 520px;
  border-collapse: collapse;
  font-family: var(--pv-mono);
  font-size: 12px;
  line-height: 1.55;
}

.pv-row {
  height: 24px;
}

.pv-ln {
  width: 48px;
  min-width: 48px;
  padding: 0 8px 0 12px;
  color: var(--t3);
  text-align: right;
  user-select: none;
  vertical-align: top;
}

.pv-prefix {
  width: 24px;
  min-width: 24px;
  padding: 0 4px;
  text-align: center;
  user-select: none;
  vertical-align: top;
  font-weight: 600;
}

.pv-code {
  padding: 0 12px 0 4px;
  white-space: pre;
  vertical-align: top;
  color: var(--t1);
}
.pv-code code {
  display: inline;
  color: inherit;
  font: inherit;
  white-space: pre;
}

/* 行背景 */
.pv-row-add {
  background: rgba(34, 197, 94, 0.1);
}
.pv-row-add .pv-ln,
.pv-row-add .pv-prefix {
  color: #16a34a;
}

.pv-row-remove {
  background: rgba(239, 68, 68, 0.1);
}
.pv-row-remove .pv-ln,
.pv-row-remove .pv-prefix {
  color: var(--danger-text);
}

.pv-row-context,
.pv-row-empty {
  background: var(--bg);
}

.pv-hunk {
  padding: 3px 12px;
  color: var(--t2);
  background: rgba(51, 112, 255, 0.1);
  white-space: pre;
  font-size: 11px;
}

.pv-meta {
  padding: 3px 12px;
  color: var(--t3);
  background: var(--bg2);
  white-space: pre;
  font-size: 11px;
}

.pv-empty {
  padding: 20px;
  color: var(--t2);
  text-align: center;
  font-size: 13px;
}

/* ── 响应式 ── */
@media (max-width: 640px) {
  .pv-summary-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .pv-file-path {
    font-size: 11px;
  }

  .pv-table {
    min-width: 400px;
    font-size: 11px;
  }

  .pv-row {
    height: 22px;
  }

  .pv-ln {
    width: 36px;
    min-width: 36px;
    padding-left: 6px;
  }

  .pv-prefix {
    width: 20px;
    min-width: 20px;
  }
}
</style>
