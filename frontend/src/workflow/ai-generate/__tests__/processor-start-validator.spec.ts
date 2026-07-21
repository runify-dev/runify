import { describe, it, expect } from 'vitest'
import { validate } from '@/workflow/nodes/start-node/content/processor/validator'

function httpNodeData(meta: Record<string, any>) {
  return { protocol: 'HTTP', meta }
}

describe('处理器开始节点校验', () => {
  it('未配置（无 nodeData / 无 meta）时不放行', () => {
    expect(validate(undefined).valid).toBe(false)
    expect(validate({}).valid).toBe(false)
    expect(validate({ protocol: 'HTTP' }).valid).toBe(false)
  })

  it('最小合法配置通过', () => {
    const result = validate(httpNodeData({ method: 'GET', path: '/users' }))
    expect(result.valid).toBe(true)
  })

  it('method 必须是合法请求方式', () => {
    expect(validate(httpNodeData({ method: '', path: '/users' })).valid).toBe(false)
    expect(validate(httpNodeData({ method: 'FETCH', path: '/users' })).valid).toBe(false)
  })

  it('path 必须以 / 开头且不含空格', () => {
    expect(validate(httpNodeData({ method: 'GET', path: '' })).valid).toBe(false)
    expect(validate(httpNodeData({ method: 'GET', path: 'users' })).valid).toBe(false)
    expect(validate(httpNodeData({ method: 'GET', path: '/users /list' })).valid).toBe(false)
  })

  it('参数项必须完整（参数名/描述/位置/类型）', () => {
    const bad = validate(
      httpNodeData({
        method: 'GET',
        path: '/users',
        parameters: [{ field: '', description: '', location: 'body', type: 'text' }]
      })
    )
    expect(bad.valid).toBe(false)
    expect(bad.errors['parameters.0.field']).toBeTruthy()
    expect(bad.errors['parameters.0.description']).toBeTruthy()
    expect(bad.errors['parameters.0.location']).toBeTruthy()
    expect(bad.errors['parameters.0.type']).toBeTruthy()

    const good = validate(
      httpNodeData({
        method: 'GET',
        path: '/users',
        parameters: [{ field: 'pageNo', description: '页码', location: 'query', type: 'integer', required: false, many: false }]
      })
    )
    expect(good.valid).toBe(true)
  })

  it('参数名重复报错', () => {
    const result = validate(
      httpNodeData({
        method: 'GET',
        path: '/users',
        parameters: [
          { field: 'id', description: 'a', location: 'query', type: 'string' },
          { field: 'id', description: 'b', location: 'query', type: 'string' }
        ]
      })
    )
    expect(result.valid).toBe(false)
    expect(result.errors['parameters.1.field']).toContain('重复')
  })

  it('路径参数与请求地址双向对应', () => {
    // 声明了 path 参数但地址里没有 :id
    const missingInPath = validate(
      httpNodeData({
        method: 'GET',
        path: '/users',
        parameters: [{ field: 'id', description: '用户id', location: 'path', type: 'uuid' }]
      })
    )
    expect(missingInPath.valid).toBe(false)
    expect(missingInPath.errors['parameters.0.field']).toContain(':id')

    // 地址里有 :id 但参数列表没声明
    const missingDeclared = validate(httpNodeData({ method: 'GET', path: '/users/:id' }))
    expect(missingDeclared.valid).toBe(false)
    expect(missingDeclared.errors['path']).toContain(':id')

    // 两边对上则通过
    const good = validate(
      httpNodeData({
        method: 'GET',
        path: '/users/:id',
        parameters: [{ field: 'id', description: '用户id', location: 'path', type: 'uuid' }]
      })
    )
    expect(good.valid).toBe(true)
  })

  it('form-data 请求体字段名必填且不重复（json 模式不检查请求体）', () => {
    const dupe = validate(
      httpNodeData({
        method: 'POST',
        path: '/upload',
        contentType: 'multipart/form-data',
        requestBody: [
          { field: 'file', description: '文件', type: 'file' },
          { field: 'file', description: '重复', type: 'file' },
          { field: '', description: '缺名', type: 'string' }
        ]
      })
    )
    expect(dupe.valid).toBe(false)
    expect(dupe.errors['requestBody.1.field']).toContain('重复')
    expect(dupe.errors['requestBody.2.field']).toBeTruthy()

    // application/json 的 requestBody 是结构描述，不做字段校验
    const json = validate(
      httpNodeData({
        method: 'POST',
        path: '/users',
        contentType: 'application/json',
        requestBody: [{ field: '', type: 'object' }]
      })
    )
    expect(json.valid).toBe(true)
  })
})
