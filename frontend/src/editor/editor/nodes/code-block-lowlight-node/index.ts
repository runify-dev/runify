/**
 * CustomCodeBlock — CodeMirror 6 内嵌方案
 *
 * 安装依赖：
 *   pnpm add @codemirror/view @codemirror/state @codemirror/language \
 *            @codemirror/language-data @codemirror/commands
 */

import { Node, mergeAttributes, textblockTypeInputRule } from "@tiptap/core";
import { Plugin, PluginKey, TextSelection } from "@tiptap/pm/state";
import type { EditorView as PMEditorView } from "@tiptap/pm/view";
import { type NodeView } from "@tiptap/pm/view";

import {
  EditorView,
  keymap,
  lineNumbers,
  highlightActiveLine,
  drawSelection,
  highlightActiveLineGutter,
} from "@codemirror/view";
import { EditorState, Compartment } from "@codemirror/state";
import {
  defaultKeymap,
  history,
  historyKeymap,
  indentWithTab,
} from "@codemirror/commands";
import {
  autocompletion,
  completionKeymap,
  closeBrackets,
  closeBracketsKeymap,
} from "@codemirror/autocomplete";
import {
  syntaxHighlighting,
  defaultHighlightStyle,
  indentOnInput,
  bracketMatching,
  LanguageDescription,
} from "@codemirror/language";
import { languages } from "@codemirror/language-data";

// ─────────────────────────────────────────────
// 语言列表
// ─────────────────────────────────────────────
const LANGUAGES = [
  { value: "", label: "Auto", color: "#a0aec0" },
  { value: "javascript", label: "JavaScript", color: "#f7df1e" },
  { value: "typescript", label: "TypeScript", color: "#3178c6" },
  { value: "python", label: "Python", color: "#3572A5" },
  { value: "go", label: "Go", color: "#00ADD8" },
  { value: "rust", label: "Rust", color: "#dea584" },
  { value: "java", label: "Java", color: "#b07219" },
  { value: "cpp", label: "C++", color: "#f34b7d" },
  { value: "c", label: "C", color: "#555555" },
  { value: "csharp", label: "C#", color: "#178600" },
  { value: "php", label: "PHP", color: "#4F5D95" },
  { value: "ruby", label: "Ruby", color: "#701516" },
  { value: "swift", label: "Swift", color: "#F05138" },
  { value: "kotlin", label: "Kotlin", color: "#A97BFF" },
  { value: "bash", label: "Bash", color: "#89e051" },
  { value: "shell", label: "Shell", color: "#89e051" },
  { value: "html", label: "HTML", color: "#e34c26" },
  { value: "css", label: "CSS", color: "#563d7c" },
  { value: "json", label: "JSON", color: "#40c4ff" },
  { value: "yaml", label: "YAML", color: "#cb171e" },
  { value: "markdown", label: "Markdown", color: "#083fa1" },
  { value: "sql", label: "SQL", color: "#e38c00" },
  { value: "xml", label: "XML", color: "#0060ac" },
  { value: "plaintext", label: "Plain Text", color: "#8b8b8b" },
];

const LANG_ALIAS: Record<string, string> = {
  "": "Plain Text",
  auto: "Plain Text",
  js: "JavaScript",
  javascript: "JavaScript",
  jsx: "JSX",
  ts: "TypeScript",
  typescript: "TypeScript",
  tsx: "TSX",
  py: "Python",
  python: "Python",
  go: "Go",
  rust: "Rust",
  java: "Java",
  cpp: "C++",
  "c++": "C++",
  c: "C",
  csharp: "C#",
  cs: "C#",
  php: "PHP",
  ruby: "Ruby",
  rb: "Ruby",
  swift: "Swift",
  kotlin: "Kotlin",
  bash: "Shell",
  shell: "Shell",
  sh: "Shell",
  html: "HTML",
  css: "CSS",
  scss: "SCSS",
  json: "JSON",
  yaml: "YAML",
  yml: "YAML",
  xml: "XML",
  sql: "SQL",
  markdown: "Markdown",
  md: "Markdown",
  plaintext: "Plain Text",
  text: "Plain Text",
};

function findLanguage(lang: string): LanguageDescription | null {
  const alias = LANG_ALIAS[lang.toLowerCase()] ?? lang;
  return (
    LanguageDescription.matchLanguageName(languages, alias, true) ??
    LanguageDescription.matchLanguageName(languages, lang, true) ??
    null
  );
}

