<template>
  <slot></slot>
  <table class="w-full text-sm text-left rtl:text-right text-gray-500 dark:text-gray-400">
    <thead class="text-xs text-gray-700 uppercase bg-gray-50 dark:bg-gray-700 dark:text-gray-400">
      <tr>
        <th scope="col" class="px-6 py-3" v-for="column in columns" :key="column.uid">
          <component :is="column.headerSlot" />
        </th>
      </tr>
    </thead>

    <tbody>
      <tr
        v-for="(row, index) in data"
        :key="row.id"
        class="bg-white border-b dark:bg-gray-800 dark:border-gray-700 border-gray-200 hover:bg-gray-50 dark:hover:bg-gray-600"
      >
        <td scope="row" class="px-6 py-4" v-for="column in columns" :key="column.uid">
          <component :is="column.defaultSlot" v-bind="{ row, index }" />
        </td>
      </tr>
    </tbody>
  </table>
</template>
<script setup lang="ts">
import { ref, provide } from 'vue'
defineProps<{ data: Array<any> }>()
const columns = ref<Array<any>>([])

provide('table', { columns })
</script>
<style lang="scss" scoped>
.component-title {
  width: 100%;
  position: absolute;
  z-index: 999;
  top: 30px;
  left: 0;
  padding: 0;
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: #888;
  text-align: center;
}

.menu-container {
  position: relative;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  padding: 2px;
}

.indicator {
  content: '';
  width: var(--label-width, 100px);
  height: 28px;
  background: #ffffff;
  position: absolute;
  top: 2px;
  left: 2px;
  z-index: 9;
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  box-shadow:
    0px 3px 8px rgba(0, 0, 0, 0.12),
    0px 3px 1px rgba(0, 0, 0, 0.04);
  border-radius: 7px;
  transition: all 0.2s ease-out;
}

.tab {
  width: var(--label-width, 100px);
  height: 28px;
  position: absolute;
  z-index: 99;
  outline: none;
  opacity: 0;
}

.tab_label {
  width: var(--label-width, 100px);

  height: 28px;

  position: relative;
  z-index: 999;

  display: flex;
  align-items: center;
  justify-content: center;

  border: 0;

  font-size: 0.75rem;
  opacity: 0.6;

  cursor: pointer;
}

.tab--1:checked ~ .indicator {
  left: 2px;
}

.tab--2:checked ~ .indicator {
  left: calc(var(--label-width, 100px) + 2px);
}

.tab--3:checked ~ .indicator {
  left: calc(var(--label-width, 100px) * 2 + 2px);
}
</style>
