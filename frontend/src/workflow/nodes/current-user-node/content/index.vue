<template>
  <div>
    <Fieldset legend="会话">
      <div class="mb-3">
        <label>会话缓存连接</label>
        <Select
          v-model="formData.sessionCacheId"
          placeholder="请选择缓存连接"
          :options="dataSourceList"
          optionLabel="label"
          option-value="value"
          class="w-full mt-2"
        />
        <Message v-if="errors.sessionCacheId" severity="error" size="small" variant="simple">
          {{ errors.sessionCacheId }}
        </Message>
      </div>

      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>凭证位置</label>
          <SelectButton
            v-model="formData.credentialLocation"
            :options="credentialLocationOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>
        <InputText
          v-model="formData.credentialField"
          placeholder="凭证字段名,如 Authorization / token"
          class="w-full"
        />
        <Message v-if="errors.credentialField" severity="error" size="small" variant="simple">
          {{ errors.credentialField }}
        </Message>
      </div>

      <div class="mb-3">
        <label>凭证前缀（可选）</label>
        <InputText
          v-model="formData.credentialPrefix"
          placeholder="取值后剥离的前缀,如 Bearer "
          class="w-full mt-2"
        />
      </div>

      <div class="mb-3">
        <label>缓存 Key 前缀（可选,需与登录侧写入规则一致）</label>
        <InputText v-model="formData.keyPrefix" placeholder="如 session:" class="w-full mt-2" />
      </div>

      <div class="mb-3">
        <label>用户标识字段（用户对象中,供角色/权限查缓存）</label>
        <InputText v-model="formData.userIdField" placeholder="默认 id" class="w-full mt-2" />
      </div>
    </Fieldset>

    <Fieldset v-for="seg in segments" :key="seg.key" :legend="seg.legend">
      <div class="mb-3 flex items-center justify-between">
        <label>{{ seg.enableLabel }}</label>
        <ToggleSwitch v-model="formData[seg.key].enabled" />
      </div>

      <template v-if="formData[seg.key].enabled">
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>来源</label>
            <SelectButton
              v-model="formData[seg.key].source"
              :options="segmentSourceOptions"
              option-label="label"
              option-value="value"
              size="small"
            />
          </div>
        </div>

        <div v-if="formData[seg.key].source === 'inline'" class="mb-3">
          <label>用户对象中的字段名</label>
          <InputText
            v-model="formData[seg.key].field"
            :placeholder="`默认 ${seg.key}`"
            class="w-full mt-2"
          />
          <Message v-if="errors[`${seg.key}.field`]" severity="error" size="small" variant="simple">
            {{ errors[`${seg.key}.field`] }}
          </Message>
        </div>

        <template v-else>
          <div class="mb-3">
            <label>缓存连接（按用户标识查询,可单独更新）</label>
            <Select
              v-model="formData[seg.key].cacheId"
              placeholder="请选择缓存连接"
              :options="dataSourceList"
              optionLabel="label"
              option-value="value"
              class="w-full mt-2"
            />
            <Message
              v-if="errors[`${seg.key}.cacheId`]"
              severity="error"
              size="small"
              variant="simple"
            >
              {{ errors[`${seg.key}.cacheId`] }}
            </Message>
          </div>
          <div class="mb-3">
            <label>缓存 Key 前缀（可选）</label>
            <InputText
              v-model="formData[seg.key].keyPrefix"
              :placeholder="seg.keyPrefixPlaceholder"
              class="w-full mt-2"
            />
          </div>
          <div class="mb-3">
            <label>值字段（可选,留空表示整个缓存值）</label>
            <InputText
              v-model="formData[seg.key].valueField"
              :placeholder="`如 ${seg.key}`"
              class="w-full mt-2"
            />
          </div>
        </template>
      </template>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { ref, inject, onMounted, reactive } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
import { credentialLocationOptions, segmentSourceOptions } from './type'
import { applyFieldList } from './index'
import databaseConnectionPoolAPI from '@/api/database-connection-pool.ts'
import { cloneDeep } from 'lodash'
import { ROOT_FOLDER_ID } from '@/constants/common.ts'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const dataSourceList = ref<any>([])

const segments = [
  {
    key: 'roles' as const,
    legend: '角色',
    enableLabel: '获取角色',
    keyPrefixPlaceholder: '如 roles:'
  },
  {
    key: 'permissions' as const,
    legend: '权限',
    enableLabel: '获取权限',
    keyPrefixPlaceholder: '如 perm:'
  }
]

const defaultSegment = (field: string) => ({
  enabled: false,
  source: 'inline' as 'inline' | 'cache',
  field,
  cacheId: '',
  keyPrefix: '',
  valueField: ''
})

const defaultFormData = () => ({
  sessionCacheId: '',
  credentialLocation: 'header' as 'header' | 'cookie' | 'query',
  credentialField: '',
  credentialPrefix: '',
  keyPrefix: '',
  userIdField: 'id',
  roles: defaultSegment('roles'),
  permissions: defaultSegment('permissions')
})

databaseConnectionPoolAPI.listResource(ROOT_FOLDER_ID).then((ok) => {
  dataSourceList.value = ok.data
    .filter((item: any) => item.dataSourceType === 'CACHE')
    .map((item: any) => ({ label: item.name, value: item.id }))
})

const formData = reactive<Record<string, any>>(defaultFormData())

const errors = reactive<Record<string, string>>({})

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])

  if (!formData.sessionCacheId) {
    errors.sessionCacheId = '请选择会话缓存连接'
  }

  if (!formData.credentialField || formData.credentialField.trim() === '') {
    errors.credentialField = '请输入凭证字段名'
  }

  for (const seg of segments) {
    const segment = formData[seg.key]
    if (!segment.enabled) continue
    if (segment.source === 'cache') {
      if (!segment.cacheId) {
        errors[`${seg.key}.cacheId`] = `请选择${seg.legend}缓存连接`
      }
    } else if (!segment.field || segment.field.trim() === '') {
      errors[`${seg.key}.field`] = `请输入${seg.legend}在用户对象中的字段名`
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
      applyFieldList(model.properties)
      return {} as Record<string, string>
    }
    return errs
  })
}

defineExpose({ validate, submit })

onMounted(() => {
  if (model.properties.nodeData) {
    const data = cloneDeep(model.properties.nodeData)
    const defaults = defaultFormData()
    Object.assign(formData, {
      ...defaults,
      ...data,
      roles: { ...defaults.roles, ...(data.roles ?? {}) },
      permissions: { ...defaults.permissions, ...(data.permissions ?? {}) }
    })
  } else {
    model.properties.nodeData = defaultFormData()
  }
})
</script>

<style lang="scss" scoped></style>
