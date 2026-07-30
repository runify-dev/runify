import { useLocalStorage, usePreferredLanguages } from '@vueuse/core'
import { computed } from 'vue'
import { createI18n, useI18n } from 'vue-i18n'
import { z } from 'zod'

// zod 校验默认信息的国际化:让 zod 的内置报错(如"期望 string,实际接收 null")跟随应用语言
const zodLocaleMap: Record<string, () => any> = {
  'zh-CN': z.locales.zhCN,
  'zh-TW': z.locales.zhTW,
  'en-US': z.locales.en,
  'ja-JP': z.locales.ja,
  'ko-KR': z.locales.ko,
  'ru-RU': z.locales.ru
}
export function applyZodLocale(lang: string) {
  z.config((zodLocaleMap[lang] || z.locales.en)())
}

// 导入语言文件
const langModules = import.meta.glob('./language/*/index.ts', { eager: true }) as Record<
  string,
  () => Promise<{ default: object }>
>

const langModuleMap = new Map<string, object>()

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

const initialLocale = useLocalStorage(localeConfigKey, getBrowserLang()).value || getBrowserLang()

// 初始化 zod 语言,跟随当前应用语言
applyZodLocale(initialLocale)

export const i18n = createI18n({
  legacy: false,
  locale: initialLocale,
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
    applyZodLocale(lang);
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
