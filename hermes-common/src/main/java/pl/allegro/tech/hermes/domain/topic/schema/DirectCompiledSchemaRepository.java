package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import static java.lang.String.format;

public class DirectCompiledSchemaRepository<T> implements CompiledSchemaRepository<T> {

    private final SchemaSourceClient schemaSourceClient;
    private final SchemaCompiler<T> schemaCompiler;

    public DirectCompiledSchemaRepository(SchemaSourceClient schemaSourceClient,
                                          SchemaCompiler<T> schemaCompiler) {
        this.schemaSourceClient = schemaSourceClient;
        this.schemaCompiler = schemaCompiler;
    }

    @Override
    public CompiledSchema<T> getSchema(Topic topic, SchemaVersion version) {
        try {
            SchemaSource schemaSource = schemaSourceClient.getSchemaSource(topic, version)
                    .orElseThrow(() -> new SchemaSourceNotFoundException(topic, version));
            return new CompiledSchema<>(schemaCompiler.compile(schemaSource), version);
        } catch (Exception e) {
            throw new CouldNotLoadSchemaException(
                    format("Could not load schema type of %s for topic %s",
                            topic.getContentType(), topic.getQualifiedName()), e);
        }
    }

}
