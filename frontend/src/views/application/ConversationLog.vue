<template>对话日志</template>
<script setup lang="ts">
import ApplicationAPI from '@/api/application'
import { onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const folderId = computed(() => {
  const {
    params: { folderId }
  } = route as any
  return folderId
})
const applicationId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})
onMounted(() => {
  ApplicationAPI.pageConversation(folderId.value, applicationId.value, 1, 10, {}).then((ok) => {
    console.log(ok)
  })
})
</script>
<style scoped>
/* Remove this container when use*/
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
