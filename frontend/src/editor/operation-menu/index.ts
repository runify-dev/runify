const operationMenus: any = import.meta.glob('./items/*/**.ts', { eager: true })
console.log(
  's',
  operationMenus,
  Object.keys(operationMenus).flatMap((key) => operationMenus[key].default),
)
export default Object.keys(operationMenus).flatMap((key) => operationMenus[key].default)
