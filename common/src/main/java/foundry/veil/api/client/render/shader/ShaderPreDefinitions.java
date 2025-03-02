package foundry.veil.api.client.render.shader;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * <p>Manages pre-defined variables and data in java that can be applied to shaders.</p>
 * <p>Regular definitions are added with {@link #set(String)} and {@link #set(String, String)}.
 * These schedule a shader recompilation every time they are set
 * so shaders can remain up-to-date. </p>
 */
public class ShaderPreDefinitions {

    private final Set<Consumer<String>> definitionCallbacks;
    private final Map<String, String> definitions;
    private final Map<String, String> definitionsView;
    private final Map<String, String> staticDefinitions;
    private final Map<String, String> staticDefinitionsView;

    /**
     * Creates a new set of predefinitions.
     */
    public ShaderPreDefinitions() {
        this.definitionCallbacks = new HashSet<>();
        this.definitions = new HashMap<>();
        this.definitionsView = Collections.unmodifiableMap(this.definitions);
        this.staticDefinitions = new HashMap<>();
        this.staticDefinitionsView = Collections.unmodifiableMap(this.staticDefinitions);
    }

    /**
     * Adds a listener for when a change happens.
     *
     * @param definitionCallback The callback for when definitions change or <code>null</code> to ignore changes
     */
    public void addListener(Consumer<String> definitionCallback) {
        this.definitionCallbacks.add(definitionCallback);
    }

    /**
     * Sets the value of a definition pair. If the value has changed, all shaders depending on it will recompile.
     *
     * @param name The name of the definition to set
     */
    public void set(String name) {
        this.set(name, null);
    }

    /**
     * Sets the value of a definition pair. If the value has changed, all shaders depending on it will recompile.
     *
     * @param name  The name of the definition to set
     * @param value The value to associate with it
     */
    public void set(String name, String value) {
        String previous = this.definitions.put(name, value != null ? value : "");
        if (!Objects.equals(previous, value)) {
            this.definitionCallbacks.forEach(callback -> callback.accept(name));
        }
    }

    /**
     * Sets a definition added to all shaders. These should be treated as static final variables.
     *
     * @param name The name of the definition to set
     */
    public void setStatic(String name) {
        this.setStatic(name, null);
    }

    /**
     * Sets a definition added to all shaders. These should be treated as static final variables.
     *
     * @param name  The name of the definition to set
     * @param value The value to associate with it
     */
    public void setStatic(String name, @Nullable String value) {
        this.staticDefinitions.put(name, value != null ? value : "");
    }

    /**
     * Removes the definition with the specified name.
     *
     * @param name The name of the definition to remove
     */
    public void remove(String name) {
        if (this.definitions.remove(name) != null) {
            this.definitionCallbacks.forEach(callback -> callback.accept(name));
        }
    }

    /**
     * Retrieves a definition by name.
     *
     * @param name The name of the definition
     * @return The definition with that name or <code>null</code> if it doesn't exist
     */
    public @Nullable String getDefinition(String name) {
        return this.definitions.get(name);
    }

    /**
     * @return A view of all definitions
     */
    public Map<String, String> getDefinitions() {
        return this.definitionsView;
    }

    /**
     * @return A view of all static definitions
     */
    public Map<String, String> getStaticDefinitions() {
        return this.staticDefinitionsView;
    }
}
