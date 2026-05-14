import {randomId} from '@/utils/common'

export const startNode = {
  id: 'start-node',
  type: 'start-node',
  text: '',
  x: 0,
  y: 0,
  label: '开始节点',
  properties: {
    width: 200,
    height: 50,
    name: '开始节点',
    isHovered: false,
    field_list: [
       {
      label:"上下文",
      value: 'messages'
    },
      {
        label: '用户问题',
        value: 'question'
      },
      {
        label: "图片",
        value: ""
      },
      {
        label: "视频",
        value: ""
      }
    ]
  }
}
export const aiChatNode = {
  type: 'ai-chat-node',
  text: '',
  x: 0,
  y: 0,
  label: 'ai对话',
  properties: {
    width: 200,
    height: 50,
    name: 'ai对话',
    isHovered: false,
    field_list: [
      {
        label: '回答',
        value: 'content',
        children: [
          {
            label: '回答',
            value: 'content'
          }
        ]
      }
    ]
  }
}

export const javaScriptNode = {
  type: 'java-script-node',
  text: '',
  x: 0,
  y: 0,
  label: 'javaScript执行',
  properties: {
    width: 200,
    height: 50,
    name: 'javaScript执行',
    isHovered: false,
    field_list: [
      {
        label: '结果',
        value: 'result'
      }
    ],
    nodeData: {
      mode: 'script',
      codeLocation: 'customize',
      codeReference: [],
      functionName: '',
      code: '',
      allowIO: false,
      parameters: []
    }
  }
}

export const databaseSearchNode = {
  type: 'database-search-node',
  text: '',
  x: 0,
  y: 0,
  label: '数据库检索',
  properties: {
    width: 200,
    height: 50,
    name: '数据库检索',
    isHovered: false,
    field_list: [],
    nodeData: {
      poolId: '',
      location: 'customize',
      reference: [],
      template: '',
      parameters: []
    }
  }
}

export const databaseInsertNode = {
  type: 'database-insert-node',
  text: '',
  x: 0,
  y: 0,
  label: '数据库插入',
  properties: {
    width: 200,
    height: 50,
    name: '数据库插入',
    isHovered: false,
    field_list: [],
    nodeData: {
      poolId: '',
      location: 'customize',
      reference: [],
      template: '',
      parameters: []
    }
  }
}

export const cacheQueryNode = {
  type: 'cache-query-node',
  text: '',
  x: 0,
  y: 0,
  label: '缓存查询',
  properties: {
    width: 200,
    height: 50,
    name: '缓存查询',
    isHovered: false,
    field_list: [{ label: '结果', value: 'result' }],
    nodeData: {
      cacheId: '',
      keyLocation: 'customize',
      keyReference: [],
      key: ''
    }
  }
}

export const cacheWriteNode = {
  type: 'cache-write-node',
  text: '',
  x: 0,
  y: 0,
  label: '缓存写入',
  properties: {
    width: 200,
    height: 50,
    name: '缓存写入',
    isHovered: false,
    field_list: [{ label: '成功', value: 'success' }],
    nodeData: {
      cacheId: '',
      keyLocation: 'customize',
      keyReference: [],
      key: '',
      valueLocation: 'customize',
      valueReference: [],
      value: '',
      ttl: null
    }
  }
}

export const fileUploadNode = {
  type: 'file-upload-node',
  text: '',
  x: 0,
  y: 0,
  label: '文件上传',
  properties: {
    width: 200,
    height: 50,
    name: '文件上传',
    isHovered: false,
    field_list: [
      { label: '文件ID', value: 'fileId' },
      { label: '文件名', value: 'fileName' },
      { label: '文件大小', value: 'fileSize' }
    ],
    nodeData: {
      location: 'customize',
      reference: [],
      pathLocation: 'customize',
      pathReference: [],
      path: '',
      fileName: ''
    }
  }
}

