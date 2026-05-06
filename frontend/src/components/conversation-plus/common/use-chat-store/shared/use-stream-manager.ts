import { reactive } from 'vue'
import { ConversationStream } from '@/api/common'

export interface StartStreamOptions {
  cid: string
  request: () => Promise<any>
  onStream: (chunk: any) => void
  onFinish?: () => void
  onFailure?: () => void
}

export interface SwitchOptions {
  cid: string
  loadMessages: (cid: string) => Promise<void>
  statusStream: () => Promise<any>
  resumeStream: () => Promise<any>
  getOnStream: () => (chunk: any) => void
  onFinish?: () => void
  onFailure?: () => void
  skipLoadMessages?: boolean
}

export function useStreamManager() {
  const streamLoadingMap = reactive(new Map<string, boolean>())
  let streamToken = 0
  let currentStream: InstanceType<typeof ConversationStream> | null = null

  const getStreamLoading = (cid: string) => streamLoadingMap.get(cid) ?? false
  const setStreamLoading = (cid: string, val: boolean) => streamLoadingMap.set(cid, val)

  const cancelStream = () => {
    if (currentStream) {
      currentStream.cancel()
      currentStream = null
    }
  }

  const startStream = ({
    cid,
    request,
    onStream,
    onFinish,
    onFailure
  }: StartStreamOptions) => {
    const token = ++streamToken

    cancelStream()
    setStreamLoading(cid, true)

    currentStream = new ConversationStream(
      request(),
      onStream,
      () => {
        if (token !== streamToken) return
        currentStream = null
        setStreamLoading(cid, false)
        onFinish?.()
      },
      () => {
        if (token !== streamToken) return
        currentStream = null
        setStreamLoading(cid, false)
        onFailure?.()
      }
    )
    currentStream.stream()

    return token
  }

  const switchConversation = async (opts: SwitchOptions) => {
    const { cid, loadMessages, statusStream, resumeStream, getOnStream, onFinish, onFailure, skipLoadMessages } = opts
    const token = ++streamToken

    cancelStream()
    setStreamLoading(cid, false)

    if (!skipLoadMessages) {
      await loadMessages(cid)
    }

    if (token !== streamToken) return

    try {
      const res = await statusStream()

      if (token !== streamToken) return
      if (res?.code !== 200 || res?.data?.status !== true) return

      // loadMessages 之后才创建 onStream，确保绑定到正确的 message 对象
      const onStream = getOnStream()

      startStream({
        cid,
        request: resumeStream,
        onStream,
        onFinish,
        onFailure
      })
    } catch (e) {
      if (token !== streamToken) return
      console.error('resume stream failed', e)
    }
  }

  return {
    getStreamLoading,
    setStreamLoading,
    cancelStream,
    startStream,
    switchConversation,
    streamLoadingMap
  }
}
