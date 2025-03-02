package foundry.veil.api.client.color.theme;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author amo
 */
@Deprecated
public class ConsumerThemeProperty implements IThemeProperty<Consumer<?>> {

    private String name;
    private Consumer<?> value;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public Consumer<?> getValue() {
        return this.value;
    }

    @Override
    public Class<?> getType() {
        return ConsumerThemeProperty.class;
    }

    public void setValue(Consumer<?> value) {
        this.value = value;
    }
}
