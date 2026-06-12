export default {
  input_type_list: {
    TextInput: '文本框',
    PasswordInput: '密码框',
    Slider: '滑块',
    SwitchInput: '开关',
    SingleSelect: '单选框',
    MultiSelect: '多选框',
    DatePicker: '日期',
    JsonInput: 'JSON文本框',
    RadioCard: '选项卡',
    RadioRow: '单行选项卡'
  },
  default: {
    label: '默认值',
    placeholder: '请输入默认值',
    requiredMessage: '为必填属性',
    show: '显示默认值'
  },
  constructor: {
    field: {
      label: '参数',
      placeholder: '请输入参数',
      requiredMessage: '参数 为必填属性',
      requiredMessage2: '只能输入字母数字和下划线'
    },
    name: {
      label: '显示名称',
      placeholder: '请输入显示名称',
      requiredMessage: '显示名称 为必填属性'
    },
    tooltip: {
      label: '参数提示说明',
      placeholder: '请输入参数提示说明'
    },
    required: {
      label: '是否必填',
      requiredMessage: '是否必填 为必填属性'
    },
    input_type: {
      label: '组件类型',
      placeholder: '请选择组件类型',
      requiredMessage: '组建类型 为必填属性'
    }
  },
  paramForm: {
    field: {
      label: '参数',
      placeholder: '请输入参数',
      requiredMessage: '参数 为必填属性',
      requiredMessage2: '只能输入字母数字和下划线'
    },
    name: {
      label: '显示名称',
      placeholder: '请输入显示名称',
      requiredMessage: '显示名称 为必填属性'
    },
    tooltip: {
      label: '参数提示说明',
      placeholder: '请输入参数提示说明'
    },
    required: {
      label: '是否必填',
      requiredMessage: '是否必填 为必填属性'
    },
    input_type: {
      label: '组件类型',
      placeholder: '请选择组件类型',
      requiredMessage: '组建类型 为必填属性'
    }
  },
  impl: {
    textMinLength: '文本长度最小',
    textMaxLength: '文本长度最大',
    passwordMinLength: '密码长度最小',
    passwordMaxLength: '密码长度最大',
    minValue: '最小值',
    maxValue: '最大值',
    step: '步长',
    defaultValue: '默认值',
    dateFormat: '日期格式',
    showTime: '显示时间',
    placeholder: '占位提示',
    datePlaceholder: '请选择日期',
    labelField: '选项显示字段',
    valueField: '选项值字段',
    optionData: '选项数据 (JSON 数组)',
    format: '格式化',
    jsonPlaceholder: '请输入合法的 JSON 格式',
    requiredField: '此项必填',
    validationFailed: '校验失败',
    mustBeGreaterThan: '必须大于',
    mustBeLessThan: '必须小于',
    optionExample: '[{"label":"选项1","value":"1"}]'
  },
  demo: {
    constructorTitle: '字段构造器',
    addField: '添加字段',
    previewForm: '预览表单',
    formPreview: '表单预览',
    getData: '获取数据',
    emptyHint: '左侧构造器添加字段后，此处实时展示',
    currentFormValue: '当前表单值：',
    addedFields: '已添加字段',
    required: '必填',
    validateFailed: '表单校验失败',
    validateFailedDetail: '请检查构造器中的必填项',
    fieldNameEmpty: '字段名为空',
    fieldNameEmptyDetail: '请填写字段名',
    fieldNameDuplicate: '字段名重复',
    fieldNameExists: '已存在',
    addSuccess: '添加成功',
    fieldAdded: '已添加',
    validatePassed: '校验通过',
    validateError: '校验失败'
  }
}
