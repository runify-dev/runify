export type Resource = 'knowledge' | 'application' | 'model' | 'note'

export type Type = "folder" | "md" | "application" | 'model'

export interface Dict<V> {
  [propName: string]: V
}
