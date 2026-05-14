<template>
  <Dialog
    v-model:visible="visible"
    :header="isEditing ? '编辑函数' : '添加函数'"
    :modal="false"
    :dismissable="false"
    style="width: 600px"
  >
    <Form ref="formRef" :resolver="resolver">
      <div class="flex flex-col gap-3">
        <div class="flex items-center justify-end">
          <Button
            label="解析JSON"
            icon="pi pi-code"
            size="small"
            severity="secondary"
            @click="showParse = !showParse"
          />
        </div>

        <div v-if="showParse">
          <label class="mb-1 block text-sm font-medium">粘贴函数 JSON</label>
          <Textarea
            v-model="parseJson"
            rows="6"
            class="w-full"
            placeholder='粘贴函数定义 JSON，例如:
{
  "name": "get_weather",
  "description": "获取天气",
  "parameters": {
    "type": "object",
    "properties": {
      "location": { "type": "string", "description": "城市" }
    },
    "required": ["location"]
  }
}'
          />
          <div class="flex gap-2 mt-2">
            <Button label="解析" size="small" @click="parseFromJson" />
            <small v-if="parseError" class="text-red-500 self-center">{{ parseError }}</small>
          </div>
        </div>

        <FormField v-slot="$field" name="name" class="mb-3">
          <label class="mb-1 block text-sm font-medium">函数名称</label>
          <InputText class="w-full" placeholder="例如: get_weather" />
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
            {{ $field.error?.message }}
          </Message>
        </FormField>

        <FormField v-slot="$field" name="description" class="mb-3">
          <label class="mb-1 block text-sm font-medium">函数描述</label>
          <Textarea rows="2" class="w-full" placeholder="描述函数的功能" />
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
            {{ $field.error?.message }}
          </Message>
        </FormField>

        <div>
          <div class="flex items-center justify-between mb-1">
            <label class="text-sm font-medium">参数列表</label>
            <Button
              label="添加参数"
              icon="pi pi-plus"
              size="small"
              severity="secondary"
              text
              @click="addParameter"
            />
          </div>

          <div v-for="(param, index) in parameters" :key="index" class="flex gap-2 mb-2 items-start">
            <div class="w-1/4">
              <InputText
                v-model="param.name"
                placeholder="参数名"
                class="w-full"
                :class="{ 'p-invalid': paramErrors[index]?.name }"
              />
              <small v-if="paramErrors[index]?.name" class="text-red-500 text-xs">
                {{ paramErrors[index].name }}
              </small>
            </div>
            <div class="w-1/4">
              <Select
                v-model="param.type"
                :options="paramTypes"
                option-label="label"
                option-value="value"
                placeholder="类型"
                class="w-full"
                :class="{ 'p-invalid': paramErrors[index]?.type }"
              />
              <small v-if="paramErrors[index]?.type" class="text-red-500 text-xs">
                {{ paramErrors[index].type }}
              </small>
            </div>
            <InputText
              v-model="param.description"
              placeholder="描述"
              class="flex-1"
            />
            <div class="flex items-center gap-1">
              <Checkbox v-model="param.required" :binary="true" />
              <label class="text-xs">必填</label>
            </div>
            <Button
              icon="pi pi-times"
              size="small"
              severity="danger"
              text
              @click="removeParameter(index)"
            />
          </div>
          <small v-if="paramListError" class="text-red-500">{{ paramListError }}</small>
        </div>
      </div>

    </Form>

      <div class="flex justify-end gap-2 mt-4">
        <Button label="取消" severity="secondary" @click="close" />
        <Button label="确定" @click="submit" />
      </div>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { z } from 'zod'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import type { Parameter, Tool } from './type'
import { paramTypes } from './type'
import type { FormInstance } from '@primevue/forms'
import bus from '@/bus'
import Drawer from "primevue/drawer";

const props = defineProps<{
  existingNames?: string[]
}>()

const emit = defineEmits<{
  (e: 'submit', tool: Tool): void
}>()

const visible = ref(false)
const isEditing = ref(false)
const editingName = ref('')
const formRef = ref<FormInstance>()
const parameters = ref<Parameter[]>([])

