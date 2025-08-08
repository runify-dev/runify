import zhCn from 'element-plus/es/locale/lang/zh-cn'
const items: any = import.meta.glob("./items/*.ts", { eager: true });
const locals = Object.keys(items).map(key => ({ [key.replace('./items/', '').replace('.ts', '')]: items[key].default }))
  .reduce((x, y) => ({ ...x, ...y }), [])
export default {
  lang: '简体中文',
  zhCn,
  ...locals
}
