export default {
  input_type_list: {
    TextInput: '텍스트 입력',
    PasswordInput: '비밀번호 입력',
    Slider: '슬라이더',
    SwitchInput: '스위치',
    SingleSelect: '단일 선택',
    MultiSelect: '다중 선택',
    DatePicker: '날짜 선택',
    JsonInput: 'JSON 입력',
    RadioCard: '라디오 카드',
    RadioRow: '라디오 행'
  },
  default: {
    label: '기본값',
    placeholder: '기본값을 입력하세요',
    requiredMessage: '필수 항목입니다',
    show: '기본값 표시'
  },
  constructor: {
    field: {
      label: '매개변수',
      placeholder: '매개변수를 입력하세요',
      requiredMessage: '매개변수는 필수입니다',
      requiredMessage2: '영문자, 숫자 및 밑줄만 허용됩니다'
    },
    name: {
      label: '표시 이름',
      placeholder: '표시 이름을 입력하세요',
      requiredMessage: '표시 이름은 필수입니다'
    },
    tooltip: {
      label: '매개변수 툴팁',
      placeholder: '매개변수 툴팁을 입력하세요'
    },
    required: {
      label: '필수',
      requiredMessage: '필수 여부는 필수입니다'
    },
    input_type: {
      label: '컴포넌트 유형',
      placeholder: '컴포넌트 유형을 선택하세요',
      requiredMessage: '컴포넌트 유형은 필수입니다'
    }
  },
  paramForm: {
    field: {
      label: '매개변수',
      placeholder: '매개변수를 입력하세요',
      requiredMessage: '매개변수는 필수입니다',
      requiredMessage2: '영문자, 숫자 및 밑줄만 허용됩니다'
    },
    name: {
      label: '표시 이름',
      placeholder: '표시 이름을 입력하세요',
      requiredMessage: '표시 이름은 필수입니다'
    },
    tooltip: {
      label: '매개변수 툴팁',
      placeholder: '매개변수 툴팁을 입력하세요'
    },
    required: {
      label: '필수',
      requiredMessage: '필수 여부는 필수입니다'
    },
    input_type: {
      label: '컴포넌트 유형',
      placeholder: '컴포넌트 유형을 선택하세요',
      requiredMessage: '컴포넌트 유형은 필수입니다'
    }
  },
  impl: {
    textMinLength: '최소 텍스트 길이',
    textMaxLength: '최대 텍스트 길이',
    passwordMinLength: '최소 비밀번호 길이',
    passwordMaxLength: '최대 비밀번호 길이',
    minValue: '최소값',
    maxValue: '최대값',
    step: '단계',
    defaultValue: '기본값',
    dateFormat: '날짜 형식',
    showTime: '시간 표시',
    placeholder: '플레이스홀더',
    datePlaceholder: '날짜 선택',
    labelField: '라벨 필드',
    valueField: '값 필드',
    optionData: '옵션 데이터 (JSON 배열)',
    format: '형식',
    jsonPlaceholder: '유효한 JSON 형식을 입력하세요',
    requiredField: '이 필드는 필수입니다',
    validationFailed: '유효성 검사 실패',
    mustBeGreaterThan: '보다 커야 합니다',
    mustBeLessThan: '보다 작아야 합니다',
    optionExample: `[{'{'}"label":"option1","value":"1"{'}'}]`
  },
  demo: {
    constructorTitle: '필드 생성기',
    addField: '필드 추가',
    previewForm: '미리보기 폼',
    formPreview: '폼 미리보기',
    getData: '데이터 가져오기',
    emptyHint: '왼쪽 생성기에서 필드를 추가하면 여기에 미리보기가 표시됩니다',
    currentFormValue: '현재 폼 값:',
    addedFields: '추가된 필드',
    required: '필수',
    validateFailed: '폼 유효성 검사 실패',
    validateFailedDetail: '생성기에서 필수 필드를 확인하세요',
    fieldNameEmpty: '필드 이름 비어있음',
    fieldNameEmptyDetail: '필드 이름을 입력하세요',
    fieldNameDuplicate: '중복된 필드 이름',
    fieldNameExists: '이(가) 이미 존재합니다',
    addSuccess: '추가 완료',
    fieldAdded: '이(가) 추가되었습니다',
    validatePassed: '유효성 검사 통과',
    validateError: '유효성 검사 실패'
  }
}
