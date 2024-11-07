package foundry.veil.impl.glsl.node.function;

import foundry.veil.impl.glsl.grammar.GlslSpecifiedType;
import foundry.veil.impl.glsl.grammar.GlslType;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.grammar.GlslParameterDeclaration;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class GlslFunctionHeaderNode implements GlslNode {

    private String name;
    private GlslSpecifiedType returnType;
    private final List<GlslParameterDeclaration> parameters;

    public GlslFunctionHeaderNode(String name, GlslType returnType, Collection<GlslParameterDeclaration> parameters) {
        this.name = name;
        this.returnType = returnType.asSpecifiedType();
        this.parameters = new ArrayList<>(parameters);
    }

    public GlslFunctionHeaderNode withParameters(GlslParameterDeclaration... parameters) {
        return new GlslFunctionHeaderNode(this.name, this.returnType, new ArrayList<>(Arrays.asList(parameters)));
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

    public GlslFunctionHeaderNode setName(String name) {
        this.name = name;
        return this;
    }

    public GlslFunctionHeaderNode setReturnType(GlslType returnType) {
        this.returnType = returnType.asSpecifiedType();
        return this;
    }

    @Override
    public void visit(GlslVisitor visitor) {

    }

    @Override
    public String toString() {
        return "GlslFunctionHeader{name='" + this.name + "', returnType=" + this.returnType +", parameters=" + this.parameters + '}';
    }
}
