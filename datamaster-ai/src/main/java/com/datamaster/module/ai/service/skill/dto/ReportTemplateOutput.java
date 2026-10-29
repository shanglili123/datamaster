package com.datamaster.module.ai.service.skill.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/** Typed report structure with explicit dynamic style and data-schema extensions. */
@Data
public class ReportTemplateOutput {
    private DataSchema dataSchema;
    private Layout layout;
    private Style style;
    private Prompt prompt;

    @Data
    public static class DataSchema {
        private List<String> required;
        private List<Field> fields;
    }
    @Data
    public static class Field {
        private String name;
        private String label;
        private String type;
        private String description;
        private List<Column> columns;
        private Map<String, Object> items;
        private Map<String, Object> properties;
    }
    @Data
    public static class Layout {
        private Map<String, String> page;
        private List<Section> sections;
    }
    @Data
    public static class Section {
        private String key;
        private String id;
        private String type;
        private String title;
        private String bind;
        private String xField;
        private String yField;
        private String nameField;
        private String valueField;
        /** Number of metric-grid columns; serialized as columns for the existing renderer. */
        private Integer gridColumns;
        private List<Column> columns;
        private List<Item> items;
        private Map<String, String> style;
    }
    @Data
    public static class Item {
        private String label;
        private String bind;
        private String format;
        private String prefix;
        private String suffix;
        private Integer precision;
        private Integer digits;
        private String unit;
    }
    @Data
    public static class Column {
        private String prop;
        private String label;
        private String type;
    }
    @Data
    public static class Style {
        private Map<String, String> theme;
        private Map<String, Map<String, String>> components;
    }
    @Data
    public static class Prompt {
        private String instruction;
        private Map<String, Object> params;
    }
}
