<template>
  <slot></slot>
  <table class="w-full text-sm text-left rtl:text-right text-gray-500 dark:text-gray-400">
    <thead
      class="block w-full text-xs text-gray-700 uppercase bg-gray-50 dark:bg-gray-700 dark:text-gray-400"
    >
      <tr class="flex w-full">
        <th scope="col" class="px-6 py-3 flex-1" v-for="column in columns" :key="column.uid">
          <component :is="column.headerSlot" />
        </th>
      </tr>
    </thead>

    <tbody :style="{ maxHeight: maxHeight + 'px' }" class="block overflow-y-auto">
      <tr
        v-for="(row, index) in data"
        :key="row.id"
        class="flex w-full bg-white border-b dark:bg-gray-800 dark:border-gray-700 border-gray-200 hover:bg-gray-50 dark:hover:bg-gray-600"
      >
        <td scope="row" class="px-6 py-4 flex-1" v-for="column in columns" :key="column.uid">
          <component :is="column.defaultSlot" v-bind="{ row, index }" />
        </td>
      </tr>
    </tbody>
  </table>
</template>
<script setup lang="ts">
import { ref, provide } from 'vue'
withDefaults(defineProps<{ data: Array<any>; maxHeight?: number }>(), { maxHeight: 300 })

const columns = ref<Array<any>>([])

provide('table', { columns })
</script>
<style lang="scss" scoped></style>
