const operationMenus: any = import.meta.glob('./items/*/**.ts', { eager: true })

export default Object.keys(operationMenus).flatMap((key) => operationMenus[key].default)
