import { Result } from '@/request/Result'
import { request } from '@/request/admin/index'
import type { FileEntity } from './type/file'


const uploadFile: (data: any, onUploadProgress?: (percent: number) => void) => Promise<Result<FileEntity>> = (data, onUploadProgress) => {
  return request.post('/storage/file', data, {
    onUploadProgress: (e) => {
      console.log('onUploadProgress')
      if (onUploadProgress && e.total) {
        onUploadProgress(Math.round((e.loaded / e.total) * 100))
      }
    }
  }).then(ok => {
    return ok.data
  })

}
export default {
  uploadFile
}
