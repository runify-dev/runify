import type { Resource } from '@/api/type/common'

type Postion = "ROOT" | 'FOLDER' | 'RESOURCE' | 'KNOWLEDGE' | 'APPLICATION' | 'MODEL' | "MARKDOWN" | "NOTE"

class Processor {
  label: string
  icon: string
  position: Array<Postion>
  execute: (event: any) => void
  constructor(label: string, icon: string, position: Array<Postion>, execute: (event: any) => void) {
    this.label = label
    this.icon = icon
    this.position = position
    this.execute = execute
  }
}
class Config {
  processor: Array<Processor>
  resource: Resource
  modifyName: (event: any) => Promise<any>
  go: (id: string, data?: any) => void
  constructor(
    resource: Resource,
    processors: Array<Processor>,
    modifyName: (event: any) => Promise<any>,
    go: (id: string, data?: any) => void) {
    this.go = go
    this.resource = resource
    this.processor = processors
    this.modifyName = modifyName
  }
  getProcessor(position: Postion) {
    return this.processor.filter(p => p.position.includes(position))
  }
}


export { Processor, Config }
