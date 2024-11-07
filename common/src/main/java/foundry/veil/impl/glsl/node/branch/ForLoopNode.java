package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;
import org.jetbrains.annotations.Nullable;

/**
 * Represents for loops.
 *
 * @author Ocelot
 */
public class ForLoopNode implements GlslNode {

    private GlslNode init;
    private GlslNode condition;
    private GlslNode increment;
    private GlslNode body;

    public ForLoopNode(GlslNode init, GlslNode condition, @Nullable GlslNode increment, GlslNode body) {
        this.init = init;
        this.condition = condition;
        this.increment = increment;
        this.body = body;
    }

    @Override
    public void visit(GlslVisitor visitor) {
    }

    public GlslNode getInit() {
        return this.init;
    }

    public GlslNode getCondition() {
        return this.condition;
    }

    public @Nullable GlslNode getIncrement() {
        return this.increment;
    }

    public GlslNode getBody() {
        return this.body;
    }

    public void setInit(GlslNode init) {
        this.init = init;
    }

    public void setCondition(GlslNode condition) {
        this.condition = condition;
    }

    public void setIncrement(GlslNode increment) {
        this.increment = increment;
    }

    public void setBody(GlslNode body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "ForLoopNode{init=" + this.init + ", condition=" + this.condition + ", increment=" + this.increment + ", body=" + this.body + '}';
    }
}
