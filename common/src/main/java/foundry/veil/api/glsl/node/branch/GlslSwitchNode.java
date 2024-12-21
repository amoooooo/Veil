package foundry.veil.api.glsl.node.branch;

import com.google.common.collect.Streams;
import foundry.veil.api.glsl.node.GlslNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Switch statement.
 *
 * @author Ocelot
 */
public class GlslSwitchNode implements GlslNode {

    private GlslNode condition;
    private final List<GlslNode> branches;

    public GlslSwitchNode(GlslNode condition, Collection<GlslNode> branches) {
        this.condition = condition;
        this.branches = new ArrayList<>(branches);
    }

    /**
     * @return The condition inside the switch <code>switch(condition) {}</code>
     */
    public GlslNode getCondition() {
        return condition;
    }

    /**
     * @return All code inside the switch, including all labels and code under those labels
     */
    public List<GlslNode> getBranches() {
        return this.branches;
    }

    /**
     * Sets the input condition for this switch statement.
     *
     * @param condition The new condition to use
     */
    public GlslSwitchNode setCondition(GlslNode condition) {
        this.condition = condition;
        return this;
    }

    /**
     * Replaces all branches with the specified values.
     *
     * @param branches The new branches
     */
    public GlslSwitchNode setBranches(Collection<GlslNode> branches) {
        this.branches.clear();
        this.branches.addAll(branches);
        return this;
    }

    /**
     * Replaces all branches with the specified values.
     *
     * @param branches The new branches
     */
    public GlslSwitchNode setBranches(GlslNode... branches) {
        this.branches.clear();
        this.branches.addAll(Arrays.asList(branches));
        return this;
    }

    @Override
    public String toString() {
        return "GlslSwitchNode{" +
                "condition=" + this.condition + ", " +
                "branches=" + this.branches + '}';
    }

    @Override
    public String getSourceString() {
        StringBuilder builder = new StringBuilder("switch(");
        builder.append(this.condition.getSourceString()).append(") {");
        for (GlslNode branch : this.branches) {
            builder.append('\t').append(branch.getSourceString());
            builder.append(branch instanceof GlslCaseLabelNode ? ':' : ';');
            builder.append('\n');
        }
        builder.append('}');
        return builder.toString();
    }

    @Override
    public Stream<GlslNode> stream() {
        return Streams.concat(Stream.of(this), this.condition.stream(), this.branches.stream().flatMap(GlslNode::stream));
    }
}
