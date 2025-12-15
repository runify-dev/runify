import _CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import { all, createLowlight } from 'lowlight'
import css from 'highlight.js/lib/languages/css'
import js from 'highlight.js/lib/languages/javascript'
import java from 'highlight.js/lib/languages/java'
import ts from 'highlight.js/lib/languages/typescript'
import html from 'highlight.js/lib/languages/xml'
const lowlight = createLowlight(all)
lowlight.register('html', html)
lowlight.register('css', css)
lowlight.register('javascript', js)
lowlight.register('ts', ts)
lowlight.register('java', java)
export const CodeBlockLowlight = _CodeBlockLowlight.configure({
  lowlight: lowlight,
  defaultLanguage: 'plaintext',
  languageClassPrefix: 'language-',
})
