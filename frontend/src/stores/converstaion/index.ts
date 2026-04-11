import { createPinia } from 'pinia'
const store = createPinia()
export { store }
import useConversationTokenStore from './modules/conversation-token'

const useStore = () => ({
  conversationToken: useConversationTokenStore()
})

export default useStore
