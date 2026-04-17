import { Result } from '@/request/Result'
import { get, post, put, del } from '@/request/admin/index'
import type {
    MarkdownNode
} from '@/api/type/knowledge'
import type { Ref } from 'vue'
import { type Node, type EditNodePojo } from '@/api/type/node'
/**
 * 根据nodeid 查询Markdown文档
 * @param node_id  节点id
 * @param loading
 * @returns
 */
const getById: (
    node_id: string,
    loading?: Ref<boolean>
) => Promise<Result<MarkdownNode>> = (node_id, loading) => {
    return get(`/knowledge/markdown/${node_id}`, undefined, loading)
}

const edit: (
    node_id: string,
    content: string,
    loading?: Ref<boolean>
) => Promise<Result<MarkdownNode>> = (node_id, content, loading) => {
    return put(`/knowledge/markdown/${node_id}`, { 'content': content }, undefined, loading)
}



export default {
    getById,
    edit
}
