interface Field {
  label: string,
  value: string
}
interface LifeCycle {
  onMounted?: () => void
  onBeforeMount?: () => void
  onBeforeUnmount?: () => void
}
export { type Field, type LifeCycle }
