<template>
  <node-view-wrapper
    as="div"
    contenteditable="false"
    class="my-4 font-sans"
    :style="{ '--tc': typeColor, '--tc-a': typeColorAlpha }"
  >
    <!-- Card -->
    <div
      class="group relative flex items-center gap-4 rounded-xl border px-4 py-3.5 transition-all duration-200"
      :class="[
        selected
          ? 'border-[color:var(--tc)]/40 shadow-[0_0_0_2px_var(--tc-a),0_8px_32px_rgba(0,0,0,0.4)]'
          : 'border-white/8 shadow-[0_8px_32px_rgba(0,0,0,0.35)] hover:border-white/14',
        status === 'uploading' ? 'animate-[uploadPulse_2s_ease-in-out_infinite]' : ''
      ]"
      style="
        background: linear-gradient(135deg, var(--p-surface-900) 0%, var(--p-surface-950) 100%);
      "
    >
      <!-- Subtle grid overlay -->
      <div
        class="pointer-events-none absolute inset-0 rounded-xl opacity-40"
        style="
          background-image:
            linear-gradient(rgba(255, 255, 255, 0.025) 1px, transparent 1px),
            linear-gradient(90deg, rgba(255, 255, 255, 0.025) 1px, transparent 1px);
          background-size: 20px 20px;
          mask-image: radial-gradient(ellipse at 70% 50%, black, transparent 75%);
        "
      />
      <!-- Bottom glow line -->
      <div
        class="pointer-events-none absolute bottom-0 left-1/4 right-1/4 h-px rounded-full transition-all duration-300 group-hover:left-4 group-hover:right-4"
        style="
          background: linear-gradient(90deg, transparent, var(--tc), transparent);
          opacity: 0.5;
        "
      />

      <!-- Icon -->
      <div class="relative z-10 flex shrink-0 flex-col items-center gap-1.5">
        <div class="relative flex h-14 w-14 items-center justify-center">
          <svg class="absolute inset-0 h-full w-full" viewBox="0 0 56 56">
            <polygon
              points="28,3 52,16 52,40 28,53 4,40 4,16"
              fill="color-mix(in srgb, var(--tc) 10%, transparent)"
              stroke="rgba(255,255,255,0.06)"
              stroke-width="1"
            />
            <polygon
              points="28,3 52,16 52,40 28,53 4,40 4,16"
              fill="none"
              stroke="var(--tc)"
              stroke-width="1.5"
              stroke-linecap="round"
              :stroke-dasharray="
                status === 'uploading' ? `${(uploadProgress ?? 0) * 1.5} 999` : '150'
              "
              :class="
                status === 'uploading'
                  ? 'animate-[hexGlow_1.5s_ease-in-out_infinite_alternate]'
                  : 'opacity-40 transition-opacity duration-300 group-hover:opacity-100'
              "
            />
          </svg>
          <div
            class="relative z-10 flex h-6 w-6 items-center justify-center transition-[filter] duration-300 group-hover:[filter:drop-shadow(0_0_5px_var(--tc))]"
            style="color: var(--tc)"
          >
            <component :is="fileIconSvg" class="h-full w-full" />
          </div>
        </div>
        <!-- Ext badge -->
        <span
          class="rounded px-1.5 py-0.5 font-mono text-[9px] font-semibold uppercase tracking-widest"
          style="
            color: var(--tc);
            background: color-mix(in srgb, var(--tc) 12%, transparent);
            border: 1px solid color-mix(in srgb, var(--tc) 28%, transparent);
          "
          >{{ extBadge }}</span
        >
      </div>

      <!-- Body -->
      <div class="relative z-10 flex min-w-0 flex-1 flex-col gap-2">
        <!-- Uploading -->
        <template v-if="status === 'uploading'">
          <p class="truncate text-sm font-medium" style="color: var(--p-text-muted-color)">
            {{ node.attrs.name || '正在上传…' }}
          </p>
          <div
            class="relative h-px w-full overflow-visible rounded-full"
            style="background: var(--p-surface-700)"
          >
            <div
              class="h-full rounded-full transition-[width] duration-300 ease-out"
              :style="{
                width: `${uploadProgress ?? 0}%`,
                background: `linear-gradient(90deg, var(--tc), var(--p-surface-0))`,
                boxShadow: `0 0 8px var(--tc)`
              }"
            />
            <div
              class="absolute top-1/2 h-2 w-2 -translate-x-1/2 -translate-y-1/2 rounded-full transition-[left] duration-300 ease-out"
              :style="{
                left: `${uploadProgress ?? 0}%`,
                background: 'var(--p-surface-0)',
                boxShadow: `0 0 10px 3px var(--tc)`
              }"
            />
          </div>
          <div class="flex items-center gap-4">
            <StatItem label="SIZE" :value="formatSize(node.attrs.size)" />
            <StatItem label="PROGRESS" :value="`${uploadProgress ?? 0}%`" accent="var(--tc)" />
            <div class="ml-1 flex items-center gap-1">
              <span
                v-for="i in 3"
                :key="i"
                class="block h-1 w-1 rounded-full animate-[dotPulse_1.2s_ease-in-out_infinite]"
                :style="{ background: 'var(--tc)', animationDelay: `${i * 0.3}s` }"
              />
            </div>
          </div>
        </template>

        <!-- Ready -->
        <template v-else>
          <div class="flex items-center">
            <span
              v-if="!editingName"
              class="block max-w-sm truncate text-sm font-semibold leading-snug tracking-wide"
              style="color: var(--p-text-color)"
              :class="
                editor.isEditable
                  ? 'cursor-text rounded px-1 py-0.5 -mx-1 transition-colors hover:bg-white/5'
                  : ''
              "
              :title="nodeName"
              @click="editor.isEditable && startEditName()"
              >{{ nodeName || '未命名文件' }}</span
            >
            <input
              v-else
              ref="nameInputRef"
              v-model="nameDraft"
              class="w-64 rounded-md border px-2 py-0.5 text-sm font-semibold outline-none transition"
              style="
                background: var(--p-surface-700);
                color: var(--p-text-color);
                border-color: color-mix(in srgb, var(--tc) 50%, transparent);
              "
              @blur="commitName"
              @keydown.enter="commitName"
              @keydown.esc="cancelName"
              @click.stop
            />
          </div>
          <div class="flex items-center gap-4 flex-wrap">
            <StatItem label="SIZE" :value="formatSize(node.attrs.size)" />
            <StatItem
              v-if="node.attrs.mime"
              label="TYPE"
              :value="node.attrs.mime"
              class="max-w-[180px] truncate"
            />
          </div>
        </template>
      </div>

      <!-- Actions -->
      <div v-if="status === 'ready'" class="relative z-10 flex shrink-0 items-center gap-1.5">
        <!-- Download -->
        <button
          class="flex h-8 items-center gap-1.5 rounded-lg border px-3 text-xs font-medium transition-all duration-150 active:scale-95 cursor-pointer"
          style="
            color: var(--tc);
            border-color: color-mix(in srgb, var(--tc) 35%, transparent);
            background: color-mix(in srgb, var(--tc) 10%, transparent);
          "
          @click.stop="downloadFile"
          @mouseenter="
            (e) =>
              ((e.currentTarget as HTMLElement).style.boxShadow =
                `0 0 16px color-mix(in srgb, var(--tc) 25%, transparent)`)
          "
          @mouseleave="(e) => ((e.currentTarget as HTMLElement).style.boxShadow = '')"
        >
          <svg
            class="h-3 w-3"
            viewBox="0 0 16 16"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
          >
            <path d="M8 2v8M5 7l3 3 3-3M2 13h12" />
          </svg>
          <span>下载</span>
        </button>

        <!-- Copy -->
        <button
          class="flex h-8 w-8 items-center justify-center rounded-lg border transition-all duration-150 active:scale-95"
          :style="
            copied
              ? 'border-color: var(--p-green-400); background: color-mix(in srgb, var(--p-green-400) 8%, transparent); color: var(--p-green-400);'
              : 'border-color: var(--p-surface-600); background: var(--p-surface-800); color: var(--p-text-muted-color);'
          "
          @click.stop="copyLink"
          title="复制链接"
        >
          <svg
            v-if="!copied"
            class="h-3.5 w-3.5"
            viewBox="0 0 16 16"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
          >
            <path d="M6 10l-1.5 1.5a3 3 0 004.24 0l3-3a3 3 0 00-4.24-4.24L6 5.5" />
            <path d="M10 6l1.5-1.5a3 3 0 00-4.24 0L4.26 7.5" />
          </svg>
          <svg
            v-else
            class="h-3.5 w-3.5"
            viewBox="0 0 16 16"
            fill="none"
            stroke="currentColor"
            stroke-width="2.2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M3 8l3.5 3.5L13 4" />
          </svg>
        </button>

        <!-- Preview (text files only) -->
        <button
          v-if="isTextFile"
          class="flex h-8 w-8 items-center justify-center rounded-lg border transition-all duration-150 active:scale-95"
          :style="
            previewOpen
              ? 'border-color: color-mix(in srgb, var(--tc) 40%, transparent); background: color-mix(in srgb, var(--tc) 12%, transparent); color: var(--tc);'
              : 'border-color: var(--p-surface-600); background: var(--p-surface-800); color: var(--p-text-muted-color);'
          "
          @click.stop="togglePreview"
          title="预览"
        >
          <svg
            v-if="!previewOpen"
            class="h-3.5 w-3.5"
            viewBox="0 0 16 16"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M1 8s2.5-5 7-5 7 5 7 5-2.5 5-7 5-7-5-7-5z" />
            <circle cx="8" cy="8" r="2" />
          </svg>
          <svg
            v-else
            class="h-3.5 w-3.5"
            viewBox="0 0 16 16"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M2 2l12 12M6.5 6.6A2 2 0 0010 10" />
            <path d="M4.2 4.3C2.8 5.3 1.7 6.8 1 8c1.2 2 3.8 5 7 5 1.4 0 2.7-.5 3.8-1.3" />
            <path d="M8 3c3.2 0 5.8 3 7 5-.4.7-1 1.6-1.8 2.4" />
          </svg>
        </button>

        <!-- Delete -->
        <button
          v-if="editor.isEditable"
          class="flex h-8 w-8 items-center justify-center rounded-lg border transition-all duration-150 active:scale-95"
          style="
            border-color: var(--p-surface-600);
            background: var(--p-surface-800);
            color: var(--p-text-muted-color);
          "
          @mouseenter="
            (e) => {
              const el = e.currentTarget as HTMLElement
              el.style.borderColor = 'color-mix(in srgb, var(--p-red-400) 35%, transparent)'
              el.style.background = 'color-mix(in srgb, var(--p-red-400) 8%, transparent)'
              el.style.color = 'var(--p-red-400)'
            }
          "
          @mouseleave="
            (e) => {
              const el = e.currentTarget as HTMLElement
              el.style.borderColor = 'var(--p-surface-600)'
              el.style.background = 'var(--p-surface-800)'
              el.style.color = 'var(--p-text-muted-color)'
            }
          "
          @click.stop="deleteNode"
          title="删除"
        >
          <svg
            class="h-3.5 w-3.5"
            viewBox="0 0 16 16"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M3 4h10M6 4V3h4v1M5 4l1 9h4l1-9" />
          </svg>
        </button>
      </div>
    </div>

    <!-- PrimeVue 4.x Drawer for file preview -->
    <Drawer
      v-model:visible="previewOpen"
      position="right"
      :style="{ '--tc': typeColor, '--tc-a': typeColorAlpha, width: '42rem' }"
      :pt="{
        root: { class: 'file-preview-drawer' },
        header: { class: 'file-preview-drawer__header' },
        content: { class: 'file-preview-drawer__content' },
        footer: { class: 'file-preview-drawer__footer' }
      }"
      @show="onDrawerShow"
    >
      <!-- Header -->
      <template #header>
        <div class="flex items-center gap-2 min-w-0 flex-1">
          <div class="flex h-5 w-5 items-center justify-center shrink-0" style="color: var(--tc)">
            <component :is="fileIconSvg" class="h-full w-full" />
          </div>
          <span
            class="font-mono text-sm font-semibold truncate"
            style="color: var(--p-text-color)"
            :title="nodeName"
          >
            {{ nodeName }}
          </span>
          <span
            class="rounded px-1.5 py-0.5 font-mono text-[9px] font-semibold uppercase tracking-widest shrink-0"
            style="
              color: var(--tc);
              background: color-mix(in srgb, var(--tc) 12%, transparent);
              border: 1px solid color-mix(in srgb, var(--tc) 28%, transparent);
            "
            >{{ extBadge }}</span
          >
        </div>
      </template>

      <!-- Body -->
      <template #default>
        <!-- Loading -->
        <div
          v-if="previewState === 'loading'"
          class="flex items-center justify-center gap-2 py-16 h-full"
        >
          <span
            v-for="i in 3"
            :key="i"
            class="block h-1.5 w-1.5 rounded-full animate-[dotPulse_1.2s_ease-in-out_infinite]"
            :style="{ background: 'var(--tc)', animationDelay: `${i * 0.25}s` }"
          />
          <span class="text-xs" style="color: var(--p-text-muted-color)">加载中…</span>
        </div>

        <!-- Error -->
        <div v-else-if="previewState === 'error'" class="flex items-center gap-2 px-4 py-10">
          <svg
            class="h-4 w-4 shrink-0"
            style="color: var(--p-red-400)"
            viewBox="0 0 16 16"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
          >
            <circle cx="8" cy="8" r="6" />
            <path d="M8 5v3.5M8 11v.5" />
          </svg>
          <span class="text-xs" style="color: var(--p-red-400)">{{ previewError }}</span>
        </div>

        <!-- Content: CodeMirror -->
        <div v-else-if="previewState === 'done'" class="relative flex flex-col h-full">
          <div ref="cmContainer" class="cm-preview-host flex-1 overflow-hidden" />
          <!-- Bottom fade when truncated -->
          <div
            v-if="previewTruncated"
            class="pointer-events-none absolute bottom-8 left-0 right-0 h-10"
            style="background: linear-gradient(to bottom, transparent, var(--p-surface-950))"
          />
          <!-- Truncation notice -->
          <div
            v-if="previewTruncated"
            class="border-t px-4 py-2 text-center font-mono text-[10px] shrink-0"
            style="
              border-color: color-mix(in srgb, var(--tc) 12%, transparent);
              color: var(--p-text-muted-color);
            "
          >
            仅显示前 {{ MAX_PREVIEW_LINES }} 行 · 完整内容请下载文件
          </div>
        </div>
      </template>

      <!-- Footer -->
      <template #footer>
        <div class="flex items-center justify-between w-full">
          <div class="flex items-center gap-3">
            <StatItem label="SIZE" :value="formatSize(node.attrs.size)" />
            <StatItem
              v-if="previewLines > 0 && previewState === 'done'"
              label="LINES"
              :value="`${previewLines}`"
              :accent="typeColor"
            />
          </div>
          <div class="flex items-center gap-2">
            <!-- Copy content -->
            <button
              class="flex h-8 items-center gap-1.5 rounded-lg border px-3 text-xs font-medium transition-all duration-150 active:scale-95 cursor-pointer"
              :style="
                contentCopied
                  ? 'color: var(--p-green-400); border-color: color-mix(in srgb, var(--p-green-400) 35%, transparent); background: color-mix(in srgb, var(--p-green-400) 10%, transparent);'
                  : 'color: var(--p-text-muted-color); border-color: var(--p-surface-600); background: var(--p-surface-800);'
              "
              @click.stop="copyContent"
            >
              <svg
                v-if="!contentCopied"
                class="h-3 w-3"
                viewBox="0 0 16 16"
                fill="none"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
              >
                <rect x="5" y="5" width="9" height="9" rx="1.5" />
                <path
                  d="M11 5V3.5A1.5 1.5 0 009.5 2H3.5A1.5 1.5 0 002 3.5v6A1.5 1.5 0 003.5 11H5"
                />
              </svg>
              <svg
                v-else
                class="h-3 w-3"
                viewBox="0 0 16 16"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M3 8l3.5 3.5L13 4" />
              </svg>
              <span>{{ contentCopied ? '已复制' : '复制内容' }}</span>
            </button>
            <!-- Download -->
            <button
              class="flex h-8 items-center gap-1.5 rounded-lg border px-3 text-xs font-medium transition-all duration-150 active:scale-95 cursor-pointer"
              style="
                color: var(--tc);
                border-color: color-mix(in srgb, var(--tc) 35%, transparent);
                background: color-mix(in srgb, var(--tc) 10%, transparent);
              "
              @click.stop="downloadFile"
            >
              <svg
                class="h-3 w-3"
                viewBox="0 0 16 16"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
              >
                <path d="M8 2v8M5 7l3 3 3-3M2 13h12" />
              </svg>
              <span>下载</span>
            </button>
          </div>
        </div>
      </template>
    </Drawer>
  </node-view-wrapper>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, defineComponent, h, shallowRef, onBeforeUnmount } from 'vue'
