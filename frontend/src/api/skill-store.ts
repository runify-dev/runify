import { get, post } from '@/request/admin/index'
import { Result } from '@/request/Result'

const TOS_BASE = 'https://runify.tos-cn-beijing.volces.com'

export interface SkillStoreItem {
  id: string
  name: string
  vendor: string
  icon: string
  tags: string[]
  summary: string
  capabilities: string[]
  requires: string[]
  note: string
  source: string
  versions: Array<{
    version: string
    time: string
  }>
}

export interface SkillVersionDetail {
  version: string
  date: string
  prerelease: boolean
  title: string
  highlights: string[]
  assets: Array<{
    os: string
    arch: string
    type: string
    label: string
    size: number
    url: string
  }>
}

export interface SkillStoreIndex {
  skills: string[]
}

export interface SkillStoreRootIndex {
  project: string
  repo: string
  releasesUrl: string
  notice: string
  versions: Array<{
    version: string
    time: string
  }>
}

const fetchJson = async <T>(url: string): Promise<T> => {
  const res = await fetch(url)
  if (!res.ok) throw new Error(`Failed to fetch: ${url}`)
  return res.json()
}

/** Get skills list from store */
export const getSkillStoreIndex = (): Promise<SkillStoreIndex> => {
  return fetchJson<SkillStoreIndex>(`${TOS_BASE}/skills/index.json`)
}

/** Get skill detail */
export const getSkillDetail = (skillId: string): Promise<SkillStoreItem> => {
  return fetchJson<SkillStoreItem>(`${TOS_BASE}/skills/${skillId}/index.json`)
}

/** Get skill version detail */
export const getSkillVersionDetail = (skillId: string, version: string): Promise<SkillVersionDetail> => {
  return fetchJson<SkillVersionDetail>(`${TOS_BASE}/skills/${skillId}/${version}/index.json`)
}

/** Get icon URL */
export const getSkillIconUrl = (skillId: string, version: string): string => {
  return `${TOS_BASE}/skills/${skillId}/${version}/icon`
}

/** Get skill zip URL */
export const getSkillZipUrl = (skillId: string, version: string): string => {
  return `${TOS_BASE}/skills/${skillId}/${version}/index.zip`
}

/** Install skill from store */
export const installFromStore = (folderId: string, storeId: string, storeVersion: string, zipUrl: string): Promise<Result<any>> => {
  return post(`/skill/folders/${folderId}/resources/install-from-store`, {
    storeId,
    storeVersion,
    zipUrl
  })
}

/** Upgrade skill from store */
export const upgradeFromStore = (skillId: string, storeVersion: string, zipUrl: string): Promise<Result<any>> => {
  return post(`/skill/resources/${skillId}/upgrade-from-store`, {
    storeVersion,
    zipUrl
  })
}

export default {
  getSkillStoreIndex,
  getSkillDetail,
  getSkillVersionDetail,
  getSkillIconUrl,
  getSkillZipUrl,
  installFromStore,
  upgradeFromStore
}
