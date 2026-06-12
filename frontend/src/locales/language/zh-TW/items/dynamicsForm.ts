export default {
  input_type_list: {
    TextInput: '文字框',
    PasswordInput: '密碼框',
    Slider: '滑桿',
    SwitchInput: '開關',
    SingleSelect: '單選框',
    MultiSelect: '多選框',
    DatePicker: '日期選擇',
    JsonInput: 'JSON文字框',
    RadioCard: '選項卡',
    RadioRow: '單行選項卡'
  },
  default: {
    label: '預設值',
    placeholder: '請輸入預設值',
    requiredMessage: '為必填屬性',
    show: '顯示預設值'
  },
  constructor: {
    field: {
      label: '參數',
      placeholder: '請輸入參數',
      requiredMessage: '參數 為必填屬性',
      requiredMessage2: '只能輸入字母數字和底線'
    },
    name: {
      label: '顯示名稱',
      placeholder: '請輸入顯示名稱',
      requiredMessage: '顯示名稱 為必填屬性'
    },
    tooltip: {
      label: '參數提示說明',
      placeholder: '請輸入參數提示說明'
    },
    required: {
      label: '是否必填',
      requiredMessage: '是否必填 為必填屬性'
    },
    input_type: {
      label: '元件類型',
      placeholder: '請選擇元件類型',
      requiredMessage: '元件類型 為必填屬性'
    }
  },
  paramForm: {
    field: {
      label: '參數',
      placeholder: '請輸入參數',
      requiredMessage: '參數 為必填屬性',
      requiredMessage2: '只能輸入字母數字和底線'
    },
    name: {
      label: '顯示名稱',
      placeholder: '請輸入顯示名稱',
      requiredMessage: '顯示名稱 為必填屬性'
    },
    tooltip: {
      label: '參數提示說明',
      placeholder: '請輸入參數提示說明'
    },
    required: {
      label: '是否必填',
      requiredMessage: '是否必填 為必填屬性'
    },
    input_type: {
      label: '元件類型',
      placeholder: '請選擇元件類型',
      requiredMessage: '元件類型 為必填屬性'
    }
  },
  impl: {
    textMinLength: '文字長度最小',
    textMaxLength: '文字長度最大',
    passwordMinLength: '密碼長度最小',
    passwordMaxLength: '密碼長度最大',
    minValue: '最小值',
    maxValue: '最大值',
    step: '步長',
    defaultValue: '預設值',
    dateFormat: '日期格式',
    showTime: '顯示時間',
    placeholder: '佔位提示',
    datePlaceholder: '請選擇日期',
    labelField: '選項顯示欄位',
    valueField: '選項值欄位',
    optionData: '選項資料 (JSON 陣列)',
    format: '格式化',
    jsonPlaceholder: '請輸入合法的 JSON 格式',
    requiredField: '此項必填',
    validationFailed: '校驗失敗',
    mustBeGreaterThan: '必須大於',
    mustBeLessThan: '必須小於',
    optionExample: '[{"label":"選項1","value":"1"}]'
  },
  demo: {
    constructorTitle: '欄位建構器',
    addField: '新增欄位',
    previewForm: '預覽表單',
    formPreview: '表單預覽',
    getData: '取得資料',
    emptyHint: '左側建構器新增欄位後，此處即時展示',
    currentFormValue: '當前表單值：',
    addedFields: '已新增欄位',
    required: '必填',
    validateFailed: '表單校驗失敗',
    validateFailedDetail: '請檢查建構器中的必填項',
    fieldNameEmpty: '欄位名稱為空',
    fieldNameEmptyDetail: '請填寫欄位名稱',
    fieldNameDuplicate: '欄位名稱重複',
    fieldNameExists: '已存在',
    addSuccess: '新增成功',
    fieldAdded: '已新增',
    validatePassed: '校驗通過',
    validateError: '校驗失敗'
  }
}
