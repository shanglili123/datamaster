package com.datamaster.metadata.service.lineage.extractor;

import com.datamaster.metadata.api.lineage.dto.*;
import com.datamaster.metadata.service.lineage.LineageExtractor;
import com.datamaster.metadata.service.lineage.LineageIdentity;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Offline, conservative extraction only. No collector dependency, metadata lookup, SQL execution,
 * storage or SQL/exception text in the result. The caller owns persistence and capture diagnostics.
 */
@Component
public class JSqlParserLineageExtractor implements LineageExtractor {
    private static final int MAX_SQL_LENGTH = 200000;

    @Override
    public LineageExtractionDTO extract(LineageSnapshotDTO snapshot) {
        Result result = new Result();
        if (snapshot == null) {
            result.diagnostics.add("INVALID_SNAPSHOT");
            return result.finish(true);
        }
        boolean captureFailed = "FAILED".equals(snapshot.getCaptureStatus());
        if (captureFailed || "PARTIAL".equals(snapshot.getCaptureStatus())) {
            result.diagnostics.add("CAPTURE_INCOMPLETE");
        }
        if (snapshot.getCaptureStatus() != null && !"COMPLETE".equals(snapshot.getCaptureStatus())
                && !"PARTIAL".equals(snapshot.getCaptureStatus()) && !captureFailed) {
            result.diagnostics.add("INVALID_CAPTURE_STATUS");
        }
        boolean failed = captureFailed;
        for (LineageUnitDTO unit : list(snapshot.getUnits())) {
            if (unit == null) {
                result.diagnostics.add("INVALID_UNIT");
                failed = true;
                continue;
            }
            UnitExtraction extraction = new UnitExtraction(unit);
            try {
                extraction.extract();
            } catch (ExtractionFailure failure) {
                extraction.diagnostics.add(failure.code);
                if (!"AMBIGUOUS_TABLE_NAMESPACE".equals(failure.code)
                        && !"UNKNOWN_TWO_PART_NAMESPACE".equals(failure.code)) failed = true;
            } catch (JSQLParserException failure) {
                extraction.diagnostics.add("PARSE_FAILED");
                failed = true;
            } catch (RuntimeException failure) {
                // Never publish a parser exception: it may contain the complete SQL or literals.
                extraction.diagnostics.add("UNSUPPORTED_AST");
                failed = true;
            } catch (StackOverflowError failure) {
                extraction.diagnostics.add("SQL_TOO_COMPLEX");
                failed = true;
            }
            result.diagnostics.addAll(extraction.diagnostics);
            result.relations.addAll(extraction.relations);
        }
        return result.finish(failed);
    }

    private static final class UnitExtraction {
        private final LineageUnitDTO unit;
        private final Set<String> diagnostics = new LinkedHashSet<>();
        private final List<LineageRelationDTO> relations = new ArrayList<>();
        private final Set<String> relationKeys = new HashSet<>();
        private final ScopedSqlLineageParser parser;

        private UnitExtraction(LineageUnitDTO unit) {
            this.unit = unit;
            parser = new ScopedSqlLineageParser(unit, diagnostics);
        }

        private void extract() throws JSQLParserException {
            boolean hasSql = !blank(unit.getReaderSql()) || !blank(unit.getTransformSql());
            if (!hasSql) {
                directRoute();
                return;
            }
            Statement transform = blank(unit.getTransformSql()) ? null : parse(unit.getTransformSql());
            if (transform != null && !(transform instanceof Select)) {
                if (!blank(unit.getReaderSql())) throw new ExtractionFailure("UNSUPPORTED_SQL_PIPELINE");
                writeStatement(transform);
                return;
            }
            ScopedSqlLineageParser.Projection reader = null;
            if (!blank(unit.getReaderSql())) {
                Statement statement = parse(unit.getReaderSql());
                if (!(statement instanceof Select)) throw new ExtractionFailure("UNSUPPORTED_READER_STATEMENT");
                reader = parser.select((Select) statement, null);
                // Reader output aliases/order come from the query AST, never from JDBC table order.
                reader = parser.withGenerated(reader);
            } else if (!list(unit.getSources()).isEmpty()) {
                reader = parser.readerProjection();
            }
            ScopedSqlLineageParser.Projection output = transform == null ? reader
                    : parser.select((Select) transform, reader);
            if (unit.getTarget() == null) {
                // Ad-hoc read-only SQL must not invent a durable output object.
                return;
            }
            validateEndpoint(unit.getTarget());
            if (output == null) {
                diagnostics.add("MISSING_READER");
                return;
            }
            tableEdges(output.tables, unit.getTarget());
            if (!list(unit.getMappings()).isEmpty()) {
                explicitMappings();
            } else {
                if (!Boolean.TRUE.equals(unit.getColumnOrderReliable())) {
                    diagnostics.add("UNRELIABLE_COLUMN_ORDER");
                } else {
                    project(output, list(unit.getTargetColumns()), unit.getTarget());
                }
            }
        }

