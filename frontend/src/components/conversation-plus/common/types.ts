export interface Msg {
  id: string,
  conversationId: string,
  applicationId: string,
  role: 'USER' | 'ASSISTANT' | 'TOOL'
  content: Array<any>
  // yyyy-MM-dd HH:mm:ss
  createTime: string,
  // yyyy-MM-dd HH:mm:ss
  updateTime: string
}

export interface Conversation {
  id?: string
  applicationId: string
  name: string,
  executeType: 'DEBUG' | 'CONVERSATION'
  // yyyy-MM-dd HH:mm:ss
  createTime: string
  // yyyy-MM-dd HH:mm:ss
  updateTime: string
}


export interface ConversationGroup {
  label: string
  items: Conversation[]
}
