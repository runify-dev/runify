<template>
  <div class="agent-tree">
    <div v-for="(item, i) in scope.items" :key="i" class="tree-item">
      <!-- 助手正文 / 思考 -->
      <div
        v-if="item.type === 'text'"
        class="entry-text"
        :class="{ reasoning: item.kind === 'reasoning' }"
      >
        {{ item.text }}
      </div>

      <!-- 工具卡 / 分组：Claude 风格，默认折叠成一行，点开看输入/输出（或子树） -->
      <div v-else class="tool-card" :class="{ err: isErr(item), open: item.expanded }">
        <button
          type="button"
          class="tool-head"
          :class="{ clickable: expandable(item) }"
          :disabled="!expandable(item)"
          @click="expandable(item) && (item.expanded = !item.expanded)"
        >
          <i class="pi tool-status" :class="statusIcon(item)"></i>
          <span class="tool-name">{{ headText(item) }}</span>
          <span v-if="preview(item)" class="tool-preview">{{ preview(item) }}</span>
          <i
            v-if="expandable(item)"
            class="pi caret"
            :class="item.expanded ? 'pi-chevron-down' : 'pi-chevron-right'"
          ></i>
        </button>

        <!-- 展开区 -->
        <div v-if="item.expanded" class="tool-body">
          <!-- 输入：任何工具都展示（委派卡也要看清委派了什么） -->
          <div v-if="item.type === 'tool' && item.args" class="io-block">
            <div class="io-label">{{ t('workflowAgent.activity.input') }}</div>
            <pre class="io-code">{{ fmtArgs(item.args) }}</pre>
          </div>

          <!-- 委派/分组：递归渲染子 agent 的工具树 -->
          <AgentActivity v-if="item.childScope" :scope="item.childScope" />

          <!-- 输出：叶子工具（无子树）才有独立结果摘要 -->
          <div
            v-if="item.type === 'tool' && !item.childScope && !item.running && item.result"
            class="io-block"
          >
            <div class="io-label">{{ t('workflowAgent.activity.output') }}</div>
            <pre class="io-code">{{ item.result }}</pre>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { t } from '@/locales'
import { toolLabel } from '../index'
import type { Scope, Entry } from '../type'

// 自引用递归渲染子树
defineOptions({ name: 'AgentActivity' })
defineProps<{ scope: Scope }>()

function isErr(item: Entry): boolean {
  return item.type === 'tool' && !item.running && !item.ok
}

/** 可展开：委派/分组有子树；叶子工具有入参或结果 */
function expandable(item: Entry): boolean {
  if (item.type === 'group') return true
  if (item.type === 'tool') return !!(item.childScope || item.args || (!item.running && item.result))
  return false
}

function labelOf(item: Entry): string {
  if (item.type === 'group') return item.label
  if (item.type === 'tool') return toolLabel(item.name)
  return ''
}

/** 行标题：execute_step 追加它的步骤标题（args.title），让用户直接看清「在干哪一步」 */
function headText(item: Entry): string {
  const label = labelOf(item)
  if (item.type === 'tool' && item.name === 'execute_step' && item.args) {
    try {
      const title = JSON.parse(item.args).title
      if (title) return `${label}：${title}`
    } catch {
      // args 尚未拼完整，忽略
    }
  }
  return label
}

/** 折叠态的单行预览：叶子工具压平入参首段，让用户不展开也能瞥见「在传什么」 */
function preview(item: Entry): string {
  if (item.type !== 'tool' || item.childScope) return ''
  if (item.name === 'execute_step') return '' // 步骤标题已并入 headText
  const raw = (item.args || '').replace(/\s+/g, ' ').trim()
  if (!raw || raw === '{}') return ''
  return raw.length > 48 ? raw.slice(0, 48) + '…' : raw
}

/** 展开态入参：能解析则美化换行，解析不了（流式未拼完）原样展示 */
function fmtArgs(args: string): string {
  try {
    return JSON.stringify(JSON.parse(args), null, 2)
  } catch {
    return args
  }
}

function statusIcon(item: Entry): string {
  if (item.type === 'group') return 'pi-sitemap grp'
  if (item.type === 'tool') {
    if (item.running) return 'pi-spin pi-spinner run'
    return item.ok ? 'pi-check-circle ok' : 'pi-times-circle bad'
  }
  return ''
}
</script>

<style lang="scss" scoped>
.agent-tree {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tree-item {
  display: flex;
  flex-direction: column;
}

.entry-text {
  font-size: 13px;
  line-height: 1.55;
  color: var(--p-text-color);
  white-space: pre-wrap;
  word-break: break-word;
  padding: 2px 4px;

  &.reasoning {
    color: var(--p-text-muted-color);
    font-style: italic;
  }
}

// 折叠态就是一行；不给底色、不给边框，靠 hover 提示可点，贴近 Claude 的极简工具行
.tool-card {
  border-radius: 6px;

  &.open {
    background: color-mix(in srgb, var(--p-content-border-color) 22%, transparent);
  }

  .tool-head {
    display: flex;
    align-items: center;
    gap: 7px;
    width: 100%;
    padding: 5px 8px;
    border: 0;
    background: transparent;
    border-radius: 6px;
    text-align: left;
    color: var(--p-text-color);
    font-size: 13px;

    &.clickable {
      cursor: pointer;
      &:hover {
        background: color-mix(in srgb, var(--p-content-border-color) 30%, transparent);
      }
    }

    .tool-status {
      font-size: 13px;
      flex-shrink: 0;
      &.run {
        color: var(--p-primary-color);
      }
      &.ok {
        color: var(--p-green-500);
      }
      &.bad {
        color: var(--p-red-500);
      }
      &.grp {
        color: var(--p-text-muted-color);
      }
    }

    .tool-name {
      font-weight: 600;
      flex-shrink: 0;
      white-space: nowrap;
    }

    // 折叠态预览：吃掉剩余宽度，超出省略号
    .tool-preview {
      flex: 1;
      min-width: 0;
      color: var(--p-text-muted-color);
      font-family: var(--font-family-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
      font-size: 12px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .caret {
      margin-left: auto;
      flex-shrink: 0;
      font-size: 11px;
      color: var(--p-text-muted-color);
    }
    // preview 已占满时把 caret 推到末尾
    .tool-preview + .caret {
      margin-left: 6px;
    }
  }

  &.err .tool-head .tool-name {
    color: var(--p-red-500);
  }

  .tool-body {
    padding: 2px 8px 8px 8px;
    display: flex;
    flex-direction: column;
    gap: 6px;

    .io-block {
      display: flex;
      flex-direction: column;
      gap: 3px;
    }

    .io-label {
      font-size: 11px;
      font-weight: 600;
      color: var(--p-text-muted-color);
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    .io-code {
      margin: 0;
      padding: 6px 8px;
      border-radius: 6px;
      background: var(--p-content-background);
      border: 1px solid var(--p-content-border-color);
      color: var(--p-text-color);
      font-family: var(--font-family-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
      font-size: 12px;
      line-height: 1.5;
      white-space: pre-wrap;
      word-break: break-word;
      max-height: 14rem;
      overflow-y: auto;
    }

    // 子树：留一点缩进 + 左引导线
    .agent-tree {
      margin-inline-start: 4px;
      padding-inline-start: 8px;
      border-inline-start: 1px solid var(--p-content-border-color);
    }
  }
}
</style>
