package foundry.veil.api.glsl.grammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class GlslFunctionHeader {

    private String name;
    private GlslSpecifiedType returnType;
    private final List<GlslParameterDeclaration> parameters;

    public GlslFunctionHeader(String name, GlslType returnType, Collection<GlslParameterDeclaration> parameters) {
        this.name = name;
        this.returnType = returnType.asSpecifiedType();
        this.parameters = new ArrayList<>(parameters);
    }

    public GlslFunctionHeader withParameters(GlslParameterDeclaration... parameters) {
        return new GlslFunctionHeader(this.name, this.returnType, new ArrayList<>(Arrays.asList(parameters)));
    }

    public String getName() {
        return this.name;
    }

    public GlslSpecifiedType getReturnType() {
        return this.returnType;
    }

    public List<GlslParameterDeclaration> getParameters() {
        return this.parameters;
    }

    public GlslFunctionHeader setName(String name) {
        this.name = name;
        return this;
    }

    public GlslFunctionHeader setReturnType(GlslType returnType) {
        this.returnType = returnType.asSpecifiedType();
        return this;
    }

    @Override
    public String toString() {
        return "GlslFunctionHeader{name='" + this.name + "', returnType=" + this.returnType + ", parameters=" + this.parameters + '}';
    }

    public String getSourceString() {
        return this.returnType.getSourceString() + this.returnType.getPostSourceString() + ' ' + this.name + '(' +
                this.parameters.stream().map(parameter -> {
                    String name = parameter.getName();
                    GlslSpecifiedType type = parameter.getType();
                    if (name != null) {
                        return type.getSourceString() + " " + name + type.getPostSourceString();
                    }
                    return type.getSourceString();
                }).collect(Collectors.joining(", ")) + ')';
    }
}
