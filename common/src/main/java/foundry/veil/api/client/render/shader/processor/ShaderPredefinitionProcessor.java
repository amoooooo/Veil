package foundry.veil.api.client.render.shader.processor;

import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;

import java.util.Locale;

/**
 * Adds the predefinition data to shaders.
 *
 * @author Ocelot
 */
public class ShaderPredefinitionProcessor implements ShaderPreProcessor {

    @Override
    public String modify(Context context, String source) {
        if (!(context instanceof VeilContext veilContext)) {
            return source;
        }

        ProgramDefinition programDefinition = veilContext.definition();
        if (programDefinition == null) {
            return source;
        }

        ShaderPreDefinitions definitions = context.preDefinitions();
        StringBuilder builder = new StringBuilder();

        definitions.addStaticDefinitions(value -> builder.append(value).append('\n'));
        for (String name : programDefinition.definitions()) {
            String definition = definitions.getDefinition(name);

            if (definition != null) {
                builder.append(definition).append('\n');
            } else {
                String definitionDefault = programDefinition.definitionDefaults().get(name);
                if (definitionDefault != null) {
                    builder.append("#define ")
                            .append(name.toUpperCase(Locale.ROOT))
                            .append(' ')
                            .append(definitionDefault)
                            .append('\n');
                }
            }

            veilContext.addPreDefinitionDependency(name);
        }

        builder.append(source);
        return builder.toString();
    }
}
