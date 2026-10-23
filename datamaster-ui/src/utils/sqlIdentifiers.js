// Table names are single identifiers, including any dots in the actual name.
// No database/schema is inferred from connection configuration.
export function quoteSqlIdentifier(identifier, datasourceType) {
  const name = String(identifier ?? '');
  const type = String(datasourceType || '').toLowerCase().replace(/[^a-z0-9]/g, '');
  if (['mysql', 'doris', 'hive'].includes(type)) return '`' + name.replace(/`/g, '``') + '`';
  if (['sqlserver', 'sqlserver2008', 'sqlerver', 'mssql'].includes(type)) {
    return '[' + name.replace(/\]/g, ']]') + ']';
  }
  return '"' + name.replace(/"/g, '""') + '"';
}

export function buildSelectSql(tableName, datasourceType) {
  return `SELECT * FROM ${quoteSqlIdentifier(tableName, datasourceType)};`;
}
