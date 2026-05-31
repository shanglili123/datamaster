export type SqlMetaData = {
  statement: string;
  metaData: MetaData[];
};
export type MetaData = {
  table: string;
  connector: string;
  columns: Column[];
};
export type Column = {
  name: string;
  type: string;
};

/**
 *  the editor's suggestions
 */
export type ISuggestions = {
  label: string;
  kind: any;
  insertText: any;
  insertTextRules: any;
  detail?: any;
};
