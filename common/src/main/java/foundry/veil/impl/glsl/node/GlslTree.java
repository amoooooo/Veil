package foundry.veil.impl.glsl.node;

import foundry.veil.impl.glsl.GlslInjectionPoint;
import foundry.veil.impl.glsl.grammar.GlslSpecifiedType;
import foundry.veil.impl.glsl.grammar.GlslTypeQualifier;
import foundry.veil.impl.glsl.grammar.GlslVersion;
import foundry.veil.impl.glsl.node.function.GlslFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslDeclaration;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;
import foundry.veil.impl.glsl.node.variable.GlslStructNode;
import foundry.veil.impl.glsl.visitor.GlslFunctionVisitor;
import foundry.veil.impl.glsl.visitor.GlslTreeVisitor;

import java.util.*;
import java.util.stream.Stream;

public class GlslTree {

    private final GlslVersion version;
    private final List<GlslNode> body;
    private final List<String> directives;
    private final Map<String, GlslNode> markers;

    public GlslTree(GlslVersion version, Collection<GlslNode> body, Collection<String> directives, Map<String, GlslNode> markers) {
        this.version = version;
        this.body = new ArrayList<>(body);
        this.directives = new ArrayList<>(directives);
        this.markers = Collections.unmodifiableMap(markers);
    }

    private void visit(GlslTreeVisitor visitor, GlslNode node) {
        if (node instanceof GlslFunctionNode functionNode) {
            GlslFunctionVisitor functionVisitor = visitor.visitFunction(functionNode);
            if (functionVisitor != null) {
                functionNode.visit(functionVisitor);
            }
            return;
        }
        if (node instanceof GlslNewNode newNode) {
            visitor.visitField(newNode);
            return;
        }
        if (node instanceof GlslStructNode struct) {
            visitor.visitStruct(struct);
            return;
        }
        if (node instanceof GlslDeclaration declaration) {
            visitor.visitDeclaration(declaration);
            return;
        }
        throw new AssertionError("Not Possible");
    }

    public void visit(GlslTreeVisitor visitor) {
        visitor.visitMarkers(this.markers);
        visitor.visitVersion(this.version);
        for (String directive : this.directives) {
            visitor.visitDirective(directive);
        }

        for (GlslNode node : this.body) {
            if (node instanceof GlslEmptyNode) {
                continue;
            }
            if (node instanceof GlslCompoundNode compoundNode) {
                for (GlslNode child : compoundNode.getChildren()) {
                    this.visit(visitor, child);
                }
                continue;
            }
            this.visit(visitor, node);
        }

        visitor.visitTreeEnd();
    }

    /**
     * Explicitly marks all outputs with a layout location if not specified.
     */
    public void markOutputs() {
        List<GlslNewNode> outputs = new ArrayList<>();
        this.fields().forEach(node -> {
            GlslSpecifiedType type = node.getType();
            boolean valid = false;
            for (GlslTypeQualifier qualifier : type.getQualifiers()) {
                if (qualifier == GlslTypeQualifier.StorageType.OUT) {
                    valid = true;
                    break;
                }
            }

            if (!valid) {
                return;
            }

            for (GlslTypeQualifier qualifier : type.getQualifiers()) {
                if (qualifier instanceof GlslTypeQualifier.Layout(
                        List<GlslTypeQualifier.LayoutId> layoutIds
                )) {
                    for (GlslTypeQualifier.LayoutId layoutId : layoutIds) {
                        if (!"location".equals(layoutId.identifier())) {
                            continue;
                        }

                        GlslNode expression = layoutId.expression();
                        if (expression == null) {
                            continue;
                        }

                        try {
                            int location = Integer.parseInt(expression.getSourceString());
                            if (location == 0) {
                                outputs.clear();
                                return;
                            }
                            valid = false;
                            break;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            if (valid) {
                outputs.add(node);
            }
        });

        for (GlslNewNode output : outputs) {
            output.getType().addLayoutId("location", GlslNode.intConstant(0));
        }
    }

    public Optional<GlslFunctionNode> mainFunction() {
        return this.functions().filter(node -> node.getHeader().getName().equals("main")).findFirst();
    }

    public Stream<GlslFunctionNode> functions() {
        return this.body.stream().filter(node -> node instanceof GlslFunctionNode).map(node -> (GlslFunctionNode) node);
    }

    public Optional<GlslNewNode> field(String name) {
        return this.body.stream().filter(node -> node instanceof GlslNewNode newNode && name.equals(newNode.getName())).findFirst().map(newNode -> (GlslNewNode) newNode);
    }

    public Stream<GlslNewNode> fields() {
        return this.body.stream().filter(node -> node instanceof GlslNewNode).map(node -> (GlslNewNode) node);
    }

    public void add(GlslNode node) {
        this.body.add(node);
    }

    public void addAll(Collection<GlslNode> nodes) {
        this.body.addAll(nodes);
    }

    public void add(GlslInjectionPoint point, GlslNode node) {
        this.body.add(this.getInjectionIndex(point), node);
    }

    public void addAll(GlslInjectionPoint point, Collection<GlslNode> nodes) {
        this.body.addAll(this.getInjectionIndex(point), nodes);
    }

    public int getInjectionIndex(GlslInjectionPoint point) {
        switch (point) {
            case BEFORE_DECLARATIONS -> {
                for (int i = 0; i < this.body.size(); i++) {
                    if (!(this.body.get(i) instanceof GlslFunctionNode)) {
                        return i;
                    }
                }
            }
            case AFTER_DECLARATIONS -> {
                for (int i = 0; i < this.body.size(); i++) {
                    if (this.body.get(i) instanceof GlslFunctionNode) {
                        return i + 1;
                    }
                }
            }
            case BEFORE_MAIN -> {
                for (int i = 0; i < this.body.size(); i++) {
                    if (this.body.get(i) instanceof GlslFunctionNode functionNode && functionNode.getHeader().getName().equals("main")) {
                        return i;
                    }
                }
            }
            case AFTER_MAIN -> {
                for (int i = 0; i < this.body.size(); i++) {
                    if (this.body.get(i) instanceof GlslFunctionNode functionNode && functionNode.getHeader().getName().equals("main")) {
                        return i + 1;
                    }
                }
            }
            case BEFORE_FUNCTIONS -> {
                for (int i = 0; i < this.body.size(); i++) {
                    if (!(this.body.get(i) instanceof GlslFunctionNode)) {
                        return i;
                    }
                }
            }
            case AFTER_FUNCTIONS -> {
                for (int i = 0; i < this.body.size(); i++) {
                    if (!(this.body.get(i) instanceof GlslFunctionNode)) {
                        return i + 1;
                    }
                }
            }
        }
        return 0;
    }

    public GlslVersion getVersion() {
        return this.version;
    }

    public List<GlslNode> getBody() {
        return this.body;
    }

    public List<String> getDirectives() {
        return this.directives;
    }

    public Map<String, GlslNode> getMarkers() {
        return this.markers;
    }

    @Override
    public String toString() {
        return "GlslTree{version=" + this.version + ", body=" + this.body + '}';
    }
}
