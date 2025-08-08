import { useLocalStorage, usePreferredLanguages } from '@vueuse/core'
import { computed } from 'vue'
import { createI18n, useI18n } from 'vue-i18n'

// 导入语言文件
const langModules = import.meta.glob('./language/*/index.ts', { eager: true }) as Record<
  string,
  () => Promise<{ default: Object }>
>

const langModuleMap = new Map<string, Object>()

export const langCode: Array<string> = []

export const localeConfigKey = 'locale'

// 获取浏览器默认语言环境
const languages = usePreferredLanguages()

export function getBrowserLang() {
  const browserLang = navigator.language ? navigator.language : languages.value[0]
  let defaultBrowserLang = ''
  if (['zh-HK', 'zh-TW'].includes(browserLang)) {
    defaultBrowserLang = 'zh-TW'
  } else if (['zh-CN', 'zh'].includes(browserLang)) {
    defaultBrowserLang = 'zh-CN'
  } else {
    defaultBrowserLang = 'en-US'
  }
  return defaultBrowserLang
}

// 生成语言模块列表
const generateLangModuleMap = () => {
  const fullPaths = Object.keys(langModules)
  fullPaths.forEach((fullPath) => {
    const k = fullPath.replace('./language', '')
    const startIndex = 1
    const lastIndex = k.lastIndexOf('/')
    const code = k.substring(startIndex, lastIndex)
    langCode.push(code)
    langModuleMap.set(code, langModules[fullPath])
  })
}

// 导出 Message
const importMessages = computed(() => {
  generateLangModuleMap()
  const message: any = {}
  langModuleMap.forEach((value: any, key) => {
    message[key] = value.default
  })
  return message
})

export const i18n = createI18n({
  legacy: false,
  locale: useLocalStorage(localeConfigKey, getBrowserLang()).value || getBrowserLang(),
  fallbackLocale: getBrowserLang(),
  messages: importMessages.value,
  globalInjection: true
})

export const langList = computed(() => {
  if (langModuleMap.size === 0) generateLangModuleMap()

  const list: any = []
  langModuleMap.forEach((value: any, key) => {
    list.push({
      label: value.default.lang,
      value: key
    })
  })

  return list
})

export const { t } = i18n.global

export function useLocale() {
  const { locale } = useI18n({ useScope: 'global' });
  function changeLocale(lang: string) {
    // 如果切换的语言不在对应语言文件里则默认为简体中文
    if (!langCode.includes(lang)) {
      lang = 'en-US';
    }

    locale.value = lang;
    useLocalStorage(localeConfigKey, 'en-US').value = lang;
  }
  const getComponentsLocale = computed(() => {
    return i18n.global.getLocaleMessage(locale.value).componentsLocale;
  });

  return {
    changeLocale,
    getComponentsLocale,
    locale,
  };
}
export default i18n
