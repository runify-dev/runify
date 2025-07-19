interface MarkdownNode {
    id: string,
    content: string,
    createTime: string,
    updateTime: string
}
interface KnowledgeEdit {
    name?: string,
    content?: string
}
export type {
    MarkdownNode,
    KnowledgeEdit
}