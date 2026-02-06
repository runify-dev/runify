export interface Options {
  labelKey?: string;
  valueKey?: string;
  childrenKey?: string;
  getLabel?: (node: any) => any;
  getValue?: (node: any) => any;
  includeSelf?: boolean;
  pathSeparator?: string;
}

/**
 * 为树形数据添加完整路径信息
 * @param {Array} treeData - 原始树形数据
 * @param {Object} options - 配置选项
 * @param {string} options.labelKey - 标签字段名，默认 'label'
 * @param {string} options.valueKey - 值字段名，默认 'value'
 * @param {string} options.childrenKey - 子节点字段名，默认 'children'
 * @param {Function} options.getLabel - 自定义获取标签的函数
 * @param {Function} options.getValue - 自定义获取值的函数
 * @param {boolean} options.includeSelf - 路径中是否包含自身，默认 true
 * @param {string} options.pathSeparator - 路径分隔符（用于字符串路径），默认 '/'
 * @returns {Array} 处理后的树形数据
 */
export function enhanceTreeWithPaths(treeData: Array<any>, options: Options = {}) {
  const { labelKey = 'label', valueKey = 'value', childrenKey = 'children', getLabel = null, getValue = null, includeSelf = true, pathSeparator = '/' } = options;

  // 内部处理函数
  const processNode = (node: any, parentPath: Array<any> = [], parentValuePath: Array<any> = []) => {
    // 获取节点的标签和值
    const label = getLabel ? getLabel(node) : node[labelKey] || node.value || '';
    const value = getValue ? getValue(node) : node[valueKey] || node.id || label;

    // 构建完整路径
    const fullPath = includeSelf ? [...parentPath, label] : parentPath;
    const fullValuePath = includeSelf ? [...parentValuePath, value] : parentValuePath;

    // 字符串形式的路径（方便显示）
    const fullPathString = fullPath.join(pathSeparator);
    const fullValueString = fullValuePath.join(pathSeparator);

    // 创建处理后的节点
    const enhancedNode = {
      ...node,
      // 保持原始字段
      [labelKey]: label,
      [valueKey]: value,
      // 添加路径信息
      _originalLabel: node[labelKey] || label,
      _originalValue: node[valueKey] || value,
      fullPath: fullPath, // 标签路径数组
      fullValue: fullValuePath, // 值路径数组
      fullPathString, // 标签路径字符串
      fullValueString, // 值路径字符串
      // 深度信息
      depth: parentPath.length + (includeSelf ? 1 : 0),
      isLeaf: false // 先标记为非叶子节点，后面会更新
    };

    // 处理子节点
    if (node[childrenKey] && Array.isArray(node[childrenKey]) && node[childrenKey].length > 0) {
      // 递归处理子节点
      enhancedNode[childrenKey] = node[childrenKey].map((child) => processNode(child, includeSelf ? fullPath : parentPath, includeSelf ? fullValuePath : parentValuePath));
      // 更新叶子节点状态
      enhancedNode.isLeaf = false;
    } else {
      enhancedNode.isLeaf = true;
    }

    return enhancedNode;
  };

  // 处理根节点
  return treeData.map((node: any) => processNode(node));
}

/**
 * 根据值路径查找节点
 * @param {Array} treeData - 处理后的树形数据
 * @param {Array} valuePath - 值路径数组
 * @param {Object} options - 配置选项
 * @returns {Object|null} 找到的节点
 */
export function findNodeByValuePath(treeData: Array<any>, valuePath: Array<any>, options: Options = {}) {
  const { valueKey = 'value', childrenKey = 'children' } = options;

  if (!valuePath || valuePath.length === 0) return null;

  const findInNodes = (nodes: Array<any>, depth = 0): any => {
    for (const node of nodes) {
      const nodeValue = node[valueKey] || node.value;

      if (nodeValue === valuePath[depth]) {
        if (depth === valuePath.length - 1) {
          return node;
        }

        if (node[childrenKey] && depth < valuePath.length - 1) {
          const result = findInNodes(node[childrenKey], depth + 1);
          if (result) return result;
        }
      }
    }
    return null;
  };

  return findInNodes(treeData);
}

/**
 * 根据值路径查找节点的所有父节点
 * @param {Array} treeData - 处理后的树形数据
 * @param {Array} valuePath - 值路径数组
 * @param {Object} options - 配置选项
 * @returns {Array} 父节点数组（从根节点到当前节点）
 */
export function findNodeAncestors(treeData: Array<any>, valuePath: Array<any>, options: Options = {}) {
  const { valueKey = 'value', childrenKey = 'children' } = options;

  const ancestors: Array<any> = [];

  const findRecursive = (nodes: Array<any>, depth = 0) => {
    for (const node of nodes) {
      const nodeValue = node[valueKey] || node.value;

      if (nodeValue === valuePath[depth]) {
        ancestors.push(node);

        if (depth === valuePath.length - 1) {
          return true;
        }

        if (node[childrenKey] && depth < valuePath.length - 1) {
          if (findRecursive(node[childrenKey], depth + 1)) {
            return true;
          }
        }

        // 如果没找到子节点，移除当前节点
        ancestors.pop();
        return false;
      }
    }
    return false;
  };

  findRecursive(treeData);
  return ancestors;
}

/**
 * 查找所有叶子节点
 * @param {Array} treeData - 处理后的树形数据
 * @param {Object} options - 配置选项
 * @returns {Array} 叶子节点数组
 */
export function findAllLeafNodes(treeData: Array<any>, options: Options = {}) {
  const { childrenKey = 'children' } = options;

  const leafNodes: Array<any> = [];

  const traverse = (nodes: Array<any>) => {
    for (const node of nodes) {
      if (node.isLeaf) {
        leafNodes.push(node);
      }

      if (node[childrenKey] && node[childrenKey].length > 0) {
        traverse(node[childrenKey]);
      }
    }
  };

  traverse(treeData);
  return leafNodes;
}

/**
 * 根据条件过滤节点
 * @param {Array} treeData - 处理后的树形数据
 * @param {Function} predicate - 过滤条件函数
 * @param {Object} options - 配置选项
 * @returns {Array} 过滤后的节点数组
 */
export function filterTreeNodes(treeData: Array<any>, predicate: any, options: Options = {}) {
  const { childrenKey = 'children' } = options;

  const result: Array<any> = [];

  const traverse = (nodes: any) => {
    for (const node of nodes) {
      if (predicate(node)) {
        result.push(node);
      }

      if (node[childrenKey] && node[childrenKey].length > 0) {
        traverse(node[childrenKey]);
      }
    }
  };

  traverse(treeData);
  return result;
}

/**
 * 根据值路径获取显示文本
 * @param {Array} treeData - 处理后的树形数据
 * @param {Array} valuePath - 值路径数组
 * @param {Object} options - 配置选项
 * @returns {string} 显示文本
 */
export function getDisplayTextByValuePath(treeData: any, valuePath: any, options: Options = {}) {
  const { pathSeparator = '/', labelKey = 'label' } = options;

  const node = findNodeByValuePath(treeData, valuePath, options);
  if (!node) {
    return valuePath ? valuePath.join(pathSeparator) : '';
  }

  // 优先使用 fullPathString，否则构建显示文本
  if (node.fullPathString) {
    return node.fullPathString;
  }

  // 如果没有 fullPathString，使用祖先节点的标签构建
  const ancestors = findNodeAncestors(treeData, valuePath, options);
  return ancestors.map((node) => node[labelKey] || node.label).join(pathSeparator);
}

// 默认导出主函数
export default enhanceTreeWithPaths;
