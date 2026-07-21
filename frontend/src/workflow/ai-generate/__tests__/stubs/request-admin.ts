/**
 * '@/request/admin/index' 的测试替身：真实模块引入 axios 拦截器与 admin router
 * （依赖 window），在 node 环境的目录冒烟测试里不可加载。
 * 冒烟测试只做静态结构断言，不发请求——任何调用都直接拒绝。
 */
const reject = () => Promise.reject(new Error('request 在冒烟测试环境不可用'))

export const request = reject
export const get = reject
export const post = reject
export const put = reject
export const del = reject
export const exportFile = reject
export const upload = reject
export const download = reject
export const postStream = reject
