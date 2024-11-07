package foundry.veil.impl.glsl.grammar;

import foundry.veil.impl.glsl.node.GlslNode;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public interface GlslTypeQualifier {

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
            return this.storageType == StorageType.SUBROUTINE ? "Storage[type=SUBROUTINE, typeNames=" + Arrays.toString(this.typeNames) + "]" : "Storage[type=" + this.storageType + ']';
        }
    }

    record Layout(LayoutId[] qualifierIds) implements GlslTypeQualifier {
    }

    record LayoutId(String identifier, @Nullable GlslNode constantExpression) {

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
        HIGH_PRECISION,
        MEDIUM_PRECISION,
        LOW_PRECISION
    }

    enum Interpolation implements GlslTypeQualifier {
        SMOOTH,
        FLAT,
        NOPERSPECTIVE
    }

    enum Invariant implements GlslTypeQualifier {
        INVARIANT
    }

    enum Precise implements GlslTypeQualifier {
        PRECISE
    }
}