export const noteSearchNode = {
  type: 'note-search-node',
  text: '',
  x: 0,
  y: 0,
  label: '笔记检索',
  properties: {
    width: 200,
    height: 50,
    name: '笔记检索',
    isHovered: false,
    field_list: [
      { label: '结果列表', value: 'hits' },
      { label: '总数', value: 'total' },
      { label: '最高分', value: 'topScore' }
    ],
    nodeData: {
      folderIds: [],
      keywordLocation: 'customize',
      keywordReference: [],
      keyword: '',
      pageNoLocation: 'customize',
      pageNoReference: [],
      pageNo: 1,
      pageSizeLocation: 'customize',
      pageSizeReference: [],
      pageSize: 10
    }
  }
}

export const jsonResponseNode = {
  type: 'response-node',
  text: '',
  x: 0,
  y: 0,
  label: '数据响应',
  properties: {
    width: 200,
    height: 50,
    name: '数据响应',
    isHovered: false,
    field_list: []
  }
}
export const loopStartNode = {
  id: 'loop-start-node',
  type: 'loop-start-node',
  text: '',
  x: 0,
  y: 0,
  label: '循环开始',
  properties: {
    width: 200,
    height: 50,
    name: '循环开始',
    isHovered: false,
    field_list: [
    
    ]
  }
}

export const loopNode = {
  type: 'loop-node',
  text: '',
  x: 0,
  y: 0,
  label: '循环',
  properties: {
    width: 280,
    height: 50,
    expanded: false,
    name: '循环',
    isHovered: false,
    field_list: [
      {
        label: '当前项',
        value: 'item'
      },
      {
        label: '当前索引',
        value: 'index'
      }
    ],
    nodeData: {
      loopType: 'foreach',
      children: {nodes: [], edges: []}
    }
  }
}

export const loopContinueNode = {
  type: 'loop-continue-node',
  text: '',
  x: 0,
  y: 0,
  label: '循环跳过',
  properties: {
    width: 200,
    height: 50,
    name: '循环跳过',
    isHovered: false,
    field_list: [],
    nodeData: {
      conditions: [],
      logic: 'and'
    }
  }
}

export const loopBreakNode = {
  type: 'loop-break-node',
  text: '',
  x: 0,
  y: 0,
  label: '跳出循环',
  properties: {
    width: 200,
    height: 50,
    name: '跳出循环',
    isHovered: false,
    field_list: [],
    nodeData: {
      conditions: [],
      logic: 'and'
    }
  }
}

export const defaulBranches = [
  {
    id: randomId(),
    type: 'if',
    logic: 'and',
    conditions: [
      {
        id: randomId(),
        variable: [],
        compare: 'eq',
        value: ''
      }
    ]
  },
  {id: randomId(), type: 'else', logic: 'and', conditions: []}
]
export const judgeNode = {
  type: 'judge-node',
  text: '',
  x: 0,
  y: 0,
  label: '条件判断',
  properties: {
    width: 200,
    height: 128,
    name: '条件判断',
    isHovered: false,
    field_list: [],
    nodeData: {
      branches: [...defaulBranches]
    }
  }
}

export const terminalNode = {
  type: 'terminal-node',
  text: '',
  x: 0,
  y: 0,
  label: '终端执行',
  properties: {
    width: 200,
    height: 50,
    name: '终端执行',
    isHovered: false,
    field_list: [
      { label: '执行结果', value: 'result' },
      { label: '标准输出', value: 'stdout' },
      { label: '错误输出', value: 'stderr' },
      { label: '退出码', value: 'exitCode' }
    ],
    nodeData: {
      location: 'customize',
      reference: [],
      code: ''
    }
  }
}

export const variableAssignNode = {
  type: 'variable-assign-node',
  text: '',
  x: 0,
  y: 0,
  label: '变量赋值',
  properties: {
    width: 200,
    height: 50,
    name: '变量赋值',
    isHovered: false,
    field_list: [],
    nodeData: {
      variables: []
    }
  }
}

