package foundry.veil.impl.client.render.perspective;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.*;
import java.util.function.Consumer;

public class VeilSectionOcclusionGraph {

    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
    private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0) * 16.0);

    private final NodeQueue nodeQueue = new NodeQueue(64);
    private ViewArea viewArea;
    private int viewDistance;

    public void update(ViewArea viewArea, boolean smartCull, LevelPerspectiveCamera camera, Frustum frustum, List<SectionRenderDispatcher.RenderSection> sections) {
        this.viewArea = viewArea;
        this.viewDistance = Math.min(viewArea.getViewDistance(), Mth.ceil(camera.getRenderDistance()));
        GraphStorage graphState = new GraphStorage(viewArea.sections.length);
        this.initializeQueueForFullUpdate(camera, viewArea);
        this.nodeQueue.forEach(node -> graphState.sectionToNodeMap.put(node.section, node));
        this.runUpdates(graphState, viewArea, camera.getPosition(), frustum, this.nodeQueue, smartCull, sections);
    }

    public void reset() {
        this.nodeQueue.trim(64);
    }

    private void initializeQueueForFullUpdate(Camera camera, ViewArea viewArea) {
        this.nodeQueue.clear();

        BlockPos pos = camera.getBlockPosition();
        SectionRenderDispatcher.RenderSection renderSection = viewArea.getRenderSectionAt(pos);
        if (renderSection != null) {
            this.nodeQueue.add(new Node(renderSection, 0));
            return;
        }

        Vec3 cameraPos = camera.getPosition();
        LevelHeightAccessor level = viewArea.getLevelHeightAccessor();
        boolean aboveVoid = pos.getY() > level.getMinBuildHeight();
        int startY = aboveVoid ? level.getMaxBuildHeight() - 8 : level.getMinBuildHeight() + 8;
        int startX = Mth.floor(cameraPos.x / 16.0) << SectionPos.SECTION_BITS;
        int startZ = Mth.floor(cameraPos.z / 16.0) << SectionPos.SECTION_BITS;
        int radius = this.viewDistance;
        List<Node> list = new ArrayList<>(4 * radius * radius);

        BlockPos.MutableBlockPos renderSectionPos = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                SectionRenderDispatcher.RenderSection section = viewArea.getRenderSectionAt(renderSectionPos.set(startX + x << SectionPos.SECTION_BITS + 8, startY, startZ + z << SectionPos.SECTION_BITS + 8));
                if (section != null && this.isInViewDistance(pos, section.getOrigin())) {
                    Direction direction = aboveVoid ? Direction.DOWN : Direction.UP;
                    Node node = new Node(section, 0);
                    node.addSourceDirection(direction);
                    node.addDirection(direction);
                    if (x > 0) {
                        node.addDirection(Direction.EAST);
                    } else if (x < 0) {
                        node.addDirection(Direction.WEST);
                    }

                    if (z > 0) {
                        node.addDirection(Direction.SOUTH);
                    } else if (z < 0) {
                        node.addDirection(Direction.NORTH);
                    }

                    list.add(node);
                }
            }
        }

        list.sort(Comparator.comparingDouble(node -> {
            BlockPos origin = node.section.getOrigin();
            return pos.distToCenterSqr(origin.getX() + 8.5, origin.getY() + 8.5, origin.getZ() + 8.5);
        }));

        this.nodeQueue.addAll(list);
    }

    private void runUpdates(
            GraphStorage graphStorage,
            ViewArea viewArea,
            Vec3 cameraPosition,
            Frustum frustum,
            Queue<Node> nodeQueue,
            boolean smartCull,
            List<SectionRenderDispatcher.RenderSection> sections
    ) {
        BlockPos cameraSectionPos = new BlockPos(
                Mth.floor(cameraPosition.x / 16.0) << SectionPos.SECTION_BITS,
                Mth.floor(cameraPosition.y / 16.0) << SectionPos.SECTION_BITS,
                Mth.floor(cameraPosition.z / 16.0) << SectionPos.SECTION_BITS);
        BlockPos cameraCenter = cameraSectionPos.offset(8, 8, 8);
        BlockPos.MutableBlockPos temp = new BlockPos.MutableBlockPos();

        LevelHeightAccessor level = viewArea.getLevelHeightAccessor();
        int maxBuildHeight = level.getMaxBuildHeight();
        int minBuildHeight = level.getMinBuildHeight();

        while (!nodeQueue.isEmpty()) {
            Node node = nodeQueue.poll();
            SectionRenderDispatcher.RenderSection renderSection = node.section;
            if (frustum.isVisible(renderSection.getBoundingBox()) && graphStorage.renderSections.add(renderSection.index)) {
                sections.add(node.section);
            }

            BlockPos origin = renderSection.getOrigin();
            boolean far = Math.abs(origin.getX() - cameraSectionPos.getX()) > MINIMUM_ADVANCED_CULLING_DISTANCE
                    || Math.abs(origin.getY() - cameraSectionPos.getY()) > MINIMUM_ADVANCED_CULLING_DISTANCE
                    || Math.abs(origin.getZ() - cameraSectionPos.getZ()) > MINIMUM_ADVANCED_CULLING_DISTANCE;

            for (Direction direction : DIRECTIONS) {
                SectionRenderDispatcher.RenderSection section = this.getRelativeFrom(cameraSectionPos, renderSection, direction);
                if (section == null) {
                    continue;
                }

                Direction opposite = direction.getOpposite();
                if (!smartCull || (node.directions & (1 << opposite.ordinal())) == 0) {
                    if (smartCull && node.sourceDirections != 0) {
                        SectionRenderDispatcher.CompiledSection compiledSection = renderSection.getCompiled();
                        boolean visible = false;

                        for (int i = 0; i < DIRECTIONS.length; i++) {
                            if ((node.sourceDirections & (1 << i)) != 0 && compiledSection.facesCanSeeEachother(DIRECTIONS[i], opposite)) {
                                visible = true;
                                break;
                            }
                        }

                        if (!visible) {
                            continue;
                        }
                    }

                    BlockPos neighborOrigin = section.getOrigin();
                    if (smartCull && far) {
                        // TODO this looks like voxel ray marching
                        int offsetX = (direction.getAxis() == Direction.Axis.X ? cameraCenter.getX() <= neighborOrigin.getX() : cameraCenter.getX() >= neighborOrigin.getX()) ? 0 : 16;
                        int offsetY = (direction.getAxis() == Direction.Axis.Y ? cameraCenter.getY() <= neighborOrigin.getY() : cameraCenter.getY() >= neighborOrigin.getY()) ? 0 : 16;
                        int offsetZ = (direction.getAxis() == Direction.Axis.Z ? cameraCenter.getZ() <= neighborOrigin.getZ() : cameraCenter.getZ() >= neighborOrigin.getZ()) ? 0 : 16;
                        temp.setWithOffset(neighborOrigin, offsetX, offsetY, offsetZ);
                        Vector3d pos = new Vector3d(cameraPosition.x - temp.getX(), cameraPosition.y - temp.getY(), cameraPosition.z - temp.getZ());
                        Vector3d step = pos.normalize(CEILED_SECTION_DIAGONAL, new Vector3d());
                        boolean visible = true;

                        while (pos.distanceSquared(cameraPosition.x, cameraPosition.y, cameraPosition.z) > MINIMUM_ADVANCED_CULLING_DISTANCE * MINIMUM_ADVANCED_CULLING_DISTANCE) {
                            pos.add(step);
                            if (pos.y > maxBuildHeight || pos.y < minBuildHeight) {
                                break;
                            }

                            SectionRenderDispatcher.RenderSection renderSection3 = viewArea.getRenderSectionAt(temp.set(Math.floor(pos.x), Math.floor(pos.y), Math.floor(pos.z)));
                            if (renderSection3 == null || graphStorage.sectionToNodeMap.get(renderSection3) == null) {
                                visible = false;
                                break;
                            }
                        }

                        if (!visible) {
                            continue;
                        }
                    }

                    Node node2 = graphStorage.sectionToNodeMap.get(section);
                    if (node2 != null) {
                        node2.addSourceDirection(direction);
                    } else {
                        Node node3 = new Node(section, node.step + 1);
                        node3.addSourceDirection(direction);
                        node3.addDirection(direction);
                        if (section.hasAllNeighbors()) {
                            nodeQueue.add(node3);
                            graphStorage.sectionToNodeMap.put(section, node3);
                        } else if (this.isInViewDistance(cameraSectionPos, neighborOrigin)) {
                            graphStorage.sectionToNodeMap.put(section, node3);
                        }
                    }
                }
            }
        }
    }

    private boolean isInViewDistance(BlockPos pos, BlockPos origin) {
        int centerX = pos.getX() >> SectionPos.SECTION_BITS;
        int centerZ = pos.getZ() >> SectionPos.SECTION_BITS;
        int x = origin.getX() >> SectionPos.SECTION_BITS;
        int z = origin.getZ() >> SectionPos.SECTION_BITS;
        return ChunkTrackingView.isWithinDistance(centerX, centerZ, this.viewDistance, x, z, false);
    }

    @Nullable
    private SectionRenderDispatcher.RenderSection getRelativeFrom(BlockPos pos, SectionRenderDispatcher.RenderSection section, Direction direction) {
        BlockPos origin = section.getRelativeOrigin(direction);
        if (!this.isInViewDistance(pos, origin)) {
            return null;
        } else {
            return Mth.abs(pos.getY() - origin.getY()) > this.viewDistance << SectionPos.SECTION_BITS ? null : this.viewArea.getRenderSectionAt(origin);
        }
    }

    private static class GraphStorage {
        public final SectionToNodeMap sectionToNodeMap;
        public final IntSet renderSections;

        public GraphStorage(int size) {
            this.sectionToNodeMap = new SectionToNodeMap(size);
            this.renderSections = new IntArraySet(size);
        }
    }

    private static class Node {

        private final SectionRenderDispatcher.RenderSection section;
        private int sourceDirections;
        private int directions;
        private final int step;

        private Node(SectionRenderDispatcher.RenderSection section, int step) {
            this.section = section;
            this.step = step;
        }

        private void addDirection(Direction direction) {
            this.directions |= 1 << direction.ordinal();
        }

        private void addSourceDirection(Direction sourceDirection) {
            this.sourceDirections |= 1 << sourceDirection.ordinal();
        }

        @Override
        public int hashCode() {
            return this.section.getOrigin().hashCode();
        }

        @Override
        public boolean equals(Object object) {
            return this.section.getOrigin().equals(((Node) object).section.getOrigin());
        }
    }

    private static class SectionToNodeMap {
        private final Node[] nodes;

        private SectionToNodeMap(int size) {
            this.nodes = new Node[size];
        }

        public void put(SectionRenderDispatcher.RenderSection section, Node node) {
            this.nodes[section.index] = node;
        }

        public @Nullable Node get(SectionRenderDispatcher.RenderSection section) {
            int index = section.index;
            return index >= 0 && index < this.nodes.length ? this.nodes[index] : null;
        }
    }

    private static class NodeQueue implements Queue<Node> {

        private Node[] data;
        private int size;
        private int readPointer;

        public NodeQueue(int initialCapacity) {
            this.data = new Node[initialCapacity];
            this.size = 0;
        }

        public void trim(int size) {
            if (this.data.length > size) {
                this.data = new Node[size];
            }
            this.size = 0;
            this.readPointer = 0;
        }

        @Override
        public int size() {
            return this.size;
        }

        @Override
        public boolean isEmpty() {
            return this.readPointer >= this.size;
        }

        @Override
        public boolean contains(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull Iterator<Node> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull <T> T[] toArray(@NotNull T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(Node node) {
            if (this.size >= this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }
            this.data[this.size++] = node;
            return true;
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends Node> c) {
            if (this.isEmpty() && c instanceof ArrayList<? extends Node> arrayList) {
                this.data = arrayList.toArray(Node[]::new);
                return true;
            }
            if (this.size + c.size() > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.size + c.size());
            }
            for (Node node : c) {
                this.data[this.size++] = node;
            }
            return true;
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            this.size = 0;
            this.readPointer = 0;
        }

        @Override
        public boolean offer(Node node) {
            return this.add(node);
        }

        @Override
        public Node remove() {
            if (this.readPointer < this.size) {
                return this.data[this.readPointer++];
            }
            throw new NoSuchElementException();
        }

        @Override
        public Node poll() {
            if (this.readPointer < this.size) {
                return this.data[this.readPointer++];
            }
            return null;
        }

        @Override
        public Node element() {
            if (this.readPointer < this.size) {
                return this.data[this.readPointer];
            }
            throw new NoSuchElementException();
        }

        @Override
        public Node peek() {
            return this.readPointer >= this.size ? null : this.data[this.readPointer];
        }

        @Override
        public void forEach(Consumer<? super Node> action) {
            for (int i = this.readPointer; i < this.size; i++) {
                action.accept(this.data[i]);
            }
        }
    }
}
