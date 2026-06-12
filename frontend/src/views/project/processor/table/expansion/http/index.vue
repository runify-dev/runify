<template>
  <div class="flex flex-col gap-4">
    <!-- 参数区域 -->
    <Fieldset :legend="t('project.http.params')" :pt="{ legend: { class: 'text-sm font-medium text-surface-700' } }">
      <Form ref="formRef">
        <div class="flex flex-col gap-3">
          <FormField
            v-slot="$field"
            v-for="parameter in meta.parameters"
            :key="parameter.field"
            :name="parameter.field"
            :resolver="getResolver(parameter.field, parameter.required)"
          >
            <div class="flex flex-col gap-1.5">
              <label class="text-sm text-surface-600">
                {{ parameter.description }}
                <span v-if="parameter.required" class="text-red-500 ml-0.5">*</span>
              </label>
              <InputText type="text" fluid class="!text-sm" />
              <Message v-if="$field.invalid" severity="error" size="small" variant="simple">
                {{ $field.error?.message }}
              </Message>
            </div>
          </FormField>
        </div>
      </Form>
    </Fieldset>

    <!-- 请求体 -->
    <Fieldset
      v-if="showBody"
      :legend="t('project.http.body')"
      :pt="{ legend: { class: 'text-sm font-medium text-surface-700' } }"
    >
      <RequestBody
        ref="requestBodyRef"
        :contentType="meta.contentType"
        :requestBody="meta.requestBody"
      />
    </Fieldset>

    <!-- 执行按钮 -->
    <Button
      :label="t('project.http.execute')"
      icon="pi pi-play"
      class="w-full"
      @click="execute"
    />

    <!-- 响应结果 -->
    <Fieldset
      :legend="t('project.http.response')"
      :pt="{ legend: { class: 'text-sm font-medium text-surface-700' } }"
    >
      <div v-loading="loading" class="min-h-[100px]">
        <pre
          v-if="result"
          class="text-sm text-surface-600 bg-surface-50 p-3 rounded-lg overflow-auto max-h-[300px]"
        >{{ JSON.stringify(result, null, 2) }}</pre>
        <div v-else class="flex items-center justify-center h-[100px] text-surface-400 text-sm">
          {{ t('project.http.executePlaceholder') }}
        </div>
      </div>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import Fieldset from 'primevue/fieldset'
import RequestBody from './body/index.vue'
import { computed, ref, inject } from 'vue'
import { t } from '@/locales'
import { Form, FormField, type FormInstance } from '@primevue/forms'
import axios from 'axios'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'

const project: any = inject('project')
const props = defineProps<{
  meta: any
}>()

const requestBodyRef = ref<InstanceType<typeof RequestBody>>()
const showBody = computed(() => {
  return ['POST', 'DELETE', 'PUT'].includes(props.meta.method)
})
const formRef = ref<FormInstance>()
const result = ref<any>()
const loading = ref<boolean>(false)

const getResolver = (field: string, required: boolean) => {
  return zodResolver(
    required
      ? z.any().refine(
          (val) => {
            return val !== undefined && val !== '' && val !== null
          },
          {
            message: field + ' ' + t('project.http.requiredField')
          }
        )
      : z.any()
  )
}

const execute = () => {
  const promiseList = []
  promiseList.push(
    formRef.value ? formRef.value.validate() : Promise.resolve({ errors: [], values: [] })
  )

  promiseList.push(
    requestBodyRef.value
      ? requestBodyRef.value.getBody()
      : Promise.resolve({ errors: [], values: [] })
  )

  Promise.all(promiseList).then((values: Array<any>) => {
    const form = values[0]
    const body = values[1].values

    if (Object.keys(form.errors).length === 0) {
      const config = createRequestConfig(props.meta, project.value.path, form.values, body)
      loading.value = true
      result.value = null
      axios(config)
        .then((ok) => {
          result.value = ok.data
        })
        .catch((e) => {
          result.value = e.response?.data || e.message
        })
        .finally(() => {
          loading.value = false
        })
    }
  })
}

const createRequestConfig = (meta: any, baseUrl: string, parameters: any, body: any) => {
  const url = `${baseUrl}${processPathParams(meta.path, parameters, meta.parameters)}`
  const config: any = {
    method: meta.method.toLowerCase(),
    url
  }
  if (meta.method === 'POST' || meta.method === 'PUT') {
    if (meta.contentType === 'multipart/form-data') {
      config.data = body
      config.headers = { 'Content-Type': 'multipart/form-data' }
    } else {
      config.data = body
      config.headers = { 'Content-Type': 'application/json' }
    }
  } else if (meta.method === 'GET') {
    config.params = parameters
  }

  return config
}

const processPathParams = (path: string, formData: any, parameters?: any[]) => {
  let processedPath = path
  parameters?.forEach((param: any) => {
    if (param.location === 'path' && formData[param.field]) {
      processedPath = processedPath.replace(
        `:${param.field}`,
        encodeURIComponent(formData[param.field])
      )
    }
  })
  return processedPath
}
</script>
