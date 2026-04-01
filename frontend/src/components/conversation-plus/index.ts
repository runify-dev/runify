class Scroll {
  /**
   * 是否吸底
   */
  bottomSuction: boolean
  /**
   * 是否是用户滚动
   */
  isProgrammaticScroll?: boolean
  /**
   * 元素
   */
  element: any
  constructor(element: any) {
    this.bottomSuction = true
    this.isProgrammaticScroll = false
    this.element = element
    this.initEventListener()
  }
  initEventListener() {
    this.element.addEventListener('scroll', () => {
      // 如果是程序滚动 那么就是吸底
      if (this.isProgrammaticScroll) {
        this.isProgrammaticScroll = undefined
      } else {
        if (this.element.scrollHeight - this.element.scrollTop <= this.element.clientHeight + 15) {
          // 如果用户滚动 那么判断滚动条到底部高度 判断是否吸底
          this.bottomSuction = true
          this.isProgrammaticScroll = true
        } else {
          this.isProgrammaticScroll = undefined
          // 不吸底
          this.bottomSuction = false
        }
      }
    })
  }
  scrollBottom() {
    // 如果吸底 就滚动到最下面
    if (this.bottomSuction) {
      this.element.scrollTop = this.element.scrollHeight
      // 程序滚动
      this.isProgrammaticScroll = true
    }
  }
}
export { Scroll }
const TEXT = (prev: any = {}, chunk: any) => {
  return {
    type: 'TEXT',
    id: chunk.id ?? prev.id,
    content: (prev.content || '') + (chunk.content || ''),
    workflowRunId: chunk.workflowRunId ?? prev.workflowRunId,
    extra: chunk.extra ?? prev.extra
  }
}
const REASONING = (prev: any, chunk: any) => {
  return {
    type: 'REASONING',
    id: chunk.id ?? prev.id,
    content: (prev.content || '') + (chunk.content || ''),
    workflowRunId: chunk.workflowRunId ?? prev.workflowRunId,
    extra: chunk.extra ?? prev.extra,
    status: chunk.status ?? prev.status,
  }
}
const FAILURE = (prev: any, chunk: any) => {
  return {
    type: 'FAILURE',
    id: chunk.id ?? prev.id,
    content: (prev.content || '') + (chunk.content || ''),
    workflowRunId: chunk.workflowRunId ?? prev.workflowRunId,
    extra: chunk.extra ?? prev.extra
  }
}
export const aggregators: any = {
  TEXT: TEXT,
  REASONING: REASONING,
  FAILURE: FAILURE
}
