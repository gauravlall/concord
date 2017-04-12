package com.walmartlabs.concord.project.yaml;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.walmartlabs.concord.project.yaml.model.YamlDefinition;
import com.walmartlabs.concord.project.yaml.model.YamlDefinitionFile;
import com.walmartlabs.concord.project.yaml.model.YamlFormField;
import com.walmartlabs.concord.project.yaml.model.YamlStep;
import io.takari.parc.Input;
import io.takari.parc.Parser;
import io.takari.parc.Result;
import io.takari.parc.Result.Failure;
import io.takari.parc.Seq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class YamlDeserializers {

    private static final JsonDeserializer<YamlStep> yamlStepDeserializer = new YamlStepDeserializer();
    private static final JsonDeserializer<YamlFormField> yamlFormFieldDeserializer = new YamlFormFieldDeserializer();
    private static final JsonDeserializer<YamlDefinitionFile> yamlDefinitionFileDeserializer = new YamlDefinitionFileDeserializer();

    public static JsonDeserializer<YamlStep> getYamlStepDeserializer() {
        return yamlStepDeserializer;
    }

    public static JsonDeserializer<YamlFormField> getYamlFormFieldDeserializer() {
        return yamlFormFieldDeserializer;
    }

    public static JsonDeserializer<YamlDefinitionFile> getYamlDefinitionFileDeserializer() {
        return yamlDefinitionFileDeserializer;
    }

    private static final class YamlStepDeserializer extends StdDeserializer<YamlStep> {

        protected YamlStepDeserializer() {
            super(YamlStep.class);
        }

        @Override
        public YamlStep deserialize(JsonParser json, DeserializationContext ctx) throws IOException {
            return parse(json, Grammar.getProcessStep());
        }
    }

    private static final class YamlFormFieldDeserializer extends StdDeserializer<YamlFormField> {

        protected YamlFormFieldDeserializer() {
            super(YamlFormField.class);
        }

        @Override
        public YamlFormField deserialize(JsonParser json, DeserializationContext ctx) throws IOException {
            return parse(json, Grammar.getFormField());
        }
    }

    private static final class YamlDefinitionFileDeserializer extends StdDeserializer<YamlDefinitionFile> {

        protected YamlDefinitionFileDeserializer() {
            super(YamlDefinitionFile.class);
        }

        @Override
        public YamlDefinitionFile deserialize(JsonParser json, DeserializationContext ctx) throws IOException, JsonProcessingException {
            Seq<YamlDefinition> defs = parse(json, Grammar.getDefinitions());

            List<YamlDefinition> l = defs.toList();
            Map<String, YamlDefinition> m = new HashMap<>(l.size());
            for (YamlDefinition d : l) {
                m.put(d.getName(), d);
            }

            return new YamlDefinitionFile(m);
        }
    }

    private static <T> T parse(JsonParser json, Parser<Atom, T> parser) throws IOException {
        List<Atom> atoms = toAtoms(json.readValueAsTree().traverse());

        Input<Atom> in = new ListInput<>(atoms);
        Result<Atom, T> result = parser.parse(in);

        if (result.isFailure()) {
            Failure<Atom, ?> f = result.toFailure();
            throw toException(f, json, atoms);
        }

        return result.toSuccess().getResult();
    }

    private static List<Atom> toAtoms(JsonParser p) throws IOException {
        List<Atom> l = new ArrayList<>();
        while (p.nextToken() != null) {
            l.add(Atom.current(p));
        }
        return l;
    }

    private static JsonParseException toException(Failure<Atom, ?> f, JsonParser p, List<Atom> atoms) {
        JsonLocation loc = null;

        int pos = f.getPosition();
        if (pos >= 0) {
            Atom a = atoms.get(f.getPosition());
            loc = a.location;
        }

        return new JsonParseException(p, "Expected: " + f.getMessage(), loc);
    }

    private YamlDeserializers() {
    }
}