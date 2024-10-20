package no.ion.gh.json;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {
    @Test
    void validity() {
        assertTrue(Json.tryFrom("this is plain text").isEmpty());
        assertTrue(Json.tryFrom("{\"starts good but isn't terminated correctly").isEmpty());
        assertTrue(Json.tryFrom("{}").isPresent());
    }

    @Test
    void example() {
        Json json = Json.from("""
                              {
                                "data": {
                                  "organization": null
                                },
                                "errors": [
                                  {
                                    "type": "NOT_FOUND",
                                    "path": ["organization"],
                                    "locations": [
                                      {"line":2, "column":3}
                                    ],
                                    "message":"Could not resolve to an Organization with the login of 'hakonhall'."
                                  }
                                ]
                              }
                              """);
        assertTrue(json.isValid());
        assertTrue(json.isObject());
        assertFalse(json.hasField("foo"));

        assertTrue(json.hasField("data"));
        assertTrue(json.asField("data").isValid());
        assertTrue(json.asField("data").isObject());
        assertTrue(json.to("data.organization").isNull());

        assertTrue(json.hasField("errors"));
        assertTrue(json.to("errors").isArray());
        assertEquals(1, json.to("errors").asArrayLength());
        assertTrue(json.to("errors.0").isObject());
        assertTrue(json.to("errors.0.type").isString());
        assertEquals("NOT_FOUND", json.to("errors.0.type").asString());
        assertTrue(json.to("errors.0.path").isArray());
        assertTrue(json.to("errors.0.path.0").isString());
        assertEquals("organization", json.to("errors.0.path.0").asString());
        assertTrue(json.to("errors.0.locations.0.line").isInt());
        assertEquals(2, json.to("errors.0.locations.0.line").asInt());
        assertEquals(BigInteger.valueOf(2L), json.to("errors.0.locations.0.line").asBigInteger());
    }

    @Test
    void integers() {
        assertTrue(Json.from("-2").isInt());
        assertEquals(-2, Json.from("-2").asInt());
        assertTrue(Json.from("-2").isLong());
        assertEquals(-2L, Json.from("-2").asLong());
        assertTrue(Json.from("-2").isBigInteger());
        assertEquals(BigInteger.valueOf(-2L), Json.from("-2").asBigInteger());

        assertFalse(Json.from("1.0").isInt());
        assertFalse(Json.from("1e0").isInt());
    }

    @Test
    void floats() {
        assertTrue(Json.from("1.1e1").isDouble());
        assertTrue(Json.from("1.1e1").isBigDecimal());
        assertEquals(11, Json.from("1.1e1").asDouble());
        assertEquals(BigDecimal.valueOf(11.0), Json.from("1.1e1").asBigDecimal());
    }
}