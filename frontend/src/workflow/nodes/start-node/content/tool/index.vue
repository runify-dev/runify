<template>
  <div>
    <div class="text-xs text-surface-400 mb-2">输入 / 配置 / 输出在「工具设置」中编辑，此处仅展示并供画布引用。</div>

    <!-- 输入参数：来自 inputSchema，下游以 [start-node, 变量名] 引用 -->
    <Fieldset legend="输入参数 (inputSchema)">
      <DataTable v-if="inputFields.length" :value="inputFields" size="small">
        <Column header="字段" field="value" />
        <Column header="显示名" field="label" />
      </DataTable>
      <div v-else class="text-center text-slate-400 py-3">暂无输入参数</div>
    </Fieldset>

    <!-- 配置：来自 configSchema，下游以 [start-node, config, 字段] 引用 -->
    <Fieldset legend="配置 (configSchema)" class="mt-3" v-if="configFields.length">
      <DataTable :value="configFields" size="small">
        <Column header="字段" field="value" />
        <Column header="显示名" field="label" />
      </DataTable>
    </Fieldset>

    <!-- 内置：调用方上下文，运行时由调用方(智能体/处理器)注入，可引用 start-node.caller.* -->
    <Fieldset legend="调用方 (caller · 内置)" class="mt-3">
      <div class="text-xs text-surface-500">
        运行时由调用方注入，画布引用 <code>start-node.caller.type</code>(chat/processor) 等，用于按调用方分支。
      </div>
    </Fieldset>

    <!-- 输出变量：来自 outputSchema，用「变量赋值」写入「参数输出」output.<字段>，结束即工具返回 -->
    <Fieldset legend="输出变量 (outputSchema)" class="mt-3">
      <div class="text-xs text-surface-500 mb-2">
        画布用「变量赋值」写入「参数输出」<code>output.&lt;字段&gt;</code>，结束时按此作为工具返回值（与自由暂存 <code>global</code> 隔离）。
      </div>
      <DataTable v-if="outputFields.length" :value="outputFields" size="small">
        <Column header="字段" field="value" />
        <Column header="显示名" field="label" />
      </DataTable>
      <div v-else class="text-center text-slate-400 py-3">
        暂无输出变量，在工具设置中声明
      </div>
    </Fieldset>

    <!-- 全局变量(暂存)：声明后画布用「变量赋值」写入 global.<字段> 作中间暂存，与对外 output 隔离 -->
    <Fieldset legend="全局变量 (global · 暂存)" class="mt-3">
      <div class="text-xs text-surface-500 mb-2">
        工作流内部中间暂存变量。声明后画布用「变量赋值」写入 <code>global.&lt;字段&gt;</code>，与对外「参数输出」<code>output</code> 隔离。
      </div>
      <div class="flex justify-end mb-2">
        <Button label="添加变量" icon="pi pi-plus" size="small" severity="secondary" @click="openGlobalDialog()" />
      </div>
      <DataTable v-if="globalVariables.length" :value="globalVariables" size="small">
        <Column header="变量名" field="name" />
        <Column header="显示名" field="label" />
        <Column header="数据类型">
          <template #body="{ data }">{{ getDataTypeLabel(data.dataType) }}</template>
        </Column>
        <Column header="操作" style="width: 90px">
          <template #body="{ data, index }">
            <Button icon="pi pi-pencil" size="small" severity="secondary" text @click="openGlobalDialog(data, index)" />
            <Button icon="pi pi-trash" size="small" severity="danger" text @click="removeGlobalVariable(index)" />
          </template>
        </Column>
      </DataTable>
      <div v-else class="text-center text-slate-400 py-3">暂无全局变量，点击上方按钮添加</div>
    </Fieldset>

    <GlobalVariableDialog ref="globalDialogRef" :existing-names="globalExistingNames" @submit="onGlobalDialogSubmit" />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, onMounted } from 'vue'
import GlobalVariableDialog from '../chat/GlobalVariableDialog.vue'
import type { GlobalVariable } from '../chat/type'
import { dataTypeOptions } from '../chat/type'

const props = defineProps<{ details: any }>()

const labelOf = (f: any) => {
  if (f.label && typeof f.label === 'object') return f.label.value || f.field
  return f.label || f.field
}
const toField = (f: any) => ({ label: labelOf(f), value: f.field })

const inputFields = computed(() => (props.details?.inputSchema || []).filter((f: any) => f.field).map(toField))
const configFields = computed(() => (props.details?.configSchema || []).filter((f: any) => f.field).map(toField))
const outputFields = computed(() => (props.details?.outputSchema || []).filter((f: any) => f.field).map(toField))

// ===== 全局变量(暂存)：纯 UI。真源与模型写入由宿主(Details.vue)持有，
// 面板只读取当前值、回写变更，避免经 reactive(model) 写不进画布模型。 =====
const getToolGlobalVariables = inject('getToolGlobalVariables', () => []) as any
const setToolGlobalVariables = inject('setToolGlobalVariables', (_: any) => {}) as any

const globalVariables = ref<GlobalVariable[]>([])
const globalDialogRef = ref()
const globalEditingIndex = ref(-1)
const globalExistingNames = computed(() => globalVariables.value.map(v => v.name).filter(Boolean))

function getDataTypeLabel(dataType?: string) {
  return dataTypeOptions.find(o => o.value === dataType)?.label || '字符串'
}
function commit() {
  setToolGlobalVariables(globalVariables.value.map(v => ({ ...v })))
}
function openGlobalDialog(variable?: GlobalVariable, index?: number) {
  globalEditingIndex.value = index ?? -1
  globalDialogRef.value?.open(variable)
}
function onGlobalDialogSubmit(variable: GlobalVariable) {
  const list = [...globalVariables.value]
  if (globalEditingIndex.value >= 0) list[globalEditingIndex.value] = variable
  else list.push(variable)
  globalVariables.value = list
  globalEditingIndex.value = -1
  globalDialogRef.value?.close()
  commit()
}
function removeGlobalVariable(index: number) {
  globalVariables.value.splice(index, 1)
  commit()
}

const validate = () => Promise.resolve({ values: {}, errors: {} })
const submit = () => {
  commit()
  return Promise.resolve({})
}

defineExpose({ validate, submit })

onMounted(() => {
  const cur = getToolGlobalVariables()
  globalVariables.value = Array.isArray(cur) ? cur.map((v: any) => ({ ...v })) : []
})
</script>

<style lang="scss" scoped></style>
