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
    </template>
    <template #main>
      <RouterView></RouterView>
    </template>
  </AppSubLayout>
</template>
<script setup lang="ts">
import AppSubLayout from '@/layout/AppSubLayout.vue'
import { computed, onMounted, ref } from 'vue'
import 'md-editor-v3/lib/style.css'
import TreeAside from '@/components/tree/index.vue'
import { toTree } from '@/utils/common'
import { type Tree } from '@/api/type/node'
import { useRouter, useRoute } from 'vue-router'
import { Config, Processor } from '@/components/tree/index'
import { TreeCommonAPI } from '@/api/tree'
import { set } from 'lodash'
const treeAsideRef = ref<typeof TreeAside>()
const treeCommonAPI = new TreeCommonAPI('application')
const router = useRouter()
const route = useRoute()
const go = (id: string, data?: any) => {
  if (['star', 'share', 'root'].includes(id)) {
    router.push({ name: 'applicationFolders', params: { id: id } })
    return
  }
  if (data) {
    if (data.type == 'folder') {
      router.push({ name: 'applicationFolders', params: { id: id } })
    } else {
      router.push({ name: 'applicationDetails', params: { id: id } })
    }
  }
}
const config = new Config(
  'application',
  [
    new Processor('创建应用', '', ['FOLDER', 'APPLICATION', 'ROOT'], (event: any) => {
      console.log(event)
      treeCommonAPI.createResource(event.data.id, {}).then((ok) => {
        if (event.data.id === 'root') {
          data.value.push({ ...ok.data, type: 'application', operate: 'rename' })
          go(ok.data.id, ok.data)
        } else {
          event.node.insertAfter(
            { data: { ...ok.data, type: 'application', operate: 'rename' } },
            event.node
          )
          go(ok.data.id, ok.data)
        }
      })
    }),
    new Processor('创建文件夹', '', ['FOLDER', 'ROOT'], (event: any) => {
      treeCommonAPI.createFolder(event.data.id, {}).then((ok) => {
        if (event.data.id === 'root') {
          data.value.push({ ...ok.data, type: 'folder', operate: 'rename' })
          go(ok.data.id, ok.data)
        } else {
          event.node.insertAfter(
            { data: { ...ok.data, type: 'folder', operate: 'rename' } },
            event.node
          )
          go(ok.data.id, ok.data)
        }
      })
    }),
    new Processor('重命名', '', ['FOLDER', 'APPLICATION'], (event: any) => {
      set(event.data, 'operate', 'rename')
    }),
    new Processor('删除', '', ['FOLDER', 'APPLICATION'], (event: any) => {
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
    console.log(toTree(ok.data))
  })
})
</script>
<style lang="scss" scoped></style>
