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
     * 节点类型
     */
    type: "folder" | "file" | "knowledge" | "application"
    /**
     * 节点所属
     */
    source: "knowledge" | "application"

    subtype: string

    excerpt?: string
    /**
     * 节点元数据
     */
    meta: any
    /**
     * 创建时间
     */
    createTime: string
    /**
     * 修改时间
     */
    updateTime: string
}
interface Tree extends Node {
    children?: Tree[];
}

interface CreateNodePojo {
    /**
    * 父节点名称
    */
    parentId: string
    /**
     * 节点名称
     */
    name: string

    /**
     * 节点类型
     */
    type: "folder" | "file" | "knowledge" | "application"
    /**
     * 节点子类型
     */
    subtype: string,
    /**
     * 节点所属
     */
    source: "knowledge" | "application"
    /**
     * 节点元数据
     */
    meta: any

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

interface CurrentNode {
    type: "star" | "share" | "all" | "tree",
    node: "star" | "share" | "all" | "tree" | Tree | any

}
export type {
    Node,
    Tree,
    CreateNodePojo,
    EditNodePojo,
    QueryNodePojo,
    CurrentNode

}