// ─────────────────────────────────────────────
// looksLikeCode
// ─────────────────────────────────────────────
function looksLikeCode(text: string): boolean {
  const lines = text.split("\n");
  if (lines.length < 2) return false;
  let score = 0;
  if (lines.filter((l) => /^[ \t]{2,}/.test(l)).length / lines.length > 0.2)
    score += 2;
  if (/[{};()=>]/.test(text)) score += 2;
  if (
    /^\s*(function|const|let|var|if|for|while|import|export|class|def|fn|pub|return)\b/m.test(
      text
    )
  )
    score += 3;
  if (/\/\/|\/\*|#\s|<!--/.test(text)) score += 2;
  if (/[;{]$/.test(text.trim())) score += 1;
  return score >= 4;
}

// ─────────────────────────────────────────────
// CM6 主题
// ─────────────────────────────────────────────
const codeBlockTheme = EditorView.theme(
  {
    "&": {
      fontSize: "13px",
      fontFamily: '"JetBrains Mono", "Fira Code", "Consolas", monospace',
      background: "#f6f8fa",
      color: "#24292f",
      borderRadius: "0 0 12px 12px",
    },
    "&.cm-focused": { outline: "none" },
    ".cm-scroller": {
      fontFamily: "inherit",
      lineHeight: "1.6",
      padding: "10px 0",
      overflowX: "auto",
    },
    ".cm-content": { padding: "0", caretColor: "#24292f" },
    ".cm-gutters": {
      background: "#f0f2f5",
      borderRight: "1px solid #e8eaed",
      color: "#b0b7c0",
      minWidth: "40px",
      userSelect: "none",
      cursor: "default",
    },
    ".cm-lineNumbers .cm-gutterElement": { padding: "0 10px 0 14px" },
    ".cm-activeLine": { backgroundColor: "rgba(0,0,0,0.03)" },
    ".cm-activeLineGutter": {
      backgroundColor: "rgba(0,0,0,0.05)",
      color: "#6a737d",
    },
    ".cm-selectionBackground, &.cm-focused .cm-selectionBackground": {
      background: "rgba(125,179,232,0.3)",
    },
    ".cm-cursor": { borderLeftColor: "#24292f" },
    ".cm-scroller::-webkit-scrollbar": { height: "6px", width: "6px" },
    ".cm-scroller::-webkit-scrollbar-track": { background: "transparent" },
    ".cm-scroller::-webkit-scrollbar-thumb": {
      background: "#d0d5db",
      borderRadius: "3px",
      cursor: "pointer",
    },
    ".cm-scroller::-webkit-scrollbar-thumb:hover": { background: "#9ca3af" },
    // 补全下拉
    ".cm-tooltip.cm-tooltip-autocomplete": {
      border: "1px solid #e1e4e8",
      borderRadius: "8px",
      boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
      background: "#fff",
      fontSize: "12.5px",
      fontFamily: '"JetBrains Mono","Fira Code","Consolas",monospace',
    },
    ".cm-tooltip-autocomplete > ul": {
      maxHeight: "220px",
      borderRadius: "8px",
      padding: "4px",
    },
    ".cm-tooltip-autocomplete > ul > li": {
      borderRadius: "4px",
      padding: "4px 10px",
      lineHeight: "1.6",
    },
    ".cm-tooltip-autocomplete > ul > li[aria-selected]": {
      background: "#ebf1ff",
      color: "#1a4c9e",
    },
    ".cm-completionLabel": { flex: "1" },
    ".cm-completionDetail": {
      color: "#8b949e",
      fontSize: "11px",
      marginLeft: "8px",
      fontStyle: "normal",
    },
    ".cm-completionIcon": { display: "none" },
  },
  { dark: false }
);

// ─────────────────────────────────────────────
// CodeMirrorNodeView
// ─────────────────────────────────────────────
class CodeMirrorNodeView implements NodeView {
  dom: HTMLElement;
  private cm: EditorView;
  private pmView: PMEditorView;
  private getPos: () => number | undefined;
  private node: any;
  private langCompartment = new Compartment();
  private editableCompartment = new Compartment();
  private updating = false;

  private langDot!: HTMLElement;
  private langLabel!: HTMLElement;

  constructor(
    node: any,
    pmView: PMEditorView,
    getPos: () => number | undefined
  ) {
    this.node = node;
    this.pmView = pmView;
    this.getPos = getPos;

    this.dom = document.createElement("div");
    this.dom.className = "cm-code-block";
    Object.assign(this.dom.style, {
      position: "relative",
      margin: "20px 0",
      borderRadius: "12px",
      border: "1px solid #e1e4e8",
      boxShadow: "0 1px 3px rgba(0,0,0,0.06)",
      fontFamily: '"JetBrains Mono","Fira Code","Consolas",monospace',
      overflow: "visible",
      transition: "border-color 0.2s, box-shadow 0.2s",
    });

    this.dom.appendChild(this.buildHeader(node));

    this.cm = new EditorView({
      state: this.buildState(
        node.textContent,
        node.attrs.language,
        pmView.editable
      ),
      parent: this.dom,
      dispatch: (tr) => this.dispatchCM(tr),
    });

    this.loadLanguage(node.attrs.language);

    // 新建节点时（内容为空）自动聚焦到 CM6 内部
    // undo/redo 重建的节点有内容，不触发 focus
    if (pmView.editable && node.textContent === "") {
      requestAnimationFrame(() => this.cm.focus());
    }
  }

  // ── Build CM6 State ──
  private buildState(
    content: string,
    lang: string,
    editable: boolean
  ): EditorState {
    return EditorState.create({
      doc: content,
      extensions: [
        history(),
        drawSelection(),
        lineNumbers(),
        highlightActiveLine(),
        highlightActiveLineGutter(),
        indentOnInput(),
        bracketMatching(),
        syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
        closeBrackets(),
        autocompletion({
          activateOnTyping: true,
          maxRenderedOptions: 20,
        }),
        keymap.of([
          ...closeBracketsKeymap,
          ...completionKeymap,
          indentWithTab,
          ...defaultKeymap,
          ...historyKeymap,
          {
            key: "Backspace",
            run: (view) => {
              const { state } = view;
              const isEmpty = state.doc.length === 0;
              const sel = state.selection.main;
              const atStart = sel.from === 0 && sel.to === 0;
              if (isEmpty || atStart) {
                const pos = this.getPos();
                if (pos !== undefined) {
                  const pmTr = this.pmView.state.tr.deleteRange(
                    pos,
                    pos + this.node.nodeSize
                  );
                  this.updating = true;
                  this.pmView.dispatch(pmTr);
                  this.updating = false;
                  this.pmView.focus();
                  return true;
                }
              }
              return false;
            },
          },
          {
            key: "Escape",
            run: () => {
              const pos = this.getPos();
              if (pos !== undefined) {
                const after = pos + this.node.nodeSize;
                const pmTr = this.pmView.state.tr.setSelection(
                  TextSelection.near(this.pmView.state.doc.resolve(after))
                );
                this.pmView.dispatch(pmTr);
                this.pmView.focus();
                return true;
              }
              return false;
            },
          },
        ]),
        this.langCompartment.of([]),
        this.editableCompartment.of(EditorView.editable.of(editable)),
        codeBlockTheme,
        EditorView.updateListener.of((update) => {
          if (update.docChanged && !this.updating) {
            this.syncToTiptap();
          }
        }),
        EditorView.domEventHandlers({}),
      ],
    });
  }

  // ── 异步加载语言 ──
  private async loadLanguage(lang: string) {
    const desc = findLanguage(lang);
    if (!desc) {
      // 无匹配语言，清除高亮
      this.cm.dispatch({ effects: this.langCompartment.reconfigure([]) });
      return;
    }
    const langSupport = await desc.load();
    this.cm.dispatch({
      effects: this.langCompartment.reconfigure(langSupport),
    });
  }

  // ── CM6 → tiptap 同步 ──
  private syncToTiptap() {
    if (this.updating) return;
    const pos = this.getPos();
    if (pos === undefined) return;
    // 节点可能已被删除（如 Backspace 删除节点后 CM6 还会触发 docChanged）
    const pmNode = this.pmView.state.doc.nodeAt(pos);
    if (!pmNode || pmNode.type.name !== "customCodeBlock") return;
    const newText = this.cm.state.doc.toString();
    const { tr, schema } = this.pmView.state;
    const nodeType = schema.nodes.customCodeBlock;
    const newNode = nodeType.create(
      this.node.attrs,
      newText ? schema.text(newText) : null
    );
    tr.replaceWith(pos, pos + this.node.nodeSize, newNode);
    tr.setMeta("addToHistory", false);
    this.updating = true;
    this.pmView.dispatch(tr);
    this.updating = false;
  }

  // ── tiptap → CM6 同步（undo/redo/外部修改） ──
  update(node: any): boolean {
    if (node.type !== this.node.type) return false;

    if (node.attrs.language !== this.node.attrs.language) {
      this.loadLanguage(node.attrs.language);
      const langInfo = LANGUAGES.find(
        (l) => l.value === node.attrs.language
      ) ?? { label: node.attrs.language || "Auto", color: "#a0aec0" };
      this.langDot.style.background = langInfo.color;
      this.langLabel.textContent = langInfo.label;
    }

    this.node = node;

    if (!this.updating) {
      const cmText = this.cm.state.doc.toString();
      if (cmText !== node.textContent) {
        this.updating = true;
        this.cm.dispatch({
          changes: {
            from: 0,
            to: this.cm.state.doc.length,
            insert: node.textContent,
          },
        });
        this.updating = false;
      }
    }

    return true;
  }

  private dispatchCM(tr: any) {
    this.cm.update([tr]);
  }

  selectNode() {
    this.dom.style.borderColor = "#c8d0da";
    this.dom.style.boxShadow =
      "0 2px 8px rgba(0,0,0,0.09), 0 0 0 3px rgba(130,150,180,0.12)";
    this.cm.focus();
  }

  deselectNode() {
    this.dom.style.borderColor = "#e1e4e8";
    this.dom.style.boxShadow = "0 1px 3px rgba(0,0,0,0.06)";
  }

  destroy() {
    this.cm.destroy();
    document.getElementById("cm-lang-dropdown")?.remove();
  }

  stopEvent(event: Event): boolean {
    if (
      event.target instanceof HTMLElement &&
      event.target.closest("#cm-lang-dropdown")
    ) {
      return false;
    }
    return true;
  }

  ignoreMutation() {
    return true;
  }

  // ─────────────────────────────────────────────
  // Header
  // ─────────────────────────────────────────────
  private buildHeader(node: any): HTMLElement {
    const lang = node.attrs.language ?? "";
    const langInfo = LANGUAGES.find((l) => l.value === lang) ?? {
      label: lang || "Auto",
      color: "#a0aec0",
    };

    const header = document.createElement("div");
    Object.assign(header.style, {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      padding: "6px 12px",
      background: "#eef0f3",
      borderBottom: "1px solid #e1e4e8",
      borderRadius: "12px 12px 0 0",
      userSelect: "none",
    });
    header.contentEditable = "false";

    // 语言选择器
    const langSelector = document.createElement("div");
    langSelector.style.cssText =
      "display:flex;align-items:center;gap:6px;cursor:pointer;padding:4px 8px;border-radius:6px;transition:background 0.15s;font-size:12px;font-weight:500;color:#444d56;";
    langSelector.addEventListener("mouseenter", () => {
      if (this.pmView.editable)
        langSelector.style.background = "rgba(0,0,0,0.06)";
    });
    langSelector.addEventListener("mouseleave", () => {
      langSelector.style.background = "";
    });

    this.langDot = document.createElement("span");
    Object.assign(this.langDot.style, {
      width: "8px",
      height: "8px",
      borderRadius: "50%",
      background: langInfo.color,
      flexShrink: "0",
      display: "inline-block",
    });

    this.langLabel = document.createElement("span");
    this.langLabel.style.cssText = "font-family:inherit;font-size:11.5px;";
    this.langLabel.textContent = langInfo.label;

    const chevron = document.createElement("span");
    chevron.innerHTML = `<svg width="10" height="10" viewBox="0 0 12 12" fill="none"><path d="M2 4L6 8L10 4" stroke="#6a737d" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
    chevron.style.cssText =
      "display:flex;align-items:center;transition:transform 0.2s;";

    langSelector.append(this.langDot, this.langLabel, chevron);
    langSelector.addEventListener("click", (e) => {
      if (!this.pmView.editable) return;
      e.stopPropagation();
      this.toggleDropdown(langSelector, chevron);
    });

    // 复制按钮
    const copyBtn = document.createElement("button");
    copyBtn.style.cssText =
      "display:inline-flex;align-items:center;gap:4px;padding:4px 8px;border:none;border-radius:6px;background:transparent;cursor:pointer;font-size:11.5px;font-weight:500;color:#6a737d;font-family:inherit;transition:background 0.15s,color 0.15s;";
    copyBtn.innerHTML = `<svg width="12" height="12" viewBox="0 0 16 16" fill="none"><rect x="5" y="5" width="9" height="9" rx="1.5" stroke="currentColor" stroke-width="1.3"/><path d="M11 5V3.5A1.5 1.5 0 009.5 2h-6A1.5 1.5 0 002 3.5v6A1.5 1.5 0 003.5 11H5" stroke="currentColor" stroke-width="1.3"/></svg><span>复制</span>`;
    copyBtn.addEventListener("mouseenter", () => {
      copyBtn.style.background = "rgba(0,0,0,0.07)";
      copyBtn.style.color = "#24292e";
    });
    copyBtn.addEventListener("mouseleave", () => {
      copyBtn.style.background = "";
      copyBtn.style.color = "#6a737d";
    });
    copyBtn.addEventListener("click", () => this.copyCode(copyBtn));

    header.append(langSelector, copyBtn);
    return header;
  }

  // ── 语言下拉 ──
  private toggleDropdown(anchor: HTMLElement, chevron: HTMLElement) {
    const existing = document.getElementById("cm-lang-dropdown");
    if (existing) {
      existing.remove();
      chevron.style.transform = "";
      return;
    }

    chevron.style.transform = "rotate(180deg)";

    if (!document.getElementById("cm-dropdown-style")) {
      const style = document.createElement("style");
      style.id = "cm-dropdown-style";
      style.textContent = `@keyframes cmDropIn{from{opacity:0;transform:translateY(-4px) scale(0.98)}to{opacity:1;transform:none}}`;
      document.head.appendChild(style);
    }

    const dropdown = document.createElement("div");
    dropdown.id = "cm-lang-dropdown";
    const rect = anchor.getBoundingClientRect();
    Object.assign(dropdown.style, {
      position: "fixed",
      top: rect.bottom + 4 + "px",
      left: rect.left + "px",
      zIndex: "9999",
      background: "#fff",
      border: "1px solid #e1e4e8",
      borderRadius: "8px",
      boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
      minWidth: "180px",
      overflow: "hidden",
      animation: "cmDropIn 0.15s ease",
    });

    const searchWrap = document.createElement("div");
    searchWrap.style.cssText = "padding:8px;border-bottom:1px solid #f0f0f0;";
    const searchInput = document.createElement("input");
    searchInput.placeholder = "搜索语言...";
    searchInput.style.cssText =
      "width:100%;box-sizing:border-box;padding:4px 8px;border:1px solid #e1e4e8;border-radius:4px;font-size:12px;font-family:inherit;outline:none;background:#f6f8fa;color:#24292e;";
    searchWrap.appendChild(searchInput);
    dropdown.appendChild(searchWrap);

    const list = document.createElement("div");
    list.style.cssText = "max-height:220px;overflow-y:auto;padding:4px;";

    const renderList = (filter = "") => {
      list.innerHTML = "";
      LANGUAGES.filter(
        (l) =>
          l.label.toLowerCase().includes(filter.toLowerCase()) ||
          l.value.toLowerCase().includes(filter.toLowerCase())
      ).forEach((l) => {
        const item = document.createElement("div");
        const isActive = l.value === this.node.attrs.language;
        item.style.cssText = `display:flex;align-items:center;gap:8px;padding:6px 10px;border-radius:4px;font-size:12.5px;cursor:pointer;transition:background 0.1s;color:${isActive ? "#1a4c9e" : "#24292e"
          };background:${isActive ? "#ebf1ff" : ""};font-weight:${isActive ? "600" : "400"
          };`;
        item.innerHTML = `<span style="width:8px;height:8px;border-radius:50%;background:${l.color};flex-shrink:0;display:inline-block;"></span>${l.label}`;
        item.addEventListener("mouseenter", () => {
          if (!isActive) item.style.background = "#f1f3f5";
        });
        item.addEventListener("mouseleave", () => {
          if (!isActive) item.style.background = "";
        });
        item.addEventListener("click", () => {
          this.setLanguage(l.value);
          dropdown.remove();
          chevron.style.transform = "";
        });
        list.appendChild(item);
      });
    };

    renderList();
    searchInput.addEventListener("input", () => renderList(searchInput.value));
    dropdown.appendChild(list);
    document.body.appendChild(dropdown);

    const close = (e: MouseEvent) => {
      if (
        !dropdown.contains(e.target as any) &&
        !anchor.contains(e.target as any)
      ) {
        dropdown.remove();
        chevron.style.transform = "";
        document.removeEventListener("click", close, true);
      }
    };
    setTimeout(() => document.addEventListener("click", close, true), 0);
    requestAnimationFrame(() => searchInput.focus());
  }

  // ── 切换语言 ──
  private setLanguage(lang: string) {
    const pos = this.getPos();
    if (pos === undefined) return;
    const tr = this.pmView.state.tr.setNodeMarkup(pos, undefined, {
      ...this.node.attrs,
      language: lang,
    });
    this.pmView.dispatch(tr);
  }

  // ── 复制 ──
  private async copyCode(btn: HTMLButtonElement) {
    const text = this.cm.state.doc.toString();
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      const el = document.createElement("textarea");
      el.value = text;
      document.body.appendChild(el);
      el.select();
      document.execCommand("copy");
      el.remove();
    }
    const span = btn.querySelector("span")!;
    span.textContent = "已复制";
    btn.style.color = "#2ea043";
    setTimeout(() => {
      span.textContent = "复制";
      btn.style.color = "#6a737d";
    }, 2000);
  }
}

// ─────────────────────────────────────────────
// Tiptap Extension
// ─────────────────────────────────────────────
export const CodeBlockLowlight = Node.create({
  name: "customCodeBlock",
  group: "block",
  content: "text*",
  marks: "",
  code: true,
  defining: true,
  isolating: true,

  addOptions() {
    return { defaultLanguage: "", HTMLAttributes: {} };
  },

  addAttributes() {
    return {
      language: {
        default: "",
        parseHTML: (el) => el.getAttribute("data-language") || "",
        renderHTML: (attrs) => ({ "data-language": attrs.language }),
      },
    };
  },

  parseHTML() {
    return [
      { tag: 'pre[data-type="custom-code-block"]', preserveWhitespace: "full" },
    ];
  },

  renderHTML({ HTMLAttributes }) {
    return [
      "pre",
      mergeAttributes(HTMLAttributes, { "data-type": "custom-code-block" }),
      ["code", {}, 0],
    ];
  },

  // @tiptap/markdown 3.x 支持
  // @ts-ignore
  markdownTokenName: "code",
  // @ts-ignore
  parseMarkdown(token: any) {
    return {
      type: "customCodeBlock",
      attrs: { language: token.lang || "" },
      content: token.text ? [{ type: "text", text: token.text }] : [],
    };
  },
  // @ts-ignore
  renderMarkdown(node: any, helpers: any) {
    const lang = node.attrs?.language || "";
    const content = helpers.renderChildren(node.content || []);
    return `\`\`\`${lang}\n${content}\n\`\`\`\n\n`;
  },

  addNodeView() {
    return ({ node, editor, getPos }) => {
      return new CodeMirrorNodeView(
        node,
        editor.view,
        getPos as () => number | undefined
      );
    };
  },

  addInputRules() {
    return [
      textblockTypeInputRule({
        find: /^```(\w*)[\s\n]$/,
        type: this.type,
        getAttributes: (match) => ({ language: match[1] || "" }),
      }),
    ];
  },

  addKeyboardShortcuts() {
    return {};
  },

  addProseMirrorPlugins() {
    const extensionThis = this;
    return [
      new Plugin({
        key: new PluginKey("codeBlockPaste"),
        props: {
          // handleDOMEvents.paste 在 ProseMirror 内部解析剪贴板之前触发，
          // 这是拦截 VSCode 粘贴的唯一可靠时机。
          // handlePaste 触发时 PM 已经决定用 text/html 解析，vscode-editor-data 就被忽略了。
          handleDOMEvents: {
            paste(view, event) {
              const clipboardData = (event as ClipboardEvent).clipboardData;
              if (!clipboardData) return false;

              // 必须有 vscode-editor-data 才处理，否则放行给 tiptap 正常处理
              const vscodeData = clipboardData.getData("vscode-editor-data");
              if (!vscodeData) return false;

              // 已在代码块内部，让 CM6 自己处理
              const { selection } = view.state;
              if (selection.$anchor.parent.type.name === extensionThis.name)
                return false;

              let language = "";
              try {
                const parsed = JSON.parse(vscodeData);
                const modeMap: Record<string, string> = {
                  typescriptreact: "typescript",
                  javascriptreact: "javascript",
                  shellscript: "bash",
                  plaintext: "",
                };
                language = modeMap[parsed.mode] ?? parsed.mode ?? "";
              } catch {
                return false;
              }

              const text = clipboardData.getData("text/plain");
              if (!text) return false;

              event.preventDefault();
              const { schema, tr } = view.state;
              const nodeType = schema.nodes[extensionThis.name];
              const codeNode = nodeType.create({ language }, schema.text(text));
              view.dispatch(
                tr
                  .replaceSelectionWith(codeNode)
                  .setMeta("paste", true)
                  .setMeta("uiEvent", "paste")
              );
              return true;
            },
          },
        },
      }),
    ];
  },
});
