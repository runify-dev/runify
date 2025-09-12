<template>
  <div
    class="p-5 text-lg font-semibold text-left rtl:text-right text-gray-900 bg-white dark:text-white dark:bg-gray-800"
  >
    <nav
      class="flex items-center flex-column flex-wrap md:flex-row justify-between pt-4"
      aria-label="Table navigation"
    >
      <span
        class="text-sm font-normal text-gray-500 dark:text-gray-400 mb-4 md:mb-0 block w-full md:inline md:w-auto"
      >
        显示
        <span class="font-semibold text-gray-900 dark:text-white"
          >{{ (currentPage - 1) * pageSize + 1 }}-{{
            Math.min(currentPage * pageSize, total)
          }}</span
        >
        共
        <span class="font-semibold text-gray-900 dark:text-white">{{ total }}</span>
      </span>

      <ul class="inline-flex -space-x-px rtl:space-x-reverse text-sm h-8">
        <li>
          <button
            :disabled="currentPage <= 1"
            @click="emit('update:currentPage', currentPage - 1)"
            class="flex items-center justify-center px-3 h-8 ms-0 leading-tight text-gray-500 bg-white border border-gray-300 rounded-s-lg hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white"
          >
            上一页
          </button>
        </li>
        <li v-for="p in pages" :key="p">
          <button
            @click="emit('update:currentPage', p)"
            :class="[
              'flex items-center justify-center px-3 h-8 leading-tight border',
              p === currentPage
                ? 'text-blue-600 border-blue-300 bg-blue-50 dark:bg-gray-700 dark:text-white'
                : 'text-gray-500 bg-white border-gray-300 hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white'
            ]"
          >
            {{ p }}
          </button>
        </li>
        <li>
          <button
            :disabled="currentPage >= totalPage"
            @click="emit('update:currentPage', currentPage + 1)"
            class="flex items-center justify-center px-3 h-8 leading-tight text-gray-500 bg-white border border-gray-300 rounded-e-lg hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white"
          >
            下一页
          </button>
        </li>
      </ul>
    </nav>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{
  pageSize: number
  currentPage: number
  total: number
  pagerCount: number
}>()
const emit = defineEmits(['update:currentPage'])

const totalPage = computed(() => Math.ceil(props.total / (props.pageSize || 1)))

const pages = computed(() => Array.from({ length: totalPage.value }, (_, i) => i + 1))
</script>
<style lang="scss" scoped></style>
