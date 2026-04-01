
import { useChatStore as useBaseChatStore } from './chat/index'
import { useChatStore as useDebugChatStore } from './debug/index'
export const useChatStore = (type: 'DEBUG' | 'CONVERSATION') => {
  if (type === 'DEBUG') {
    return useDebugChatStore()
  } else {
    return useBaseChatStore()
  }
}
