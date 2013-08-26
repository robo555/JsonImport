import java.sql.Types;

/**
 * Created with IntelliJ IDEA.
 * User: Robo
 *
 * Immutable object for storing the field name and field type of a table field.
 * <p>
 * When this object is shared for concurrent use, no synchronization is necessary.
 */
final class Field {
    private final String fieldName;

    //refers to java.sql.Types
    private final int fieldType;

    private final String fieldTypeName;

    /**
     * Creates a {@code Field} to represent the metadata of a table field.
     * @param fieldName name of the field.
     * @param fieldType data type of the field. One of the values in java.sql.Types.
     * @param fieldTypeName string representation of {@code fieldType}.
     */
    Field(String fieldName, int fieldType, String fieldTypeName) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.fieldTypeName = fieldTypeName;
    }

    /**
     * Return the name of the field.
     * @return the name of the field.
     */
    String getFieldName() {
        return fieldName;
    }

    /**
     * Return the data type of the field. The integer returned is a value in java.sql.Types.
     * @return the data type of the field.
     */
    int getFieldType() {
        return fieldType;
    }

    /**
     * Return the string representation of the field type.
     * @return  the string representation of the field type.
     */
    String getFieldTypeName() {
        return fieldTypeName;
    }

    /**
     * Return true if {@code fieldType} is {@code NCHAR}, {@code NVARCHAR}, {@code CHAR}, {@code LONGNVARCHAR},
     * {@code LONGVARCHAR}, or {@code VARCHAR},
     * @return true if {@code fieldType} is text.
     */
    boolean isTextual() {
        return (fieldType == Types.NCHAR)
                || (fieldType == Types.NVARCHAR)
                || (fieldType == Types.CHAR)
                || (fieldType == Types.LONGNVARCHAR)
                || (fieldType == Types.LONGVARCHAR)
                || (fieldType == Types.VARCHAR);
    }

    /**
     * Return true if {@code fieldType} is {@code BIT}.
     * @return true if {@code fieldType} is boolean.
     */
    boolean isBoolean() {
        return (fieldType == Types.BIT);
    }

    /**
     * Return true if {@code fieldType} is {@code TINYINT}, {@code SMALLINT}, or {@code INTEGER}.
     * @return true if {@code fieldType} is integer.
     */
    boolean isInt() {
        return (fieldType == Types.TINYINT)
                || (fieldType == Types.SMALLINT)
                || (fieldType == Types.INTEGER);
    }

    /**
     * Return true if {@code fieldType} is {@code BIGINT}.
     * @return true if {@code fieldType} is long integer.
     */
    boolean isLong() {
        return (fieldType == Types.BIGINT);
    }

    /**
     * Return true if {@code fieldType} is {@code REAL}, {@code FLOAT}, or {@code DOUBLE}.
     * @return true if {@code fieldType} is a floating point number.
     */
    boolean isFloatingPoint() {
        return (fieldType == Types.REAL)
                || (fieldType == Types.FLOAT)
                || (fieldType == Types.DOUBLE);
    }

    /**
     * Return true if {@code fieldType} is {@code NUMERIC}, or {@code DECIMAL}.
     * @return true if {@code fieldType} is a precision number.
     */
    boolean isDecimal() {
        return (fieldType == Types.NUMERIC)
                || (fieldType == Types.DECIMAL);
    }
}
