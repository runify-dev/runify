import ru from 'element-plus/es/locale/lang/ru'
const items: any = import.meta.glob("./items/*.ts", { eager: true });
const locals = Object.keys(items).map(key => ({ [key.replace('./items/', '').replace('.ts', '')]: items[key].default }))
  .reduce((x, y) => ({ ...x, ...y }), [])
const language = {
  lang: 'Русский',
  ru,
  ...locals
}
export default language
