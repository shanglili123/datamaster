

package com.datamaster.module.collector.utils.model;

public class PreviousTaskNode {

    /**
     * code
     */
    private long code;

    /**
     * name
     */
    private String name;

    /**
     * version
     */
    private int version;

    public PreviousTaskNode() {

    }

    public PreviousTaskNode(long code, String name, int version) {
        this.code = code;
        this.name = name;
        this.version = version;
    }

    public long getCode() {
        return code;
    }

    public void setCode(long code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
