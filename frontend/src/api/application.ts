import { Result } from '@/request/Result'
import { get, post, put, del } from '@/request/index'
import type {
    MarkdownNode
} from '@/api/type/knowledge'
import type { Ref } from 'vue'


const edit: (
    folderId: string,
    node_id: String,
    workflow: any,
    loading?: Ref<boolean>
) => Promise<Result<MarkdownNode>> = (folderId, node_id, workflow, loading) => {
    return put(`/application/folder/${folderId}/resource/${node_id}`, { 'workflow': workflow }, undefined, loading)
}



export default {
    edit
}