import LogicFlow from '@logicflow/core'
import '@logicflow/core/dist/index.css';
import RunEdge from './common/edge'
const nodes: any = import.meta.glob('./nodes/**/index.ts', { eager: true })
export class Workflow {
    lf
    constructor(container: HTMLElement) {
        this.lf = new LogicFlow({
            // 容器配置
            container: container,
            stopZoomGraph: false,
            snapline: true,
            nodeTextEdit: false,
            edgeTextEdit: false,
            nodeTextDraggable: false,
            edgeTextDraggable: false,
            background: {
                color: '#F0F0F0',
                backgroundColor: '#f5f6f7'
            },
            grid: {
                size: 10,
                visible: true,
                type: 'dot',
                config: {
                    color: '#ababab',
                    thickness: 1,
                },
            },
            keyboard: {
                enabled: true
            },
        })
        this.lf.setTheme({
            bezier: {
                stroke: '#afafaf',
                strokeWidth: 1
            }
        })
        this.lf.batchRegister(Object.keys(nodes).map((key) => nodes[key].default))


    }

    render(data?: LogicFlow.GraphConfigData) {
        this.lf.render(data ? data : {})
    }
}
