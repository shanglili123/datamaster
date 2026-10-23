package com.datamaster.metadata.service.lineage.extractor;

import com.datamaster.metadata.api.lineage.dto.*;
import com.datamaster.metadata.service.lineage.LineageIdentity;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;

import static com.datamaster.metadata.service.lineage.extractor.JSqlParserLineageExtractor.*;

/** Package-private scoped AST evaluator. Unknown syntax is not traversed as if it were supported. */
final class ScopedSqlLineageParser {
    private static final int MAX_DEPTH = 64;
    private final LineageUnitDTO unit;
    private final Set<String> diagnostics;
    private int depth;

    ScopedSqlLineageParser(LineageUnitDTO unit, Set<String> diagnostics) {
        this.unit = unit;
        this.diagnostics = diagnostics;
    }

    Projection select(Select select, Projection virtual) {
        return select(select, virtual, Collections.<WithItem>emptyList());
    }

    Projection select(Select select, Projection virtual, List<WithItem> outerWith) {
        Environment environment = new Environment(virtual);
        withItems(outerWith, environment);
        withItems(select.getWithItemsList(), environment);
        return body(select.getSelectBody(), environment);
    }

    private void withItems(List<WithItem> items, Environment environment) {
        Set<String> local = new HashSet<>();
        for (WithItem item : list(items)) {
            String key = key(item.getName());
            if (item.isRecursive()) throw new ExtractionFailure("RECURSIVE_CTE");
            if (!local.add(key)) throw new ExtractionFailure("DUPLICATE_CTE");
            environment.ctes.remove(key); // Local CTE names shadow outer scopes, including in recursion checks.
            environment.pending.add(key);
        }
        for (WithItem item : list(items)) {
            if (item.getSubSelect() == null || item.isUseValues()) throw new ExtractionFailure("UNSUPPORTED_CTE");
            Projection projection = subquery(item.getSubSelect(), environment);
            if (!list(item.getWithItemList()).isEmpty()) {
                List<LineageColumnDTO> names = new ArrayList<>();
                for (SelectItem column : item.getWithItemList()) {
                    if (!(column instanceof SelectExpressionItem)
                            || !(((SelectExpressionItem) column).getExpression() instanceof Column)) {
                        throw new ExtractionFailure("UNSUPPORTED_CTE_COLUMNS");
                    }
                    names.add(column(((Column) ((SelectExpressionItem) column).getExpression()).getColumnName()));
                }
                projection = rename(projection, names);
            }
            environment.ctes.put(key(item.getName()), projection);
            environment.pending.remove(key(item.getName()));
        }
    }

    private Projection body(SelectBody body, Environment environment) {
        if (++depth > MAX_DEPTH) throw new ExtractionFailure("SQL_TOO_COMPLEX");
        try {
            if (body instanceof PlainSelect) return plain((PlainSelect) body, environment);
            if (body instanceof SetOperationList) return union((SetOperationList) body, environment);
            throw new ExtractionFailure("UNSUPPORTED_SELECT_BODY");
        } finally {
            depth--;
        }
    }

