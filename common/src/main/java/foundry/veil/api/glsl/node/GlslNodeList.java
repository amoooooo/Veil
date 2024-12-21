package foundry.veil.api.glsl.node;

import foundry.veil.api.glsl.GlslInjectionPoint;
import foundry.veil.api.glsl.node.function.GlslFunctionNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.UnaryOperator;

public class GlslNodeList implements List<GlslNode> {

    private final ArrayList<GlslNode> nodes;

    public GlslNodeList() {
        this.nodes = new ArrayList<>();
    }

    public GlslNodeList(Collection<GlslNode> nodes) {
        this.nodes = new ArrayList<>(nodes);
    }

    private int getInjectionIndex(GlslInjectionPoint point) {
        switch (point) {
            case BEFORE_DECLARATIONS -> {
                for (int i = 0; i < this.nodes.size(); i++) {
                    if (!(this.nodes.get(i) instanceof GlslFunctionNode)) {
                        return i;
                    }
                }
            }
            case AFTER_DECLARATIONS -> {
                for (int i = 0; i < this.nodes.size(); i++) {
                    if (this.nodes.get(i) instanceof GlslFunctionNode) {
                        return i + 1;
                    }
                }
            }
            case BEFORE_MAIN -> {
                for (int i = 0; i < this.nodes.size(); i++) {
                    if (this.nodes.get(i) instanceof GlslFunctionNode functionNode && "main".equals(functionNode.getHeader().getName())) {
                        return i;
                    }
                }
            }
            case AFTER_MAIN -> {
                for (int i = 0; i < this.nodes.size(); i++) {
                    if (this.nodes.get(i) instanceof GlslFunctionNode functionNode && "main".equals(functionNode.getHeader().getName())) {
                        return i + 1;
                    }
                }
            }
            case BEFORE_FUNCTIONS -> {
                for (int i = 0; i < this.nodes.size(); i++) {
                    if (!(this.nodes.get(i) instanceof GlslFunctionNode)) {
                        return i;
                    }
                }
            }
            case AFTER_FUNCTIONS -> {
                for (int i = 0; i < this.nodes.size(); i++) {
                    if (!(this.nodes.get(i) instanceof GlslFunctionNode)) {
                        return i + 1;
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public int size() {
        return this.nodes.size();
    }

    @Override
    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof GlslNode node)) {
            return false;
        }
        return this.nodes.contains(node);
    }

    @Override
    public @NotNull Iterator<GlslNode> iterator() {
        return this.nodes.iterator();
    }

    @Override
    public @NotNull Object[] toArray() {
        return this.nodes.toArray();
    }

    @Override
    public @NotNull <T> T[] toArray(@NotNull T[] a) {
        return this.nodes.toArray(a);
    }

    @Override
    public boolean add(GlslNode glslNode) {
        return this.nodes.add(glslNode);
    }

    @Override
    public void add(int index, GlslNode element) {
        this.nodes.add(index, element);
    }

    public void add(GlslInjectionPoint point, GlslNode element) {
        this.nodes.add(this.getInjectionIndex(point), element);
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof GlslNode node)) {
            return false;
        }
        return this.nodes.remove(node);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return new HashSet<>(this.nodes).containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends GlslNode> c) {
        return this.nodes.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends GlslNode> c) {
        return this.nodes.addAll(index, c);
    }

    public void addAll(GlslInjectionPoint point, Collection<GlslNode> nodes) {
        this.nodes.addAll(this.getInjectionIndex(point), nodes);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return this.nodes.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return this.nodes.retainAll(c);
    }

    @Override
    public void replaceAll(@NotNull UnaryOperator<GlslNode> operator) {
        this.nodes.replaceAll(operator);
    }

    @Override
    public void sort(@Nullable Comparator<? super GlslNode> c) {
        this.nodes.sort(c);
    }

    @Override
    public void clear() {
        this.nodes.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslNodeList glslNodes = (GlslNodeList) o;
        return this.nodes.equals(glslNodes.nodes);
    }

    @Override
    public int hashCode() {
        return this.nodes.hashCode();
    }

    @Override
    public GlslNode get(int index) {
        return this.nodes.get(index);
    }

    @Override
    public GlslNode set(int index, GlslNode element) {
        return this.nodes.set(index, element);
    }

    @Override
    public GlslNode remove(int index) {
        return this.nodes.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return this.nodes.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return this.nodes.lastIndexOf(o);
    }

    @Override
    public @NotNull ListIterator<GlslNode> listIterator() {
        return this.nodes.listIterator();
    }

    @Override
    public @NotNull ListIterator<GlslNode> listIterator(int index) {
        return this.nodes.listIterator(index);
    }

    @Override
    public @NotNull List<GlslNode> subList(int fromIndex, int toIndex) {
        return this.nodes.subList(fromIndex, toIndex);
    }

    @Override
    public @NotNull Spliterator<GlslNode> spliterator() {
        return this.nodes.spliterator();
    }

    @Override
    public void addFirst(GlslNode glslNode) {
        this.nodes.addFirst(glslNode);
    }

    @Override
    public void addLast(GlslNode glslNode) {
        this.nodes.addLast(glslNode);
    }

    @Override
    public GlslNode removeFirst() {
        return this.nodes.removeFirst();
    }

    @Override
    public GlslNode removeLast() {
        return this.nodes.removeLast();
    }

    @Override
    public String toString() {
        return this.nodes.toString();
    }
}

