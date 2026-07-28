<template>
  <div class="relative h-full flex flex-col">
    <!-- 顶部操作栏 -->
    <div class="flex items-center justify-between px-4 py-3 border-b" style="border-color: var(--p-content-border-color);">
      <div class="flex items-center gap-2">
        <i class="pi pi-wrench text-primary-500"></i>
        <span class="font-semibold">{{ form.label || form.name }}</span>
        <Tag :value="form.runtime" severity="secondary" />
      </div>
      <Button icon="pi pi-save" :label="t('common.save')" size="small" @click="save" :loading="saving" />
    </div>

    <div class="flex-1 overflow-auto p-4" v-if="loaded">
      <div class="grid grid-cols-1 xl:grid-cols-2 gap-4">
        <!-- 基本信息 -->
        <Fieldset legend="基本信息">
          <div class="flex flex-col gap-3">
            <div class="flex flex-col gap-1">
              <label class="text-sm font-semibold">名称 (name)</label>
              <InputText v-model="form.name" placeholder="合法标识符，作为 LLM function name" fluid />
            </div>
            <div class="flex flex-col gap-1">
              <label class="text-sm font-semibold">展示名 (label)</label>
              <InputText v-model="form.label" fluid />
            </div>
            <div class="flex flex-col gap-1">
              <label class="text-sm font-semibold">描述 (description)</label>
              <Textarea v-model="form.description" rows="2" auto-resize fluid placeholder="给 LLM 看的 function.description" />
            </div>
            <div class="flex flex-col gap-1">
              <label class="text-sm font-semibold">运行时 (runtime)</label>
              <SelectButton v-model="form.runtime" :options="runtimeOptions" option-label="label" option-value="value" :allow-empty="false" />
            </div>
          </div>
        </Fieldset>

        <!-- 实现 -->
        <Fieldset legend="实现">
          <template v-if="form.runtime === 'JS'">
            <div class="flex flex-col gap-3">
              <div class="flex items-center gap-4">
                <div class="flex flex-col gap-1 flex-1">
                  <label class="text-sm font-semibold">模式</label>
                  <SelectButton v-model="jsBody.mode" :options="jsModeOptions" option-label="label" option-value="value" :allow-empty="false" />
                </div>
                <div class="flex flex-col gap-1 flex-1" v-if="jsBody.mode === 'function'">
                  <label class="text-sm font-semibold">函数名</label>
                  <InputText v-model="jsBody.functionName" placeholder="handler" fluid />
                </div>
              </div>
              <div class="flex items-center gap-2">
                <Checkbox v-model="jsBody.allowIO" :binary="true" inputId="allowIO" />
                <label for="allowIO" class="text-sm">允许 IO / 进程 (谨慎)</label>
              </div>
              <div class="flex flex-col gap-1">
                <label class="text-sm font-semibold">代码</label>
                <Textarea v-model="jsBody.code" rows="12" fluid class="font-mono !text-xs"
                  placeholder="function handler(input){ return { ... } }  可用: input.*, config.*, api" />
              </div>
            </div>
          </template>
          <template v-else>
            <div class="text-sm text-surface-500 mb-2">工作流运行时：在画布中编辑，以 response 节点映射 outputSchema 输出。</div>
            <div class="h-[280px] border rounded overflow-hidden" style="border-color: var(--p-content-border-color);">
              <Workflow ref="workflowRef" />
            </div>
          </template>
        </Fieldset>

        <!-- inputSchema -->
        <Fieldset legend="调用参数 inputSchema">
          <div class="flex justify-end mb-2">
            <Button label="添加参数" icon="pi pi-plus" size="small" variant="outlined" @click="inputDrawerRef?.open()" />
          </div>
          <DataTable :value="form.inputSchema" v-if="form.inputSchema.length > 0" size="small">
            <Column field="field" header="字段" />
            <Column header="显示名"><template #body="s">{{ s.data.label?.value }}</template></Column>
            <Column field="type" header="类型" />
            <Column header="" style="width: 90px">
              <template #body="s">
                <div class="flex gap-1">
                  <Button icon="pi pi-file-edit" variant="text" rounded size="small" @click="inputDrawerRef?.open(s.data, s.index)" />
                  <Button icon="pi pi-trash" variant="text" severity="danger" rounded size="small" @click="form.inputSchema.splice(s.index, 1)" />
                </div>
              </template>
            </Column>
          </DataTable>
          <FieldFormDrawer ref="inputDrawerRef" header="调用参数" :add-params="(d, i) => addField(form.inputSchema, d, i)" />
        </Fieldset>

        <!-- outputSchema -->
        <Fieldset legend="返回字段 outputSchema">
          <div class="flex justify-end mb-2">
            <Button label="添加字段" icon="pi pi-plus" size="small" variant="outlined" @click="outputDrawerRef?.open()" />
          </div>
          <DataTable :value="form.outputSchema" v-if="form.outputSchema.length > 0" size="small">
            <Column field="field" header="字段" />
            <Column field="label" header="显示名" />
            <Column field="type" header="类型" />
            <Column header="" style="width: 90px">
              <template #body="s">
                <div class="flex gap-1">
                  <Button icon="pi pi-file-edit" variant="text" rounded size="small" @click="outputDrawerRef?.open(s.data, s.index)" />
                  <Button icon="pi pi-trash" variant="text" severity="danger" rounded size="small" @click="form.outputSchema.splice(s.index, 1)" />
                </div>
              </template>
            </Column>
          </DataTable>
          <OutputFieldDrawer ref="outputDrawerRef" :add-params="(d, i) => addField(form.outputSchema, d, i)" />
        </Fieldset>

        <!-- configSchema + config -->
        <Fieldset legend="配置 configSchema" class="xl:col-span-2">
          <div class="flex justify-end mb-2">
            <Button label="添加配置" icon="pi pi-plus" size="small" variant="outlined" @click="configDrawerRef?.open()" />
          </div>
          <DataTable :value="form.configSchema" v-if="form.configSchema.length > 0" size="small">
            <Column field="field" header="字段" />
            <Column header="显示名"><template #body="s">{{ s.data.label?.value }}</template></Column>
            <Column header="类型">
              <template #body="s">{{ s.data.type }}<span v-if="isSecret(s.data)" class="text-xs text-surface-400 ml-1">(密钥)</span></template>
            </Column>
            <Column header="" style="width: 90px">
              <template #body="s">
                <div class="flex gap-1">
                  <Button icon="pi pi-file-edit" variant="text" rounded size="small" @click="configDrawerRef?.open(s.data, s.index)" />
                  <Button icon="pi pi-trash" variant="text" severity="danger" rounded size="small" @click="form.configSchema.splice(s.index, 1)" />
                </div>
              </template>
            </Column>
          </DataTable>
          <FieldFormDrawer ref="configDrawerRef" header="配置字段 (密钥选 PasswordInput 类型)" :add-params="(d, i) => addField(form.configSchema, d, i)" />

          <div v-if="form.configSchema.length > 0" class="mt-4">
            <div class="text-sm font-semibold mb-2">默认配置值</div>
            <div v-for="f in form.configSchema" :key="f.field" class="flex items-center gap-2 mb-2">
              <span class="w-40 text-sm truncate">{{ f.field }}</span>
              <Password v-if="isSecret(f)" v-model="form.config[f.field]" :feedback="false" toggle-mask input-class="w-full" class="flex-1" />
              <InputText v-else v-model="form.config[f.field]" class="flex-1" />
            </div>
          </div>
        </Fieldset>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, provide, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { t } from '@/locales'
