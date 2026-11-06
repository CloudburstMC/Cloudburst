package org.cloudburstmc.api.nbt.internal;

import org.cloudburstmc.nbt.NbtMap;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.Base64Variant;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.Version;
import tools.jackson.core.base.ParserMinimalBase;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A Jackson parser that reads a compound tag as a token stream.
 * <p>
 * Compounds become objects. Lists and the byte, int and long array tags become arrays.
 * Numbers keep their Java type, except that bytes and shorts are reported as ints.
 * Numeric accessors convert between widths with plain casts, so any numeric tag can
 * bind to any numeric property.
 */
public final class NbtParser extends ParserMinimalBase {

    private final NbtMap root;
    private @Nullable Cursor cursor;

    public NbtParser(NbtMap root) {
        super(ObjectReadContext.empty());
        this.root = root;
        this.cursor = new RootCursor(root);
    }

    /**
     * Returns the tag value of the current token: the compound, list or array a start
     * token opens, or the scalar of a value token.
     */
    public @Nullable Object currentTag() {
        Cursor cursor = this.cursor;
        if (_closed || cursor == null || _currToken == null) {
            return null;
        }
        if (_currToken.isStructStart()) {
            Cursor parent = cursor.parent;
            return parent == null ? null : parent.value;
        }
        return _currToken.isStructEnd() ? null : cursor.value;
    }

    /*
     * Traversal
     */

    @Override
    public @Nullable JsonToken nextToken() throws JacksonException {
        Cursor cursor = this.cursor;
        if (_closed || cursor == null) {
            return null;
        }
        JsonToken token = cursor.next();
        if (token == null) {
            close();
            return _updateTokenToNull();
        }
        switch (token) {
            case START_OBJECT -> this.cursor = new CompoundCursor(cursor, (NbtMap) cursor.value);
            case START_ARRAY -> this.cursor = new ListCursor(cursor, cursor.value);
            case END_OBJECT, END_ARRAY -> this.cursor = cursor.parent;
            default -> {
            }
        }
        return _updateToken(token);
    }

    @Override
    public JsonParser skipChildren() throws JacksonException {
        Cursor cursor = this.cursor;
        if (cursor != null && _currToken != null && _currToken.isStructStart()) {
            this.cursor = cursor.parent;
            _updateToken(_currToken == JsonToken.START_OBJECT ? JsonToken.END_OBJECT : JsonToken.END_ARRAY);
        }
        return this;
    }

    @Override
    public @Nullable String currentName() {
        Cursor cursor = this.cursor;
        if (cursor != null && _currToken != null && _currToken.isStructStart()) {
            cursor = cursor.parent;
        }
        return cursor == null ? null : cursor.currentName();
    }

    @Override
    public @Nullable TokenStreamContext streamReadContext() {
        return cursor;
    }

    @Override
    public @Nullable Object currentValue() {
        Cursor cursor = this.cursor;
        return cursor == null ? null : cursor.currentValue();
    }

    @Override
    public void assignCurrentValue(Object v) {
        Cursor cursor = this.cursor;
        if (cursor != null) {
            cursor.assignCurrentValue(v);
        }
    }

    @Override
    public TokenStreamLocation currentTokenLocation() {
        return TokenStreamLocation.NA;
    }

    @Override
    public TokenStreamLocation currentLocation() {
        return TokenStreamLocation.NA;
    }

