export type Resource = 'knowledge' | 'application' | 'model' | 'note' | 'project' | 'datasource'

export type Type = "folder" | "md" | "application" | 'model'

export interface Dict<V> {
  [propName: string]: V
}
export interface VueModule {
  default: any
}
