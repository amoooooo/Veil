package foundry.veil.api.client.color.theme;

import java.util.Objects;

/**
 * @author amo
 */
@Deprecated
public class BooleanThemeProperty implements IThemeProperty<Boolean> {

    private String name;
    private boolean value;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public Boolean getValue() {
        return this.value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public Class<?> getType() {
        return BooleanThemeProperty.class;
    }
}