    @Override
    public NbtMap streamReadInputSource() {
        return root;
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public void close() {
        if (!_closed) {
            _closed = true;
            cursor = null;
        }
    }

    @Override
    protected void _closeInput() {
    }

    @Override
    protected void _releaseBuffers() {
    }

    @Override
    protected void _handleEOF() {
        _throwInternal();
    }

    /*
     * Text
     */

    @Override
    public @Nullable String getString() {
        if (_currToken == null) {
            return null;
        }
        return switch (_currToken) {
            case PROPERTY_NAME -> currentName();
            case VALUE_STRING -> (String) currentTag();
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> String.valueOf(currentTag());
            default -> _currToken.asString();
        };
    }

    @Override
    public char[] getStringCharacters() {
        String text = getString();
        return text == null ? new char[0] : text.toCharArray();
    }

    @Override
    public int getStringLength() {
        String text = getString();
        return text == null ? 0 : text.length();
    }

    @Override
    public int getStringOffset() {
        return 0;
    }

    @Override
    public boolean hasStringCharacters() {
        return false;
    }

    /*
     * Numbers
     */

    @Override
    public Number getNumberValue() {
        Number number = number();
        return number instanceof Byte || number instanceof Short ? Integer.valueOf(number.intValue()) : number;
    }

    @Override
    public @Nullable NumberType getNumberType() {
        if (!(currentTag() instanceof Number number)) {
            return null;
        }
        return switch (number) {
            case Long ignored -> NumberType.LONG;
            case Float ignored -> NumberType.FLOAT;
            case Double ignored -> NumberType.DOUBLE;
            default -> NumberType.INT;
        };
    }

    @Override
    public NumberTypeFP getNumberTypeFP() {
        return switch (currentTag()) {
            case Float ignored -> NumberTypeFP.FLOAT32;
            case Double ignored -> NumberTypeFP.DOUBLE64;
            case null, default -> NumberTypeFP.UNKNOWN;
        };
    }

    @Override
    public byte getByteValue() {
        return number().byteValue();
    }

    @Override
    public short getShortValue() {
        return number().shortValue();
    }

    @Override
    public int getIntValue() {
        return number().intValue();
    }

    @Override
    public long getLongValue() {
        return number().longValue();
    }

    @Override
    public float getFloatValue() {
        return number().floatValue();
    }

    @Override
    public double getDoubleValue() {
        return number().doubleValue();
    }

    @Override
    public BigInteger getBigIntegerValue() {
        Number number = number();
        if (number instanceof Float || number instanceof Double) {
            return getDecimalValue().toBigInteger();
        }
        return BigInteger.valueOf(number.longValue());
    }

    @Override
    public BigDecimal getDecimalValue() {
        return switch (number()) {
            case Float f -> new BigDecimal(Float.toString(f));
            case Double d -> BigDecimal.valueOf(d);
            case Number n -> BigDecimal.valueOf(n.longValue());
        };
    }

    @Override
    public boolean isNaN() {
        return switch (currentTag()) {
            case Float f -> f.isNaN();
            case Double d -> d.isNaN();
            case null, default -> false;
        };
    }

    private Number number() {
        if (_currToken != null && _currToken.isNumeric() && currentTag() instanceof Number number) {
            return number;
        }
        throw _constructNotNumericType(_currToken, 0);
    }

    /*
     * Other values
     */

    @Override
    public @Nullable Object getEmbeddedObject() {
        return _currToken == JsonToken.VALUE_EMBEDDED_OBJECT ? currentTag() : null;
    }

    @Override
    public byte[] getBinaryValue(Base64Variant variant) throws JacksonException {
        if (currentTag() instanceof byte[] bytes) {
            return bytes;
        }
        throw _constructReadException("Current token (%s) is not binary data", _currToken);
    }

    /*
     * Cursors
     */

    private static JsonToken tokenOf(@Nullable Object value) {
        return switch (value) {
            case NbtMap ignored -> JsonToken.START_OBJECT;
            case List<?> ignored -> JsonToken.START_ARRAY;
            case byte[] ignored -> JsonToken.START_ARRAY;
            case int[] ignored -> JsonToken.START_ARRAY;
            case long[] ignored -> JsonToken.START_ARRAY;
            case String ignored -> JsonToken.VALUE_STRING;
            case Float ignored -> JsonToken.VALUE_NUMBER_FLOAT;
            case Double ignored -> JsonToken.VALUE_NUMBER_FLOAT;
            case Number ignored -> JsonToken.VALUE_NUMBER_INT;
            case null -> JsonToken.VALUE_NULL;
            default -> JsonToken.VALUE_EMBEDDED_OBJECT;
        };
    }

    /**
     * A read context that also walks the values of its container.
     */
    private abstract static sealed class Cursor extends TokenStreamContext
            permits RootCursor, CompoundCursor, ListCursor {

        final @Nullable Cursor parent;
        /**
         * The tag value of the token last returned by {@link #next}.
         */
        @Nullable Object value;
        private @Nullable Object currentValue;

        Cursor(int type, @Nullable Cursor parent) {
            super(type, -1);
            this.parent = parent;
            this._nestingDepth = parent == null ? 0 : parent._nestingDepth + 1;
        }

        /**
         * Advances to the next token of this container, or returns {@code null} once the
         * root is exhausted.
         */
        abstract @Nullable JsonToken next();

        @Override
        public @Nullable Cursor getParent() {
            return parent;
        }

        @Override
        public @Nullable String currentName() {
            return null;
        }

        @Override
        public @Nullable Object currentValue() {
            return currentValue;
        }

        @Override
        public void assignCurrentValue(Object v) {
            this.currentValue = v;
        }
    }

    private static final class RootCursor extends Cursor {
        private boolean started;

        RootCursor(NbtMap root) {
            super(TYPE_ROOT, null);
            this.value = root;
        }

        @Override
        @Nullable JsonToken next() {
            if (started) {
                return null;
            }
            started = true;
            _index = 0;
            return JsonToken.START_OBJECT;
        }
    }

    private static final class CompoundCursor extends Cursor {
        private final Iterator<Map.Entry<String, Object>> entries;
        private @Nullable String name;
        private boolean atName = true;

        CompoundCursor(Cursor parent, NbtMap compound) {
            super(TYPE_OBJECT, parent);
            this.entries = compound.entrySet().iterator();
        }

        @Override
        JsonToken next() {
            if (!atName) {
                atName = true;
                return tokenOf(value);
            }
            if (!entries.hasNext()) {
                name = null;
                value = null;
                return JsonToken.END_OBJECT;
            }
            Map.Entry<String, Object> entry = entries.next();
            _index++;
            name = entry.getKey();
            value = entry.getValue();
            atName = false;
            return JsonToken.PROPERTY_NAME;
        }

        @Override
        public @Nullable String currentName() {
            return name;
        }
    }

    private static final class ListCursor extends Cursor {
        private final Object elements;
        private final int size;

        ListCursor(Cursor parent, @Nullable Object elements) {
            super(TYPE_ARRAY, parent);
            this.elements = elements;
            this.size = elements instanceof List<?> list ? list.size() : Array.getLength(elements);
        }

        @Override
        JsonToken next() {
            if (_index + 1 >= size) {
                _index = size;
                value = null;
                return JsonToken.END_ARRAY;
            }
            _index++;
            value = switch (elements) {
                case List<?> list -> list.get(_index);
                case byte[] bytes -> bytes[_index];
                case int[] ints -> ints[_index];
                case long[] longs -> longs[_index];
                default -> throw new IllegalStateException("Not a list: " + elements.getClass().getName());
            };
            return tokenOf(value);
        }
    }
}
