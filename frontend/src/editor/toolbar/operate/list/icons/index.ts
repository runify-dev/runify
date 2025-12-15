const icons: any = import.meta.glob('./**.vue', { eager: true })
export function iconComponent(name: string) {
  const url = `./${name}-icon.vue`
  return icons[url]?.default || null
}
