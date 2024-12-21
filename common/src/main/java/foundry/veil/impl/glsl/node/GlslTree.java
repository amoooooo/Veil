package foundry.veil.impl.glsl.node;

import com.mojang.datafixers.util.Pair;
import foundry.veil.impl.glsl.grammar.GlslSpecifiedType;
import foundry.veil.impl.glsl.grammar.GlslTypeQualifier;
import foundry.veil.impl.glsl.grammar.GlslVersionStatement;
import foundry.veil.impl.glsl.node.branch.GlslSelectionNode;
import foundry.veil.impl.glsl.node.function.GlslFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslDeclaration;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;
import foundry.veil.impl.glsl.node.variable.GlslStructNode;
import foundry.veil.impl.glsl.visitor.GlslFunctionVisitor;
import foundry.veil.impl.glsl.visitor.GlslStringWriter;
import foundry.veil.impl.glsl.visitor.GlslTreeVisitor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public class GlslTree {

    private final GlslVersionStatement versionStatement;
    private final GlslNodeList body;
    private final List<String> directives;
    private final Map<String, GlslNode> markers;

    public GlslTree() {
        this(new GlslVersionStatement(), Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
    }

    public GlslTree(GlslVersionStatement versionStatement, Collection<GlslNode> body, Collection<String> directives, Map<String, GlslNode> markers) {
        this.versionStatement = versionStatement;
        this.body = new GlslNodeList(body);
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
        throw new AssertionError("Not Possible: " + node.getClass());
    }

    public void visit(GlslTreeVisitor visitor) {
        visitor.visitMarkers(this.markers);
        visitor.visitVersion(this.versionStatement);
        for (String directive : this.directives) {
            visitor.visitDirective(directive);
        }

        for (GlslNode node : this.body) {
            if (node instanceof GlslEmptyNode) {
                continue;
            }
            if (node instanceof GlslCompoundNode(List<GlslNode> children)) {
                for (GlslNode child : children) {
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
                if (qualifier instanceof GlslTypeQualifier.Layout(List<GlslTypeQualifier.LayoutId> layoutIds)) {
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

    public Stream<GlslNewNode> searchField(String name) {
        return this.body.stream().flatMap(GlslNode::stream).filter(node -> node instanceof GlslNewNode newNode && name.equals(newNode.getName())).map(node -> (GlslNewNode) node);
    }

    public Optional<Pair<GlslNodeList, Integer>> containingBlock(GlslNode node) {
        Pair<GlslNodeList, Integer> block = this.containingBlock(this.body, node);
        return block != null && block.getFirst() == this.body ? Optional.of(Pair.of(this.mainFunction().orElseThrow().getBody(), 0)) : Optional.ofNullable(block);
    }

    private @Nullable Pair<GlslNodeList, Integer> containingBlock(GlslNodeList body, GlslNode node) {
        for (int i = 0; i < body.size(); i++) {
            GlslNode element = body.get(i);
            if (element == node) {
                return Pair.of(body, i);
            }
            if (element instanceof GlslSelectionNode selectionNode) {
                Pair<GlslNodeList, Integer> firstFound = this.containingBlock(selectionNode.getFirst(), node);
                if (firstFound != null) {
                    return firstFound;
                }

                Pair<GlslNodeList, Integer> secondFound = this.containingBlock(selectionNode.getSecond(), node);
                if (secondFound != null) {
                    return secondFound;
                }
            }
            GlslNodeList elementBody = element.getBody();
            if (elementBody != null) {
                Pair<GlslNodeList, Integer> found = this.containingBlock(elementBody, node);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public GlslVersionStatement getVersionStatement() {
        return this.versionStatement;
    }

    public List<String> getDirectives() {
        return this.directives;
    }

    public Map<String, GlslNode> getMarkers() {
        return this.markers;
    }

    public GlslNodeList getBody() {
        return this.body;
    }

    public String toSourceString() {
        GlslStringWriter writer = new GlslStringWriter();
        this.visit(writer);
        return writer.toString();
    }

    @Override
    public String toString() {
        return "GlslTree{version=" + this.versionStatement + ", body=" + this.body + '}';
    }
}
