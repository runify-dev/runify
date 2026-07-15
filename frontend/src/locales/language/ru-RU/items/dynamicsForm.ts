export default {
  input_type_list: {
    TextInput: 'Текстовое поле',
    PasswordInput: 'Поле пароля',
    Slider: 'Ползунок',
    SwitchInput: 'Переключатель',
    SingleSelect: 'Одиночный выбор',
    MultiSelect: 'Множественный выбор',
    DatePicker: 'Выбор даты',
    JsonInput: 'JSON ввод',
    RadioCard: 'Радиокарта',
    RadioRow: 'Радиоряд'
  },
  default: {
    label: 'Значение по умолчанию',
    placeholder: 'Введите значение по умолчанию',
    requiredMessage: 'обязательно',
    show: 'Показать значение по умолчанию'
  },
  constructor: {
    field: {
      label: 'Параметр',
      placeholder: 'Введите параметр',
      requiredMessage: 'Параметр обязателен',
      requiredMessage2: 'Допустимы только буквы, цифры и подчёркивания'
    },
    name: {
      label: 'Отображаемое имя',
      placeholder: 'Введите отображаемое имя',
      requiredMessage: 'Отображаемое имя обязательно'
    },
    tooltip: {
      label: 'Подсказка параметра',
      placeholder: 'Введите подсказку параметра'
    },
    required: {
      label: 'Обязательный',
      requiredMessage: 'Обязательность обязательна'
    },
    input_type: {
      label: 'Тип компонента',
      placeholder: 'Выберите тип компонента',
      requiredMessage: 'Тип компонента обязателен'
    }
  },
  paramForm: {
    field: {
      label: 'Параметр',
      placeholder: 'Введите параметр',
      requiredMessage: 'Параметр обязателен',
      requiredMessage2: 'Допустимы только буквы, цифры и подчёркивания'
    },
    name: {
      label: 'Отображаемое имя',
      placeholder: 'Введите отображаемое имя',
      requiredMessage: 'Отображаемое имя обязательно'
    },
    tooltip: {
      label: 'Подсказка параметра',
      placeholder: 'Введите подсказку параметра'
    },
    required: {
      label: 'Обязательный',
      requiredMessage: 'Обязательность обязательна'
    },
    input_type: {
      label: 'Тип компонента',
      placeholder: 'Выберите тип компонента',
      requiredMessage: 'Тип компонента обязателен'
    }
  },
  impl: {
    textMinLength: 'Мин. длина текста',
    textMaxLength: 'Макс. длина текста',
    passwordMinLength: 'Мин. длина пароля',
    passwordMaxLength: 'Макс. длина пароля',
    minValue: 'Мин. значение',
    maxValue: 'Макс. значение',
    step: 'Шаг',
    defaultValue: 'Значение по умолчанию',
    dateFormat: 'Формат даты',
    showTime: 'Показать время',
    placeholder: 'Заполнитель',
    datePlaceholder: 'Выберите дату',
    labelField: 'Поле метки',
    valueField: 'Поле значения',
    optionData: 'Данные вариантов (JSON массив)',
    format: 'Формат',
    jsonPlaceholder: 'Введите допустимый формат JSON',
    requiredField: 'Это поле обязательно',
    validationFailed: 'Проверка не пройдена',
    mustBeGreaterThan: 'должно быть больше',
    mustBeLessThan: 'должно быть меньше',
    optionExample: `[{'{'}"label":"вариант1","value":"1"{'}'}]`
  },
  demo: {
    constructorTitle: 'Конструктор полей',
    addField: 'Добавить поле',
    previewForm: 'Предпросмотр формы',
    formPreview: 'Предпросмотр формы',
    getData: 'Получить данные',
    emptyHint: 'Добавьте поля в конструкторе слева для предпросмотра здесь',
    currentFormValue: 'Текущее значение формы:',
    addedFields: 'Добавленные поля',
    required: 'Обязательный',
    validateFailed: 'Проверка формы не пройдена',
    validateFailedDetail: 'Проверьте обязательные поля в конструкторе',
    fieldNameEmpty: 'Имя поля пустое',
    fieldNameEmptyDetail: 'Введите имя поля',
    fieldNameDuplicate: 'Дублирование имени поля',
    fieldNameExists: 'уже существует',
    addSuccess: 'Успешно добавлено',
    fieldAdded: 'было добавлено',
    validatePassed: 'Проверка пройдена',
    validateError: 'Проверка не пройдена'
  }
}
