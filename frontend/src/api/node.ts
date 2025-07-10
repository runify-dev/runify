import { Result } from '@/request/Result'
import { get, post, put, del } from '@/request/index'
import type {
    CreateNodePojo,
    QueryNodePojo
} from '@/api/type/node'
import type { Ref } from 'vue'
import { type Node, type EditNodePojo } from '@/api/type/node'
/**
 * 创建节点
 * @param createNodePojo 创建节点对象
 * @param loading loading
 * @returns 
 */
const create: (
    createNodePojo: CreateNodePojo,
    loading?: Ref<boolean>
) => Promise<Result<Node>> = (createNodePojo, loading) => {
    return post(`/node`, createNodePojo, undefined, loading)
}
/**
 * 获取节点列表
 * @param loading  
 * @returns 
 */
const list: (
    query?: QueryNodePojo,
    loading?: Ref<boolean>
) => Promise<Result<Array<Node>>> = (query, loading) => {
    return get(`/node`, query, loading)
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

/**
 * 删除节点 根据节点id
 * @param nodeId  节点id
 * @param loading 加载器
 * @returns 
 */
const remove: (
    nodeId: string,
    loading?: Ref<boolean>
) => Promise<Result<Node>> = (nodeId, loading) => {
    return del(`/node/${nodeId}`, undefined, loading)
}

export default { create, list, edit, remove }