        private void directRoute() {
            if (unit.getTarget() == null) {
                diagnostics.add("MISSING_TARGET");
                return;
            }
            validateEndpoint(unit.getTarget());
            for (LineageEndpointDTO source : list(unit.getSources())) {
                validateEndpoint(source);
                add(source, null, unit.getTarget(), null);
            }
            if (!list(unit.getMappings()).isEmpty()) {
                explicitMappings();
                return;
            }
            if (list(unit.getSources()).size() != 1) {
                diagnostics.add("EXPLICIT_MAPPING_REQUIRED");
                return;
            }
            if (!Boolean.TRUE.equals(unit.getColumnOrderReliable())) {
                diagnostics.add("UNRELIABLE_COLUMN_ORDER");
                return;
            }
            project(parser.readerProjection(), list(unit.getTargetColumns()), unit.getTarget());
        }

        private void explicitMappings() {
            for (LineageMappingDTO mapping : list(unit.getMappings())) {
                if (mapping == null || mapping.getSource() == null || mapping.getSourceColumn() == null
                        || mapping.getTargetColumn() == null || blank(mapping.getSourceColumn().getName())
                        || blank(mapping.getTargetColumn().getName())) {
                    diagnostics.add("INVALID_MAPPING");
                    continue;
                }
                validateEndpoint(mapping.getSource());
                if (parser.generated(mapping.getSourceColumn())) continue;
                // Contract: mappings already name physical columns, not transform aliases.
                add(mapping.getSource(), null, unit.getTarget(), null);
                add(mapping.getSource(), mapping.getSourceColumn(), unit.getTarget(), mapping.getTargetColumn());
            }
        }

        private void writeStatement(Statement statement) {
            if (statement instanceof Insert) {
                Insert insert = (Insert) statement;
                if (insert.getSelect() == null || insert.isUseDuplicate() || insert.isUseSet()
                        || insert.getConflictAction() != null || insert.getOutputClause() != null
                        || !list(insert.getReturningExpressionList()).isEmpty()) {
                    throw new ExtractionFailure("UNSUPPORTED_INSERT");
                }
                LineageEndpointDTO target = parser.target(insert.getTable());
                ScopedSqlLineageParser.Projection output = parser.select(insert.getSelect(), null, insert.getWithItemsList());
                tableEdges(output.tables, target);
                List<LineageColumnDTO> targets = new ArrayList<>();
                for (Column column : list(insert.getColumns())) targets.add(parser.column(column.getColumnName()));
                if (targets.isEmpty()) {
                    // INSERT without a column list cannot use SELECT aliases as writer columns.
                    diagnostics.add("EXPLICIT_TARGET_COLUMNS_REQUIRED");
                } else {
                    project(output, targets, target);
                }
            } else if (statement instanceof CreateTable) {
                CreateTable create = (CreateTable) statement;
                if (create.getSelect() == null) return; // Bare DDL has no data-flow relation.
                LineageEndpointDTO target = parser.target(create.getTable());
                ScopedSqlLineageParser.Projection output = parser.select(create.getSelect(), null);
                tableEdges(output.tables, target);
                List<LineageColumnDTO> targets = new ArrayList<>();
                for (String name : list(create.getColumns())) targets.add(parser.column(name));
                if (targets.isEmpty() && !list(create.getColumnDefinitions()).isEmpty()) {
                    create.getColumnDefinitions().forEach(c -> targets.add(parser.column(c.getColumnName())));
                }
                if (targets.isEmpty()) {
                    for (ScopedSqlLineageParser.Cell cell : output.cells) targets.add(cell.name);
                }
                project(output, targets, target);
            } else if (statement instanceof Update) {
                update((Update) statement);
            } else {
                throw new ExtractionFailure("UNSUPPORTED_STATEMENT");
            }
        }

        private void update(Update update) {
            if (update.getFromItem() != null || !list(update.getJoins()).isEmpty()
                    || !list(update.getStartJoins()).isEmpty()
                    || !list(update.getWithItemsList()).isEmpty() || update.getOutputClause() != null
                    || !list(update.getReturningExpressionList()).isEmpty()) {
                throw new ExtractionFailure("UNSUPPORTED_UPDATE");
            }
            LineageEndpointDTO target = parser.target(update.getTable());
            ScopedSqlLineageParser.Scope scope = parser.updateScope(update.getTable(), target);
            List<LineageRelationDTO> pending = new ArrayList<>();
            // Evaluate all RHS against the pre-update scope, not sequential SET assignments.
            for (UpdateSet set : list(update.getUpdateSets())) {
                if (set.getColumns().size() != 1 || set.getExpressions().size() != 1) {
                    throw new ExtractionFailure("UNSUPPORTED_UPDATE");
                }
                ScopedSqlLineageParser.Cell cell = parser.expression(set.getExpressions().get(0), scope);
                LineageColumnDTO targetColumn = parser.column(set.getColumns().get(0).getColumnName());
                if (cell.known) {
                    for (ScopedSqlLineageParser.Ref ref : cell.refs) {
                        pending.add(relation(ref.table, ref.column, target, targetColumn));
                    }
                }
            }
            // WHERE is not a value source. Unsupported/subquery predicates are diagnosed separately.
            parser.conditions(update.getWhere(), scope);
            for (LineageRelationDTO relation : pending) {
                add(relation.getSource(), null, target, null);
                add(relation.getSource(), relation.getSourceColumn(), relation.getTarget(), relation.getTargetColumn());
            }
            tableEdges(scope.tables, target);
        }