import { NodeViewWrapper } from '@tiptap/vue-3'
import type { NodeViewProps } from '@tiptap/vue-3'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'
import type { Extension } from '@codemirror/state'
import { oneDark } from '@codemirror/theme-one-dark'
import { StreamLanguage } from '@codemirror/language'

const props = defineProps<NodeViewProps>()

// ── Tiny inline stat component ──
const StatItem = defineComponent({
  props: { label: String, value: String, accent: String },
  setup(p) {
    return () =>
      h('div', { class: 'flex items-center gap-1.5' }, [
        h(
          'span',
          {
            class: 'font-mono text-[9px] uppercase tracking-[0.14em]',
            style: 'color: var(--p-text-muted-color); opacity: 0.5;'
          },
          p.label
        ),
        h(
          'span',
          {
            class: 'font-mono text-[11px] truncate',
            style: p.accent
              ? `color: ${p.accent}; text-shadow: 0 0 8px ${p.accent}`
              : 'color: var(--p-text-muted-color)'
          },
          p.value
        )
      ])
  }
})

// ── Refs ──
const nameInputRef = ref<HTMLInputElement | null>(null)
const nameDraft = ref('')
const editingName = ref(false)
const copied = ref(false)
const contentCopied = ref(false)

// ── Preview state ──
const MAX_PREVIEW_LINES = 2000
const previewOpen = ref(false)
const previewState = ref<'idle' | 'loading' | 'done' | 'error'>('idle')
const previewContent = ref('')
const previewError = ref('')
const previewTruncated = ref(false)
const previewLines = computed(() => previewContent.value.split('\n').length)

