package org.cloudburstmc.api.nbt.internal;

import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.Base64Variant;
import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.SerializableString;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteCapability;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.core.Version;
import tools.jackson.core.base.GeneratorBase;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.JacksonFeatureSet;
import tools.jackson.core.util.SimpleStreamWriteContext;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A Jackson generator that builds NBT values instead of text.
 * <p>
 * Every value is converted to the tag type expected for it: the type set by
 * {@link #expect} for the next property, else the element type of the enclosing list or
 * compound, else the natural type of the written token.
 */
public final class NbtGenerator extends GeneratorBase {

    private final boolean clientView;
    private SimpleStreamWriteContext writeContext = SimpleStreamWriteContext.createRootContext(null);
    private Container container = new Root();
    private @Nullable NbtTagType expectedValue;
    private NbtTagType expectedElement = NbtTagType.AUTO;

    /**
     * Creates a generator that writes every property.
     */
    public NbtGenerator() {
        this(false);
    }

    /**
     * Creates a generator.
     *
     * @param clientView whether to write only the client properties of the root object
     */
    public NbtGenerator(boolean clientView) {
        super(ObjectWriteContext.empty(), new IOContext(StreamReadConstraints.defaults(),
                StreamWriteConstraints.defaults(), ErrorReportConfiguration.defaults(), null,
                ContentReference.unknown(), false, null).markBufferRecyclerReleased(),
                StreamWriteFeature.collectDefaults());
        this.clientView = clientView;
    }

    /**
     * Returns whether a property about to be written is left out: in the client view,
     * a property of the root object that is not a client property. Nested values are
     * written whole.
     *
     * @param clientProperty whether the property is a client property
     */
    public boolean omits(boolean clientProperty) {
        return clientView && !clientProperty && container instanceof Compound compound
                && compound.parent instanceof Root;
    }

    /**
     * Sets the tag types of the next value written.
     *
     * @param value   the type of the value
     * @param element the type of its list elements or compound values
     */
    public void expect(NbtTagType value, NbtTagType element) {
        this.expectedValue = value;
        this.expectedElement = element;
    }

    /**
     * Drops an expectation that no value consumed, for example because the property was null.
     */
    public void clearExpectation() {
        this.expectedValue = null;
        this.expectedElement = NbtTagType.AUTO;
    }

    /**
     * Returns the root value written, or {@code null} if none was.
     */
    public @Nullable Object result() {
        return ((Root) container).value;
    }

    /*
     * Structure
     */

    @Override
    public JsonGenerator writeStartObject() throws JacksonException {
        return writeStartObject(null);
    }

    @Override
    public JsonGenerator writeStartObject(Object forValue) throws JacksonException {
        _verifyValueWrite("start an object");
        NbtTagType element = expectedElement;
        NbtTagType type = takeExpected();
        if (type != NbtTagType.AUTO && type != NbtTagType.COMPOUND) {
            throw mismatch("an object", type);
        }
        writeContext = writeContext.createChildObjectContext(forValue);
        container = new Compound(container, element);
        return this;
    }

    @Override
    public JsonGenerator writeEndObject() throws JacksonException {
        if (!(container instanceof Compound compound) || !writeContext.inObject()) {
            throw _constructWriteException("Current context not an object but %s", writeContext.typeDesc());
        }
        writeContext = writeContext.clearAndGetParent();
        container = compound.parent;
        container.add(compound.builder.build());
        return this;
    }

    @Override
    public JsonGenerator writeStartArray() throws JacksonException {
        return writeStartArray(null);
    }

    @Override
    public JsonGenerator writeStartArray(Object forValue) throws JacksonException {
        _verifyValueWrite("start an array");
        NbtTagType element = expectedElement;
        NbtTagType type = takeExpected();
        container = switch (type) {
            case AUTO, LIST -> new ListContainer(container, element);
            case BYTE_ARRAY, INT_ARRAY, LONG_ARRAY -> new ArrayContainer(container, type);
            default -> throw mismatch("an array", type);
        };
        writeContext = writeContext.createChildArrayContext(forValue);
        return this;
    }

    @Override
    public JsonGenerator writeEndArray() throws JacksonException {
        if (!writeContext.inArray()) {
            throw _constructWriteException("Current context not an array but %s", writeContext.typeDesc());
        }
        writeContext = writeContext.clearAndGetParent();
        Container finished = container;
        container = finished.parent;
        container.add(finished.build());
        return this;
    }

    @Override
    public JsonGenerator writeName(String name) throws JacksonException {
        if (!writeContext.writeName(name)) {
            throw _constructWriteException("Cannot write a property name, expecting a value");
        }
        ((Compound) container).name = name;
        return this;
    }

    @Override
    public JsonGenerator writePropertyId(long id) throws JacksonException {
        return writeName(Long.toString(id));
    }

    /*
     * Scalars
     */

    @Override
    public JsonGenerator writeString(String value) throws JacksonException {
        if (value == null) {
            return writeNull();
        }
        _verifyValueWrite(WRITE_STRING);
        NbtTagType type = takeExpected();
        if (type != NbtTagType.AUTO && type != NbtTagType.STRING) {
            throw mismatch("a string", type);
        }
        container.add(value);
        return this;
    }

    @Override
    public JsonGenerator writeString(char[] buffer, int offset, int len) throws JacksonException {
        return writeString(new String(buffer, offset, len));
    }

    @Override
    public JsonGenerator writeRawUTF8String(byte[] buffer, int offset, int len) throws JacksonException {
        return writeString(new String(buffer, offset, len, StandardCharsets.UTF_8));
    }

    @Override
    public JsonGenerator writeUTF8String(byte[] buffer, int offset, int len) throws JacksonException {
        return writeString(new String(buffer, offset, len, StandardCharsets.UTF_8));
    }

    @Override
    public JsonGenerator writeBinary(Base64Variant variant, byte[] data, int offset, int len) throws JacksonException {
        _verifyValueWrite(WRITE_BINARY);
        NbtTagType type = takeExpected();
        if (type != NbtTagType.AUTO && type != NbtTagType.BYTE_ARRAY) {
            throw mismatch("binary data", type);
        }
        container.add(Arrays.copyOfRange(data, offset, offset + len));
        return this;
    }

    @Override
    public JsonGenerator writeArray(int[] array, int offset, int length) throws JacksonException {
        if (!expects(NbtTagType.INT_ARRAY)) {
            return super.writeArray(array, offset, length);
        }
        _verifyValueWrite("write an int array");
        takeExpected();
        container.add(Arrays.copyOfRange(array, offset, offset + length));
        return this;
    }

    @Override
    public JsonGenerator writeArray(long[] array, int offset, int length) throws JacksonException {
        if (!expects(NbtTagType.LONG_ARRAY)) {
            return super.writeArray(array, offset, length);
        }
        _verifyValueWrite("write a long array");
        takeExpected();
        container.add(Arrays.copyOfRange(array, offset, offset + length));
        return this;
    }

    @Override
    public JsonGenerator writeNumber(short v) throws JacksonException {
        return writeNumber(v, NbtTagType.SHORT);
    }

    @Override
    public JsonGenerator writeNumber(int v) throws JacksonException {
        return writeNumber(v, NbtTagType.INT);
    }

    @Override
    public JsonGenerator writeNumber(long v) throws JacksonException {
        return writeNumber(v, NbtTagType.LONG);
    }

    @Override
    public JsonGenerator writeNumber(BigInteger v) throws JacksonException {
        return v == null ? writeNull() : writeNumber(v, NbtTagType.AUTO);
    }

    @Override
    public JsonGenerator writeNumber(double v) throws JacksonException {
        return writeNumber(v, NbtTagType.DOUBLE);
    }

    @Override
    public JsonGenerator writeNumber(float v) throws JacksonException {
        return writeNumber(v, NbtTagType.FLOAT);
    }

    @Override
    public JsonGenerator writeNumber(BigDecimal v) throws JacksonException {
        return v == null ? writeNull() : writeNumber(v, NbtTagType.AUTO);
    }

    @Override
    public JsonGenerator writeNumber(String encodedValue) throws JacksonException {
        return _reportUnsupportedOperation("NBT cannot hold encoded numbers");
    }

    @Override
    public JsonGenerator writeBoolean(boolean state) throws JacksonException {
        return writeNumber((byte) (state ? 1 : 0), NbtTagType.BYTE);
    }

    @Override
    public JsonGenerator writeNull() throws JacksonException {
        _verifyValueWrite(WRITE_NULL);
        takeExpected();
        if (!(container instanceof Compound compound)) {
            throw _constructWriteException("NBT cannot hold null outside a compound");
        }
        compound.name = null;
        return this;
    }

    /**
     * Writes an {@link NbtMap} or {@link NbtList} unchanged.
     */
    @Override
    public JsonGenerator writeEmbeddedObject(Object object) throws JacksonException {
        if (object == null) {
            return writeNull();
        }
        if (!(object instanceof NbtMap) && !(object instanceof NbtList<?>)) {
            return super.writeEmbeddedObject(object);
        }
        _verifyValueWrite("write an NBT value");
        takeExpected();
        container.add(object);
        return this;
    }

    private JsonGenerator writeNumber(Number value, NbtTagType natural) {
        _verifyValueWrite(WRITE_NUMBER);
        NbtTagType expected = takeExpected();
        NbtTagType type = expected == NbtTagType.AUTO ? natural : expected;
        container.add(switch (type) {
            case BYTE -> value.byteValue();
            case SHORT -> value.shortValue();
            case INT -> value.intValue();
            case LONG -> value.longValue();
            case FLOAT -> value.floatValue();
            case DOUBLE -> value.doubleValue();
            default -> throw mismatch("the number " + value, type);
        });
        return this;
    }

    /*
     * Expected types
     */

    private boolean expects(NbtTagType type) {
        NbtTagType expected = expectedValue != null ? expectedValue : container.element;
        return expected == NbtTagType.AUTO || expected == type;
    }

    private NbtTagType takeExpected() {
        NbtTagType type = expectedValue != null ? expectedValue : container.element;
        clearExpectation();
        return type;
    }

    private JacksonException mismatch(String what, NbtTagType expected) {
        String name = writeContext.currentName();
        return _constructWriteException(String.format("Cannot write %s %s%s", what,
                expected == NbtTagType.AUTO ? "without a declared NBT type" : "as NBT " + expected,
                name == null ? "" : " (property '" + name + "')"));
    }

    private static NbtType<?> nbtType(NbtTagType type) {
        return switch (type) {
            case AUTO -> NbtType.END;
            case BYTE -> NbtType.BYTE;
            case SHORT -> NbtType.SHORT;
            case INT -> NbtType.INT;
            case LONG -> NbtType.LONG;
            case FLOAT -> NbtType.FLOAT;
            case DOUBLE -> NbtType.DOUBLE;
            case STRING -> NbtType.STRING;
            case BYTE_ARRAY -> NbtType.BYTE_ARRAY;
            case INT_ARRAY -> NbtType.INT_ARRAY;
            case LONG_ARRAY -> NbtType.LONG_ARRAY;
            case LIST -> NbtType.LIST;
            case COMPOUND -> NbtType.COMPOUND;
        };
    }

    /*
     * Value containers
     */

    private abstract static sealed class Container permits Root, Compound, ListContainer, ArrayContainer {
        final Container parent;
        /**
         * The type of values written into this container without an explicit expectation.
         */
        final NbtTagType element;

        Container(Container parent, NbtTagType element) {
            this.parent = parent;
            this.element = element;
        }

        abstract void add(Object value);

        Object build() {
            throw new IllegalStateException();
        }
    }

    private static final class Root extends Container {
        Object value;

        Root() {
            super(null, NbtTagType.AUTO);
        }

        @Override
        void add(Object value) {
            this.value = value;
        }
    }

    private static final class Compound extends Container {
        final NbtMapBuilder builder = NbtMap.builder();
        String name;

        Compound(Container parent, NbtTagType element) {
            super(parent, element);
        }

        @Override
        void add(Object value) {
            builder.put(name, value);
            name = null;
        }
    }

    private final class ListContainer extends Container {
        final List<Object> values = new ArrayList<>();
        NbtType<?> type;

        ListContainer(Container parent, NbtTagType element) {
            super(parent, element);
        }

        @Override
        void add(Object value) {
            NbtType<?> valueType = value instanceof NbtList<?> ? NbtType.LIST
                    : value instanceof NbtMap ? NbtType.COMPOUND
                    : NbtType.byClass(value.getClass());
            if (type == null) {
                type = valueType;
            } else if (type != valueType) {
                throw _constructWriteException("NBT list of %s cannot hold a %s", type.getTypeName(),
                        valueType.getTypeName());
            }
            values.add(value);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object build() {
            return new NbtList(type != null ? type : nbtType(element), values);
        }
    }

    private static final class ArrayContainer extends Container {
        final NbtTagType kind;
        final List<Number> values = new ArrayList<>();

        ArrayContainer(Container parent, NbtTagType kind) {
            super(parent, switch (kind) {
                case BYTE_ARRAY -> NbtTagType.BYTE;
                case INT_ARRAY -> NbtTagType.INT;
                default -> NbtTagType.LONG;
            });
            this.kind = kind;
        }

        @Override
        void add(Object value) {
            values.add((Number) value);
        }

        @Override
        Object build() {
            int size = values.size();
            return switch (kind) {
                case BYTE_ARRAY -> {
                    byte[] array = new byte[size];
                    for (int i = 0; i < size; i++) {
                        array[i] = values.get(i).byteValue();
                    }
                    yield array;
                }
                case INT_ARRAY -> values.stream().mapToInt(Number::intValue).toArray();
                default -> values.stream().mapToLong(Number::longValue).toArray();
            };
        }
    }

    /*
     * Generator plumbing
     */

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public TokenStreamContext streamWriteContext() {
        return writeContext;
    }

    @Override
    public Object currentValue() {
        return writeContext.currentValue();
    }

    @Override
    public void assignCurrentValue(Object v) {
        writeContext.assignCurrentValue(v);
    }

    @Override
    public Object streamWriteOutputTarget() {
        return null;
    }

    @Override
    public int streamWriteOutputBuffered() {
        return -1;
    }

    @Override
    public JacksonFeatureSet<StreamWriteCapability> streamWriteCapabilities() {
        return DEFAULT_BINARY_WRITE_CAPABILITIES;
    }

    @Override
    public JsonGenerator writeRaw(String text) throws JacksonException {
        return _reportUnsupportedOperation("NBT cannot hold raw content");
    }

    @Override
    public JsonGenerator writeRaw(String text, int offset, int len) throws JacksonException {
        return _reportUnsupportedOperation("NBT cannot hold raw content");
    }

    @Override
    public JsonGenerator writeRaw(char[] text, int offset, int len) throws JacksonException {
        return _reportUnsupportedOperation("NBT cannot hold raw content");
    }

    @Override
    public JsonGenerator writeRaw(char c) throws JacksonException {
        return _reportUnsupportedOperation("NBT cannot hold raw content");
    }

    @Override
    public JsonGenerator writeRaw(SerializableString text) throws JacksonException {
        return _reportUnsupportedOperation("NBT cannot hold raw content");
    }

    @Override
    public void flush() {
    }

    @Override
    protected void _closeInput() {
    }

    @Override
    protected void _releaseBuffers() {
    }

    @Override
    protected void _verifyValueWrite(String typeMsg) throws JacksonException {
        if (!writeContext.writeValue()) {
            throw _constructWriteException("Cannot %s, expecting a property name", typeMsg);
        }
    }
}
