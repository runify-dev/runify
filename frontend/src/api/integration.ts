import {Result} from '@/request/Result'
import {get, put} from '@/request/admin/index'
import {TreeCommonAPI} from '@/api/tree'
import {ROOT_FOLDER_ID} from '@/constants/common'
import type {Ref} from 'vue'

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
  /** 回调路径(展示给用户去平台后台配置), :integrationId 由前端替换 */
  callbackPath: string
  fields: IntegrationConfigField[]
}

export const INTEGRATION_TYPES: IntegrationTypeMeta[] = [
  {
    type: 'WECOM',
    label: '企业微信应用',
    callbackPath: '/integration/{id}/callback',
    fields: [
      {field: 'corpId', label: 'CorpID', placeholder: '企业ID'},
      {field: 'agentId', label: 'AgentID', placeholder: '应用 AgentId'},
      {field: 'secret', label: 'Secret', placeholder: '应用 Secret', secret: true},
      {field: 'token', label: 'Token', placeholder: '回调 Token', secret: true},
      {field: 'aesKey', label: 'EncodingAESKey', placeholder: '回调 EncodingAESKey', secret: true}
    ]
  },
  {
    type: 'WECOM_ROBOT',
    label: '企业微信机器人',
    callbackPath: '/integration/{id}/callback',
    fields: [
      {field: 'token', label: 'Token', placeholder: '回调 Token', secret: true},
      {field: 'aesKey', label: 'EncodingAESKey', placeholder: '回调 EncodingAESKey', secret: true}
    ]
  },
  {
    type: 'FEISHU',
    label: '飞书',
    callbackPath: '/integration/{id}/callback',
    fields: [
      {field: 'appId', label: 'App ID', placeholder: '飞书应用 App ID'},
      {field: 'appSecret', label: 'App Secret', placeholder: '飞书应用 App Secret', secret: true},
      {field: 'verifyToken', label: 'Verification Token', placeholder: '事件订阅 Verification Token', secret: true},
      {field: 'encryptKey', label: 'Encrypt Key', placeholder: '事件订阅 Encrypt Key', secret: true}
    ]
  },
  {
    type: 'DINGTALK',
    label: '钉钉',
    callbackPath: '/integration/{id}/callback',
    fields: [
      {field: 'appKey', label: 'AppKey', placeholder: '机器人 AppKey/ClientId'},
      {field: 'appSecret', label: 'AppSecret', placeholder: '机器人 AppSecret(验签用)', secret: true}
    ]
  },
  {
    type: 'WEIXIN',
    label: '微信(个人号)',
    callbackPath: '',
    fields: []
  },
  {
    type: 'WECHAT',
    label: '微信公众号',
    callbackPath: '/integration/{id}/callback',
    fields: [
      {field: 'appId', label: 'AppID', placeholder: '公众号 AppID'},
      {field: 'appSecret', label: 'AppSecret', placeholder: '公众号 AppSecret', secret: true},
      {field: 'token', label: 'Token', placeholder: '服务器配置 Token', secret: true},
      {field: 'aesKey', label: 'EncodingAESKey', placeholder: '消息加解密密钥', secret: true}
    ]
  }
]

export const getTypeMeta = (type: string): IntegrationTypeMeta | undefined =>
  INTEGRATION_TYPES.find((t) => t.type === type)

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
