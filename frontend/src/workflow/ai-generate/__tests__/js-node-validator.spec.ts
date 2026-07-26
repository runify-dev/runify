import { describe, it, expect } from 'vitest'
import { validate } from '@/workflow/nodes/java-script-node/content/validator'

describe('java-script-node validator - parameters', () => {
  const base = { mode: 'script', codeLocation: 'customize', code: 'return 1' }

  it('flags a reference parameter with no value selected', () => {
    const r = validate({
      ...base,
      parameters: [{ field: 'postsResult', location: 'reference' }]
    })
    expect(r.valid).toBe(false)
    expect(r.errors['parameters.0.value']).toBeTruthy()
  })

  it('flags a reference parameter with empty array value', () => {
    const r = validate({
      ...base,
      parameters: [{ field: 'postsResult', location: 'reference', value: [] }]
    })
    expect(r.valid).toBe(false)
  })

  it('passes when all parameters have values', () => {
    const r = validate({
      ...base,
      parameters: [
        { field: 'postsResult', location: 'reference', value: ['start-node', 'posts'] },
        { field: 'countResult', location: 'customize', value: '10' }
      ]
    })
    expect(r.valid).toBe(true)
  })
})
