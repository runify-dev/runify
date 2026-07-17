import { Result } from '@/request/Result'
import { get, post, put, del, request } from '@/request/admin/index'

export interface Document {
  id: string
  parentId: string
  knowledgeId: string
  name: string
  icon: string
  type: 'folder' | 'document'
  content: string
  excerpt: string
  createTime: string
  updateTime: string
}

const tree = (resourceId: string): Promise<Result<Document[]>> => {
  return get(`/knowledge/resources/${resourceId}/documents/tree`)
}

const listChildren = (resourceId: string, parentId: string): Promise<Result<Document[]>> => {
  return get(`/knowledge/resources/${resourceId}/documents/${parentId}/children`)
}

const getDocument = (resourceId: string, documentId: string): Promise<Result<Document>> => {
  return get(`/knowledge/resources/${resourceId}/documents/${documentId}`)
}

const createFolder = (resourceId: string, parentId: string, name: string): Promise<Result<Document>> => {
  return post(`/knowledge/resources/${resourceId}/documents/${parentId}/folder`, { name })
}

const createText = (resourceId: string, parentId: string, name: string): Promise<Result<Document>> => {
  return post(`/knowledge/resources/${resourceId}/documents/${parentId}/text`, { name })
}

const updateContent = (resourceId: string, documentId: string, content: string): Promise<Result<Document>> => {
  return put(`/knowledge/resources/${resourceId}/documents/${documentId}/content`, { content })
}

const importDocument = (resourceId: string, parentId: string, file: File): Promise<Result<Document>> => {
  const fd = new FormData()
  fd.append('file', file)
  return request
    .post(`/knowledge/resources/${resourceId}/documents/${parentId}/import`, fd)
    .then(ok => ok.data)
}

const rename = (resourceId: string, documentId: string, name: string): Promise<Result<Document>> => {
  return put(`/knowledge/resources/${resourceId}/documents/${documentId}/rename`, { name })
}

const remove = (resourceId: string, documentId: string): Promise<Result<boolean>> => {
  return del(`/knowledge/resources/${resourceId}/documents/${documentId}`)
}

const edit = (resourceId: string, data: { name?: string; icon?: string; desc?: string }): Promise<Result<any>> => {
  return put(`/knowledge/resources/${resourceId}`, data)
}

export default {
  tree,
  listChildren,
  getDocument,
  createFolder,
  createText,
  importDocument,
  updateContent,
  rename,
  remove,
  edit
}