import bus from '@/bus'
import toolApi from '@/api/tool'
import Workflow from '@/workflow/index.vue'
import { baseWorkflow, WorkflowType } from '@/workflow/common/data'
import { buildToolStartFields } from '@/workflow/nodes/start-node/content/tool'
import FieldFormDrawer from './components/FieldFormDrawer.vue'
import OutputFieldDrawer from './components/OutputFieldDrawer.vue'

const route = useRoute()
const loaded = ref(false)
const saving = ref(false)
const workflowRef = ref<InstanceType<typeof Workflow>>()
const inputDrawerRef = ref<InstanceType<typeof FieldFormDrawer>>()
const configDrawerRef = ref<InstanceType<typeof FieldFormDrawer>>()
const outputDrawerRef = ref<InstanceType<typeof OutputFieldDrawer>>()

// 全局变量(暂存)真源。由宿主持有并用原始 model 可靠写入 start 节点，
// 避免开始节点面板经 reactive(model) 代理写不进 graphModel(下游读不到)。
const toolGlobalVariables = ref<any[]>([])

const runtimeOptions = [
  { label: '工作流', value: 'WORKFLOW' },
  { label: 'JS 脚本', value: 'JS' }
]
const jsModeOptions = [
  { label: '函数', value: 'function' },
  { label: '脚本', value: 'script' }
]

// 工具工作流：专属 TOOL 开始节点（节点集暂复用 processor）
// getDetails 返回响应式 form，开始节点据此反映 input/config/output
provide('WorkflowType', WorkflowType.TOOL)
provide('getDetails', () => form)
// 开始节点面板只当 UI：读取当前全局变量、回写变更；真正写模型由宿主 syncStartNodeFields 完成
provide('getToolGlobalVariables', () => toolGlobalVariables.value)
provide('setToolGlobalVariables', (list: any[]) => {
  toolGlobalVariables.value = Array.isArray(list) ? list : []
  nextTick(syncStartNodeFields)
})

