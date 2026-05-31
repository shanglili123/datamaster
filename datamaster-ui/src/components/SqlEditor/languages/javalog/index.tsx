import { CustomEditorLanguage } from '../constants';
import { buildMonarchTokensProvider } from './function';

export function LogLanguage(monacoLanguages) {
  // Register a new language
  monacoLanguages?.register({
    id: CustomEditorLanguage.JavaLog,
    extensions: [],
    mimetypes: [
      'text/x-java-log',
      'text/x-javalog',
      'text/x-java-source',
      'text/x-java',
      'text/java'
    ],
    aliases: ['javalog', 'Javalog', 'jl', 'log']
  });

  buildMonarchTokensProvider(monacoLanguages);
}
