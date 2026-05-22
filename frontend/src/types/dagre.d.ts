declare module 'dagre' {
  export const graphlib: {
    Graph: new () => any
  }
  export function layout(g: any): void
}
