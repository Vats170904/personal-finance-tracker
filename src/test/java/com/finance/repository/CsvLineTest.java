package com.finance.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.finance.exception.ValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvLineTest {

    @Test
    void plainFieldsAreNotQuoted() {
        assertEquals("a,b,c", CsvLine.format(List.of("a", "b", "c")));
    }

    @Test
    void fieldsWithCommasQuotesOrOuterSpacesAreQuoted() {
        assertEquals("\"a,b\",\"say \"\"hi\"\"\",\" padded \"",
                CsvLine.format(List.of("a,b", "say \"hi\"", " padded ")));
    }

    @Test
    void parseIsTheInverseOfFormat() {
        List<String> fields = List.of("1", "", "a,b", "say \"hi\"", " padded ", "Café ☕");
        assertEquals(fields, CsvLine.parse(CsvLine.format(fields)));
    }

    @Test
    void emptyFieldsArePreserved() {
        assertEquals(List.of("", "", ""), CsvLine.parse(",,"));
    }

    @Test
    void unterminatedQuoteIsRejected() {
        assertThrows(ValidationException.class, () -> CsvLine.parse("1,\"open"));
    }
}
