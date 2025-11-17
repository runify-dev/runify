import { Result } from '@/request/Result'
import { get, post, put, del } from '@/request/index'
import type {
  MarkdownNode
} from '@/api/type/knowledge'
import type { Ref } from 'vue'
import { type KnowledgeEdit } from '@/api/type/knowledge'

const edit: (
  resourceId: string,
  knowledge: KnowledgeEdit,
  loading?: Ref<boolean>
) => Promise<Result<MarkdownNode>> = (resourceId, knowledge, loading) => {
  return put(`/knowledge/resources/${resourceId}`, knowledge, loading)
}


export default {
  edit
}