        private void project(ScopedSqlLineageParser.Projection output, List<LineageColumnDTO> targets,
                             LineageEndpointDTO target) {
            if (!output.widthKnown) {
                diagnostics.add("UNKNOWN_PROJECTION_WIDTH");
                return;
            }
            if (targets.isEmpty() || output.cells.size() != targets.size()) {
                diagnostics.add("COLUMN_COUNT_MISMATCH");
                return;
            }
            for (int i = 0; i < targets.size(); i++) {
                LineageColumnDTO column = targets.get(i);
                if (column == null || blank(column.getName())) {
                    diagnostics.add("UNNAMED_TARGET_COLUMN");
                    continue;
                }
                ScopedSqlLineageParser.Cell cell = output.cells.get(i);
                if (!cell.known) continue;
                for (ScopedSqlLineageParser.Ref ref : cell.refs) add(ref.table, ref.column, target, column);
            }
        }

        private void tableEdges(Collection<LineageEndpointDTO> tables, LineageEndpointDTO target) {
            for (LineageEndpointDTO table : tables) add(table, null, target, null);
        }

        private void add(LineageEndpointDTO source, LineageColumnDTO sourceColumn,
                         LineageEndpointDTO target, LineageColumnDTO targetColumn) {
            LineageRelationDTO relation = relation(source, sourceColumn, target, targetColumn);
            LineageIdentity identity = new LineageIdentity();
            String key = identity.relationId(1L, relation);
            if (relationKeys.add(key)) relations.add(relation);
        }

        private LineageRelationDTO relation(LineageEndpointDTO source, LineageColumnDTO sourceColumn,
                                            LineageEndpointDTO target, LineageColumnDTO targetColumn) {
            LineageRelationDTO relation = new LineageRelationDTO();
            relation.setSource(source);
            relation.setSourceColumn(sourceColumn);
            relation.setTarget(target);
            relation.setTargetColumn(targetColumn);
            relation.setUnitKey(unit.getUnitKey());
            relation.setRouteKey(unit.getRouteKey());
            relation.setKind(sourceColumn == null ? "TABLE" : "COLUMN");
            relation.setConfidence("EXACT");
            return relation;
        }
    }

    private static Statement parse(String sql) throws JSQLParserException {
        if (sql.length() > MAX_SQL_LENGTH) throw new ExtractionFailure("SQL_TOO_LARGE");
        // This is rejection only, never a text/regex fallback for extracting lineage.
        if (sql.contains("${") || sql.contains("#{") || sql.contains("{{")) {
            throw new ExtractionFailure("DYNAMIC_SQL");
        }
        return CCJSqlParserUtil.parse(sql);
    }

    static void validateEndpoint(LineageEndpointDTO endpoint) {
        try {
            LineageIdentity.canonical(endpoint);
        } catch (IllegalArgumentException failure) {
            throw new ExtractionFailure("UNRESOLVED_IDENTITY");
        }
    }

    static boolean blank(String text) { return text == null || text.trim().isEmpty(); }
    static <T> List<T> list(List<T> value) { return value == null ? Collections.<T>emptyList() : value; }

    static final class ExtractionFailure extends RuntimeException {
        private final String code;
        ExtractionFailure(String code) { super(code); this.code = code; }
    }

    private static final class Result {
        private final Set<String> diagnostics = new LinkedHashSet<>();
        private final List<LineageRelationDTO> relations = new ArrayList<>();
        private LineageExtractionDTO finish(boolean failed) {
            LineageExtractionDTO result = new LineageExtractionDTO();
            result.setRelations(relations);
            result.setStatus(diagnostics.isEmpty() ? "COMPLETE" : failed && relations.isEmpty() ? "FAILED" : "PARTIAL");
            StringBuilder codes = new StringBuilder();
            for (String diagnostic : diagnostics) {
                if (codes.length() + diagnostic.length() + (codes.length() == 0 ? 0 : 1) > 500) break;
                if (codes.length() != 0) codes.append(',');
                codes.append(diagnostic);
            }
            result.setErrorCode(codes.length() == 0 ? null : codes.toString());
            return result;
        }
    }
}
