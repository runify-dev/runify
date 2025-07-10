import { Result } from '@/request/Result'
import { get, post, put, del } from '@/request/index'
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
const oneMarkdown: (
    node_id: String,
    loading?: Ref<boolean>
) => Promise<Result<MarkdownNode>> = (node_id, loading) => {
    return get(`/knowledge/markdown/${node_id}`, undefined, loading)
}


export default {
    oneMarkdown
}