import { Result } from '@/request/Result'
import { get, post, put, del } from '@/request/index'
import type {
    CreateNodePojo,
    QueryNodePojo
} from '@/api/type/node'
import type { Ref } from 'vue'
import { type Node, type EditNodePojo } from '@/api/type/node'
import type { ResourceType } from './type/common'
/**
 * 创建节点
 * @param createNodePojo 创建节点对象
 * @param loading loading
 * @returns 
 */
const create: (
    resource_type: ResourceType,
    folderId: string,
    createNodePojo: CreateNodePojo,
    loading?: Ref<boolean>
) => Promise<Result<Node>> = (resourceType, folderId, createNodePojo, loading) => {
    return post(`/${resourceType}/folder/${folderId ? folderId : 'root'}`, createNodePojo, undefined, loading)
}
/**
 * 获取节点列表
 * @param loading  
 * @returns 
 */
const listResource: (
    resource_type: ResourceType,
    folder_id?: string,
    loading?: Ref<boolean>
) => Promise<Result<Array<Node>>> = (resource_type, folder_id, loading) => {

    return get(`/${resource_type}/folder/${folder_id ? folder_id : 'root'}/resource`, {}, loading)
}



const listTree: (
    resource_type: ResourceType,
    folder_id?: string,
    loading?: Ref<boolean>
) => Promise<Result<Array<Node>>> = (resource_type, folder_id, loading) => {
    return get(`/${resource_type}/folder/${folder_id ? folder_id : 'root'}/tree`, {}, loading)
}

/**
 * 修改节点
 * @param nodeId 节点id
 * @param editNodePojo 修改节点数据对象
 * @param loading 
 * @returns 
 */
const edit: (
    nodeId: string,
    editNodePojo: EditNodePojo,
    loading?: Ref<boolean>
) => Promise<Result<Node>> = (nodeId, editNodePojo, loading) => {
    return put(`/node/${nodeId}`, editNodePojo, undefined, loading)
}

export const ResourceTypeNameMap = {
    'knowledge': '全部知识库',
    'application': '全部应用',
    'model': "全部模型"
}
/**
 * 获取资源下 文件夹 info信息
 * @param resourceType 
 * @param floderId 
 * @param loading 
 * @returns 
 */
const resourceInfo: (resourceType: ResourceType, floderId: string, sourceId: string, loading?: Ref<boolean>) => Promise<Result<Node>> = (resourceType, floderId, sourceId, loading) => {
    if (floderId) {
        return get(`/${resourceType}/folder/${floderId}/resource/${sourceId}`, undefined, loading)
    } else {
        return Promise.resolve({
            message: "",
            code: 1,
            data: {
                name: ResourceTypeNameMap[resourceType]
            }
        })
    }

}

/**resource_type
 * 删除节点 根据节点id
 * @param nodeId  节点id
 * @param loading 加载器
 * @returns 
 */
const remove: (
    resourceType: ResourceType,
    floderId: string,
    sourceId: string,
    loading?: Ref<boolean>
) => Promise<Result<Node>> = (resourceType, floderId, sourceId, loading) => {
    return del(`/${resourceType}/folder/${floderId}/resource/${sourceId}`, undefined, loading)
}
const rename: (
    resourceType: ResourceType,
    floderId: string,
    sourceId: string,
    name: string,
    loading?: Ref<boolean>
) => Promise<Result<Node>> = (resourceType, floderId, sourceId, name, loading) => {
    return post(`/${resourceType}/folder/${floderId ? floderId : 'root'}/resource/${sourceId}/rename`, { name }, {}, loading)
}

export default { create, listResource, rename, edit, remove, listTree, resourceInfo }