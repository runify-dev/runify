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
            <InputText placeholder="搜索应用" />
            <InputGroupAddon>
              <Button icon="pi pi-search" severity="secondary" variant="text" />
            </InputGroupAddon>
          </InputGroup>
        </template>
        <template #end> <Button @click="openCreateApplication" label="新建应用"></Button></template>
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
import CardBox from '@/components/card-box/index.vue'
import { computed, onMounted, ref, watch } from 'vue'
import { type Node } from '@/api/type/node'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import bus from '@/bus/index'
import { TreeCommonAPI } from '@/api/tree'
const treeCommonAPI = new TreeCommonAPI('model')
const router = useRouter()
const nodeList = ref<Array<Node>>([])
const folder = ref<Node>()
const route = useRoute()
const searchText = ref<string>('')
const openCreateApplication = () => {
  bus.emit('open:create:application:dialog', folderId.value)
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
.responsive-row {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(1, 1fr); /* 默认1列 */
}

/* 响应式断点 */
@media (min-width: 640px) {
  .responsive-row {
    grid-template-columns: repeat(2, 1fr); /* 小屏幕：2列 */
  }
}

@media (min-width: 768px) {
  .responsive-row {
    grid-template-columns: repeat(3, 1fr); /* 中屏幕：3列 */
  }
}

@media (min-width: 1024px) {
  .responsive-row {
    grid-template-columns: repeat(4, 1fr); /* 大屏幕：4列 */
  }
}

.item {
  width: 100%;
}
</style>
