import { computed, reactive, watch, onMounted, onUnmounted } from 'vue';

// 初始化：优先用户偏好，否则跟随系统
const saved = localStorage.getItem('theme-dark');
const initialDark = saved !== null ? saved === 'true' : window.matchMedia('(prefers-color-scheme: dark)').matches;

const layoutConfig = reactive({
  preset: 'Aura',
  primary: 'emerald',
  surface: null,
  darkTheme: initialDark,
  menuMode: 'static'
});

const layoutState = reactive({
  staticMenuInactive: false,
  overlayMenuActive: false,
  profileSidebarVisible: false,
  configSidebarVisible: false,
  sidebarExpanded: false,
  menuHoverActive: false,
  activeMenuItem: null,
  activePath: null,
  mobileMenuActive: false,
  anchored: false,
  showContentMenu: false
});

export function useLayout() {
  // 暗色模式变化时同步 class 和 localStorage
  watch(() => layoutConfig.darkTheme, (val) => {
    document.documentElement.classList.toggle('app-dark', val);
    localStorage.setItem('theme-dark', String(val));
  });

  // 监听系统主题，仅在用户未手动设置时跟随
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
  const onSystemThemeChange = (e: MediaQueryListEvent) => {
    if (localStorage.getItem('theme-dark') === null) {
      layoutConfig.darkTheme = e.matches;
    }
  };
  onMounted(() => mediaQuery.addEventListener('change', onSystemThemeChange));
  onUnmounted(() => mediaQuery.removeEventListener('change', onSystemThemeChange));

  const toggleDarkMode = () => {
    if (document.startViewTransition) {
      document.startViewTransition(() => { layoutConfig.darkTheme = !layoutConfig.darkTheme; });
    } else {
      layoutConfig.darkTheme = !layoutConfig.darkTheme;
    }
  };

  const toggleMenu = () => {
    if (isDesktop()) {
      if (layoutConfig.menuMode === 'static') {
        layoutState.staticMenuInactive = !layoutState.staticMenuInactive;
      }
    } else {
      layoutState.mobileMenuActive = !layoutState.mobileMenuActive;
    }
  };

  const toggleConfigSidebar = () => {
    layoutState.configSidebarVisible = !layoutState.configSidebarVisible;
  };

  const hideMobileMenu = () => {
    layoutState.mobileMenuActive = false;
  };

  const changeMenuMode = (event: any) => {
    layoutConfig.menuMode = event.value;
    layoutState.staticMenuInactive = false;
    layoutState.mobileMenuActive = false;
    layoutState.sidebarExpanded = false;
    layoutState.menuHoverActive = false;
    layoutState.anchored = false;
  };

  const isDarkTheme = computed(() => layoutConfig.darkTheme);
  const isDesktop = () => window.innerWidth > 991;

  const hasOpenOverlay = computed(() => layoutState.overlayMenuActive);
  const changeShowContentMenu = (event: boolean) => {
    layoutState.showContentMenu = event
  }
  return {
    layoutConfig,
    layoutState,
    isDarkTheme,
    toggleDarkMode,
    toggleConfigSidebar,
    toggleMenu,
    hideMobileMenu,
    changeMenuMode,
    isDesktop,
    hasOpenOverlay,
    changeShowContentMenu
  };
}
