<template>
  <div class="relative overflow-x-auto shadow-md sm:rounded-lg">
    <!-- 搜索栏 -->
    <div class="p-5 text-lg font-semibold text-left rtl:text-right text-gray-900 bg-white dark:text-white dark:bg-gray-800">
      <div class="pb-4 bg-white dark:bg-gray-900">
        <label for="table-search" class="sr-only">Search</label>
        <div class="relative mt-1">
          <div class="absolute inset-y-0 rtl:inset-r-0 start-0 flex items-center ps-3 pointer-events-none">
            <svg class="w-4 h-4 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 20 20">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="m19 19-4-4m0-7A7 7 0 1 1 1 8a7 7 0 0 1 14 0Z"/>
            </svg>
          </div>
          <input type="text" id="table-search"
                 class="block pt-2 ps-10 text-sm text-gray-900 border border-gray-300 rounded-lg w-80 bg-gray-50 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500"
                 placeholder="搜索"
                 v-model="keyword">
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <table class="w-full text-sm text-left rtl:text-right text-gray-500 dark:text-gray-400">
      <thead class="text-xs text-gray-700 uppercase bg-gray-50 dark:bg-gray-700 dark:text-gray-400">
        <tr>
          <th scope="col" class="px-6 py-3">摘要内容</th>
          <th scope="col" class="px-6 py-3">对话提问次数</th>
          <th scope="col" class="px-6 py-3">对话用户</th>
          <th scope="col" class="px-6 py-3">反馈</th>
          <th scope="col" class="px-6 py-3">时间</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="row in nodeList.records" :key="row.id"
            class="bg-white border-b dark:bg-gray-800 dark:border-gray-700 border-gray-200 hover:bg-gray-50 dark:hover:bg-gray-600">
          <th scope="row" class="px-6 py-4 font-medium text-gray-900 whitespace-nowrap dark:text-white">
            {{ row.name }}
          </th>
          <td class="px-6 py-4">{{ row.conversationRecordCount }}</td>
          <td class="px-6 py-4">{{ row.conversationUserType }}</td>
          <td class="px-6 py-4">{{ row.starNum }} / {{ row.trampleNum }}</td>
          <td class="px-6 py-4">{{ row.createTime }}</td>
        </tr>
      </tbody>
    </table>

    <!-- 分页 -->
    <div class="p-5 text-lg font-semibold text-left rtl:text-right text-gray-900 bg-white dark:text-white dark:bg-gray-800">
      <nav class="flex items-center flex-column flex-wrap md:flex-row justify-between pt-4" aria-label="Table navigation">
        <span class="text-sm font-normal text-gray-500 dark:text-gray-400 mb-4 md:mb-0 block w-full md:inline md:w-auto">
          显示
          <span class="font-semibold text-gray-900 dark:text-white">{{ (page-1)*pageSize+1 }}-{{ Math.min(page*pageSize, total) }}</span>
          共
          <span class="font-semibold text-gray-900 dark:text-white">{{ total }}</span>
        </span>

        <ul class="inline-flex -space-x-px rtl:space-x-reverse text-sm h-8">
          <li>
            <button :disabled="page <= 1" @click="page--"
                    class="flex items-center justify-center px-3 h-8 ms-0 leading-tight text-gray-500 bg-white border border-gray-300 rounded-s-lg hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white">上一页</button>
          </li>
          <li v-for="p in pages" :key="p">
            <button @click="page = p"
                    :class="['flex items-center justify-center px-3 h-8 leading-tight border', p === page ? 'text-blue-600 border-blue-300 bg-blue-50 dark:bg-gray-700 dark:text-white' : 'text-gray-500 bg-white border-gray-300 hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white']">
              {{ p }}
            </button>
          </li>
          <li>
            <button :disabled="page >= totalPage" @click="page++"
                    class="flex items-center justify-center px-3 h-8 leading-tight text-gray-500 bg-white border border-gray-300 rounded-e-lg hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white">下一页</button>
          </li>
        </ul>
      </nav>
    </div>
  </div>
</template>
<script setup lang="ts">
import ApplicationAPI from '@/api/application'
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const nodeList = ref<Array<Node>>([])
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

const listConversationLog = () => {
    ApplicationAPI.pageConversation(folderId.value, applicationId.value, 1, 10, {}).then((ok) => {
      nodeList.value = ok.data
  })
}

/* ------------- 响应式状态 ------------- */
const keyword = ref('')
const page    = ref(1)
const pageSize = 10

// 总条数
const total = computed(() => nodeList.value.total || 0)

// 总页数
const totalPage = computed(() =>
  Math.ceil(total.value / (nodeList.value.size || 1))
)

// 页码按钮
const pages = computed(() =>
  Array.from({ length: totalPage.value }, (_, i) => i + 1)
)

onMounted(() => {
  listConversationLog()
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
