<template>
  <AppSubLayout>
    <template #aside>
      <TreeAside
        ref="treeAsideRef"
        :currentId="folderId"
        @update:currentId="(currentId, node) => go(currentId, node)"
        :data="data"
        :config="config"
      >
      </TreeAside>
      <CreateProjectDialog ref="createProjectDialogRef"></CreateProjectDialog>
    </template>
    <template #main>
      <RouterView :key="route.fullPath"></RouterView>
    </template>
  </AppSubLayout>
</template>
<script setup lang="ts">
import AppSubLayout from '@/layout/AppSubLayout.vue'
import { computed, onMounted, ref, provide } from 'vue'
import 'md-editor-v3/lib/style.css'
import TreeAside from '@/components/tree/index.vue'
import { toTree } from '@/utils/common'
import { type Tree } from '@/api/type/node'
import { useRouter, useRoute } from 'vue-router'
import { Config, Processor } from '@/components/tree/index'
import { TreeCommonAPI } from '@/api/tree'
import { set } from 'lodash'
import CreateProjectDialog from '@/views/project/components/CreateProjectDialog .vue'
const createProjectDialogRef = ref<InstanceType<typeof CreateProjectDialog>>()
const treeAsideRef = ref<typeof TreeAside>()
const treeCommonAPI = new TreeCommonAPI('project')
provide('treeCommonAPI', treeCommonAPI)
const router = useRouter()
const route = useRoute()
const go = (id: string, data?: any) => {
  if (['star', 'share', 'root'].includes(id)) {
    router.push({ name: 'projectFolders', params: { id: id } })
    return
  }
  if (data) {
    if (data.type == 'folder') {
      router.push({ name: 'projectFolders', params: { id: id } })
    } else {
      router.push({ name: 'projectDetails', params: { id: id } })
    }
  }
}
const config = new Config(
  'note',
  [
    new Processor('创建项目', '', ['FOLDER', 'NOTE', 'ROOT'], (event: any) => {
      createProjectDialogRef.value?.open(event, data.value)
    }),
    new Processor('创建文件夹', '', ['FOLDER', 'ROOT'], (event: any) => {
      treeCommonAPI.createFolder(event.data.id, {}).then((ok) => {
        if (event.data.id === 'root') {
          data.value.push({ ...ok.data, type: 'folder', operate: 'rename' })
          go(ok.data.id, { ...ok.data, type: 'folder' })
        } else {
          event.node.insertAfter(
            { data: { ...ok.data, type: 'folder', operate: 'rename' } },
            event.node
          )
          go(ok.data.id, { ...ok.data, type: 'folder' })
        }
      })
    }),
    new Processor('重命名', '', ['FOLDER', 'PROJECT'], (event: any) => {
      set(event.data, 'operate', 'rename')
    }),
    new Processor('删除', '', ['FOLDER', 'PROJECT'], (event: any) => {
      ;(event.data.type == 'folder' ? treeCommonAPI.removeFolder : treeCommonAPI.removeResource)(
        event.data.id
      ).then(() => {
        event.node.remove()
      })
    })
  ],
  (event: any) => {
    return (
      event.data.type == 'folder'
        ? treeCommonAPI.modifyFolderName
        : treeCommonAPI.modifyResourceName
    )(event.data.id, event.name).then(() => {
      return true
    })
  },
  go
)

const folderId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})

const data = ref<Array<Tree>>([])

onMounted(() => {
  treeCommonAPI.listTree('root').then((ok) => {
    data.value = toTree(ok.data)
  })
})
</script>
<style lang="scss" scoped></style>
