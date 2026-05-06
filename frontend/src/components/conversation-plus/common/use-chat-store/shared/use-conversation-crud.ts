import { ref, computed } from 'vue'
import type { Ref } from 'vue'
import type { Conversation } from '../../types'
import type { QueryConversationVO } from '@/api/type/conversation'

const PAGE_SIZE = 30

// 单例 — 所有 store 实例共享同一个会话列表
const chats = ref<Conversation[]>([])
const currentPage = ref(1)
const total = ref(0)
const loadingMore = ref(false)

interface ConversationCrudDeps {
  pageConversationAPI: (query: any, loading?: Ref<boolean>) => Promise<any>
}

export function useConversationCrud(deps: ConversationCrudDeps) {
  const { pageConversationAPI } = deps

  const hasMore = computed(() => chats.value.length < total.value)

  const pageConversation = async (
    query: Omit<QueryConversationVO, 'currentPage' | 'pageSize'>,
    loading?: Ref<boolean>
  ) => {
    currentPage.value = 1

    const res = await pageConversationAPI(
      { ...query, currentPage: 1, pageSize: PAGE_SIZE },
      loading
    )

    chats.value = res.data.records
    total.value = res.data.total
  }

  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value) return

    loadingMore.value = true

    try {
      const res = await pageConversationAPI({
        currentPage: currentPage.value + 1,
        pageSize: PAGE_SIZE
      })

      chats.value.push(...res.data.records)
      total.value = res.data.total
      currentPage.value += 1
    } finally {
      loadingMore.value = false
    }
  }

  return {
    chats,
    hasMore,
    loadingMore,
    total,
    pageConversation,
    loadMore
  }
}