// CodeMirror
const cmContainer = ref<HTMLElement | null>(null)
const cmView = shallowRef<EditorView | null>(null)

onBeforeUnmount(() => cmView.value?.destroy())

// Text-previewable MIME types
const TEXT_MIMES = new Set([
  'text/plain',
  'text/html',
  'text/css',
  'text/csv',
  'text/javascript',
  'text/markdown',
  'text/xml',
  'text/x-scss',
  'text/x-sass',
  'text/x-less',
  'text/x-java-source',
  'text/x-kotlin',
  'text/x-swift',
  'text/x-python',
  'text/x-ruby',
  'text/x-go',
  'text/x-rust',
  'text/x-c',
  'text/x-c++',
  'text/x-csharp',
  'text/x-php',
  'text/x-sh',
  'text/x-bash',
  'text/x-vue',
  'text/x-svelte',
  'text/x-sql',
  'text/x-dockerfile',
  'text/x-toml',
  'text/x-ini',
  'text/x-properties',
  'application/json',
  'application/ld+json',
  'application/javascript',
  'application/typescript',
  'application/xml',
  'application/xhtml+xml',
  'application/x-yaml',
  'application/yaml',
  'application/x-sh',
  'application/x-shellscript',
  'application/x-httpd-php',
  'application/x-python-code',
  'application/graphql',
  'application/x-toml',
  'application/wasm'
])

