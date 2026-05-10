<template>
  <component
    :is="impl[type]"
    :formField="formField"
    :formFieldList="formFieldList"
    :formValue="formValue"
  ></component>
</template>
<script setup lang="ts">
import type { FormField } from '@/components/dynamics-form-plus/type'
import type { VueModule } from '@/api/type/common'
import { computed } from 'vue'
const impl: Record<string, any> = Object.fromEntries(
  Object.entries(
    import.meta.glob<VueModule>('./impl/**/*.vue', {
      eager: true
    })
  ).map(([path, module]) => [path.replace(/^.*\/(.+)\.vue$/, '$1'), module.default])
)
const props = defineProps<{
  formField: FormField
  formFieldList: Array<FormField>
  formValue: Record<string, any>
}>()

const type: any = computed(() => {
  return props.formField.label?.type || 'TextLabel'
})
</script>
<style lang="scss"></style>
