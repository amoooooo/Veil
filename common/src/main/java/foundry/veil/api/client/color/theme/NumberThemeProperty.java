package foundry.veil.api.client.color.theme;

import java.util.Objects;

/**
 * @author amo
 */
@Deprecated
public class NumberThemeProperty implements IThemeProperty<Number> {

    private String name;
    private Number value;
    private Class<?> type = NumberThemeProperty.class;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public Number getValue() {
        return this.value;
    }

    @Override
    public Class<?> getType() {
        return this.type;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    public void setType(Class<?> type) {
        if (!Number.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Type must be a subclass of Number");
        }

        this.type = type;
    }

    // return the value and cast it to the specified type
    public <T extends Number> T getValue(Class<T> type) {
        return type.cast(this.value);
    }
}
