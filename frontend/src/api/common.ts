class ConversationStream {
  response: any
  reader?: any
  onStream: (chunk: any) => void
  /**
   * 结束
   */
  onFinish: () => void
  /**
   * 错误
   */
  onFailure: (e: any) => void

  tempChunk = ''
  constructor(response: any,
    onStream: (chunk: any) => void,
    onFinish: () => void,
    onFailure: (e: any) => void) {
    this.response = response;
    this.onStream = onStream;
    this.onFailure = onFailure;
    this.onFinish = onFinish

  }
  stream() {
    this.response.then((res: any) => {
      this.reader = res.body.getReader();
      this.reader.read().then(this.write_stream)
    })
  }

  write_stream = ({ done, value }: { done: boolean; value: any }) => {
    try {
      if (done) {
        this.onFinish()
        return
      }
      const decoder = new TextDecoder('utf-8')
      let str = decoder.decode(value, { stream: true })
      // 这里解释一下 start 因为数据流返回流并不是按照后端chunk返回 我们希望得到的chunk是data:{xxx}\n\n 但是它获取到的可能是 data:{ -> xxx}\n\n 总而言之就是 fetch不能保证每个chunk都说以data:开始 \n\n结束
      this.tempChunk += str
      const split = this.tempChunk.match(/data:.*}\n\n/g)
      if (split) {
        str = split.join('')
        this.tempChunk = this.tempChunk.replace(str, '')
      } else {
        return this.reader.read().then(this.write_stream)
      }
      // 这里解释一下 end
      if (str && str.startsWith('data:')) {
        if (split) {
          for (const index in split) {
            const chunk = JSON?.parse(split[index].replace('data:', ''))
            this.onStream(chunk)
          }
        }
      }
    } catch (e) {
      return this.onFailure(e)
    }
    return this.reader.read().then(this.write_stream)
  }

}
export { ConversationStream }
