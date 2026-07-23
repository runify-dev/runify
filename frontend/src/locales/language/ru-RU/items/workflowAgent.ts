export default {
  entry: 'ИИ-генерация рабочего процесса',
  title: 'ИИ-генерация рабочего процесса',
  modelLabel: 'Модель',
  modelPlaceholder: 'Выберите модель',
  requirementLabel: 'Описание задачи',
  requirementPlaceholder:
    'Опишите нужный рабочий процесс, например: Q&A-приложение на основе базы знаний',
  doneToast: 'Генерация завершена, проверьте и сохраните',
  followUpPlaceholder: 'Продолжить диалог: что нужно изменить… (Enter — отправить)',
  status: {
    paused: 'Пауза',
    running: 'Генерация…'
  },
  action: {
    generate: 'Сгенерировать',
    pause: 'Пауза',
    resume: 'Продолжить',
    stop: 'Стоп',
    retry: 'Повторить',
    restart: 'Начать заново',
    expand: 'Показать журнал',
    collapse: 'Свернуть',
    send: 'Отправить'
  },
  log: {
    started: 'Генерация рабочего процесса…',
    done: 'Генерация завершена',
    paused: 'Пауза. Нажмите «Продолжить» для возобновления',
    resumed: 'Возобновление…',
    stopped: 'Остановлено. Холст сохраняет текущий прогресс',
    retrying: 'Повтор…',
    maxIterations: 'Достигнут лимит итераций, автопауза. Нажмите «Продолжить»'
  },
  tool: {
    clear_workflow: 'Очистка холста',
    plan: 'Обновление плана',
    get_node_schema: 'Схема узла',
    add_node: 'Добавить узел',
    update_node: 'Обновить узел',
    add_edge: 'Соединить',
    delete_edge: 'Удалить связь',
    delete_node: 'Удалить узел',
    get_workflow: 'Чтение холста',
    get_node_detail: 'Чтение конфигурации узла',
    get_models: 'Список моделей',
    get_knowledge_bases: 'Список баз знаний',
    get_database_pools: 'Список подключений к БД',
    get_database_tables: 'Список таблиц БД',
    get_database_columns: 'Структура колонок таблицы',
    validate_workflow: 'Проверка процесса',
    locate_node: 'Перейти к узлу',
    finish: 'Готово'
  }
}
