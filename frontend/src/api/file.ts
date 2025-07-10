import { Result } from '@/request/Result'
import { get, post, put, del } from '@/request/index'
import type { FileEntity } from './type/file'


const uploadFile: (data: any) => Promise<Result<FileEntity>> = (data) => {
    return post('/file', data)
}
export default {
    uploadFile
}