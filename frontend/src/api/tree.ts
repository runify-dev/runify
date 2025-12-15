import { Result } from '@/request/Result'
import { get, post, put, del } from '@/request/admin/index'
import type {
  CreateFolderPojo
} from '@/api/type/tree'
import type { Ref } from 'vue'
import { type Node, type EditNodePojo } from '@/api/type/node'
import type { Resource } from './type/common'

export class TreeCommonAPI {
  resource: Resource
  constructor(resource: Resource) {
    this.resource = resource
  }
  /**
   * 创建节点
   * @param createNodePojo 创建节点对象
   * @param loading loading
   * @returns
   */
  createFolder: (
    folderId: string,
    createNodePojo: CreateFolderPojo,
    loading?: Ref<boolean>
  ) => Promise<Result<Node>> = (folderId, createNodePojo, loading) => {
    return post(`/${this.resource}/folders/${folderId}`, createNodePojo, undefined, loading)
  }
  /**
   * 创建资源
   * @param folderId  文件夹id
   * @param createNodePojo  创建资源对象
   * @param loading
   * @returns
   */
  createResource: (
    folderId: string,
    createNodePojo: CreateFolderPojo,
    loading?: Ref<boolean>
  ) => Promise<Result<Node>> = (folderId, createNodePojo, loading) => {
    return post(`/${this.resource}/folders/${folderId}/resources`, createNodePojo, undefined, loading)
  }
  /**
 *
 * 获取节点列表
 * @param loading
 * @returns
 */
  listResource: (
    folderId: string,
    loading?: Ref<boolean>
  ) => Promise<Result<Array<Node>>> = (folderId, loading) => {
    return get(`/${this.resource}/folders/${folderId}/resources`, {}, loading)
  }
  /**
   * 获取用户树
   * @param folderId 文件夹id
   * @param loading
   * @returns
   */
  listTree: (
    folderId: string,
    loading?: Ref<boolean>
  ) => Promise<Result<Array<Node>>> = (folderId, loading) => {
    return get(`/${this.resource}/folders/${folderId}/subtree`, {}, loading)
  }
  /**
   * 获取单个资源

   * @param resourceId 文件夹id
   * @param loading
   * @returns
   */
  getResource: (
    resourceId: string,
    loading?: Ref<boolean>
  ) => Promise<Result<any>> = (resourceId, loading) => {
    return get(`/${this.resource}/resources/${resourceId}`, {}, loading)
  }
  /**
   * 获取文件夹信息
   * @param folderId 文件夹id
   * @param loading
   * @returns
   */
  getFolder: (
    folderId: string,
    loading?: Ref<boolean>
  ) => Promise<Result<Node>> = (folderId, loading) => {
    return get(`/${this.resource}/folders/${folderId}`, {}, loading)
  }


  /**
   * 删除资源
   * @param resourceId
   * @param sourceId
   * @param loading
   * @returns
   */
  removeResource: (
    resourceId: string,
    loading?: Ref<boolean>
  ) => Promise<Result<Node>> = (resourceId, loading) => {
    return del(`/${this.resource}/resources/${resourceId}`, undefined, loading)
  }
  /**
     * 删除文件夹
     * @param resourceId
     * @param sourceId
     * @param loading
     * @returns
     */
  removeFolder: (
    folderId: string,
    loading?: Ref<boolean>
  ) => Promise<Result<Node>> = (folderId, loading) => {
    return del(`/${this.resource}/folders/${folderId}`, undefined, loading)
  }
  /**
   * 修改资源名称
   * @param sourceId 资源id
   * @param name     新名称
   * @param loading
   * @returns
   */
  modifyResourceName: (
    sourceId: string,
    name: string,
    loading?: Ref<boolean>
  ) => Promise<Result<Node>> = (sourceId, name, loading) => {
    return post(`/${this.resource}/resources/${sourceId}/modify-name`, { name }, {}, loading)
  }
  /**
   * 修改资源名称
   * @param sourceId 资源id
   * @param name     新名称
   * @param loading
   * @returns
   */
  modifyFolderName: (
    folderId: string,
    name: string,
    loading?: Ref<boolean>
  ) => Promise<Result<Node>> = (folderId, name, loading) => {
    return post(`/${this.resource}/folders/${folderId}/modify-name`, { name }, {}, loading)
  }
  /**
   * 获取资源权限列表
   * @param userId
   * @param loading
   * @returns
   */
  listResourcePermission: (
    userId: string,
    loading?: Ref<boolean>
  ) => Promise<Result<Array<Node>>> = (userId, loading) => {
    return get(`/${this.resource}/permissions/${userId}`, {}, loading)
  }

  /**
     * 获取资源权限列表
     * @param userId
     * @param loading
     * @returns
     */
  authResourcePermission: (
    userId: string,
    target: string,
    permission: string,
    loading?: Ref<boolean>
  ) => Promise<Result<Array<Node>>> = (userId, target, permission, loading) => {
    return put(`/${this.resource}/permissions/${userId}/authorization/${target}/${permission}`, {}, loading)
  }
}






