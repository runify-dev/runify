import { Result } from '@/request/Result'
import { get, post, put, del } from '@/request/admin/index'

export interface SkillFile {
  id: string
  parentId: string
  skillId: string
  name: string
  type: 'folder' | 'text' | 'file'
  content?: string
  fileId?: string
  fileName?: string
  fileSize?: number
  desc?: string
  createTime: string
  updateTime: string
}

const tree = (resourceId: string): Promise<Result<SkillFile[]>> => {
  return get(`/skill/resources/${resourceId}/files/tree`)
}

const listChildren = (resourceId: string, parentId: string): Promise<Result<SkillFile[]>> => {
  return get(`/skill/resources/${resourceId}/files/${parentId}/children`)
}

const getFile = (resourceId: string, fileId: string): Promise<Result<SkillFile>> => {
  return get(`/skill/resources/${resourceId}/files/${fileId}`)
}

const createFolder = (resourceId: string, parentId: string, name: string): Promise<Result<SkillFile>> => {
  return post(`/skill/resources/${resourceId}/files/${parentId}/folder`, { name })
}

const createText = (resourceId: string, parentId: string, name: string): Promise<Result<SkillFile>> => {
  return post(`/skill/resources/${resourceId}/files/${parentId}/text`, { name })
}

const uploadFile = (resourceId: string, parentId: string, formData: FormData, onProgress?: (percent: number) => void): Promise<Result<SkillFile>> => {
  return post(`/skill/resources/${resourceId}/files/${parentId}/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e: any) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded / e.total) * 100))
      }
    }
  })
}

const updateContent = (resourceId: string, fileId: string, content: string): Promise<Result<SkillFile>> => {
  return put(`/skill/resources/${resourceId}/files/${fileId}/content`, { content })
}

const rename = (resourceId: string, fileId: string, name: string): Promise<Result<SkillFile>> => {
  return put(`/skill/resources/${resourceId}/files/${fileId}/rename`, { name })
}

const remove = (resourceId: string, fileId: string): Promise<Result<boolean>> => {
  return del(`/skill/resources/${resourceId}/files/${fileId}`)
}

const edit = (resourceId: string, data: { name?: string; icon?: string; desc?: string; parameterValue?: string; skillParameterForm?: any[] }): Promise<Result<any>> => {
  return put(`/skill/resources/${resourceId}`, data)
}

export default {
  tree,
  listChildren,
  getFile,
  createFolder,
  createText,
  uploadFile,
  updateContent,
  rename,
  remove,
  edit
}
