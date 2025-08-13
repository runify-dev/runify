class Workflow {
  edges: Array<any>
  nodes: Array<any>
  nodeMap: any
  edgeMap: any
  upNodeMap: any
  nextNodeMap: any
  constructor(edges: Array<any>, nodes: Array<any>) {
    this.edges = edges
    this.nodes = nodes
    this.edgeMap = edges.reduce((acc, current) => {
      if (current.id) {
        acc[current.id] = current;
      }
      return acc;
    }, {});
    this.nodeMap = nodes.reduce((acc, current) => {
      if (current.id) {
        acc[current.id] = current;
      }
      return acc;
    }, {});


  }


}