    private Projection plain(PlainSelect select, Environment environment) {
        Scope scope = new Scope(environment);
        if (select.getFromItem() != null) bind(select.getFromItem(), scope);
        boolean unsupportedJoin = false;
        for (Join join : list(select.getJoins())) {
            bind(join.getRightItem(), scope);
            if (join.isNatural() || !list(join.getUsingColumns()).isEmpty() || join.isApply()
                    || join.isSemi() || join.isWindowJoin()) unsupportedJoin = true;
        }
        Projection result = new Projection();
        for (Binding binding : scope.bindings) result.tables.addAll(binding.projection.tables);
        scope.tables = result.tables;
        for (SelectItem item : list(select.getSelectItems())) {
            if (item instanceof AllColumns) {
                if (scope.bindings.isEmpty()) unknownStar(result);
                for (Binding binding : scope.bindings) expand(binding.projection, result);
            } else if (item instanceof AllTableColumns) {
                Binding binding = scope.binding(((AllTableColumns) item).getTable());
                if (binding == null) unknownStar(result);
                else expand(binding.projection, result);
            } else if (item instanceof SelectExpressionItem) {
                SelectExpressionItem expression = (SelectExpressionItem) item;
                Cell cell = expression(expression.getExpression(), scope);
                cell.name = expression.getAlias() == null ? outputName(expression.getExpression())
                        : column(expression.getAlias().getName());
                result.cells.add(cell);
            } else {
                diagnostics.add("UNSUPPORTED_SELECT_ITEM");
                result.widthKnown = false;
            }
        }
        // Predicate tables are table-level evidence only; predicate columns never enter result cells.
        conditions(select.getWhere(), scope);
        conditions(select.getHaving(), scope);
        for (Join join : list(select.getJoins())) {
            if (join.getOnExpressions() != null) {
                for (Expression condition : join.getOnExpressions()) conditions(condition, scope);
            }
        }
        boolean unsupported = unsupportedJoin || !list(select.getIntoTables()).isEmpty()
                || scope.bindings.stream().anyMatch(binding -> binding.unsupported)
                || select.getOracleHierarchical() != null || select.getKsqlWindow() != null
                || select.getForXmlPath() != null || select.isEmitChanges();
        if (unsupported) {
            diagnostics.add("UNSUPPORTED_SELECT_CLAUSE");
            for (Cell cell : result.cells) cell.known = false;
        }
        return result;
    }

    private Projection union(SetOperationList set, Environment environment) {
        Projection result = null;
        boolean supported = true;
        for (SetOperation operation : list(set.getOperations())) {
            if (!(operation instanceof UnionOp) || !((UnionOp) operation).isAll()) supported = false;
        }
        for (SelectBody branch : list(set.getSelects())) {
            Projection projection = body(branch, new Environment(environment));
            if (result == null) {
                result = projection.copy();
            } else {
                result.tables.addAll(projection.tables);
                if (!result.widthKnown || !projection.widthKnown || result.cells.size() != projection.cells.size()) {
                    result.widthKnown = false;
                    diagnostics.add("UNION_COLUMN_COUNT_UNKNOWN");
                } else {
                    for (int i = 0; i < result.cells.size(); i++) {
                        result.cells.get(i).merge(projection.cells.get(i));
                    }
                }
            }
        }
        if (result == null) throw new ExtractionFailure("UNSUPPORTED_SELECT_BODY");
        if (!supported) {
            diagnostics.add("UNSUPPORTED_SET_OPERATION");
            for (Cell cell : result.cells) cell.known = false;
        }
        return result;
    }

    private Projection subquery(SubSelect subquery, Environment parent) {
        Environment environment = new Environment(parent);
        withItems(subquery.getWithItemsList(), environment);
        return body(subquery.getSelectBody(), environment);
    }