export const approvalNode = {
  type: 'approval-node',
  text: '',
  x: 0,
  y: 0,
  label: '审批',
  properties: {
    width: 200,
    height: 50,
    name: '审批',
    isHovered: false,
    field_list: [{ label: '审批结果', value: 'approved' }],
    nodeData: {
      location: 'customize',
      reference: [],
      prompt: ''
    }
  }
}

export const globNode = {
  type: 'glob-node',
  text: '',
  x: 0,
  y: 0,
  label: '文件搜索',
  properties: {
    width: 200,
    height: 50,
    name: '文件搜索',
    isHovered: false,
    field_list: [
      { label: '文件列表', value: 'content' },
      { label: '摘要', value: 'summary' },
      { label: '文件数', value: 'files' }
    ],
    nodeData: {
      location: 'customize', reference: [],
      patternLocation: 'customize', patternReference: [], pattern: '',
      pathLocation: 'customize', pathReference: [], path: '',
      maxResultsLocation: 'customize', maxResultsReference: [], maxResults: null
    }
  }
}

export const grepNode = {
  type: 'grep-node',
  text: '',
  x: 0,
  y: 0,
  label: '内容搜索',
  properties: {
    width: 200,
    height: 50,
    name: '内容搜索',
    isHovered: false,
    field_list: [
      { label: '搜索结果', value: 'content' },
      { label: '摘要', value: 'summary' },
      { label: '匹配数', value: 'matches' },
      { label: '文件数', value: 'files' }
    ],
    nodeData: {
      location: 'customize', reference: [],
      patternLocation: 'customize', patternReference: [], pattern: '',
      pathLocation: 'customize', pathReference: [], path: '',
      filePatternLocation: 'customize', filePatternReference: [], filePattern: '',
      contextLinesLocation: 'customize', contextLinesReference: [], contextLines: null,
      maxResultsLocation: 'customize', maxResultsReference: [], maxResults: null
    }
  }
}

export const extractNode = {
  type: 'extract-node',
  text: '',
  x: 0,
  y: 0,
  label: '参数提取',
  properties: {
    width: 200,
    height: 50,
    name: '参数提取',
    isHovered: false,
    field_list: [],
    nodeData: {
      sourceReference: [],
      rules: []
    }
  }
}

export const listDirNode = {
  type: 'list-dir-node',
  text: '',
  x: 0,
  y: 0,
  label: '目录列表',
  properties: {
    width: 200,
    height: 50,
    name: '目录列表',
    isHovered: false,
    field_list: [
      { label: '目录树', value: 'content' },
      { label: '摘要', value: 'summary' },
      { label: '文件数', value: 'files' },
      { label: '目录数', value: 'dirs' }
    ],
    nodeData: {
      location: 'customize',
      reference: [],
      pathLocation: 'customize',
      pathReference: [],
      path: '',
      depthLocation: 'customize',
      depthReference: [],
      depth: null
    }
  }
}

export const readFileNode = {
  type: 'read-file-node',
  text: '',
  x: 0,
  y: 0,
  label: '读取文件',
  properties: {
    width: 200,
    height: 50,
    name: '读取文件',
    isHovered: false,
    field_list: [
      { label: '带行号内容', value: 'content' },
      { label: '原始内容', value: 'rawContent' },
      { label: '总行数', value: 'totalLines' },
      { label: '读取行数', value: 'lines' }
    ],
    nodeData: {
      location: 'customize',
      reference: [],
      pathLocation: 'customize',
      pathReference: [],
      path: '',
      offsetLocation: 'customize',
      offsetReference: [],
      offset: null,
      limitLocation: 'customize',
      limitReference: [],
      limit: null
    }
  }
}

export const applyPatchNode = {
  type: 'apply-patch-node',
  text: '',
  x: 0,
  y: 0,
  label: '数据修补',
  properties: {
    width: 200,
    height: 50,
    name: '数据修补',
    isHovered: false,
    field_list: [
      { label: '执行结果', value: 'result' },
      { label: '标准输出', value: 'stdout' },
      { label: '错误输出', value: 'stderr' }
    ],
    nodeData: {
      location: 'customize',
      reference: [],
      pathLocation: 'customize',
      pathReference: [],
      path: '',
      patchLocation: 'customize',
      patchReference: [],
      patch: ''
    }
  }
}

