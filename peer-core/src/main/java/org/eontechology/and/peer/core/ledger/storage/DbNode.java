package org.eontechology.and.peer.core.ledger.storage;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings("unused")
@DatabaseTable(tableName = "nodes")
public class DbNode {

    @DatabaseField(columnName = "key", canBeNull = false)
    private String key;

    @DatabaseField(columnName = "index", canBeNull = false)
    private long index;

    @DatabaseField(columnName = "timestamp", canBeNull = false)
    private int timestamp;

    @DatabaseField(columnName = "mask", canBeNull = false)
    private long mask;

    @DatabaseField(columnName = "mask_length", canBeNull = false)
    private int maskLength;

    @DatabaseField(columnName = "type", canBeNull = false)
    private int type;

    @DatabaseField(columnName = "right_node_id")
    private String rightNode;

    @DatabaseField(columnName = "left_node_id")
    private String leftNode;

    @DatabaseField(columnName = "value", canBeNull = false)
    private String value;

    @DatabaseField(columnName = "color", canBeNull = false)
    private int color;

    public DbNode() {

    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public long getMask() {
        return mask;
    }

    public void setMask(long mask) {
        this.mask = mask;
    }

    public int getMaskLength() {
        return maskLength;
    }

    public void setMaskLength(int maskLength) {
        this.maskLength = maskLength;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getRightNode() {
        return rightNode;
    }

    public void setRightNode(String rightNode) {
        this.rightNode = rightNode;
    }

    public String getLeftNode() {
        return leftNode;
    }

    public void setLeftNode(String leftNode) {
        this.leftNode = leftNode;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
