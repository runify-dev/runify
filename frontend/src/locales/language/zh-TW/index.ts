import zhTw from 'element-plus/es/locale/lang/zh-tw'
const items: any = import.meta.glob("./items/*.ts", { eager: true });
const locals = Object.keys(items).map(key => ({ [key.replace('./items/', '').replace('.ts', '')]: items[key].default }))
  .reduce((x, y) => ({ ...x, ...y }), [])
export default {
  lang: '繁體中文',
  zhTw,
  ...locals
}
