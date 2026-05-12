<template>
  <div>
    <Fieldset legend="检索配置">
      <!-- 目录筛选 -->
      <div class="mb-3">
        <label class="mb-1 block">目录</label>
        <TreeSelect
          v-model="formData.folderIds"
          :options="folderOptions"
          placeholder="请选择目录"
          class="w-full"
          selectionMode="multiple"
          display="chip"
          fluid
        />
        <Message v-if="errors.folderIds" severity="error" size="small" variant="simple">
          {{ errors.folderIds }}
        </Message>
      </div>

      <!-- 检索文本 -->
      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>检索文本</label>
          <SelectButton
            v-model="formData.keywordLocation"
            :options="locationOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>

        <Cascader
          v-if="formData.keywordLocation === 'reference'"
          placeholder="请选择检索文本变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.keywordReference"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full"
        />
        <Message v-if="formData.keywordLocation === 'reference' && errors.keywordReference" severity="error" size="small" variant="simple">
          {{ errors.keywordReference }}
        </Message>

        <InputText
          v-if="formData.keywordLocation === 'customize'"
          v-model="formData.keyword"
          placeholder="输入检索关键词"
          class="w-full mt-1"
        />
        <Message v-if="formData.keywordLocation === 'customize' && errors.keyword" severity="error" size="small" variant="simple">
          {{ errors.keyword }}
        </Message>
      </div>

      <!-- 页码 -->
      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>页码</label>
          <SelectButton
            v-model="formData.pageNoLocation"
            :options="locationOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>

        <Cascader
          v-if="formData.pageNoLocation === 'reference'"
          placeholder="请选择页码变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.pageNoReference"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full"
        />

        <InputNumber
          v-if="formData.pageNoLocation === 'customize'"
          v-model="formData.pageNo"
          :min="1"
          placeholder="1"
          class="w-full mt-1"
        />
      </div>

      <!-- 每页条数 -->
      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>每页条数</label>
          <SelectButton
            v-model="formData.pageSizeLocation"
            :options="locationOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>

        <Cascader
          v-if="formData.pageSizeLocation === 'reference'"
          placeholder="请选择每页条数变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.pageSizeReference"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full"
        />

        <InputNumber
          v-if="formData.pageSizeLocation === 'customize'"
          v-model="formData.pageSize"
          :min="1"
          :max="100"
          placeholder="10"
          class="w-full mt-1"
        />
      </div>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, ref, reactive } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
import Cascader from '@/components/cascader/index.vue'
import { TreeCommonAPI } from '@/api/tree'
import { toTree } from '@/components/tree/index'
import { locationOptions } from './type'
import { cloneDeep } from 'lodash'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const fieldOptions = getNodeFieldOptions()

const noteTreeAPI = new TreeCommonAPI('note')
const ROOT_OPTION = { key: 'root', label: '全部笔记', icon: 'pi pi-folder' }
const folderOptions = ref<any[]>([ROOT_OPTION])

function toTreeSelectNodes(treeNodes: any[]): any[] {
  return treeNodes
    .filter((n) => n.data?.type === 'folder')
    .map((n) => {
      const children = n.children ? toTreeSelectNodes(n.children) : []
      return {
        key: n.key,
        label: n.label,
        icon: 'pi pi-folder',
        children: children.length > 0 ? children : undefined
      }
    })
}

noteTreeAPI.listTree('root').then((res) => {
  const tree = toTree(res.data || [])
  const folders = toTreeSelectNodes(tree)
  folderOptions.value = folders.length > 0 ? folders : [ROOT_OPTION]
})

function extractFolderKeys(val: any): string[] {
  if (!val) return []
  if (Array.isArray(val)) return val
  if (typeof val === 'object') return Object.keys(val)
  return []
}

function toTreeSelectValue(keys: string[] | null | undefined): Record<string, boolean> | null {
  if (!keys || keys.length === 0) return null
  const obj: Record<string, boolean> = {}
  keys.forEach((k) => (obj[k] = true))
  return obj
}

const formData = reactive({
  folderIds: null as Record<string, boolean> | null,
  keywordLocation: 'customize' as 'reference' | 'customize',
  keywordReference: [] as string[],
  keyword: '',
  pageNoLocation: 'customize' as 'reference' | 'customize',
  pageNoReference: [] as string[],
  pageNo: 1,
  pageSizeLocation: 'customize' as 'reference' | 'customize',
  pageSizeReference: [] as string[],
  pageSize: 10
})

const errors = reactive<Record<string, string>>({})

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])

  const keys = extractFolderKeys(formData.folderIds)
  if (keys.length === 0) {
    errors.folderIds = '请选择目录'
  }

  if (formData.keywordLocation === 'reference') {
    if (!formData.keywordReference || formData.keywordReference.length === 0) {
      errors.keywordReference = '请选择引用变量'
    }
  } else {
    if (!formData.keyword || formData.keyword.trim() === '') {
      errors.keyword = '请输入检索关键词'
    }
  }

  const valid = Object.keys(errors).length === 0
  return Promise.resolve({ values: cloneDeep({ ...formData }), errors: valid ? {} : errors })
}

function submit() {
  return validate().then(({ values, errors: errs }) => {
    if (Object.keys(errs).length === 0) {
      const data = { ...values, folderIds: extractFolderKeys(values.folderIds) }
      model.properties.nodeData = data
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
      folderIds: toTreeSelectValue(data.folderIds),
      keywordLocation: data.keywordLocation || 'customize',
      keywordReference: data.keywordReference || [],
      keyword: data.keyword || '',
      pageNoLocation: data.pageNoLocation || 'customize',
      pageNoReference: data.pageNoReference || [],
      pageNo: data.pageNo || 1,
      pageSizeLocation: data.pageSizeLocation || 'customize',
      pageSizeReference: data.pageSizeReference || [],
      pageSize: data.pageSize || 10
    })
  }
  model.properties.field_list = [
    { label: '结果列表', value: 'hits' },
    { label: '总数', value: 'total' },
    { label: '最高分', value: 'topScore' }
  ]
})
</script>

<style lang="scss" scoped></style>
