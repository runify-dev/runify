<template>
  <div>
    <Fieldset legend="基本信息">
      <!-- 源变量 -->
      <div class="mb-3">
        <label class="mb-2 block">源变量</label>
        <Cascader
          placeholder="请选择要提取的变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.sourceReference"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full"
        />
        <Message v-if="errors.sourceReference" severity="error" size="small" variant="simple">
          {{ errors.sourceReference }}
        </Message>
      </div>
    </Fieldset>

    <Fieldset legend="提取规则">
      <div v-for="(rule, i) in formData.rules" :key="i" class="rule-item">
        <div class="rule-header">
          <span class="rule-index">#{{ i + 1 }}</span>
          <Button icon="pi pi-trash" severity="danger" text size="small" @click="removeRule(i)" />
        </div>
        <div class="rule-fields">
          <div class="rule-field">
            <label>字段名</label>
            <InputText v-model="rule.name" placeholder="如 userName" class="w-full" size="small" />
          </div>
          <div class="rule-field">
            <label>描述</label>
            <InputText v-model="rule.description" placeholder="如 用户名称" class="w-full" size="small" />
          </div>
          <div class="rule-field full">
            <label>JSONPath 表达式</label>
            <InputText v-model="rule.path" placeholder="如 $.name 或 $.items[0].id" class="w-full" size="small" />
          </div>
        </div>
      </div>

      <div v-if="errors.rules" class="mt-1">
        <Message severity="error" size="small" variant="simple">{{ errors.rules }}</Message>
      </div>

      <Button label="添加规则" icon="pi pi-plus" text size="small" class="mt-2" @click="addRule" />
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { ref, inject, onMounted, reactive, watch } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { cloneDeep } from 'lodash'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const fieldOptions = getNodeFieldOptions()

interface Rule {
  name: string
  description: string
  path: string
}

const formData = reactive({
  sourceReference: [] as string[],
  rules: [] as Rule[]
})

const errors = reactive<Record<string, string>>({})

function addRule() {
  formData.rules.push({ name: '', description: '', path: '' })
}

function removeRule(index: number) {
  formData.rules.splice(index, 1)
}

function syncFieldList() {
  model.properties.field_list = formData.rules
    .filter((r) => r.name && r.name.trim())
    .map((r) => ({
      label: r.description || r.name,
      value: r.name
    }))
}

watch(() => formData.rules, syncFieldList, { deep: true })

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])

  if (!formData.sourceReference || formData.sourceReference.length === 0) {
    errors.sourceReference = '请选择源变量'
  }

  if (!formData.rules || formData.rules.length === 0) {
    errors.rules = '请添加至少一条提取规则'
  } else {
    for (let i = 0; i < formData.rules.length; i++) {
      const rule = formData.rules[i]
      if (!rule.name || rule.name.trim() === '') {
        errors[`rule_${i}_name`] = '字段名不能为空'
        break
      }
      if (!rule.path || rule.path.trim() === '') {
        errors[`rule_${i}_path`] = 'JSONPath 不能为空'
        break
      }
    }
  }

  const valid = Object.keys(errors).length === 0
  const values = cloneDeep({ ...formData })
  return Promise.resolve({ values, errors: valid ? {} : errors })
}

function submit() {
  return validate().then(({ values, errors: errs }) => {
    if (Object.keys(errs).length === 0) {
      model.properties.nodeData = values
      return {} as Record<string, string>
    }
    return errs
  })
}

defineExpose({ validate, submit })

onMounted(() => {
  if (model.properties.nodeData) {
    const data = cloneDeep(model.properties.nodeData)
    Object.assign(formData, {
      sourceReference: data.sourceReference || [],
      rules: data.rules || []
    })
  } else {
    model.properties.nodeData = {
      sourceReference: [],
      rules: []
    }
  }
  syncFieldList()
})
</script>

<style lang="scss" scoped>
.rule-item {
  border: 1px solid var(--p-content-border-color);
  border-radius: 6px;
  padding: 8px;
  margin-bottom: 8px;
}

.rule-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.rule-index {
  font-size: 12px;
  font-weight: 600;
  color: var(--p-text-muted-color);
}

.rule-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}

.rule-field {
  display: flex;
  flex-direction: column;
  gap: 2px;

  label {
    font-size: 11px;
    color: var(--p-text-muted-color);
  }

  &.full {
    grid-column: 1 / -1;
  }
}
</style>
