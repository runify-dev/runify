import LogicFlow from '@logicflow/core'
import '@logicflow/core/dist/index.css';
import RunEdge from './common/edge'
const nodes: any = import.meta.glob('./nodes/**/index.ts', { eager: true })

const getVar = (name: string, fallback: string) => {
    if (typeof document === 'undefined') return fallback
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
}

export class Workflow {
    lf
    constructor(container: HTMLElement) {
        this.lf = new LogicFlow({
            container: container,
            stopZoomGraph: false,
            snapline: true,
            nodeTextEdit: false,
            edgeTextEdit: false,
            nodeTextDraggable: false,
            edgeTextDraggable: false,
            background: {
                color: getVar('--p-content-background', '#f5f6f7'),
                backgroundColor: getVar('--p-content-background', '#f5f6f7')
            },
            grid: {
                size: 10,
                visible: true,
                type: 'dot',
                config: {
                    color: getVar('--p-surface-400', '#ababab'),
                    thickness: 1,
                },
            },
            keyboard: {
                enabled: true
            },
        })
        this.lf.setTheme({
            bezier: {
                stroke: getVar('--p-content-border-color', '#afafaf'),
                strokeWidth: 1
            }
        })
        this.lf.batchRegister(Object.keys(nodes).map((key) => nodes[key].default))


    }

    render(data?: LogicFlow.GraphConfigData) {
        this.lf.render(data ? data : {})
    }
}
