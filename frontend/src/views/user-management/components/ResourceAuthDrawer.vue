<template>
  <div class="placeholder">
    <el-drawer size="60%" v-model="visible" title="资源授权" direction="rtl" :before-close="close">
      <div class="main-contaner">
        <div class="app-aside">
          <div class="menu-contaner">
            <Menu :menu="menu" v-model="active" v-for="(menu, index) in menuList" :key="index">
            </Menu>
          </div>
        </div>
        <div class="content">
          <el-table
            :data="tableData"
            style="width: 100%; margin-bottom: 20px"
            row-key="id"
            default-expand-all
          >
            <el-table-column type="selection" width="55" />

            <el-table-column prop="name" label="名称">
              <template #default="scope">
                <span class="mr-3">{{ scope.row.name }} </span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="权限">
              <template #default="scope">
                <el-radio-group
                  v-bind:modelValue="scope.row.permission"
                  @update:modelValue="scope.row.change($event)"
                >
                  <el-radio value="INHERIT" v-if="scope.row.parentId">继承</el-radio>
                  <el-radio value="NOT_AUTH">不授权</el-radio>
                  <el-radio value="VIEW">查看</el-radio>
                  <el-radio value="MANAGE">管理</el-radio>
                </el-radio-group>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import Menu from './Menu.vue'
import { TreeCommonAPI } from '@/api/tree'
const visible = ref<boolean>(false)
const userId = ref<string>()
import { toTree } from '@/utils/common'
const active = ref<'application' | 'knowledge' | 'model'>('application')
const tableData = ref<Array<any>>([])
const menuList: Array<any> = [
  {
    path: '/application',
    name: 'application',
    meta: { title: '应用', icon: 'app-application' }
  },
  {
    path: '/knowledge',
    name: 'knowledge',
    meta: { title: '知识库', icon: 'app-document', activeMenu: 'knowledge' }
  },
  {
    path: '/model',
    name: 'model',
    meta: { title: '模型', icon: 'app-model', activeMenu: 'model' }
  }
]
const close = () => {
  visible.value = false
}
watch(active, () => {
  listResourcePermission()
})
const listResourcePermission = () => {
  const api = new TreeCommonAPI(active.value)
  api.listResourcePermission(userId.value as string).then((ok) => {
    ok.data.forEach((item: any) => {
      item.change = function (permission: string) {
        api.authResourcePermission(userId.value as string, this.id, permission).then(() => {
          this.permission = permission
        })
      }
    })
    tableData.value = toTree(ok.data)
  })
}
const open = (uId: string) => {
  visible.value = true
  userId.value = uId
  listResourcePermission()
}

defineExpose({ close, open })
</script>
<style lang="scss" scoped>
.placeholder {
  :deep(.el-drawer__body) {
    padding: 0 !important;
  }
}

.main-contaner {
  height: 100%;
  display: flex;

  .app-aside {
    height: 100%;
    width: 50px;
    background-color: var(--app-background-color, #f1f3f5);
    padding: 10px 0;
  }
  .content {
    width: calc(100% - 50px);
  }
}
</style>
