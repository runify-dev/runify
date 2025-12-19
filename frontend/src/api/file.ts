import { Result } from '@/request/Result'
import { get, post, put, del } from '@/request/admin/index'
import type { FileEntity } from './type/file'


const uploadFile: (data: any) => Promise<Result<FileEntity>> = (data) => {
  return post('/storage/file', data)
}
export default {
  uploadFile
}
