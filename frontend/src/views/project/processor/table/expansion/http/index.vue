<template>
  <div>
    <Fieldset legend="参数">
      <Form ref="formRef">
        <FormField
          v-slot="$field"
          v-for="parameter in meta.parameters"
          :key="parameter.field"
          :name="parameter.field"
          :resolver="getResolver(parameter.field, parameter.required)"
        >
          <div class="gap-2 flex">
            <label class="mb-2"> {{ parameter.description }}</label>
            <span v-if="parameter.required" class="text-red-500">*</span>
          </div>

          <InputText type="text" fluid />
          <Message v-if="$field.invalid" severity="error" size="small" variant="simple">{{
            $field.error?.message
          }}</Message>
        </FormField>
      </Form>
    </Fieldset>
    <Fieldset v-if="showBody" legend="请求体"
      ><RequestBody
        ref="requestBodyRef"
        :contentType="meta.contentType"
        :requestBody="meta.requestBody"
      ></RequestBody>
    </Fieldset>
    <Button
      type="submit"
      severity="secondary"
      label="execute"
      class="w-full mt-4"
      @click="execute"
    />
    <Fieldset legend="响应结果">
      <div v-loading="loading">
        {{ result }}
      </div>
    </Fieldset>
  </div>
</template>
<script setup lang="ts">
import Fieldset from 'primevue/fieldset'
import RequestBody from './body/index.vue'
import { computed, ref, inject } from 'vue'
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
            message: field + ' ' + '此项必填'
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
    console.log(values)
    const body = values[1].values

    if (Object.keys(form.errors).length == 0) {
      const config = createRequestConfig(props.meta, project.value.path, form.values, body)
      loading.value = true
      axios(config)
        .then((ok) => {
          result.value = ok.data
        })
        .catch((e) => {
          result.value = e
        })
        .finally(() => {
          loading.value = false
        })
    }
  })
}

// 3. 创建请求配置
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
<style lang="scss" scoped></style>
