interface Node {
  /**
   * 节点名称
   */
  id: string
  /**
   * 父节点名称
   */
  parentId: string
  /**
   * 节点名称
   */
  name: string
  /**
   * 资源类型
   */
  type: string
  /**
   * 创建时间
   */
  createTime: string
  /**
   * 修改时间
   */
  updateTime: string
  [key: string]: any;
}
interface Tree extends Node {
  children?: Tree[];
}

interface CreateFolderPojo {

  /**
   * 节点名称
   */
  name?: string

}
interface QueryNodePojo {
  parentId?: string
  source?: "knowledge" | "application"
  name?: string,
  depth?: number
  type?: "folder" | "file" | "knowledge" | "application"
  star?: boolean
  share?: boolean
}
interface EditNodePojo {
  parentId?: string
  name?: string
}


export type {
  CreateFolderPojo,
}
