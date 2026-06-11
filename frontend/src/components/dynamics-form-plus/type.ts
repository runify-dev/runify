import type { Dict } from '@/api/type/common'

interface ViewCardItem {
  /**
   * 类型
   */
  type: 'eval' | 'default'
  /**
   * 标题
   */
  title: string
  /**
   * 值 根据类型不一样 取值也不一样 default= row[value_field] eval `${parseFloat(row.number).toLocaleString("zh-CN",{style: "decimal",maximumFractionDigits:1})}%&nbsp;&nbsp;&nbsp;`
   */
  valueField: string
}

interface TableColumn {
  /**
   * 字段|组件名称|可计算的模板字符串
   */
  property: string
  /**
   *表头
   */
  label: string
  /**
   * 表数据字段
   */
  valueField?: string

  attrs?: Attrs
  /**
   * 类型
   */
  type: 'eval' | 'component' | 'default'

  propsInfo?: PropsInfo
}
interface ColorItem {
  /**
   * 颜色#f56c6c
   */
  color: string
  /**
   * 进度
   */
  percentage: number
}
interface Attrs {
  /**
   * 提示语
   */
  placeholder?: string
  /**
   * 标签的长度，例如 '50px'。 作为 Form 直接子元素的 form-item 会继承该值。 可以使用 auto。
   */
  labelWidth?: string
  /**
   * 表单域标签的后缀
   */
  labelSuffix?: string
  /**
   * 星号的位置。
   */
  requireAsteriskPosition?: 'left' | 'right'

  color?: Array<ColorItem>

  [propName: string]: any
}
interface PropsInfo {
  /**
   * 表格选择的card
   */
  viewCard?: Array<ViewCardItem>
  /**
   * 表格选择
   */
  tableColumns?: Array<TableColumn>
  /**
   * 选中 message
   */
  activeMsg?: string

  /**
   * 组件样式
   */
  style?: Dict<any>

  /**
   * el-form-item 样式
   */
  itemStyle?: Dict<any>
  /**
   * 表单校验 这个和element校验一样
   */
  rules?: Dict<any>

  /**
   *tabs的时候使用
   */
  tabsLabel?: string

  [propName: string]: any
}

interface ShowCondition {
  field: string
  compare: 'eq' | 'neq' | 'gt' | 'lt' | 'gte' | 'lte' | 'in' | 'nin' | 'empty' | 'notempty'
  value?: any
}

interface ShowRule {
  condition: 'and' | 'or'
  conditions: ShowCondition[]
}

interface FormField {
  field: string
  /**
   * 输入框类型
   */
  type: string
  /**
   * 提示
   */
  label?: string | any
  /**
   * 是否 必填
   */
  required?: boolean
  /**
   * 默认值
   */
  defaultValue?: any
  /**
   * 是否显示默认值
   */
  showDefaultValue?: boolean
  /**
   * 显示规则，条件满足时才显示该字段
   */
  showRules?: ShowRule
  /**
   * 前端attr数据
   */
  attrs?: Attrs
  /**
   * 其他额外信息
   */
  propsInfo?: PropsInfo
  /**
   * 下拉选字段field
   */
  labelField?: string
  /**
   * 下拉选 value
   */
  valueField?: string
  /**
   * 下拉选数据
   */
  optionList?: Array<any>

  children?: Array<FormField>
}
export type { FormField, ShowRule, ShowCondition }
