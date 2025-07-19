import { Result } from '@/request/Result'
import { get, post, put, del } from '@/request/index'
import type {
    MarkdownNode
} from '@/api/type/knowledge'
import type { Ref } from 'vue'
import { type KnowledgeEdit } from '@/api/type/knowledge'

const edit: (
    folderId: string,
    resourceId: string,
    knowledge: KnowledgeEdit,
    loading?: Ref<boolean>
) => Promise<Result<MarkdownNode>> = (folderId, resourceId, knowledge, loading) => {
    return put(`/knowledge/folder/${folderId}/resource/${resourceId}`, knowledge, loading)
}


export default {
    edit
}