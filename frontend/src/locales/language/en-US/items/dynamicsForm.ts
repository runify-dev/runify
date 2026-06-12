export default {
  input_type_list: {
    TextInput: 'Text Input',
    PasswordInput: 'Password Input',
    Slider: 'Slider',
    SwitchInput: 'Switch',
    SingleSelect: 'Single Select',
    MultiSelect: 'Multi Select',
    DatePicker: 'Date Picker',
    JsonInput: 'JSON Input',
    RadioCard: 'Radio Card',
    RadioRow: 'Radio Row'
  },
  default: {
    label: 'Default Value',
    placeholder: 'Please enter default value',
    requiredMessage: 'is required',
    show: 'Show Default Value'
  },
  constructor: {
    field: {
      label: 'Parameter',
      placeholder: 'Please enter parameter',
      requiredMessage: 'Parameter is required',
      requiredMessage2: 'Only letters, numbers and underscores allowed'
    },
    name: {
      label: 'Display Name',
      placeholder: 'Please enter display name',
      requiredMessage: 'Display Name is required'
    },
    tooltip: {
      label: 'Parameter Tooltip',
      placeholder: 'Please enter parameter tooltip'
    },
    required: {
      label: 'Required',
      requiredMessage: 'Required is required'
    },
    input_type: {
      label: 'Component Type',
      placeholder: 'Please select component type',
      requiredMessage: 'Component Type is required'
    }
  },
  paramForm: {
    field: {
      label: 'Parameter',
      placeholder: 'Please enter parameter',
      requiredMessage: 'Parameter is required',
      requiredMessage2: 'Only letters, numbers and underscores allowed'
    },
    name: {
      label: 'Display Name',
      placeholder: 'Please enter display name',
      requiredMessage: 'Display Name is required'
    },
    tooltip: {
      label: 'Parameter Tooltip',
      placeholder: 'Please enter parameter tooltip'
    },
    required: {
      label: 'Required',
      requiredMessage: 'Required is required'
    },
    input_type: {
      label: 'Component Type',
      placeholder: 'Please select component type',
      requiredMessage: 'Component Type is required'
    }
  },
  impl: {
    textMinLength: 'Min Text Length',
    textMaxLength: 'Max Text Length',
    passwordMinLength: 'Min Password Length',
    passwordMaxLength: 'Max Password Length',
    minValue: 'Min Value',
    maxValue: 'Max Value',
    step: 'Step',
    defaultValue: 'Default Value',
    dateFormat: 'Date Format',
    showTime: 'Show Time',
    placeholder: 'Placeholder',
    datePlaceholder: 'Select date',
    labelField: 'Label Field',
    valueField: 'Value Field',
    optionData: 'Option Data (JSON Array)',
    format: 'Format',
    jsonPlaceholder: 'Please enter valid JSON format',
    requiredField: 'This field is required',
    validationFailed: 'Validation failed',
    mustBeGreaterThan: 'must be greater than',
    mustBeLessThan: 'must be less than',
    optionExample: '[{"label":"option1","value":"1"}]'
  },
  demo: {
    constructorTitle: 'Field Constructor',
    addField: 'Add Field',
    previewForm: 'Preview Form',
    formPreview: 'Form Preview',
    getData: 'Get Data',
    emptyHint: 'Add fields in the constructor on the left to preview here',
    currentFormValue: 'Current Form Value:',
    addedFields: 'Added Fields',
    required: 'Required',
    validateFailed: 'Form Validation Failed',
    validateFailedDetail: 'Please check required fields in the constructor',
    fieldNameEmpty: 'Field Name Empty',
    fieldNameEmptyDetail: 'Please enter a field name',
    fieldNameDuplicate: 'Duplicate Field Name',
    fieldNameExists: 'already exists',
    addSuccess: 'Added Successfully',
    fieldAdded: 'has been added',
    validatePassed: 'Validation Passed',
    validateError: 'Validation Failed'
  }
}
