<template>
  <Panel :header="t('database.connectionInfo')">
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="space-y-1">
        <div class="font-bold text-gray-600">{{ t('database.host') }}</div>
        <div>{{ row.meta.collection.host }}</div>
      </div>

      <div class="space-y-1">
        <div class="font-bold text-gray-600">{{ t('database.port') }}</div>
        <div>{{ row.meta.collection.port }}</div>
      </div>

      <div class="space-y-1">
        <div class="font-bold text-gray-600">{{ t('database.database') }}</div>
        <div>{{ row.meta.collection.database }}</div>
      </div>

      <div class="space-y-1">
        <div class="font-bold text-gray-600">{{ t('database.username') }}</div>
        <div>{{ row.meta.collection.user }}</div>
      </div>
      <div class="space-y-1">
        <div class="font-bold text-gray-600">{{ t('database.password') }}</div>
        <div>{{ row.meta.collection.password }}</div>
      </div>
    </div>
  </Panel>
  <Panel :header="t('database.poolInfo')" class="mt-4">
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="space-y-1">
        <div class="font-bold text-gray-600">{{ t('database.maxConnections') }}</div>
        <div>{{ row.meta.pool.maxSize }}</div>
      </div>
    </div>
  </Panel>
</template>
<script setup lang="ts">
import { t } from '@/locales'

defineProps<{
  row: any
}>()
</script>
<style lang="scss" scoped></style>
