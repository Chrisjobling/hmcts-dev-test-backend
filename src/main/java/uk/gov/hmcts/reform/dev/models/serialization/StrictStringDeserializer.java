package uk.gov.hmcts.reform.dev.models.serialization;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class StrictStringDeserializer extends StdDeserializer<String> {

    public StrictStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return context.reportInputMismatch(String.class, "caseNumber must be a string");
        }
        return parser.getString();
    }
}
