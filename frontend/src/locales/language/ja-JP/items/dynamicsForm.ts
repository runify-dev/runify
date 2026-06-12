export default {
  input_type_list: {
    TextInput: 'テキスト入力',
    PasswordInput: 'パスワード入力',
    Slider: 'スライダー',
    SwitchInput: 'スイッチ',
    SingleSelect: '単一選択',
    MultiSelect: '複数選択',
    DatePicker: '日付選択',
    JsonInput: 'JSON入力',
    RadioCard: 'ラジオカード',
    RadioRow: 'ラジオ行'
  },
  default: {
    label: 'デフォルト値',
    placeholder: 'デフォルト値を入力してください',
    requiredMessage: 'は必須です',
    show: 'デフォルト値を表示'
  },
  constructor: {
    field: {
      label: 'パラメータ',
      placeholder: 'パラメータを入力してください',
      requiredMessage: 'パラメータは必須です',
      requiredMessage2: '英字、数字、アンダースコアのみ使用できます'
    },
    name: {
      label: '表示名',
      placeholder: '表示名を入力してください',
      requiredMessage: '表示名は必須です'
    },
    tooltip: {
      label: 'パラメータツールチップ',
      placeholder: 'パラメータのツールチップを入力してください'
    },
    required: {
      label: '必須',
      requiredMessage: '必須項目です'
    },
    input_type: {
      label: 'コンポーネントタイプ',
      placeholder: 'コンポーネントタイプを選択してください',
      requiredMessage: 'コンポーネントタイプは必須です'
    }
  },
  paramForm: {
    field: {
      label: 'パラメータ',
      placeholder: 'パラメータを入力してください',
      requiredMessage: 'パラメータは必須です',
      requiredMessage2: '英字、数字、アンダースコアのみ使用できます'
    },
    name: {
      label: '表示名',
      placeholder: '表示名を入力してください',
      requiredMessage: '表示名は必須です'
    },
    tooltip: {
      label: 'パラメータツールチップ',
      placeholder: 'パラメータのツールチップを入力してください'
    },
    required: {
      label: '必須',
      requiredMessage: '必須項目です'
    },
    input_type: {
      label: 'コンポーネントタイプ',
      placeholder: 'コンポーネントタイプを選択してください',
      requiredMessage: 'コンポーネントタイプは必須です'
    }
  },
  impl: {
    textMinLength: '最小テキスト長',
    textMaxLength: '最大テキスト長',
    passwordMinLength: '最小パスワード長',
    passwordMaxLength: '最大パスワード長',
    minValue: '最小値',
    maxValue: '最大値',
    step: 'ステップ',
    defaultValue: 'デフォルト値',
    dateFormat: '日付形式',
    showTime: '時刻を表示',
    placeholder: 'プレースホルダー',
    datePlaceholder: '日付を選択',
    labelField: 'ラベルフィールド',
    valueField: '値フィールド',
    optionData: 'オプションデータ（JSON配列）',
    format: '形式',
    jsonPlaceholder: '有効なJSON形式を入力してください',
    requiredField: 'この項目は必須です',
    validationFailed: 'バリデーション失敗',
    mustBeGreaterThan: 'より大きい必要があります',
    mustBeLessThan: 'より小さい必要があります',
    optionExample: '[{"label":"オプション1","value":"1"}]'
  },
  demo: {
    constructorTitle: 'フィールドコンストラクタ',
    addField: 'フィールド追加',
    previewForm: 'フォームプレビュー',
    formPreview: 'フォームプレビュー',
    getData: 'データ取得',
    emptyHint: '左側のコンストラクタでフィールドを追加するとここにプレビューされます',
    currentFormValue: '現在のフォーム値:',
    addedFields: '追加済みフィールド',
    required: '必須',
    validateFailed: 'フォームバリデーション失敗',
    validateFailedDetail: 'コンストラクタの必須フィールドを確認してください',
    fieldNameEmpty: 'フィールド名が空です',
    fieldNameEmptyDetail: 'フィールド名を入力してください',
    fieldNameDuplicate: 'フィールド名が重複しています',
    fieldNameExists: 'は既に存在します',
    addSuccess: '追加成功',
    fieldAdded: 'が追加されました',
    validatePassed: 'バリデーション通過',
    validateError: 'バリデーション失敗'
  }
}