const TEXT_EXTENSIONS = new Set([
  'html',
  'htm',
  'css',
  'scss',
  'sass',
  'less',
  'js',
  'mjs',
  'cjs',
  'jsx',
  'ts',
  'tsx',
  'vue',
  'svelte',
  'json',
  'json5',
  'jsonc',
  'yaml',
  'yml',
  'toml',
  'ini',
  'env',
  'properties',
  'conf',
  'config',
  'xml',
  'xsl',
  'xslt',
  'svg',
  'csv',
  'tsv',
  'md',
  'mdx',
  'markdown',
  'rst',
  'txt',
  'text',
  'log',
  'tex',
  'bib',
  'sh',
  'bash',
  'zsh',
  'fish',
  'ps1',
  'psm1',
  'bat',
  'cmd',
  'c',
  'h',
  'cpp',
  'cc',
  'cxx',
  'hpp',
  'hxx',
  'cs',
  'fs',
  'fsx',
  'java',
  'kt',
  'kts',
  'gradle',
  'groovy',
  'scala',
  'clj',
  'cljs',
  'go',
  'rs',
  'swift',
  'mm',
  'm',
  'py',
  'pyw',
  'pyi',
  'rb',
  'rake',
  'gemspec',
  'php',
  'phtml',
  'lua',
  'pl',
  'pm',
  'r',
  'jl',
  'dart',
  'elm',
  'ex',
  'exs',
  'erl',
  'hrl',
  'hs',
  'lhs',
  'makefile',
  'mk',
  'cmake',
  'dockerfile',
  'dockerignore',
  'gitignore',
  'gitattributes',
  'gitmodules',
  'editorconfig',
  'prettierrc',
  'eslintrc',
  'babelrc',
  'htaccess',
  'sql',
  'prisma',
  'graphql',
  'gql',
  'diff',
  'patch'
])

