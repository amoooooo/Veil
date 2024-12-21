package foundry.veil.impl.glsl.node.branch;

import com.google.common.collect.Streams;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.node.GlslNodeList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Represents for loops.
 *
 * @author Ocelot
 */
public class ForLoopNode implements GlslNode {

    private GlslNode init;
    private GlslNode condition;
    private GlslNode increment;
    private final GlslNodeList body;

    public ForLoopNode(GlslNode init, GlslNode condition, @Nullable GlslNode increment, Collection<GlslNode> body) {
        this.init = init;
        this.condition = condition;
        this.increment = increment;
        this.body = new GlslNodeList(body);
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

    @Override
    public GlslNodeList getBody() {
        return this.body;
    }

    public ForLoopNode setInit(GlslNode init) {
        this.init = init;
        return this;
    }

    public ForLoopNode setCondition(GlslNode condition) {
        this.condition = condition;
        return this;
    }

    public ForLoopNode setIncrement(@Nullable GlslNode increment) {
        this.increment = increment;
        return this;
    }

    @Override
    public String toString() {
        return "ForLoopNode{init=" + this.init + ", condition=" + this.condition + ", increment=" + this.increment + ", body=" + this.body + '}';
    }

    @Override
    public String getSourceString() {
        StringBuilder builder = new StringBuilder();
        builder.append("for (").append(this.init.getSourceString()).append("; ").append(this.condition.getSourceString()).append(';');
        if (this.increment != null) {
            builder.append(' ').append(this.increment.getSourceString());
        }
        builder.append(") {\n");
        for (GlslNode node : this.body) {
            builder.append('\t').append(NEWLINE.matcher(node.getSourceString()).replaceAll("\n\t")).append(";\n");
        }
        builder.append('}');
        return builder.toString();
    }

    @Override
    public Stream<GlslNode> stream() {
        return Streams.concat(this.init.stream(), this.condition.stream(), this.increment.stream());
    }
}
