package foundry.veil.impl.glsl.node.function;

import foundry.veil.impl.glsl.grammar.GlslFunctionHeader;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.node.branch.GlslReturnNode;
import foundry.veil.impl.glsl.node.expression.GlslAssignmentNode;
import foundry.veil.impl.glsl.visitor.GlslFunctionVisitor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Defines a function in a GLSL file with an optional body.
 *
 * @author Ocelot
 */
public class GlslFunctionNode implements GlslNode {

    private GlslFunctionHeader header;
    private List<GlslNode> body;

    public GlslFunctionNode(GlslFunctionHeader header, @Nullable GlslNode body) {
        this.header = header;
        this.body = body != null ? body.toList() : null;
    }

    public void visit(GlslFunctionVisitor visitor) {
        for (GlslNode node : this.body) {
            if (node instanceof GlslReturnNode returnNode) {
                visitor.visitReturn(returnNode);
                return;
            }
            if(node instanceof GlslAssignmentNode assignmentNode) {
                visitor.visitAssignment(assignmentNode);
                return;
            }
            System.out.println(node);
        }
        visitor.visitFunctionEnd();
    }

    /**
     * @return The full signature of this function
     */
    public GlslFunctionHeader getHeader() {
        return this.header;
    }

    /**
     * @return The body of the function or <code>null</code> if this is just a function prototype
     */
    public @Nullable List<GlslNode> getBody() {
        return this.body;
    }

    /**
     * Sets the function header of this function to the specified value.
     *
     * @param header The new header
     */
    public void setHeader(GlslFunctionHeader header) {
        this.header = header;
    }

    /**
     * Sets the body of this function or <code>null</code> to make this a function prototype.
     *
     * @param body The new function body
     */
    public void setBody(@Nullable Collection<GlslNode> body) {
        if (body != null) {
            this.body = new ArrayList<>(body);
        } else {
            this.body = null;
        }
    }

    /**
     * Sets the body of this function or <code>null</code> to make this a function prototype.
     *
     * @param body The new function body
     */
    public void setBody(GlslNode... body) {
        if (body != null) {
            this.body = new ArrayList<>(Arrays.asList(body));
        } else {
            this.body = null;
        }
    }

    @Override
    public String getSourceString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.header.getSourceString());

        if (this.body == null) {
            return builder + ";";
        }

        builder.append(" {\n");
        for (GlslNode node : this.body) {
            builder.append('\t').append(node.getSourceString().replaceAll("\n", "\n\t")).append(";\n");
        }
        builder.append("}\n");

        return builder.toString();
    }

    @Override
    public String toString() {
        return "GlslFunction{header=" + this.header + ", body=" + this.body + '}';
    }
}
