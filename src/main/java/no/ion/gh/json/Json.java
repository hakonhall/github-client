package no.ion.gh.json;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.ion.gh.text.Text;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;

import static no.ion.gh.util.Exceptions.uncheckIO;

public class Json {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Json INVALID_JSON = new Json(null);

    private final JsonNode jsonNode;

    /** Return a string that is equal to raw, except all ", \, \n, and \t have been escaped. */
    public static String escapeString(String raw) {
        return Text.replace(raw, c -> switch (c) {
            case '\\', '"' -> "\\" + c;
            case '\n' -> "\\n";
            case '\t' -> "\\t";
            default -> null;
        });
    }

    public static Optional<Json> tryFrom(String json) {
        try {
            JsonNode jsonNode = mapper.readTree(json);
            return Optional.of(new Json(jsonNode));
        } catch (JsonParseException e) {
            return Optional.empty();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Json from(String json) {
        JsonNode jsonNode = uncheckIO(() -> mapper.readTree(json));
        return new Json(jsonNode);
    }

    public static Json ofInvalid() { return INVALID_JSON; }

    private Json(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    public boolean isValid() {
        if (jsonNode == null) return false;
        return switch (jsonNode.getNodeType()) {
            case BINARY, MISSING, POJO -> false;
            case OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL -> true;
        };
    }
    public boolean isInvalid() { return !isValid(); }

    public boolean isObject()           { return jsonNode != null && jsonNode.isObject(); }
    public boolean isArray()            { return jsonNode != null && jsonNode.isArray(); }
    public boolean isString()           { return jsonNode != null && jsonNode.isTextual(); }
    public boolean isBoolean()          { return jsonNode != null && jsonNode.isBoolean(); }
    public boolean isNull()             { return jsonNode != null && jsonNode.isNull(); }
    // Number methods
    // TODO: isInt => isLong etc. Verify this in unit test. isInt => isDouble? isInt => isFloat if exact? 1.0 is or is not an int of value 1? What about 1e1? 1.0e1?
    public boolean isInt()        { return jsonNode != null &&  jsonNode.isInt(); }
    public boolean isLong()       { return jsonNode != null && (jsonNode.isInt() || jsonNode.isLong()); }
    public boolean isBigInteger() { return jsonNode != null && (jsonNode.isInt() || jsonNode.isLong() || jsonNode.isBigInteger()); }
    public boolean isFloat()      { return jsonNode != null &&  jsonNode.isFloat(); }
    public boolean isDouble()     { return jsonNode != null && (jsonNode.isFloat() || jsonNode.isDouble()); }
    public boolean isBigDecimal() { return jsonNode != null && (jsonNode.isFloat() || jsonNode.isDouble() || jsonNode.isBigDecimal()); }

    public boolean hasField(String name) { return asField(name).isValid(); }

    public Map<String, Json> asObject()                                   { return asObject(Map.of()); }
    public Map<String, Json> asObject(Map<String, Json> valueIfNotObject) { return ifObject().orElse(valueIfNotObject); }
    public Optional<Map<String, Json>> ifObject() {
        if (!isObject()) return Optional.empty();
        var map = new TreeMap<String, Json>();
        for (Map.Entry<String, JsonNode> entry : jsonNode.properties()) {
            map.put(entry.getKey(), new Json(entry.getValue()));
        }
        return Optional.of(map);
    }

    /** Return {@code asField(names[0]).asField(names[1])...}. */
    public Json of(String... names) {
        Json json = this;
        for (String name : names) {
            json = json.asField(name);
        }
        return json;
    }

    /**
     * A convenience method for accessing multiple levels of an object and array hierarchy.  The object
     * names must not be a pure number, nor contain ".".  Examples:
     *
     * <ol>
     *     <li>{@code ""} returns {@code this}.</li>
     *     <li>{@code "foo"} returns {@code this.asField("foo")}</li>
     *     <li>{@code "1"} return {@code this.asArrayElement(1)}</li>
     *     <li>{@code "foo.1.54x"} returns {@code this.asField("foo").asArrayElement(1).asField("54x")}</li>
     * </ol>
     */
    public Json to(String key) {
        Json json = this;

        int startIndex = 0, endIndex;
        for (; startIndex < key.length(); startIndex = endIndex + 1) {
            endIndex = key.indexOf('.', startIndex);
            if (endIndex == -1)
                endIndex = key.length();

            String element = key.substring(startIndex, endIndex);

            try {
                int arrayIndex = Integer.parseInt(element);
                json = json.asArrayElement(arrayIndex);
            } catch (NumberFormatException ignored) {
                json = json.asField(element);
            }
        }

        return json;
    }

    public Json asField(String name)                             { return asField(name, ofInvalid()); }
    public Json asField(String name, Json valueIfNotAFieldValue) { return ifField(name).orElse(valueIfNotAFieldValue); }
    public Optional<Json> ifField(String name)                   { return Optional.ofNullable(jsonNode).map(n -> n.get(name)).map(Json::new); }

    public List<Json> asArray()                           { return asArray(List.of()); }
    public List<Json> asArray(List<Json> valueIfNotArray) { return ifArray().orElse(valueIfNotArray); }
    public Optional<List<Json>> ifArray() {
        if (!isArray()) return Optional.empty();
        int size = jsonNode.size();
        if (size == 0) return Optional.of(List.of());
        var array = new ArrayList<Json>(size);
        Iterator<JsonNode> elements = jsonNode.elements();
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            array.add(new Json(element));
        }
        return Optional.of(array);
    }

    public int asArrayLength()                    { return asArrayLength(0); }
    public int asArrayLength(int valueIfNotArray) { return ifArrayLength().orElse(valueIfNotArray); }
    public Optional<Integer> ifArrayLength()      { return isArray() ? Optional.of(jsonNode.size()) : Optional.empty(); }

    public Json asArrayElement(int index)                       { return asArrayElement(index, ofInvalid()); }
    public Json asArrayElement(int index, Json valueIfNotArray) { return ifArrayElement(index).orElse(valueIfNotArray); }
    public Optional<Json> ifArrayElement(int index)             {
        return isArray() && 0 <= index && index < jsonNode.size() ?
               Optional.of(new Json(jsonNode.get(index))) :
               Optional.empty();
    }

    public void forEachArrayElement(Consumer<Json> consumer) {
        if (isArray())
            jsonNode.forEach(elementJsonNode -> consumer.accept(new Json(elementJsonNode)));
    }

    public String asString()                        { return asString(""); }
    public String asString(String valueIfNotString) { return ifString().orElse(valueIfNotString); }
    public Optional<String> ifString()              { return isString() ? Optional.of(jsonNode.textValue()) : Optional.empty(); }

    public boolean asBoolean()                          { return asBoolean(false); }
    public boolean asBoolean(boolean valueIfNotBoolean) { return ifBoolean().orElse(valueIfNotBoolean); }
    public Optional<Boolean> ifBoolean()                { return isBoolean() ? Optional.of(jsonNode.booleanValue()) : Optional.empty(); }

    public int asInt()                  { return asInt(0); }
    public int asInt(int valueIfNotInt) { return ifInt().orElse(valueIfNotInt); }
    public Optional<Integer> ifInt()     { return isInt() ? Optional.of(jsonNode.intValue()) : Optional.empty(); }

    public long asLong()                            { return asLong(0L); }
    public long asLong(long valueIfNotLong)         { return ifLong().orElse(valueIfNotLong); }
    public Optional<Long> ifLong()                  { return isLong() ? Optional.of(jsonNode.longValue()) : Optional.empty(); }

    public BigInteger asBigInteger()                                { return asBigInteger(BigInteger.valueOf(0L)); }
    public BigInteger asBigInteger(BigInteger valueIfNotBigInteger) { return ifBigInteger().orElse(valueIfNotBigInteger); }
    public Optional<BigInteger> ifBigInteger()                      { return isBigInteger() ? Optional.of(jsonNode.bigIntegerValue()) : Optional.empty(); }

    public float asFloat()                      { return asFloat(0); }
    public float asFloat(float valueIfNotFloat) { return ifFloat().orElse(valueIfNotFloat); }
    public Optional<Float> ifFloat()            { return isFloat() ? Optional.of(jsonNode.floatValue()) : Optional.empty(); }

    public double asDouble()                        { return asDouble(0L); }
    public double asDouble(double valueIfNotDouble) { return ifDouble().orElse(valueIfNotDouble); }
    public Optional<Double> ifDouble()              { return isDouble() ? Optional.of(jsonNode.doubleValue()) : Optional.empty(); }

    public BigDecimal asBigDecimal()                                { return asBigDecimal(BigDecimal.valueOf(0L)); }
    public BigDecimal asBigDecimal(BigDecimal valueIfNotBigDecimal) { return ifBigDecimal().orElse(valueIfNotBigDecimal); }
    public Optional<BigDecimal> ifBigDecimal()                      { return isBigDecimal() ? Optional.of(jsonNode.decimalValue()) : Optional.empty(); }

    @Override public String toString() { return jsonNode.toString(); }

    // Implement these when/if necessary
    @Override public boolean equals(Object o) { throw new UnsupportedOperationException(); }
    @Override public int hashCode() { throw new UnsupportedOperationException(); }
}