const isTextFile = computed(() => {
  const m = nodeMime.value
  if (TEXT_MIMES.has(m) || m.startsWith('text/')) return true
  const name = (nodeName.value || '').toLowerCase()
  const dot = name.lastIndexOf('.')
  if (dot !== -1) {
    const ext = name.slice(dot + 1)
    if (TEXT_EXTENSIONS.has(ext)) return true
  }
  const bare = name.split('/').pop() ?? ''
  if (['makefile', 'dockerfile', 'gemfile', 'rakefile', 'procfile', 'vagrantfile'].includes(bare))
    return true
  return false
})

// ── Language detection → dynamic import ──
async function getLanguageExtension(name: string, mime: string): Promise<Extension | null> {
  const ext = name.toLowerCase().split('.').pop() ?? ''
  const js = () => import('@codemirror/lang-javascript').then((m) => m.javascript())
  const ts = () =>
    import('@codemirror/lang-javascript').then((m) => m.javascript({ typescript: true }))
  const jsx = () => import('@codemirror/lang-javascript').then((m) => m.javascript({ jsx: true }))
  const tsx = () =>
    import('@codemirror/lang-javascript').then((m) => m.javascript({ typescript: true, jsx: true }))

  const MAP: Record<string, () => Promise<Extension>> = {
    js,
    mjs: js,
    cjs: js,
    jsx,
    ts,
    mts: ts,
    cts: ts,
    tsx,
    vue: () => import('@codemirror/lang-vue').then((m) => m.vue()),
    html: () => import('@codemirror/lang-html').then((m) => m.html()),
    htm: () => import('@codemirror/lang-html').then((m) => m.html()),
    css: () => import('@codemirror/lang-css').then((m) => m.css()),
    scss: () => import('@codemirror/lang-sass').then((m) => m.sass({ indented: false })),
    sass: () => import('@codemirror/lang-sass').then((m) => m.sass({ indented: true })),
    less: () => import('@codemirror/lang-less').then((m) => m.less()),
    json: () => import('@codemirror/lang-json').then((m) => m.json()),
    json5: () => import('@codemirror/lang-json').then((m) => m.json()),
    jsonc: () => import('@codemirror/lang-json').then((m) => m.json()),
    xml: () => import('@codemirror/lang-xml').then((m) => m.xml()),
    xsl: () => import('@codemirror/lang-xml').then((m) => m.xml()),
    svg: () => import('@codemirror/lang-xml').then((m) => m.xml()),
    yaml: () => import('@codemirror/lang-yaml').then((m) => m.yaml()),
    yml: () => import('@codemirror/lang-yaml').then((m) => m.yaml()),
    md: () => import('@codemirror/lang-markdown').then((m) => m.markdown()),
    mdx: () => import('@codemirror/lang-markdown').then((m) => m.markdown()),
    markdown: () => import('@codemirror/lang-markdown').then((m) => m.markdown()),
    py: () => import('@codemirror/lang-python').then((m) => m.python()),
    pyw: () => import('@codemirror/lang-python').then((m) => m.python()),
    pyi: () => import('@codemirror/lang-python').then((m) => m.python()),
    rs: () => import('@codemirror/lang-rust').then((m) => m.rust()),
    go: () => import('@codemirror/lang-go').then((m) => m.go()),
    java: () => import('@codemirror/lang-java').then((m) => m.java()),
    kt: () => import('@codemirror/lang-java').then((m) => m.java()),
    kts: () => import('@codemirror/lang-java').then((m) => m.java()),
    c: () => import('@codemirror/lang-cpp').then((m) => m.cpp()),
    h: () => import('@codemirror/lang-cpp').then((m) => m.cpp()),
    cpp: () => import('@codemirror/lang-cpp').then((m) => m.cpp()),
    cc: () => import('@codemirror/lang-cpp').then((m) => m.cpp()),
    cxx: () => import('@codemirror/lang-cpp').then((m) => m.cpp()),
    hpp: () => import('@codemirror/lang-cpp').then((m) => m.cpp()),
    cs: () => import('@codemirror/lang-cpp').then((m) => m.cpp()),
    php: () => import('@codemirror/lang-php').then((m) => m.php()),
    phtml: () => import('@codemirror/lang-php').then((m) => m.php()),
    sql: () => import('@codemirror/lang-sql').then((m) => m.sql()),
    prisma: () => import('@codemirror/lang-sql').then((m) => m.sql()),
    sh: () =>
      import('@codemirror/legacy-modes/mode/shell').then((m) => StreamLanguage.define(m.shell)),
    bash: () =>
      import('@codemirror/legacy-modes/mode/shell').then((m) => StreamLanguage.define(m.shell)),
    zsh: () =>
      import('@codemirror/legacy-modes/mode/shell').then((m) => StreamLanguage.define(m.shell)),
    wat: () =>
      import('@codemirror/legacy-modes/mode/shell').then((m) => StreamLanguage.define(m.shell))
  }

  try {
    const loader = MAP[ext]
    if (loader) return await loader()
  } catch {}
  return null
}

