import { Result } from '@/request/Result'
import { get, post, put, del } from '@/request/admin/index'
import type { Ref } from 'vue'


const edit: (
  resourceId: string,
  content: string,
  loading?: Ref<boolean>
) => Promise<Result<any>> = (resourceId, content, loading) => {
  return put(`/note/resources/${resourceId}`, { content }, loading)
}


export default {
  edit
}
