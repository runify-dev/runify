export type Resource = 'knowledge' | 'application' | 'model'

export type Type = "folder" | "md" | "application" | 'model'

export interface Dict<V> {
  [propName: string]: V
}