async function mountCodeMirror(content: string) {
  await nextTick()
  if (!cmContainer.value) return
  cmView.value?.destroy()

  const langExt = await getLanguageExtension(nodeName.value, nodeMime.value)

  const extensions: Extension[] = [
    basicSetup,
    oneDark,
    EditorView.editable.of(false),
    EditorState.readOnly.of(true),
    EditorView.theme({
      '&': {
        fontSize: '12px',
        background: 'transparent !important',
        height: '100%'
      },
      '.cm-scroller': {
        overflow: 'auto',
        fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
        height: '100%'
      },
      '.cm-gutters': { background: 'transparent', borderRight: '1px solid rgba(255,255,255,0.06)' },
      '.cm-content': { padding: '12px 0' }
    })
  ]
  if (langExt) extensions.push(langExt)

  cmView.value = new EditorView({
    state: EditorState.create({ doc: content, extensions }),
    parent: cmContainer.value
  })
}

async function loadPreview() {
  const src = nodeSrc.value
  if (!src) {
    previewState.value = 'error'
    previewError.value = '无可用链接'
    return
  }
  previewState.value = 'loading'
  try {
    const res = await fetch(src)
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const text = await res.text()
    const lines = text.split('\n')
    if (lines.length > MAX_PREVIEW_LINES) {
      previewContent.value = lines.slice(0, MAX_PREVIEW_LINES).join('\n')
      previewTruncated.value = true
    } else {
      previewContent.value = text
      previewTruncated.value = false
    }
    previewState.value = 'done'
    await mountCodeMirror(previewContent.value)
  } catch (e: any) {
    previewState.value = 'error'
    previewError.value = e?.message ?? '加载失败'
  }
}

// ── Drawer show handler ──
async function onDrawerShow() {
  if (previewState.value === 'idle') {
    await loadPreview()
  } else if (previewState.value === 'done') {
    // Re-mount CodeMirror since DOM was destroyed when drawer closed
    await nextTick()
    await mountCodeMirror(previewContent.value)
  }
}

function togglePreview() {
  previewOpen.value = !previewOpen.value
}

async function copyContent() {
  if (!previewContent.value) return
  try {
    await navigator.clipboard.writeText(previewContent.value)
    contentCopied.value = true
    setTimeout(() => {
      contentCopied.value = false
    }, 2000)
  } catch {}
}

// ── Attrs ──
const uploadProgress = computed(() => props.node.attrs.uploadProgress as number | null)
const nodeName = computed(() => props.node.attrs.name as string)
const nodeSrc = computed(() => props.node.attrs.src as string | null)
const nodeMime = computed(() => (props.node.attrs.mime as string) || '')
const status = computed<'uploading' | 'ready'>(() =>
  uploadProgress.value !== null ? 'uploading' : 'ready'
)

// ── Type map ──
interface TypeEntry {
  colorVar: string
  icon: string
  label: string
}

