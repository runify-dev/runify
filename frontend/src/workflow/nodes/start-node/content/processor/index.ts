import type { NodeInitContext } from '@/workflow/common/type'
import { init as initHttp } from './http'

const initMap: Record<string, (ctx: NodeInitContext) => void> = {
  HTTP: initHttp
}

export function init(ctx: NodeInitContext) {
  const protocol = ctx.details?.protocol
  if (protocol && initMap[protocol]) {
    initMap[protocol](ctx)
  }
}
