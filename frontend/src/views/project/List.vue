<template>
  <div>
    <div class="card" style="height: calc(100vh - 8rem)">
      <Toolbar
        :pt="{
          root: {
            style: {
              border: 0
            }
          }
        }"
      >
        <template #start> </template>
        <template #center>
          <InputGroup>
            <InputText placeholder="搜索项目" />
            <InputGroupAddon>
              <Button icon="pi pi-search" severity="secondary" variant="text" />
            </InputGroupAddon>
          </InputGroup>
        </template>
        <template #end> <Button @click="openCreateNoteDialog" label="新建笔记"></Button></template>
      </Toolbar>
      <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 mt-5">
        <template v-for="item in nodeList" :key="item.id">
          <Card style="height: 100px">
            <template #title>{{ item.name }}</template>
            <template #content>
              <p class="m-0">
                {{ item.desc }}
              </p>
            </template>
          </Card>
        </template>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed, onMounted, ref, watch } from 'vue'
import { type Node } from '@/api/type/node'
import { useRouter } from 'vue-router'
import bus from '@/bus/index'
import { TreeCommonAPI } from '@/api/tree'
const treeCommonAPI = new TreeCommonAPI('project')
const nodeList = ref<Array<Node>>([])
const folder = ref<Node>()
const route = useRoute()
const searchText = ref<string>('')
const openCreateNoteDialog = () => {
  bus.emit('open:create:note:dialog', folderId.value)
}
const lisResource = () => {
  treeCommonAPI.listResource(folderId.value).then((ok) => {
    nodeList.value = ok.data
  })
}

const folderId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})

const forderInfo = () => {
  if (!['root', 'shar', 'share'].includes(folderId.value)) {
    treeCommonAPI.getFolder(folderId.value).then((ok) => {
      folder.value = ok.data
    })
  }
}

watch(folderId, () => {
  lisResource()
  forderInfo()
})

onMounted(() => {
  forderInfo()
  lisResource()
})
</script>
<style lang="scss" scoped>
:deep(.el-skeleton__p) {
  height: 6px;
  margin-top: 0px;
}

:deep(.md-editor-preview) {
  font-size: 8px;
}
</style>