const TYPE_MAP: Record<string, TypeEntry> = {
  'application/pdf': { colorVar: 'var(--p-red-400)', icon: 'pdf', label: 'PDF' },
  'application/zip': { colorVar: 'var(--p-orange-400)', icon: 'zip', label: 'ZIP' },
  'application/x-zip-compressed': { colorVar: 'var(--p-orange-400)', icon: 'zip', label: 'ZIP' },
  'application/x-rar-compressed': { colorVar: 'var(--p-orange-400)', icon: 'zip', label: 'RAR' },
  'application/x-7z-compressed': { colorVar: 'var(--p-orange-400)', icon: 'zip', label: '7Z' },
  'application/msword': { colorVar: 'var(--p-blue-400)', icon: 'doc', label: 'DOC' },
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': {
    colorVar: 'var(--p-blue-400)',
    icon: 'doc',
    label: 'DOCX'
  },
  'application/vnd.ms-excel': { colorVar: 'var(--p-green-400)', icon: 'sheet', label: 'XLS' },
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': {
    colorVar: 'var(--p-green-400)',
    icon: 'sheet',
    label: 'XLSX'
  },
  'application/vnd.ms-powerpoint': { colorVar: 'var(--p-teal-400)', icon: 'ppt', label: 'PPT' },
  'application/vnd.openxmlformats-officedocument.presentationml.presentation': {
    colorVar: 'var(--p-teal-400)',
    icon: 'ppt',
    label: 'PPTX'
  },
  'text/plain': { colorVar: 'var(--p-slate-400)', icon: 'text', label: 'TXT' },
  'text/csv': { colorVar: 'var(--p-green-400)', icon: 'sheet', label: 'CSV' },
  'text/html': { colorVar: 'var(--p-primary-400)', icon: 'code', label: 'HTML' },
  'application/json': { colorVar: 'var(--p-yellow-400)', icon: 'code', label: 'JSON' },
  'application/javascript': { colorVar: 'var(--p-yellow-400)', icon: 'code', label: 'JS' },
  'text/javascript': { colorVar: 'var(--p-yellow-400)', icon: 'code', label: 'JS' },
  'application/typescript': { colorVar: 'var(--p-blue-400)', icon: 'code', label: 'TS' }
}

const FALLBACK: TypeEntry = { colorVar: 'var(--p-primary-400)', icon: 'file', label: 'FILE' }

const typeEntry = computed<TypeEntry>(() => {
  if (TYPE_MAP[nodeMime.value]) return TYPE_MAP[nodeMime.value]
  if (nodeMime.value.startsWith('text/'))
    return { colorVar: 'var(--p-slate-400)', icon: 'text', label: 'TXT' }
  return FALLBACK
})

const typeColor = computed(() => typeEntry.value.colorVar)
const typeColorAlpha = computed(
  () => `color-mix(in srgb, ${typeEntry.value.colorVar} 15%, transparent)`
)

const extBadge = computed(() => {
  if (TYPE_MAP[nodeMime.value]) return TYPE_MAP[nodeMime.value].label
  const name = nodeName.value || ''
  const dot = name.lastIndexOf('.')
  if (dot !== -1)
    return name
      .slice(dot + 1)
      .toUpperCase()
      .slice(0, 5)
  return 'FILE'
})

