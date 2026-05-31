import { CustomEditorLanguage } from "../constants";
import { buildFlinkSQLConfiguration, buildMonarchTokensProvider, registerFlinkSQLCompilation } from "./function";

export function FlinkSQLLanguage(monacoLanguages, monacoEditor, registerCompletion) {
  // Register a new language
  monacoLanguages?.register({
    id: CustomEditorLanguage.FlinkSQL,
    extensions: [".sql"],
    mimetypes: ["text/x-flinksql", "text/x-flinksql", "text/x-flinksql", "text/flinksql"],
    aliases: ["flinksql", "fsql", "flinkSQL", "FlinkSQL"],
  });
  buildMonarchTokensProvider(monacoLanguages);

  // Register a completion item provider for the new language
  if (registerCompletion) {
    registerFlinkSQLCompilation(monacoLanguages);
  }
  buildFlinkSQLConfiguration(monacoLanguages);

  monacoLanguages?.onLanguageEncountered(CustomEditorLanguage.FlinkSQL, () => {
    monacoEditor?.getModels().forEach((model) => {
      model.onDidChangeLanguage(() => {
        if (model.getLanguageId() === CustomEditorLanguage.FlinkSQL) {
          buildFlinkSQLConfiguration(monacoLanguages);
        }
      });
    });
    buildMonarchTokensProvider(monacoLanguages);
  });
}