const showParse = ref(false)
const parseJson = ref('')
const parseError = ref('')

interface ParamError {
  name?: string
  type?: string
}

const paramErrors = ref<ParamError[]>([])
const paramListError = ref('')

const schema = z.object({
  name: z.string().min(1, { error: '请输入函数名称' }),
  description: z.string().min(1, { error: '请输入函数描述' })
})

const resolver = computed(() => {
  return zodResolver(
    schema.refine(
      (data) => {
        // 编辑时允许保持原名称
        if (isEditing.value && data.name === editingName.value) return true
        return !props.existingNames?.includes(data.name)
      },
      { error: '函数名称已存在', path: ['name'] }
    )
  )
})

function open(tool?: Tool) {
  isEditing.value = !!tool
  showParse.value = false
  parseJson.value = ''
  parseError.value = ''
  paramErrors.value = []
  paramListError.value = ''

  if (tool) {
    editingName.value = tool.function.name
    parameters.value = parseParameters(tool.function.parameters)
    setTimeout(() => {
      formRef.value?.setValues({
        name: tool.function.name,
        description: tool.function.description
      })
    }, 100)
  } else {
    editingName.value = ''
    parameters.value = []
    setTimeout(() => {
      formRef.value?.setValues({
        name: '',
        description: ''
      })
    }, 100)
  }

  visible.value = true
}

function close() {
  visible.value = false
}

function parseFromJson() {
  parseError.value = ''

  try {
    const parsed = JSON.parse(parseJson.value)
    const func = parsed.function || parsed

    if (!func.name) {
      parseError.value = '未找到函数名称'
      return
    }

    formRef.value?.setValues({
      name: func.name || '',
      description: func.description || ''
    })
    parameters.value = parseParameters(func.parameters)
    showParse.value = false
  } catch {
    parseError.value = 'JSON 解析失败，请检查格式'
  }
}

function parseParameters(params: any): Parameter[] {
  if (!params?.properties) return []

  const required = params.required || []
  return Object.entries(params.properties).map(([paramName, prop]: [string, any]) => ({
    name: paramName,
    type: prop.type || 'string',
    description: prop.description || '',
    required: required.includes(paramName)
  }))
}

function buildParameters(params: Parameter[]) {
  const properties: Record<string, any> = {}
  const required: string[] = []

  params.forEach((param) => {
    if (param.name) {
      properties[param.name] = {
        type: param.type,
        description: param.description
      }
      if (param.required) {
        required.push(param.name)
      }
    }
  })

  return {
    type: 'object' as const,
    properties,
    ...(required.length > 0 ? { required } : {})
  }
}

function addParameter() {
  parameters.value.push({
    name: '',
    type: 'string',
    description: '',
    required: false
  })
}

function removeParameter(index: number) {
  parameters.value.splice(index, 1)
  paramErrors.value.splice(index, 1)
}

function validateParameters(): boolean {
  paramErrors.value = []
  paramListError.value = ''

  if (parameters.value.length === 0) {
    return true // 参数列表可以为空
  }

  let isValid = true
  const nameSet = new Set<string>()

  parameters.value.forEach((param, index) => {
    const errors: ParamError = {}

    if (!param.name?.trim()) {
      errors.name = '请输入参数名'
      isValid = false
    } else if (nameSet.has(param.name)) {
      errors.name = '参数名重复'
      isValid = false
    } else {
      nameSet.add(param.name)
    }

    if (!param.type) {
      errors.type = '请选择类型'
      isValid = false
    }

    paramErrors.value[index] = errors
  })

  return isValid
}

async function submit() {
  const { errors, values } = await formRef.value!.validate()
  const paramsValid = validateParameters()

  if (Object.keys(errors).length > 0 || !paramsValid) {
    bus.emit('message:error', '请检查表单输入')
    return
  }

  const tool: Tool = {
    type: 'function',
    function: {
      name: values.name,
      description: values.description,
      parameters: buildParameters(parameters.value)
    }
  }

  emit('submit', tool)
}

defineExpose({ open, close })
</script>