// ── Icons ──
const ICONS: Record<string, () => any> = {
  pdf: () =>
    h(
      'svg',
      {
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '1.5',
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round'
      },
      [
        h('path', { d: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z' }),
        h('path', { d: 'M14 2v6h6' }),
        h('path', { d: 'M9 15h1.5a1.5 1.5 0 000-3H9v6' })
      ]
    ),
  zip: () =>
    h(
      'svg',
      {
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '1.5',
        'stroke-linecap': 'round'
      },
      [
        h('path', { d: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z' }),
        h('path', { d: 'M14 2v6h6M11 13h2v2h-2v2h2' })
      ]
    ),
  doc: () =>
    h(
      'svg',
      {
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '1.5',
        'stroke-linecap': 'round'
      },
      [
        h('path', { d: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z' }),
        h('path', { d: 'M14 2v6h6' }),
        h('line', { x1: '8', y1: '13', x2: '16', y2: '13' }),
        h('line', { x1: '8', y1: '17', x2: '13', y2: '17' })
      ]
    ),
  sheet: () =>
    h(
      'svg',
      {
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '1.5',
        'stroke-linecap': 'round'
      },
      [
        h('rect', { x: '3', y: '3', width: '18', height: '18', rx: '2' }),
        h('line', { x1: '3', y1: '9', x2: '21', y2: '9' }),
        h('line', { x1: '3', y1: '15', x2: '21', y2: '15' }),
        h('line', { x1: '9', y1: '3', x2: '9', y2: '21' })
      ]
    ),
  ppt: () =>
    h(
      'svg',
      {
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '1.5',
        'stroke-linecap': 'round'
      },
      [
        h('rect', { x: '2', y: '3', width: '20', height: '14', rx: '2' }),
        h('line', { x1: '8', y1: '21', x2: '16', y2: '21' }),
        h('line', { x1: '12', y1: '17', x2: '12', y2: '21' })
      ]
    ),
  code: () =>
    h(
      'svg',
      {
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '1.5',
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round'
      },
      [
        h('path', { d: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z' }),
        h('path', { d: 'M14 2v6h6M9 15l-2 2 2 2M15 15l2 2-2 2' })
      ]
    ),
  text: () =>
    h(
      'svg',
      {
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '1.5',
        'stroke-linecap': 'round'
      },
      [
        h('path', { d: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z' }),
        h('path', { d: 'M14 2v6h6' }),
        h('line', { x1: '8', y1: '13', x2: '16', y2: '13' }),
        h('line', { x1: '8', y1: '17', x2: '16', y2: '17' })
      ]
    ),
  file: () =>
    h(
      'svg',
      {
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '1.5',
        'stroke-linecap': 'round'
      },
      [
        h('path', { d: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z' }),
        h('path', { d: 'M14 2v6h6' })
      ]
    )
}

const fileIconSvg = computed(() => defineComponent(() => ICONS[typeEntry.value.icon] ?? ICONS.file))

// ── Name editing ──
function startEditName() {
  nameDraft.value = nodeName.value
  editingName.value = true
  nextTick(() => nameInputRef.value?.focus())
}
function commitName() {
  props.updateAttributes({ name: nameDraft.value.trim() })
  editingName.value = false
}
function cancelName() {
  editingName.value = false
}

// ── Download ──
function downloadFile() {
  const src = nodeSrc.value
  if (!src) return
  const a = document.createElement('a')
  a.href = src
  a.download = nodeName.value || 'file'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

const getFullUrl = (src: string): string => {
  if (/^https?:\/\//.test(src)) return src
  const { protocol, host } = window.location
  return `${protocol}//${host}${src.startsWith('/') ? src : '/' + src}`
}

// ── Copy link ──
async function copyLink() {
  if (!nodeSrc.value) return
  try {
    await navigator.clipboard.writeText(getFullUrl(nodeSrc.value))
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch {}
}

// ── Delete ──
function deleteNode() {
  const { state, dispatch } = props.editor.view
  const pos = props.getPos?.()
  if (pos == null) return
  const node = state.doc.nodeAt(pos)
  if (!node) return
  dispatch(state.tr.delete(pos, pos + node.nodeSize))
}

// ── Helpers ──
function formatSize(bytes: number): string {
  if (!bytes) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
}
</script>

<style>
@keyframes uploadPulse {
  0%,
  100% {
    border-color: var(--p-surface-700);
  }
  50% {
    border-color: color-mix(in srgb, var(--tc) 35%, transparent);
    box-shadow: 0 0 24px color-mix(in srgb, var(--tc) 12%, transparent);
  }
}
@keyframes hexGlow {
  from {
    filter: drop-shadow(0 0 2px var(--tc));
  }
  to {
    filter: drop-shadow(0 0 8px var(--tc));
  }
}
@keyframes dotPulse {
  0%,
  100% {
    opacity: 0.2;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1.4);
    box-shadow: 0 0 6px var(--tc);
  }
}

/* ── Drawer overrides ── */
.file-preview-drawer.p-drawer {
  background: var(--p-surface-950) !important;
  border-left: 1px solid color-mix(in srgb, var(--tc, var(--p-primary-400)) 15%, transparent);
  display: flex;
  flex-direction: column;
}

.file-preview-drawer .p-drawer-header {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid color-mix(in srgb, var(--tc, var(--p-primary-400)) 15%, transparent);
  background: color-mix(in srgb, var(--tc, var(--p-primary-400)) 5%, var(--p-surface-900));
  flex-shrink: 0;
}

/* Remove default PrimeVue header title text so our custom header slot fills cleanly */
.file-preview-drawer .p-drawer-header .p-drawer-title {
  display: none;
}

.file-preview-drawer .p-drawer-content {
  padding: 0;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--p-surface-950);
}

.file-preview-drawer .p-drawer-footer {
  padding: 0.625rem 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  background: var(--p-surface-900);
  flex-shrink: 0;
}

/* CodeMirror host fills drawer content */
.file-preview-drawer .cm-preview-host {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.file-preview-drawer .cm-preview-host .cm-editor {
  background: transparent !important;
  flex: 1;
  height: 100%;
}
.file-preview-drawer .cm-preview-host .cm-editor.cm-focused {
  outline: none !important;
}
.file-preview-drawer .cm-preview-host .cm-scroller {
  overflow: auto !important;
  height: 100%;
  max-height: none !important;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

/* Thinner scrollbar */
.file-preview-drawer .cm-preview-host .cm-scroller::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
.file-preview-drawer .cm-preview-host .cm-scroller::-webkit-scrollbar-track {
  background: transparent;
}
.file-preview-drawer .cm-preview-host .cm-scroller::-webkit-scrollbar-thumb {
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.12);
}
.file-preview-drawer .cm-preview-host .cm-scroller::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.22);
}
</style>
