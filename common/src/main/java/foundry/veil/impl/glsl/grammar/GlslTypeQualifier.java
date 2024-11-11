package foundry.veil.impl.glsl.grammar;

import foundry.veil.impl.glsl.node.GlslNode;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;

public sealed interface GlslTypeQualifier {

    String getSourceString();

    static GlslTypeQualifier storage(Storage.StorageType storageType) {
        if (storageType == StorageType.SUBROUTINE) {
            throw new IllegalArgumentException("Subroutine storage must specify operand names");
        }
        return new Storage(storageType, null);
    }

    static GlslTypeQualifier storage(String[] typeNames) {
        return new Storage(Storage.StorageType.SUBROUTINE, typeNames);
    }

    static Layout layout(LayoutId... ids) {
        return new Layout(ids);
    }

    static LayoutId identifierLayoutId(String identifier, @Nullable GlslNode constantExpression) {
        return new LayoutId(identifier, constantExpression);
    }

    static LayoutId sharedLayoutId() {
        return new LayoutId("shared", null);
    }

    /**
     * A storage qualifier for a operand.
     *
     * @param storageType The operand of storage qualifier
     * @param typeNames   The operand names for subroutines. <code>null</code> if {@link #storageType} is not SUBROUTINE
     * @author Ocelot
     */
    record Storage(StorageType storageType, @Nullable String[] typeNames) implements GlslTypeQualifier {
        @Override
        public String toString() {
            return this.storageType == StorageType.SUBROUTINE ? "Storage[operand=SUBROUTINE, typeNames=" + Arrays.toString(this.typeNames) + "]" : "Storage[operand=" + this.storageType + ']';
        }

        @Override
        public String getSourceString() {
            if (this.typeNames != null && this.typeNames.length > 0) {
                return "subroutine(" + String.join(",", this.typeNames) + ")";
            }

            return this.storageType.name().toLowerCase(Locale.ROOT);
        }
    }

    record Layout(LayoutId[] layoutIds) implements GlslTypeQualifier {
        @Override
        public String getSourceString() {
            StringBuilder builder = new StringBuilder();
            for (GlslTypeQualifier.LayoutId layoutId : this.layoutIds) {
                if (layoutId.shared()) {
                    builder.append("shared ");
                } else {
                    builder.append(layoutId.identifier());
                    GlslNode expression = layoutId.expression();
                    if (expression != null) {
                        builder.append('=').append(expression.getSourceString());
                    }
                    builder.append(" ");
                }
            }
            builder.deleteCharAt(builder.length() - 1);
            return "layout(" + builder + ")";
        }
    }

    record LayoutId(String identifier, @Nullable GlslNode expression) {

        public boolean shared() {
            return "shared".equals(this.identifier);
        }
    }

    enum StorageType {
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
        WRITEONLY,
        SUBROUTINE
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
