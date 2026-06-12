import ja from 'element-plus/es/locale/lang/ja'
const items: any = import.meta.glob("./items/*.ts", { eager: true });
const locals = Object.keys(items).map(key => ({ [key.replace('./items/', '').replace('.ts', '')]: items[key].default }))
  .reduce((x, y) => ({ ...x, ...y }), [])
const language = {
  lang: '日本語',
  ja,
  ...locals
}
export default language
