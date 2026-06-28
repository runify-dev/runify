import {Result} from '@/request/Result'
import {get, put} from '@/request/admin/index'
import {TreeCommonAPI} from '@/api/tree'
import {ROOT_FOLDER_ID} from '@/constants/common'
import {ref, type Ref} from 'vue'

/**
 * 各平台凭证字段定义(用于动态渲染表单)
 */
export interface IntegrationConfigField {
  field: string
  label: string
  placeholder?: string
  secret?: boolean
}

export interface IntegrationTypeMeta {
  type: string
  label: string
  /** 回调路径(展示给用户去平台后台配置), :integrationId 由前端替换; 空表示无需公网回调(自驱动连接) */
  callbackPath: string
  /** 认证方式: credential=凭证表单, qrcode=扫码登录(个人微信) */
  authMode?: string
  fields: IntegrationConfigField[]
}

/**
 * 平台类型目录(唯一真源在后端 IntegrationTypeCatalog): 进程内缓存, 首次拉取后复用。
 * 响应式 ref, 供组件在加载完成后自动重渲染下拉与表单。
 */
const integrationTypes = ref<IntegrationTypeMeta[]>([])
let typesLoaded = false

export const useIntegrationTypes = (): Ref<IntegrationTypeMeta[]> => integrationTypes

export const loadIntegrationTypes = async (loading?: Ref<boolean>): Promise<IntegrationTypeMeta[]> => {
  if (!typesLoaded) {
    const ok = await get('/integration/types', {}, loading)
    integrationTypes.value = (ok.data || []) as IntegrationTypeMeta[]
    typesLoaded = true
  }
  return integrationTypes.value
}

export const getTypeMeta = (type: string): IntegrationTypeMeta | undefined =>
  integrationTypes.value.find((t) => t.type === type)

const edit: (
  resourceId: string,
  integration: any,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (resourceId, integration, loading) => {
  return put(`/integration/resources/${resourceId}`, integration, {}, loading)
}

/**
 * 拉取全部应用, 用于"绑定应用"下拉。
 * subtree 走闭包表(ancestor_id 命中所有层级后代), 一次调用即返回整棵子树的扁平列表,
 * 过滤掉文件夹节点即得到所有应用。
 */
const applicationTreeAPI = new TreeCommonAPI('application')

const listAllApplications = async (
  folderId: string = ROOT_FOLDER_ID,
  loading?: Ref<boolean>
): Promise<Array<{id: string; name: string}>> => {
  const ok = await applicationTreeAPI.listTree(folderId, loading)
  return (ok.data || [])
    .filter((node: any) => node.type !== 'folder')
    .map((node: any) => ({id: node.id, name: node.name}))
}

/** 微信(个人号/iLink) 获取登录二维码 */
const weixinQrcode: (
  resourceId: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (resourceId, loading) => {
  return get(`/integration/resources/${resourceId}/weixin/qrcode`, {}, loading)
}

/** 微信(个人号/iLink) 轮询扫码状态 */
const weixinQrcodeStatus: (
  resourceId: string,
  qrcode: string,
  baseUrl?: string
) => Promise<Result<any>> = (resourceId, qrcode, baseUrl) => {
  return get(`/integration/resources/${resourceId}/weixin/qrcode-status`, {qrcode, baseUrl})
}

export default {
  edit,
  listAllApplications,
  weixinQrcode,
  weixinQrcodeStatus
}
