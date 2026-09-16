import { reactive, ref } from 'vue'
import { t } from '@/locales'
import type { AgentEvent } from '../type'
import type { Scope, Entry, TextEntry, ToolEntry } from './type'

/**
 * 生成面板逻辑：把 createAgent 下发的扁平 AgentEvent 流（每条带 position 层级路径）拼成一棵可展开的树。
 * - 同一 position 下的 narration/reasoning 拼成文本条目、tool_* 拼成工具卡；
 * - 子 agent（configure_node / plan / validate / execute_step 等委派工具起的）事件 position 更深，
 *   其作用域挂到「起它的那张委派工具卡」下，成为可展开的子树（匹配不到则作为独立分组兜底）。
 * 消费方（面板 index.vue / 递归组件 AgentActivity.vue）只读这棵树渲染，不关心事件如何拼。
 */

/**
 * 工具名 → 展示标签：优先 i18n 的 workflowAgent.tool.*，缺失回退原名。
 * 活动树与状态条共用（避免两处重复）。
 */
export function toolLabel(name: string): string {
  const key = `workflowAgent.tool.${name}`
  const label = t(key)
  return label === key ? name : label
}

/** 委派工具起的子作用域，其 position 末段的「语义键」——用于把子作用域挂回对应工具卡 */
function spawnKeyOf(entry: ToolEntry): string | undefined {
  if (entry.name === 'configure_node') {
    try {
      return JSON.parse(entry.args || '{}').nodeId
    } catch {
      return undefined
    }
  }
  // plan 委派规划器子 agent，其 position 末段固定 'plan'
  if (entry.name === 'plan') return 'plan'
  // validate 委派校验器子 agent，其 position 末段固定 'validate'
  if (entry.name === 'validate') return 'validate'
  return undefined
}

export function createActivity() {
  const root = reactive<Scope>({ key: '', items: [] })
  /** 收起态状态条用：最近一条活动（工具名或正文尾巴） */
  const lastActivity = ref('')

  const scopeMap = new Map<string, Scope>([['', root]])
  const toolById = new Map<string, ToolEntry>()
  const lastEntryByScope = new Map<string, Entry>()

  function reset() {
    root.items = []
    scopeMap.clear()
    scopeMap.set('', root)
    toolById.clear()
    lastEntryByScope.clear()
    lastActivity.value = ''
  }

  /**
   * 取（惰性建）某 position 的作用域，并把它挂到父作用域里对应的委派工具卡（或兜底分组）下。
   *
   * 关键：position 不是作用域的唯一标识——同一节点可能被二次 configure_node、validate 会反复跑到 qualified，
   * 它们的 position 末段完全一样（nodeId / 'validate'）。若按 position 缓存，第二轮事件会串进第一轮的卡、
   * 新卡永远挂不上子树。所以真正的「身份」是父作用域里那张委派工具卡：
   * 每当父作用域冒出一张「还没挂子作用域」的匹配宿主卡，就认定为新一轮委派——给它开全新子作用域，
   * 并把该 position 的「当前作用域」切到这张新卡，后续同 position 事件都并进它。
   */
  function ensureScope(key: string): Scope {
    if (key === '') return root // 根作用域，无宿主/父

    const idx = key.lastIndexOf('/')
    const parentKey = idx >= 0 ? key.slice(0, idx) : ''
    const seg = idx >= 0 ? key.slice(idx + 1) : key
    const parent = ensureScope(parentKey)

    // 从后往前找一张「还没挂子作用域」的委派工具卡当宿主：
    // - configure_node/plan/validate 按语义键(nodeId/'plan'/'validate')精确匹配；
    // - execute_step 的子作用域键是 "step:序号"（工具卡不含该键），按名匹配最近一张未挂载的（同作用域内 step 顺序执行，故最近一张即当前步）。
    const host = [...parent.items]
      .reverse()
      .find(
        (it) =>
          it.type === 'tool' &&
          !it.childScope &&
          (spawnKeyOf(it) === seg || (it.name === 'execute_step' && seg.startsWith('step:')))
      ) as ToolEntry | undefined

    if (host) {
      // 未挂载的宿主卡 = 新一轮委派：开全新子作用域，切走该 position 的当前作用域
      const scope = reactive<Scope>({ key, items: [] })
      host.childScope = scope
      scopeMap.set(key, scope)
      lastEntryByScope.delete(key) // 新一轮的文本另起，不接上一轮的文本条目
      return scope
    }

    const existing = scopeMap.get(key)
    if (existing) return existing

    // 匹配不到宿主卡：兜底成一个独立分组
    const scope = reactive<Scope>({ key, items: [] })
    scopeMap.set(key, scope)
    parent.items.push({ type: 'group', label: seg, childScope: scope, expanded: false })
    return scope
  }

  function pushEvent(ev: AgentEvent) {
    const key = ev.position.join('/')
    const scope = ensureScope(key)
    switch (ev.type) {
      case 'narration':
      case 'reasoning': {
        const kind = ev.type === 'reasoning' ? 'reasoning' : 'narration'
        const last = lastEntryByScope.get(key)
        if (last && last.type === 'text' && last.kind === kind) {
          last.text += ev.delta
          lastActivity.value = tail(last.text)
        } else {
          const entry = reactive<TextEntry>({ type: 'text', kind, text: ev.delta })
          scope.items.push(entry)
          lastEntryByScope.set(key, entry)
          lastActivity.value = tail(entry.text)
        }
        break
      }
      case 'tool_start': {
        const entry = reactive<ToolEntry>({
          type: 'tool',
          id: ev.id,
          name: ev.name,
          args: '',
          result: '',
          ok: true,
          running: true,
          // 默认折叠成一行（Claude 风格），用户点开看输入/输出
          expanded: false
        })
        scope.items.push(entry)
        lastEntryByScope.set(key, entry)
        toolById.set(ev.id, entry)
        lastActivity.value = toolLabel(ev.name)
        break
      }
      case 'tool_args': {
        const entry = toolById.get(ev.id)
        if (entry) entry.args += ev.delta
        break
      }
      case 'tool_end': {
        const entry = toolById.get(ev.id)
        if (entry) {
          entry.running = false
          entry.ok = ev.ok
          entry.result = ev.result
        }
        break
      }
      // done / error 的终态由消费方按 position 深度处理（顶层总结/报错），此处不入树
    }
  }

  return { root, lastActivity, pushEvent, reset }
}

function tail(text: string): string {
  const t = text.trim()
  return t.length > 40 ? '…' + t.slice(-40) : t
}
