
import { useChatStore as useBaseChatStore } from './chat/index'
import { useChatStore as useDebugChatStore } from './debug/index'
import { useChatStore as useAdminConversationChatStore } from './admin-conversation/index'
export const useChatStore = (type: 'DEBUG' | 'CONVERSATION' | 'ADMIN_CONVERSATION') => {
  if (type === 'DEBUG') {
    return useDebugChatStore()
  } else if (type === 'ADMIN_CONVERSATION') {
    return useAdminConversationChatStore()
  } else {
    return useBaseChatStore()
  }
}
