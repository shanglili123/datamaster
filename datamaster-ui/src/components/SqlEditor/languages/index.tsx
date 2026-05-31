import { CustomEditorLanguage } from '@/components/CustomEditor/languages/constants';
import { FlinkSQLLanguage } from '@/components/CustomEditor/languages/flinksql';
import { LogLanguage } from '@/components/CustomEditor/languages/javalog';
import { Monaco } from '@monaco-editor/react';

/**
 * 避免重复加载语言, 通过获取到 language 的 id 来判断是否已经加载过
 * @param monacoLanguages
 * @param language
 */
function canLoadLanguage(monacoLanguages: Monaco['languages'] | undefined, language: string) {
  return !monacoLanguages?.getEncodedLanguageId(language);
}

/**
 * 加载自定义语言
 * @param monacoLanguages
 * @param monacoEditor
 * @param registerCompletion 是否注册自动补全 (默认不注册)
 * @constructor
 */
export function LoadCustomEditorLanguage(
  monacoLanguages?: Monaco['languages'] | undefined,
  monacoEditor?: Monaco['editor'] | undefined,
  registerCompletion: boolean = false
) {
  if (canLoadLanguage(monacoLanguages, CustomEditorLanguage.FlinkSQL)) {
    FlinkSQLLanguage(monacoLanguages, monacoEditor, registerCompletion);
  }
  if (canLoadLanguage(monacoLanguages, CustomEditorLanguage.JavaLog)) {
    LogLanguage(monacoLanguages);
  }
}
