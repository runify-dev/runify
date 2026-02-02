<template>
  <Form ref="formRef">
    <Fieldset legend="基本设置">
      <FormField v-slot="$field" name="method" initial-value="GET" :resolver="resolvers.method">
        <SelectButton
          class="mt-2"
          option-label="label"
          option-value="value"
          :options="methodOptions"
          fluid
        />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
      <FormField
        v-slot="$field"
        name="path"
        initial-value=""
        :resolver="resolvers.path"
        class="mt-2"
      >
        <IftaLabel>
          <InputText type="text" fluid />
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
            $field.error?.message
          }}</Message>
          <label>请求地址</label>
        </IftaLabel>
      </FormField>
    </Fieldset>

    <FormField v-slot="$field: any" name="parameters" :initial-value="[]" class="mt-2">
      <Parameters
        v-bind:parameters="$field.value"
        @update:parameters="(v) => $field.onChange({ value: v })"
        :updateFieldList="updateFieldList"
      ></Parameters>
      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
  </Form>
</template>
<script setup lang="ts">
import { onMounted, ref, inject, computed, nextTick } from 'vue'
import Parameters from './parameter/index.vue'
import processorAPI from '@/api/processor'
import type { BaseNodeModel } from '@logicflow/core'
import databaseConnectionPoolAPI from '@/api/database-connection-pool'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import _ from 'lodash'
import type { FormInstance } from '@primevue/forms'
const resolvers = {
  method: zodResolver(z.string().min(1, { error: '请选择请求方式' })),
  path: zodResolver(z.string().min(1, { error: '请输入请求地址' })),

  many: zodResolver(z.boolean({ error: '请选择是否为多参数' }))
}
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const props = defineProps<{
  processor: any
}>()

const methodOptions = ref<Array<any>>([
  {
    label: 'GET',
    value: 'GET'
  },
  {
    label: 'POST',
    value: 'POST'
  },
  {
    label: 'PUT',
    value: 'PUT'
  },
  {
    label: 'DELETE',
    value: 'DELETE'
  }
])

const formRef = ref<FormInstance>()
const validate = () => {
  return formRef.value?.validate()
}
const submit = () => {
  return validate()
    ?.then(({ values, errors }) => {
      if (Object.keys(errors).length == 0) {
        return processorAPI
          .editProcessor(props.processor.projectId, props.processor.id, {
            meta: values
          })
          .then(() => {
            return Promise.resolve(values)
          })
      }
      return Promise.reject(errors)
    })
    .then((values) => {
      model.properties.nodeData = {
        meta: _.cloneDeep(values),
        protocol: props.processor.protocol
      }
    })
}
const databaseCollectionPoolList = ref<Array<any>>()
const getDatabasePool = () => {
  if (databaseCollectionPoolList.value) {
    return Promise.resolve(
      databaseCollectionPoolList.value.map((item: any) => {
        return { label: item.name, value: item.id }
      })
    )
  }
  return databaseConnectionPoolAPI.query(props.processor.projectId, {}).then((ok) => {
    databaseCollectionPoolList.value = ok.data
    return ok.data.map((item: any) => {
      return { label: item.name, value: item.id }
    })
  })
}

const parameters = computed(() => {
  return formRef.value?.getFieldState('parameters')?.value || []
})
const updateFieldList = () => {
  getDatabasePool().then((ok) => {
    console.log(ok)
    model.properties.field_list = [
      ...parameters.value.map((item: any) => ({
        label: item.description,
        value: item.field
      })),
      {
        label: '连接池',
        value: 'pools',
        children: ok
      }
    ]
  })
}
onMounted(() => {
  nextTick(() => {
    formRef.value?.setValues(JSON.parse(JSON.stringify(model.properties.nodeData.meta)))
  })
  updateFieldList()
})
defineExpose({ validate, submit })
</script>
<style lang="scss"></style>