    private void bind(FromItem item, Scope scope) {
        if (item instanceof Table) {
            Table table = (Table) item;
            Projection projection;
            boolean unqualified = blank(table.getSchemaName())
                    && (table.getDatabase() == null || blank(table.getDatabase().getDatabaseName()));
            String name = key(table.getName());
            if (unqualified && scope.environment.ctes.containsKey(name)) {
                projection = scope.environment.ctes.get(name);
            } else if (unqualified && scope.environment.pending.contains(name)) {
                throw new ExtractionFailure("RECURSIVE_OR_FORWARD_CTE");
            } else if (unqualified && ((!quoted(table.getName()) && "sourcetable".equalsIgnoreCase(table.getName()))
                    || quoted(table.getName()) && "sourceTable".equals(unquote(table.getName())))) {
                if (scope.environment.virtual == null) throw new ExtractionFailure("UNBOUND_VIRTUAL_SOURCE");
                projection = scope.environment.virtual;
            } else {
                LineageEndpointDTO endpoint = resolve(table, false);
                projection = new Projection();
                projection.tables.add(endpoint);
                // Physical metadata is not assumed complete, ordered, or even provided by a reader query.
                projection.widthKnown = false;
                Binding binding = new Binding(table, table.getAlias(), projection, endpoint);
                scope.add(binding);
                binding.unsupported |= table.getPivot() != null || table.getUnPivot() != null;
                return;
            }
            Binding binding = new Binding(table, table.getAlias(), projection, null);
            binding.unsupported |= table.getPivot() != null || table.getUnPivot() != null;
            scope.add(binding);
        } else if (item instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) item;
            if (subSelect.getAlias() == null) {
                diagnostics.add("SUBQUERY_ALIAS_REQUIRED");
            }
            Binding binding = new Binding(null, subSelect.getAlias(), subquery(subSelect, scope.environment), null);
            binding.unsupported |= subSelect.getPivot() != null || subSelect.getUnPivot() != null;
            scope.add(binding);
        } else {
            throw new ExtractionFailure("UNSUPPORTED_FROM_ITEM");
        }
    }

    Cell expression(Expression expression, Scope scope) {
        if (++depth > MAX_DEPTH) throw new ExtractionFailure("SQL_TOO_COMPLEX");
        try {
            if (expression == null) return unknown("UNSUPPORTED_EXPRESSION");
            if (expression instanceof Column) return scope.column((Column) expression);
            if (expression instanceof LongValue || expression instanceof DoubleValue || expression instanceof StringValue
                    || expression instanceof NullValue || expression instanceof DateValue
                    || expression instanceof TimeValue || expression instanceof TimestampValue) return new Cell();
            if (expression instanceof JdbcParameter || expression instanceof JdbcNamedParameter) return unknown("DYNAMIC_SQL");
            if (expression instanceof Parenthesis) return expression(((Parenthesis) expression).getExpression(), scope);
            if (expression instanceof SignedExpression) return expression(((SignedExpression) expression).getExpression(), scope);
            if (expression instanceof CastExpression) {
                CastExpression cast = (CastExpression) expression;
                if (cast.getRowConstructor() != null) return unknown("UNSUPPORTED_EXPRESSION");
                return expression(cast.getLeftExpression(), scope);
            }
            if (expression instanceof Function) {
                Function function = (Function) expression;
                if (function.getNamedParameters() != null || function.getAttribute() != null
                        || function.getKeep() != null || !list(function.getOrderByElements()).isEmpty()) {
                    return unknown("UNSUPPORTED_FUNCTION");
                }
                Cell result = new Cell();
                if (function.getParameters() != null) {
                    for (Expression argument : list(function.getParameters().getExpressions())) {
                        result.merge(expression(argument, scope));
                    }
                }
                // COUNT(*) has row/table dependence, but no physical column value source.
                return result;
            }
            if (expression instanceof BinaryExpression && supportedBinary(expression)) {
                BinaryExpression binary = (BinaryExpression) expression;
                Cell result = expression(binary.getLeftExpression(), scope);
                result.merge(expression(binary.getRightExpression(), scope));
                return result;
            }
            if (expression instanceof CaseExpression) {
                CaseExpression caseExpression = (CaseExpression) expression;
                Cell result = new Cell();
                // CASE predicates select a value; they are not value columns themselves.
                conditions(caseExpression.getSwitchExpression(), scope);
                for (WhenClause when : list(caseExpression.getWhenClauses())) {
                    conditions(when.getWhenExpression(), scope);
                    result.merge(expression(when.getThenExpression(), scope));
                }
                if (caseExpression.getElseExpression() != null) result.merge(expression(caseExpression.getElseExpression(), scope));
                return result;
            }
            if (expression instanceof SubSelect) {
                // Uncorrelated scalar subqueries are safe; outer/correlated references cannot bind here.
                Projection projection = subquery((SubSelect) expression, scope.environment);
                scope.tables.addAll(projection.tables);
                if (!projection.widthKnown || projection.cells.size() != 1) return unknown("SCALAR_SUBQUERY_WIDTH");
                return projection.cells.get(0).copy();
            }
            // Analytics, JSON, arrays, dialect nodes, row constructors etc. are explicitly not inferred.
            return unknown("UNSUPPORTED_EXPRESSION");
        } finally {
            depth--;
        }
    }

    private boolean supportedBinary(Expression expression) {
        String type = expression.getClass().getName();
        return type.startsWith("net.sf.jsqlparser.expression.operators.arithmetic.")
                || type.startsWith("net.sf.jsqlparser.expression.operators.conditional.")
                || type.equals("net.sf.jsqlparser.expression.operators.relational.EqualsTo")
                || type.equals("net.sf.jsqlparser.expression.operators.relational.NotEqualsTo")
                || type.equals("net.sf.jsqlparser.expression.operators.relational.GreaterThan")
                || type.equals("net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals")
                || type.equals("net.sf.jsqlparser.expression.operators.relational.MinorThan")
                || type.equals("net.sf.jsqlparser.expression.operators.relational.MinorThanEquals");
    }

    void conditions(Expression expression, final Scope scope) {
        if (expression == null) return;
        expression.accept(new ExpressionVisitorAdapter() {
            @Override
            public void visit(SubSelect subSelect) {
                Projection projection = subquery(subSelect, scope.environment);
                scope.tables.addAll(projection.tables);
            }
            @Override
            public void visit(JdbcParameter parameter) { diagnostics.add("DYNAMIC_SQL"); }
            @Override
            public void visit(JdbcNamedParameter parameter) { diagnostics.add("DYNAMIC_SQL"); }
        });
    }

    Projection readerProjection() {
        Projection result = new Projection();
        result.tables.addAll(list(unit.getSources()));
        if (list(unit.getSources()).size() != 1) {
            result.widthKnown = false;
            diagnostics.add("EXPLICIT_MAPPING_REQUIRED");
            return result;
        }
        LineageEndpointDTO source = unit.getSources().get(0);
        validateEndpoint(source);
        if (!Boolean.TRUE.equals(unit.getColumnOrderReliable()) || list(unit.getSourceColumns()).isEmpty()) {
            result.widthKnown = false;
            diagnostics.add("UNRELIABLE_SOURCE_COLUMNS");
            return result;
        }
        for (LineageColumnDTO column : list(unit.getSourceColumns())) {
            Cell cell = new Cell();
            cell.name = column;
            if (column == null || blank(column.getName())) {
                cell.known = false;
                diagnostics.add("INVALID_SOURCE_COLUMN");
            } else if (!generated(column)) {
                cell.refs.add(new Ref(source, column));
            }
            result.cells.add(cell);
        }
        return withGenerated(result);
    }

    Projection withGenerated(Projection source) {
        Projection result = source.copy();
        for (LineageColumnDTO generated : list(unit.getGeneratedColumns())) {
            if (generated == null || blank(generated.getName())) {
                diagnostics.add("INVALID_GENERATED_COLUMN");
                continue;
            }
            boolean present = false;
            for (Cell cell : result.cells) {
                if (same(cell.name, generated)) {
                    // Explicit generated-column declaration has no physical source, even on name collision.
                    cell.refs.clear();
                    cell.known = true;
                    present = true;
                }
            }
            if (!present) {
                Cell cell = new Cell();
                cell.name = generated;
                result.cells.add(cell);
            }
        }
        return result;
    }

    boolean generated(LineageColumnDTO column) {
        for (LineageColumnDTO generated : list(unit.getGeneratedColumns())) if (same(column, generated)) return true;
        return false;
    }

    Scope updateScope(Table table, LineageEndpointDTO target) {
        Scope scope = new Scope(new Environment((Projection) null));
        Projection projection = new Projection();
        projection.tables.add(target);
        projection.widthKnown = false;
        scope.add(new Binding(table, table.getAlias(), projection, target));
        return scope;
    }

    LineageEndpointDTO target(Table table) { return resolve(table, true); }

    private LineageEndpointDTO resolve(Table table, boolean target) {
        if (table == null || blank(table.getName())) throw new ExtractionFailure("UNRESOLVED_IDENTITY");
        if (table.getDatabase() != null && table.getDatabase().getServer() != null) {
            throw new ExtractionFailure("UNSUPPORTED_REMOTE_IDENTITY");
        }
        List<LineageEndpointDTO> matches = new ArrayList<>();
        List<LineageEndpointDTO> candidates = target && unit.getTarget() != null
                ? Collections.singletonList(unit.getTarget()) : list(unit.getSources());
        String catalog = table.getDatabase() == null ? null : table.getDatabase().getDatabaseName();
        String prefix = table.getSchemaName();
        boolean twoPart = blank(catalog) && !blank(prefix);
        Set<Boolean> namespaces = new HashSet<>();
        for (LineageEndpointDTO endpoint : candidates) {
            if (endpoint == null || !blank(endpoint.getKind()) && !"TABLE".equalsIgnoreCase(endpoint.getKind())
                    || !identifierEquals(table.getName(), endpoint.getTable(), endpoint.getTableQuoted(), endpoint.getCaseRule())) continue;
            if (twoPart) {
                Set<Boolean> endpointNamespaces = namespaces(prefix, endpoint);
                if (endpointNamespaces.size() > 1) throw new ExtractionFailure("AMBIGUOUS_TABLE_NAMESPACE");
                if (!endpointNamespaces.isEmpty()) {
                    matches.add(endpoint);
                    namespaces.addAll(endpointNamespaces);
                }
            } else if ((blank(prefix) || identifierEquals(prefix, endpoint.getSchema(), endpoint.getSchemaQuoted(), endpoint.getCaseRule()))
                    && (blank(catalog) || identifierEquals(catalog, endpoint.getCatalog(), endpoint.getCatalogQuoted(), endpoint.getCaseRule()))) {
                matches.add(endpoint);
            }
        }
        if (namespaces.size() > 1) throw new ExtractionFailure("AMBIGUOUS_TABLE_NAMESPACE");
        if (matches.size() > 1) throw new ExtractionFailure("AMBIGUOUS_TABLE");
        if (matches.size() == 1) {
            validateEndpoint(matches.get(0));
            return matches.get(0);
        }
        LineageEndpointDTO context = unit.getContext();
        if (context == null && list(unit.getSources()).size() == 1) context = unit.getSources().get(0);
        if (context == null || context.getDatasourceId() == null) throw new ExtractionFailure("UNRESOLVED_IDENTITY");
        boolean catalogPrefix = false;
        if (twoPart) {
            // JSqlParser puts the first token of db.table in schemaName even on catalog-only
            // engines. Only structured identities can tell us what that token means.
            namespaces.addAll(namespaces(prefix, context));
            for (LineageEndpointDTO source : list(unit.getSources())) {
                if (source != null && Objects.equals(context.getDatasourceId(), source.getDatasourceId())) {
                    namespaces.addAll(namespaces(prefix, source));
                }
            }
            if (namespaces.size() > 1) throw new ExtractionFailure("AMBIGUOUS_TABLE_NAMESPACE");
            if (namespaces.isEmpty()) throw new ExtractionFailure("UNKNOWN_TWO_PART_NAMESPACE");
            catalogPrefix = namespaces.contains(Boolean.TRUE);
        }
        LineageEndpointDTO endpoint = new LineageEndpointDTO();
        endpoint.setKind("TABLE");
        endpoint.setDatasourceId(context.getDatasourceId());
        endpoint.setCaseRule(context.getCaseRule());
        endpoint.setTable(unquote(table.getName()));
        endpoint.setTableQuoted(quoted(table.getName()));
        endpoint.setSchema(catalogPrefix ? null : blank(prefix) ? context.getSchema() : unquote(prefix));
        endpoint.setSchemaQuoted(catalogPrefix ? null : blank(prefix) ? context.getSchemaQuoted() : quoted(prefix));
        endpoint.setCatalog(catalogPrefix ? unquote(prefix) : blank(catalog) ? context.getCatalog() : unquote(catalog));
        endpoint.setCatalogQuoted(catalogPrefix ? quoted(prefix) : blank(catalog) ? context.getCatalogQuoted() : quoted(catalog));
        validateEndpoint(endpoint);
        return endpoint;
    }

    /** true = catalog-only prefix, false = schema prefix; no dialect is inferred from a name. */
    private Set<Boolean> namespaces(String prefix, LineageEndpointDTO endpoint) {
        Set<Boolean> result = new HashSet<>();
        if (blank(endpoint.getKind()) || "TABLE".equalsIgnoreCase(endpoint.getKind())) {
            if (!blank(endpoint.getSchema()) && identifierEquals(prefix, endpoint.getSchema(), endpoint.getSchemaQuoted(), endpoint.getCaseRule())) {
                result.add(Boolean.FALSE);
            }
            if (blank(endpoint.getSchema()) && !blank(endpoint.getCatalog())
                    && identifierEquals(prefix, endpoint.getCatalog(), endpoint.getCatalogQuoted(), endpoint.getCaseRule())) {
                result.add(Boolean.TRUE);
            }
        }
        return result;
    }

    private boolean identifierEquals(String sql, String name, Boolean isQuoted, String caseRule) {
        return Objects.equals(LineageIdentity.normalize(unquote(sql), quoted(sql), caseRule),
                LineageIdentity.normalize(name, isQuoted, caseRule));
    }

    private Projection rename(Projection source, List<LineageColumnDTO> names) {
        Projection result = source.copy();
        if (!result.widthKnown || result.cells.size() != names.size()) {
            result.widthKnown = false;
            diagnostics.add("CTE_COLUMN_COUNT_MISMATCH");
            return result;
        }
        for (int i = 0; i < names.size(); i++) result.cells.get(i).name = names.get(i);
        return result;
    }

    private void expand(Projection source, Projection result) {
        result.tables.addAll(source.tables);
        if (!source.widthKnown) {
            unknownStar(result);
            return;
        }
        for (Cell cell : source.cells) result.cells.add(cell.copy());
    }

    private void unknownStar(Projection result) {
        result.widthKnown = false;
        diagnostics.add("UNKNOWN_STAR");
    }

    private Cell unknown(String code) {
        diagnostics.add(code);
        Cell result = new Cell();
        result.known = false;
        return result;
    }

    private LineageColumnDTO outputName(Expression expression) {
        return expression instanceof Column ? column(((Column) expression).getColumnName()) : null;
    }

    LineageColumnDTO column(String name) {
        LineageColumnDTO column = new LineageColumnDTO();
        column.setName(unquote(name));
        column.setQuoted(quoted(name));
        return column;
    }

    private String key(String name) {
        return LineageIdentity.normalize(unquote(name), quoted(name), caseRule());
    }

    private String caseRule() {
        if (unit.getContext() != null) return unit.getContext().getCaseRule();
        return list(unit.getSources()).size() == 1 ? unit.getSources().get(0).getCaseRule() : "PRESERVE";
    }

    private boolean same(LineageColumnDTO left, LineageColumnDTO right) {
        return left != null && right != null && Objects.equals(
                LineageIdentity.normalize(left.getName(), left.getQuoted(), caseRule()),
                LineageIdentity.normalize(right.getName(), right.getQuoted(), caseRule()));
    }

    private static boolean quoted(String value) {
        if (value == null || value.length() < 2) return false;
        char first = value.charAt(0), last = value.charAt(value.length() - 1);
        return first == '"' && last == '"' || first == '`' && last == '`' || first == '[' && last == ']';
    }

    private static String unquote(String value) {
        if (!quoted(value)) return value;
        char quote = value.charAt(0);
        String content = value.substring(1, value.length() - 1);
        return quote == '[' ? content.replace("]]", "]") : content.replace("" + quote + quote, "" + quote);
    }

    final class Scope {
        private final Environment environment;
        private final List<Binding> bindings = new ArrayList<>();
        Set<LineageEndpointDTO> tables = new LinkedHashSet<>();
        private Scope(Environment environment) { this.environment = environment; }
        private void add(Binding binding) {
            bindings.add(binding);
            if (binding.unsupported) diagnostics.add("UNSUPPORTED_FROM_ITEM");
        }
        private Binding binding(Table table) {
            List<Binding> matches = new ArrayList<>();
            for (Binding binding : bindings) {
                if (binding.matches(table)) matches.add(binding);
            }
            if (matches.size() != 1) {
                diagnostics.add(matches.isEmpty() ? "UNRESOLVED_QUALIFIER" : "AMBIGUOUS_QUALIFIER");
                return null;
            }
            return matches.get(0);
        }
        private Cell column(Column column) {
            Table qualifier = column.getTable();
            Binding binding;
            if (qualifier != null && !blank(qualifier.getName())) {
                binding = binding(qualifier);
            } else if (bindings.size() == 1) {
                binding = bindings.get(0);
            } else {
                return unknown(bindings.isEmpty() ? "UNRESOLVED_COLUMN" : "AMBIGUOUS_COLUMN");
            }
            if (binding == null) return unknown("UNRESOLVED_COLUMN");
            if (binding.unsupported) return unknown("UNSUPPORTED_FROM_ITEM");
            if (binding.physical != null) {
                Cell cell = new Cell();
                cell.refs.add(new Ref(binding.physical, ScopedSqlLineageParser.this.column(column.getColumnName())));
                return cell;
            }
            if (!binding.projection.widthKnown) return unknown("UNKNOWN_DERIVED_COLUMNS");
            Cell match = null;
            LineageColumnDTO wanted = ScopedSqlLineageParser.this.column(column.getColumnName());
            for (Cell cell : binding.projection.cells) {
                if (same(cell.name, wanted)) {
                    if (match != null) return unknown("AMBIGUOUS_COLUMN");
                    match = cell;
                }
            }
            return match == null ? unknown("UNRESOLVED_COLUMN") : match.copy();
        }
    }

    private final class Binding {
        private final Table table;
        private final Alias alias;
        private final Projection projection;
        private final LineageEndpointDTO physical;
        private boolean unsupported;
        private Binding(Table table, Alias alias, Projection projection, LineageEndpointDTO physical) {
            this.table = table;
            this.alias = alias;
            this.projection = projection;
            this.physical = physical;
            if (alias != null && !list(alias.getAliasColumns()).isEmpty()) unsupported = true;
        }
        private boolean matches(Table qualifier) {
            if (alias != null) {
                return blank(qualifier.getSchemaName()) && (qualifier.getDatabase() == null
                        || blank(qualifier.getDatabase().getDatabaseName()))
                        && Objects.equals(key(alias.getName()), key(qualifier.getName()));
            }
            if (table == null || !Objects.equals(key(table.getName()), key(qualifier.getName()))) return false;
            if (!blank(qualifier.getSchemaName()) && !Objects.equals(key(table.getSchemaName()), key(qualifier.getSchemaName()))) return false;
            String catalog = qualifier.getDatabase() == null ? null : qualifier.getDatabase().getDatabaseName();
            return blank(catalog) || table.getDatabase() != null && Objects.equals(key(catalog), key(table.getDatabase().getDatabaseName()));
        }
    }

    private static final class Environment {
        private final Map<String, Projection> ctes = new LinkedHashMap<>();
        private final Set<String> pending = new HashSet<>();
        private final Projection virtual;
        private Environment(Projection virtual) { this.virtual = virtual; }
        private Environment(Environment parent) {
            virtual = parent.virtual;
            ctes.putAll(parent.ctes);
            pending.addAll(parent.pending);
        }
    }

    static final class Projection {
        final List<Cell> cells = new ArrayList<>();
        final Set<LineageEndpointDTO> tables = new LinkedHashSet<>();
        boolean widthKnown = true;
        Projection copy() {
            Projection result = new Projection();
            result.widthKnown = widthKnown;
            result.tables.addAll(tables);
            for (Cell cell : cells) result.cells.add(cell.copy());
            return result;
        }
    }

    static final class Cell {
        LineageColumnDTO name;
        final Set<Ref> refs = new LinkedHashSet<>();
        boolean known = true;
        void merge(Cell cell) { refs.addAll(cell.refs); known &= cell.known; }
        Cell copy() {
            Cell result = new Cell();
            result.name = name;
            result.refs.addAll(refs);
            result.known = known;
            return result;
        }
    }

    static final class Ref {
        final LineageEndpointDTO table;
        final LineageColumnDTO column;
        Ref(LineageEndpointDTO table, LineageColumnDTO column) { this.table = table; this.column = column; }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Ref)) return false;
            Ref ref = (Ref) other;
            return Objects.equals(table, ref.table) && Objects.equals(column, ref.column);
        }
        @Override public int hashCode() { return Objects.hash(table, column); }
    }
}
