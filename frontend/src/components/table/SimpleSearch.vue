<template>
  <div
    class="flex justify-between content-center p-5 text-lg font-semibold text-left rtl:text-right text-gray-900 bg-white dark:text-white dark:bg-gray-800"
  >
    <div class="pb-4 bg-white dark:bg-gray-900">
      <label for="table-search" class="sr-only">Search</label>
      <div class="relative mt-1">
        <div
          class="absolute inset-y-0 rtl:inset-r-0 start-0 flex items-center ps-3 pointer-events-none"
        >
          <svg
            class="w-4 h-4 text-gray-500 dark:text-gray-400"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 20 20"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="m19 19-4-4m0-7A7 7 0 1 1 1 8a7 7 0 0 1 14 0Z"
            />
          </svg>
        </div>
        <input
          type="text"
          id="table-search"
          class="block pt-2 ps-10 text-sm text-gray-900 border border-gray-300 rounded-lg w-80 bg-gray-50 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500"
          placeholder="搜索"
          v-model="keyword"
        />
      </div>
    </div>
    <slot></slot>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ modelValue?: string }>()
const emit = defineEmits(['update:modelValue'])
const keyword = computed({
  get: () => {
    return props.modelValue
  },
  set: (_v) => {
    emit('update:modelValue', _v)
  }
})
</script>
<style lang="scss" scoped></style>
