import type { Resource } from '@/api/type/common'
import type { TreeNode } from 'primevue/treenode'
import { type Node, type Tree } from "@/api/type/node"
import { cloneDeep } from "lodash"

type Postion = "ROOT" | 'FOLDER' | 'RESOURCE' | 'KNOWLEDGE' | 'APPLICATION' | 'MODEL' | "MARKDOWN" | "NOTE" | "PROJECT"

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


const toTreeNode = (node: Node) => {
  const result: TreeNode = {
    key: node.id,
    label: node.name,
    data: node,
    type: node.type,
    parentId: node.parentId,
    icon: 'custom-check',

  }

  return result;
}

export const toTree = (nodeList: Array<any>) => {
  nodeList = cloneDeep(nodeList).map(toTreeNode)
  const nodeMap = Object.fromEntries(nodeList.map(item => [item.key, item]))
  const childrenList = new Set<string>();
  for (let index = 0; index < nodeList.length; index++) {
    const element = nodeList[index]
    if (!element.children) {
      element.children = []
    }
    if (element.parentId) {
      const pNode = nodeMap[element.parentId]
      if (pNode) {
        if (!pNode.children) {
          pNode.children = []
        }
        childrenList.add(element.key)
        pNode.children.push(element)
      }
    }
  }
  const result = nodeList.filter(item => !childrenList.has(item.key))
  return result
}
type TreeNodeWithParent = {
  node: TreeNode | null;
  parent: TreeNode | null;
};
class TreeManager {
  private tree: TreeNode[];

  constructor(treeData: TreeNode[]) {
    this.tree = treeData
  }

  /**
   * 深拷贝树数据
   */
  private deepClone<T>(obj: T): T {
    return JSON.parse(JSON.stringify(obj));
  }

  /**
   * 获取当前树数据
   */
  getTree(): TreeNode[] {
    return this.deepClone(this.tree);
  }

  /**
   * 根据key查找节点
   */
  findNodeByKey(key: string): TreeNode | null {
    return this.findNodeRecursive(this.tree, key);
  }

  private findNodeRecursive(nodes: TreeNode[], key: string): TreeNode | null {
    for (const node of nodes) {
      if (node.key === key) {
        return node;
      }

      if (node.children && node.children.length > 0) {
        const found = this.findNodeRecursive(node.children, key);
        if (found) return found;
      }
    }
    return null;
  }

  /**
   * 查找父节点
   */
  findParentNode(key: string): TreeNode | null {
    return this.findParentRecursive(this.tree, key, null);
  }

  private findParentRecursive(
    nodes: TreeNode[],
    key: string,
    parent: TreeNode | null
  ): TreeNode | null {
    for (const node of nodes) {
      if (node.key === key) {
        return parent;
      }

      if (node.children && node.children.length > 0) {
        const found = this.findParentRecursive(node.children, key, node);
        if (found) return found;
      }
    }
    return null;
  }

  /**
   * 删除节点 - 直接修改原树
   */
  remove(key: string): void {
    this.removeRecursive(this.tree, key);
  }

  private removeRecursive(nodes: TreeNode[], key: string): boolean {
    // 在当前层级查找并移除
    const index = nodes.findIndex(node => node.key === key);
    if (index !== -1) {
      nodes.splice(index, 1);
      return true;
    }

    // 递归查找子层级
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        const removed = this.removeRecursive(node.children, key);
        if (removed) {
          // 如果子节点被删除后，子节点数组为空，可以清理空数组
          if (node.children.length === 0) {
            delete node.children;
          }
          return true;
        }
      }
    }

    return false;
  }

  /**
   * 在指定节点前插入 - 直接修改原树
   */
  insertBefore(targetKey: string, newNode: TreeNode): boolean {
    return this.insertBeforeRecursive(this.tree, targetKey, newNode);
  }

  private insertBeforeRecursive(
    nodes: TreeNode[],
    targetKey: string,
    newNode: TreeNode
  ): boolean {
    const index = nodes.findIndex(node => node.key === targetKey);
    if (index !== -1) {
      nodes.splice(index, 0, newNode);
      return true;
    }

    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        const inserted = this.insertBeforeRecursive(node.children, targetKey, newNode);
        if (inserted) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * 在指定节点后插入 - 直接修改原树
   */
  insertAfter(targetKey: string, newNode: TreeNode): boolean {
    return this.insertAfterRecursive(this.tree, targetKey, newNode);
  }

  private insertAfterRecursive(
    nodes: TreeNode[],
    targetKey: string,
    newNode: TreeNode
  ): boolean {
    const index = nodes.findIndex(node => node.key === targetKey);
    if (index !== -1) {
      nodes.splice(index + 1, 0, newNode);
      return true;
    }

    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        const inserted = this.insertAfterRecursive(node.children, targetKey, newNode);
        if (inserted) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * 添加子节点 - 直接修改原树
   */
  addChild(parentKey: string, newChild: TreeNode): boolean {
    if (parentKey) {
      return this.addChildRecursive(this.tree, parentKey, newChild);
    }
    this.tree.push(newChild)
    return true
  }

  private addChildRecursive(
    nodes: TreeNode[],
    parentKey: string,
    newChild: TreeNode
  ): boolean {
    for (const node of nodes) {
      if (node.key === parentKey) {
        if (!node.children) {
          node.children = [];
        }
        node.children.push(newChild);
        return true;
      }

      if (node.children && node.children.length > 0) {
        const added = this.addChildRecursive(node.children, parentKey, newChild);
        if (added) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * 更新节点数据 - 直接修改原树
   */
  updateNode(key: string, updates: Partial<TreeNode>): boolean {
    return this.updateNodeRecursive(this.tree, key, updates);
  }

  private updateNodeRecursive(
    nodes: TreeNode[],
    key: string,
    updates: Partial<TreeNode>
  ): boolean {
    for (const node of nodes) {
      if (node.key === key) {
        Object.assign(node, updates);
        return true;
      }

      if (node.children && node.children.length > 0) {
        const updated = this.updateNodeRecursive(node.children, key, updates);
        if (updated) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * 获取节点深度（根节点为1）
   */
  getNodeDepth(key: string): number {
    return this.getDepthRecursive(this.tree, key, 1);
  }

  private getDepthRecursive(
    nodes: TreeNode[],
    key: string,
    currentDepth: number
  ): number {
    for (const node of nodes) {
      if (node.key === key) {
        return currentDepth;
      }

      if (node.children && node.children.length > 0) {
        const depth = this.getDepthRecursive(node.children, key, currentDepth + 1);
        if (depth > 0) return depth;
      }
    }
    return 0;
  }

  /**
   * 遍历所有节点
   */
  traverse(callback: (node: TreeNode) => void): void {
    this.traverseRecursive(this.tree, callback);
  }

  private traverseRecursive(nodes: TreeNode[], callback: (node: TreeNode) => void): void {
    for (const node of nodes) {
      callback(node);

      if (node.children && node.children.length > 0) {
        this.traverseRecursive(node.children, callback);
      }
    }
  }

  /**
   * 生成新的唯一key
   */
  generateUniqueKey(prefix: string = 'node'): string {
    const timestamp = Date.now();
    const random = Math.floor(Math.random() * 10000);
    return `${prefix}-${timestamp}-${random}`;
  }
}

export { Processor, Config, TreeManager, toTreeNode }
