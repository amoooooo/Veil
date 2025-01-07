package foundry.veil.api.glsl.grammar;

import foundry.veil.api.glsl.node.GlslNode;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public sealed interface GlslTypeQualifier {

    String getSourceString();

    static GlslTypeQualifier storage(String... typeNames) {
        return new StorageSubroutine(typeNames);
    }

    static Layout layout(Collection<LayoutId> ids) {
        return new Layout(new ArrayList<>(ids));
    }

    static Layout layout(LayoutId... ids) {
        return layout(Arrays.asList(ids));
    }

    static LayoutId identifierLayoutId(String identifier, @Nullable GlslNode constantExpression) {
        return new LayoutId(identifier, constantExpression);
    }

    static LayoutId sharedLayoutId() {
        return new LayoutId("shared", null);
    }

    /**
     * A storage qualifier for a subroutine operand.
     *
     * @param typeNames The operand names for subroutines
     * @author Ocelot
     */
    record StorageSubroutine(String[] typeNames) implements GlslTypeQualifier {
        @Override
        public String toString() {
            return "Storage[operand=SUBROUTINE, typeNames=" + Arrays.toString(this.typeNames) + "]";
        }

        @Override
        public String getSourceString() {
            if (this.typeNames.length > 0) {
                return "subroutine(" + String.join(",", this.typeNames) + ")";
            }

            return "subroutine";
        }
    }

    record Layout(List<LayoutId> layoutIds) implements GlslTypeQualifier {
        @Override
        public String getSourceString() {
            StringBuilder builder = new StringBuilder();
            for (GlslTypeQualifier.LayoutId layoutId : this.layoutIds) {
                if (layoutId.shared()) {
                    builder.append("shared, ");
                } else {
                    builder.append(layoutId.identifier());
                    GlslNode expression = layoutId.expression();
                    if (expression != null) {
                        builder.append(" = ").append(expression.getSourceString());
                    }
                    builder.append(", ");
                }
            }
            if (!this.layoutIds.isEmpty()) {
                builder.delete(builder.length() - 2, builder.length());
            }
            return "layout(" + builder + ")";
        }
    }

    record LayoutId(String identifier, @Nullable GlslNode expression) {

        public boolean shared() {
            return "shared".equals(this.identifier);
        }
    }

    enum StorageType implements GlslTypeQualifier {
        CONST,
        IN,
        OUT,
        INOUT,
        CENTROID,
        PATCH,
        SAMPLE,
        UNIFORM,
        BUFFER,
        SHARED,
        COHERENT,
        VOLATILE,
        RESTRICT,
        READONLY,
        WRITEONLY;

        @Override
        public String getSourceString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    enum Precision implements GlslTypeQualifier {
        HIGH_PRECISION("highp"),
        MEDIUM_PRECISION("mediump"),
        LOW_PRECISION("lowp");

        private final String sourceName;

        Precision(String sourceName) {
            this.sourceName = sourceName;
        }

        @Override
        public String getSourceString() {
            return this.sourceName;
        }
    }

    enum Interpolation implements GlslTypeQualifier {
        SMOOTH,
        FLAT,
        NOPERSPECTIVE;

        @Override
        public String getSourceString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    enum Invariant implements GlslTypeQualifier {
        INVARIANT;

        @Override
        public String getSourceString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    enum Precise implements GlslTypeQualifier {
        PRECISE;

        @Override
        public String getSourceString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
