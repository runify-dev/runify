<template>
  <el-row :gutter="10" class="p-8">
    <el-col :span="8">
      <el-input
        v-model="searchText"
        style="max-width: 600px"
        placeholder="搜索笔记"
        class="input-with-select"
      >
        <template #append>
          <el-button :icon="Search" />
        </template> </el-input
    ></el-col>
    <el-col :span="8">
      <div class="flex items-center justify-center">
        <div>{{ folder?.name }}</div>
      </div>
    </el-col>
    <el-col :span="8"><el-button class="float-right">新建项目</el-button></el-col>
  </el-row>
  <el-row :gutter="10" class="pr-8 pl-8">
    <el-col
      v-for="node in nodeList"
      :key="node.id"
      :xs="24"
      :sm="12"
      :md="8"
      :lg="6"
      :xl="6"
      class="pt-4"
    >
      <CardBox
        :icon="node.icon"
        :title="node.name"
        @click="
          router.push({
            name: 'projectDetails',
            params: { folderId: node.parentId ? node.parentId : 'root', id: node.id }
          })
        "
      >
        <template #description>
          <div v-if="node.excerpt">
            <MdPreview :modelValue="node.excerpt"> </MdPreview>
          </div>
          <el-skeleton v-else :rows="4" />
        </template>
      </CardBox>
    </el-col>
  </el-row>
</template>
<script setup lang="ts">
import { useRoute } from 'vue-router'
import CardBox from '@/components/card-box/index.vue'
import { MdPreview } from 'md-editor-v3'
import { computed, onMounted, ref, watch } from 'vue'
import { type Node } from '@/api/type/node'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { TreeCommonAPI } from '@/api/tree'
const treeCommonAPI = new TreeCommonAPI('project')
const router = useRouter()
const nodeList = ref<Array<Node>>([])
const folder = ref<Node>()
const route = useRoute()
const searchText = ref<string>('')
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
