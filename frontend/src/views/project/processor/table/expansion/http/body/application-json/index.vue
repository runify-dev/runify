<template>
  <Form ref="fromRef">
    <Tabs value="value" scrollable>
      <TabList>
        <Tab value="value">Edit Value</Tab>
        <Tab value="schema">Schema</Tab>
      </TabList>
      <TabPanels>
        <TabPanel value="schema">
          <TreeTable :value="treeData" :expandedKeys="expandedKeys" class="p-datatable-sm">
            <Column field="field" header="Field" expander style="width: 30%">
              <template #body="slotProps">
                <div class="field-cell">
                  <span class="field-name p-text-bold">{{ slotProps.node.data.field }}</span>
                  <Tag
                    v-if="slotProps.node.data.required"
                    value="Required"
                    severity="danger"
                    class="p-ml-2"
                    size="small"
                  />
                </div>
              </template>
            </Column>

            <Column field="type" header="Type" style="width: 15%">
              <template #body="slotProps">
                <Chip
                  :label="slotProps.node.data.type"
                  :class="getTypeClass(slotProps.node.data.type)"
                  size="small"
                />
              </template>
            </Column>

            <Column field="description" header="Description" style="width: 30%">
              <template #body="slotProps">
                <span class="description-text">{{ slotProps.node.data.description }}</span>
              </template>
            </Column>

            <Column field="defaultValue" header="Default Value" style="width: 25%">
              <template #body="slotProps">
                <div
                  v-if="slotProps.node.data.defaultValue !== undefined"
                  class="default-value-cell"
                >
                  <Tag
                    v-if="slotProps.node.data.type === 'boolean'"
                    :value="slotProps.node.data.defaultValue ? 'true' : 'false'"
                    severity="success"
                    size="small"
                  />
                  <span v-else-if="slotProps.node.data.type === 'string'" class="string-value">{{
                    slotProps.node.data.defaultValue
                  }}</span>
                  <span v-else class="number-value">{{ slotProps.node.data.defaultValue }}</span>
                </div>
                <span v-else class="p-text-muted">-</span>
              </template>
            </Column>
          </TreeTable>
        </TabPanel>
        <TabPanel value="value">
          <FormField v-slot="$field: any" name="body">
            <CodeEditor
              lang="JSON"
              :title="t('project.http.body')"
              v-bind:modelValue="$field.value"
              @update:modelValue="
                (v: string) => {
                  try {
                    $field.onChange({ value: v })
                  } catch (e) {}
                }
              "
            >
            </CodeEditor>
          </FormField>
        </TabPanel>
      </TabPanels>
    </Tabs>
  </Form>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { t } from '@/locales'
import CodeEditor from '@/components/code-editor/index.vue'
import type { FormInstance } from '@primevue/forms'
const props = defineProps<{ requestBody: Array<any> }>()
const expandedKeys = ref([])
const fromRef = ref<FormInstance>()
const generateValueByType = (type: string) => {
  const typeMap: any = {
    string: '',
    integer: 0,
    number: 0,
    float: 0.0,
    boolean: false,
    array: [],
    object: {},
    null: null
  }

  return typeMap[type.toLowerCase()] !== undefined ? typeMap[type.toLowerCase()] : ''
}
function getTypeClass(type: string) {
  const classMap: any = {
    string: 'type-string',
    integer: 'type-number',
    number: 'type-number',
    float: 'type-number',
    boolean: 'type-boolean',
    object: 'type-object',
    array: 'type-array'
  }
  return classMap[type] || ''
}
const treeData = computed(() => {
  return convertToTreeNodes(props.requestBody)
})
function convertToTreeNodes(fields: Array<any>, parentKey: string = '') {
  return fields.map((field, index) => {
    const key = parentKey ? `${parentKey}_${field.field}_${index}` : `${field.field}_${index}`
    const node: any = {
      key,
      data: {
        field: field.field,
        type: field.type,
        description: field.description,
        required: field.required,
        defaultValue: field.defaultValue
      },
      children: field.children ? convertToTreeNodes(field.children, key) : null
    }
    return node
  })
}
const generateJsonFromRequestBody = (requestBody: Array<any>) => {
  const result: any = {}
  requestBody.forEach((item) => {
    const { field, type, defaultValue, children, required } = item
    if (type === 'object' && children && Array.isArray(children)) {
      result[field] = generateJsonFromRequestBody(children)
    } else if (defaultValue !== undefined) {
      result[field] = defaultValue
    } else if (required) {
      result[field] = generateValueByType(type)
    }
  })

  return result
}
const getBody = () => {
  return fromRef.value?.validate().then(({ errors, values }) => {
    console.log(values)
    if (Object.keys(errors).length == 0) {
      return Promise.resolve({ errors, values: JSON.parse(values.body) })
    }
    return Promise.reject({ errors, values })
  })
}

defineExpose({ getBody })
onMounted(() => {
  fromRef.value?.setFieldValue(
    'body',
    JSON.stringify(generateJsonFromRequestBody(props.requestBody), null, 4)
  )
})
</script>
<style lang="scss" scoped></style>