const form = reactive<any>({
  name: '', label: '', description: '', icon: '', runtime: 'JS',
  inputSchema: [], outputSchema: [], configSchema: [], config: {}, body: {}
})

const jsBody = reactive({ mode: 'function', code: '', functionName: 'handler', allowIO: false })

const isSecret = (f: any) => f.secret === true || f.type === 'PasswordInput'

// 通用：新增/更新一条字段（按 field 去重）
const addField = (list: any[], data: any, index?: number) => {
  const dup = list.find((it: any, i: number) => it.field === data.field && i !== index)
  if (dup) {
    bus.emit('message:error', `字段 ${data.field} 已存在`)
    return false
  }
  if (index !== undefined) list.splice(index, 1, data)
  else list.push(data)
  return true
}

const load = async () => {
  const res = await toolApi.getResource(route.params.id as string)
  const d = res.data
  Object.assign(form, {
    name: d.name || '', label: d.label || '', description: d.description || '', icon: d.icon || '',
    runtime: d.runtime || 'JS',
    inputSchema: d.inputSchema || [], outputSchema: d.outputSchema || [], configSchema: d.configSchema || [],
    config: d.config || {}, body: d.body || {}
  })
  if (form.runtime === 'JS') {
    Object.assign(jsBody, {
      mode: form.body.mode || 'function', code: form.body.code || '',
      functionName: form.body.functionName || 'handler', allowIO: form.body.allowIO || false
    })
  }
  loaded.value = true
  if (form.runtime === 'WORKFLOW') {
    renderCanvas()
  }
}

const renderCanvas = () => {
  setTimeout(() => {
    workflowRef.value?.render(form.body?.nodes ? form.body : baseWorkflow)
    nextTick(() => {
      // 回填已保存的全局变量（存于 start 节点 nodeData.globalVariables），再统一同步
      const startNode = workflowRef.value?.getLf?.()?.graphModel?.getNodeModelById('start-node')
      const saved = startNode?.properties?.nodeData?.globalVariables
      if (Array.isArray(saved)) toolGlobalVariables.value = saved
      syncStartNodeFields()
    })
  }, 0)
}

// 权威同步：起始节点是画布虚拟渲染的组件，其 init/watch 在节点未渲染时不执行，
// 依赖它会导致 field_list/outputFieldList 写不进模型、下游引用不到。
// 这里在宿主(始终挂载)按 input/config/output 直接写 start 节点模型，与节点组件生命周期解耦。
const syncStartNodeFields = () => {
  if (form.runtime !== 'WORKFLOW') return
  const lf = workflowRef.value?.getLf?.()
  const startNode = lf?.graphModel?.getNodeModelById('start-node')
  if (!startNode) return
  const { fieldList, outputFieldList } = buildToolStartFields(form)
  const globalFieldList = toolGlobalVariables.value
    .filter((v: any) => v.name?.trim())
    .map((v: any) => ({ label: v.label || v.name, value: v.name }))
  // 宿主统一用原始 model 写入：input/output 派生字段 + 全局变量(派生 globalFieldList，
  // 并把真源 globalVariables 存进 nodeData 以随 body 持久化)。
  startNode.setProperties({
    field_list: fieldList,
    outputFieldList,
    globalFieldList,
    nodeData: { ...(startNode.properties?.nodeData || {}), globalVariables: toolGlobalVariables.value }
  })
}

// input/config/output 变化即同步进 start 节点模型（无需打开开始节点面板）
watch(
  () => [form.inputSchema, form.configSchema, form.outputSchema],
  () => nextTick(syncStartNodeFields),
  { deep: true }
)

// 切到工作流时按需渲染画布
watch(() => form.runtime, (rt) => {
  if (rt === 'WORKFLOW' && loaded.value) renderCanvas()
})

const buildBody = () => {
  if (form.runtime === 'JS') {
    return { mode: jsBody.mode, code: jsBody.code, functionName: jsBody.functionName, allowIO: jsBody.allowIO }
  }
  return workflowRef.value?.getGraphData() || form.body || {}
}

const save = async () => {
  saving.value = true
  try {
    await toolApi.edit(route.params.id as string, {
      name: form.name, label: form.label, description: form.description, icon: form.icon,
      runtime: form.runtime,
      inputSchema: form.inputSchema, outputSchema: form.outputSchema, configSchema: form.configSchema,
      config: form.config, body: buildBody()
    })
    bus.emit('message:success', t('common.saveSuccess'))
  } finally {
    saving.value = false
  }
}

watch(() => route.params.id, () => { loaded.value = false; load() })
onMounted(load)
</script>

<style lang="scss" scoped></style>