export const contextPushNode = {
  type: 'context-push-node',
  text: '',
  x: 0,
  y: 0,
  label: '推送上下文',
  properties: {
    width: 200,
    height: 50,
    name: '推送上下文',
    isHovered: false,
    field_list: [],
    nodeData: {
      items: []
    }
  }
}

export const baseWorkflow = {
  nodes: [startNode],
  edges: []
}

export enum WorkflowType {
  APPLICATION = 'APPLICATION',
  PROCESSOR = 'PROCESSOR',
  APPLICATION_LOOP = 'APPLICATION_LOOP',
  PROCESSOR_LOOP = 'PROCESSOR_LOOP'
}

export const iconMap = {
  connected: (
    color: string
  ) => `<svg width="100%" height="100%" viewBox="0 0 42 42" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <g filter="url(#filter0_d_5119_232585)">
                  <path d="M20.9998 29.8333C28.0875 29.8333 33.8332 24.0876 33.8332 17C33.8332 9.91231 28.0875 4.16663 20.9998 4.16663C13.9122 4.16663 8.1665 9.91231 8.1665 17C8.1665 24.0876 13.9122 29.8333 20.9998 29.8333Z" fill="white"/>
                  <path fill-rule="evenodd" clip-rule="evenodd" d="M20.9998 27.5C26.7988 27.5 31.4998 22.799 31.4998 17C31.4998 11.201 26.7988 6.49996 20.9998 6.49996C15.2008 6.49996 10.4998 11.201 10.4998 17C10.4998 22.799 15.2008 27.5 20.9998 27.5ZM33.8332 17C33.8332 24.0876 28.0875 29.8333 20.9998 29.8333C13.9122 29.8333 8.1665 24.0876 8.1665 17C8.1665 9.91231 13.9122 4.16663 20.9998 4.16663C28.0875 4.16663 33.8332 9.91231 33.8332 17Z" fill="${color}"/>
                  </g>
                  <defs>
                  <filter id="filter0_d_5119_232585" x="-1" y="-1" width="44" height="44" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
                  <feFlood flood-opacity="0" result="BackgroundImageFix"/>
                  <feColorMatrix in="SourceAlpha" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"/>
                  <feOffset dy="4"/>
                  <feGaussianBlur stdDeviation="4"/>
                  <feComposite in2="hardAlpha" operator="out"/>
                  <feColorMatrix type="matrix" values="0 0 0 0 0.2 0 0 0 0 0.439216 0 0 0 0 1 0 0 0 0.1 0"/>
                  <feBlend mode="normal" in2="BackgroundImageFix" result="effect1_dropShadow_5119_232585"/>
                  <feBlend mode="normal" in="SourceGraphic" in2="effect1_dropShadow_5119_232585" result="shape"/>
                  </filter>
                  </defs>
                  </svg>`,
  not_connected: (
    color: string
  ) => `<svg width="100%" height="100%" viewBox="0 0 42 42" fill="none" xmlns="http://www.w3.org/2000/svg">
            <g filter="url(#filter0_d_5199_166905)">
            <path d="M20.9998 29.8333C28.0875 29.8333 33.8332 24.0876 33.8332 17C33.8332 9.91231 28.0875 4.16663 20.9998 4.16663C13.9122 4.16663 8.1665 9.91231 8.1665 17C8.1665 24.0876 13.9122 29.8333 20.9998 29.8333Z" fill="${color}"/>
            <path d="M19.8332 11.75C19.8332 11.4278 20.0943 11.1666 20.4165 11.1666H21.5832C21.9053 11.1666 22.1665 11.4278 22.1665 11.75V15.8333H26.2498C26.572 15.8333 26.8332 16.0945 26.8332 16.4166V17.5833C26.8332 17.9055 26.572 18.1666 26.2498 18.1666H22.1665V22.25C22.1665 22.5721 21.9053 22.8333 21.5832 22.8333H20.4165C20.0943 22.8333 19.8332 22.5721 19.8332 22.25V18.1666H15.7498C15.4277 18.1666 15.1665 17.9055 15.1665 17.5833V16.4166C15.1665 16.0945 15.4277 15.8333 15.7498 15.8333H19.8332V11.75Z" fill="white"/>
            </g>
            <defs>
            <filter id="filter0_d_5199_166905" x="-1" y="-1" width="44" height="44" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
            <feFlood flood-opacity="0" result="BackgroundImageFix"/>
            <feColorMatrix in="SourceAlpha" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"/>
            <feOffset dy="4"/>
            <feGaussianBlur stdDeviation="4"/>
            <feComposite in2="hardAlpha" operator="out"/>
            <feColorMatrix type="matrix" values="0 0 0 0 0.2 0 0 0 0 0.439216 0 0 0 0 1 0 0 0 0.1 0"/>
            <feBlend mode="normal" in2="BackgroundImageFix" result="effect1_dropShadow_5199_166905"/>
            <feBlend mode="normal" in="SourceGraphic" in2="effect1_dropShadow_5199_166905" result="shape"/>
            </filter>
            </defs>
            </svg>`,
  close: (color: string) => ` <svg
      @click="deleteEdge"
      width="22"
      height="22"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M12 23.0001C5.925 23.0001 1 18.0751 1 12.0001C1 5.92512 5.925 1.00012 12 1.00012C18.075 1.00012 23 5.92512 23 12.0001C23 18.0751 18.075 23.0001 12 23.0001Z"
        fill="#3370FF"
      />
      <path
        d="M9.02524 7.61124L12.0002 10.5862L14.9752 7.61124C15.069 7.5175 15.1962 7.46484 15.3287 7.46484C15.4613 7.46484 15.5885 7.5175 15.6822 7.61124L16.3892 8.31824C16.483 8.412 16.5356 8.53915 16.5356 8.67174C16.5356 8.80432 16.483 8.93147 16.3892 9.02524L13.4142 12.0002L16.3892 14.9752C16.483 15.069 16.5356 15.1962 16.5356 15.3287C16.5356 15.4613 16.483 15.5885 16.3892 15.6822L15.6822 16.3892C15.5885 16.483 15.4613 16.5356 15.3287 16.5356C15.1962 16.5356 15.069 16.483 14.9752 16.3892L12.0002 13.4142L9.02524 16.3892C8.93147 16.483 8.80432 16.5356 8.67174 16.5356C8.53916 16.5356 8.412 16.483 8.31824 16.3892L7.61124 15.6822C7.5175 15.5885 7.46484 15.4613 7.46484 15.3287C7.46484 15.1962 7.5175 15.069 7.61124 14.9752L10.5862 12.0002L7.61124 9.02524C7.5175 8.93147 7.46484 8.80432 7.46484 8.67174C7.46484 8.53915 7.5175 8.412 7.61124 8.31824L8.31824 7.61124C8.412 7.5175 8.53916 7.46484 8.67174 7.46484C8.80432 7.46484 8.93147 7.5175 9.02524 7.61124Z"
        fill="white"
      />
    </svg>`
}

export const anchorIconMap: any = {
  left: {
    success: {
      connected: iconMap.connected('var(--p-primary-color)'),
      not_connected: iconMap.not_connected('var(--p-primary-color)')
    },
    fail: {
      connected: iconMap.connected('var(--p-red-500)'),
      not_connected: iconMap.not_connected('var(--p-red-500)')
    }
  },
  right: {
    success: {
      connected: iconMap.connected('var(--p-primary-color)'),
      not_connected: iconMap.not_connected('var(--p-primary-color)')
    },
    fail: {
      connected: iconMap.connected('var(--p-red-500)'),
      not_connected: iconMap.not_connected('var(--p-red-500)')
    }
  }
}
