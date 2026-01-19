
import { createApp } from "vue";
import { config } from 'md-editor-v3';
import App from "./App.vue";
import router from "@/router/admin/index";
import "element-plus/dist/index.css";
import "@/styles/index.scss";
import "@/styles/tailwind.css"
import directives from '@/directives'
import 'md-editor-v3/lib/preview.css';
import 'md-editor-v3/lib/style.css';
import { createPinia } from 'pinia';
import i18n from '@/locales'
import components from '@/components'
import markdownit from 'markdown-it';
import screenfull from 'screenfull';
import katex from 'katex';
import 'katex/dist/katex.min.css';
import Cropper from 'cropperjs';
import mermaid from 'mermaid';
import highlight from 'highlight.js';
import 'highlight.js/styles/atom-one-dark.css';
import * as prettier from 'prettier';
import parserMarkdown from 'prettier/plugins/markdown';
import PrimeVue from 'primevue/config';
import Aura from '@primeuix/themes/aura';
import 'primeicons/primeicons.css'
const parseAttributes = (str: string) => {
  if (!str) {
    return undefined
  }
  return str.split(/\s+/).map(pair => {
    const [key, value] = pair.split('=');
    return [key, value.replace(/^"|"$/g, '')];
  })
};

// 示例：创建 <my-tag> 自定义标签
const myTagPlugin = (md: markdownit) => {

  // 1. 添加解析规则
  md.block.ruler.before(
    'paragraph', // 在段落解析前插入
    'my_tag',    // 规则名称
    (state, startLine) => {
      /* 解析逻辑 */
      const text = state.src.slice(state.bMarks[startLine], state.eMarks[startLine]);
      const match = text.match(/<my_tag(.*?)>/);
      if (match) {
        const token = state.push('my_tag', 'my_tag', 1);
        token.content = ""
        const attrs = parseAttributes(match[1])
        if (attrs) {
          token.attrs = attrs as Array<[string, string]>;
        }

        state.line = startLine + 1;
        return true;
      }
      return false;
    }
  );

  // 2. 定义渲染规则
  md.renderer.rules.my_tag_open = (tokens, idx) => {
    console.log(tokens, 'tokens')
    return '<my_tag>'; // 开始标签
  };
  md.renderer.rules.my_tag_close = () => {
    return '</my_tag>'; // 结束标签
  };
  md.renderer.rules.my_tag = (token) => {
    console.log('token', token)
    return "<my_tag></my_tag>"
  }

  const imageRender = md.renderer.rules.image as markdownit.Renderer.RenderRule;
  md.renderer.rules.image = (tokens, idx, options, env, slf) => {
    tokens.forEach(token => {
      const fileId = token.attrGet('src')
      if (!fileId?.startsWith('/api/file')) {
        const src = `/api/file/${fileId}`
        token.attrSet('src', src)
      }

    });
    return imageRender(tokens, idx, options, env, slf)
  }
};
config({
  editorExtensions: {
    prettier: {
      prettierInstance: prettier,
      parserMarkdownInstance: parserMarkdown,
    },
    highlight: {
      instance: highlight,
    },

    screenfull: {
      instance: screenfull,
    },
    katex: {
      instance: katex,
    },
    cropper: {
      instance: Cropper,
    },
    mermaid: {
      instance: mermaid,
    },
  },
  markdownItConfig(md, options) {
    myTagPlugin(md)

  },
  markdownItPlugins(plugins, a) {

    return plugins.map((p) => {

      if (p.type === 'image') {

        return {
          ...p,

          options: {
            ...p.options,
            src: "xx",
            classes: 'my-class',
          },
        };
      }

      return p;
    });
  },
});
import { h } from 'vue';

// 创建自定义图标组件
const CustomCheckIcon = {
  name: 'CustomCheckIcon',
  render() {
    return h('svg', {
      xmlns: 'http://www.w3.org/2000/svg',
      viewBox: '0 0 1024 1024',
      width: '1em',
      height: '1em',
      fill: 'currentColor'
    }, [
      h('path', {
        d: 'M736.005 696.494H174.18c-17.673 0-32-14.327-32-32V255.582c0-17.673 14.327-32 32-32h157.213c7.96 0 15.635 2.967 21.525 8.321l47.547 43.222h335.54c17.673 0 32 14.327 32 32v357.369c0 17.673-14.327 32-32 32z m-529.825-64h497.825V339.125H388.094a32.002 32.002 0 0 1-21.525-8.321l-47.547-43.222H206.18v344.912z'
      }),
      h('path', {
        d: 'M853.18 821.092H317.509c-17.673 0-32-14.327-32-32s14.327-32 32-32H821.18V414.206c0-17.673 14.327-32 32-32s32 14.327 32 32v374.886c0 17.673-14.327 32-32 32z'
      })
    ]);
  }
};
const app = createApp(App);
app.use(router);
app.use(i18n)
app.use(createPinia())
app.use(directives);
app.use(components)
app.use(PrimeVue, {
  theme: {
    preset: Aura
  }
});
app.mount("#app");
