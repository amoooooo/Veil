package foundry.veil.impl.resource.tree;

import foundry.veil.api.resource.VeilResource;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A resource folder for a tree-structure
 */
public class VeilResourceFolder {

    private final String name;
    private final Map<String, VeilResourceFolder> subFolders = new TreeMap<>();
    private final Map<String, VeilResource<?>> resources = new TreeMap<>();
    private final ArrayList<VeilResource<?>> renderResources = new ArrayList<>();
    private final List<VeilResource<?>> renderResourcesView = Collections.unmodifiableList(this.renderResources);
    private boolean dirty;

    public VeilResourceFolder(String name) {
        this.name = name;
    }

    /**
     * Adds a resource to this folder, creating sub-folders if necessary
     *
     * @param path     The path of the resource
     * @param resource The resource to add
     */
    public void addResource(String path, VeilResource<?> resource) {
        // If the path contains a slash, we need to create a sub-folder
        if (path.contains("/")) {
            String[] parts = path.split("/", 2);
            VeilResourceFolder folder = this.subFolders.computeIfAbsent(parts[0], VeilResourceFolder::new);
            folder.addResource(parts[1], resource);
            return;
        }

        this.resources.put(path, resource);
        this.dirty = true;
    }

    /**
     * Adds a folder to this folder
     *
     * @param folder The folder to add, with a pre-known name
     */
    public void addFolder(VeilResourceFolder folder) {
        this.subFolders.put(folder.name, folder);
    }

    /**
     * @return An iterable collection of all folders contained within this folder
     */
    public Collection<VeilResourceFolder> getSubFolders() {
        return this.subFolders.values();
    }

    /**
     * @return An iterable collection of all resources contained within this folder
     */
    public Collection<VeilResource<?>> getResources() {
        return this.resources.values();
    }

    /**
     * @return A view of all visible resources contained within this folder
     */
    public List<VeilResource<?>> getRenderResources() {
        if (this.dirty) {
            Collection<VeilResource<?>> values = this.resources.values();
            this.renderResources.clear();
            this.renderResources.ensureCapacity(values.size());
            for (VeilResource<?> resource : values) {
                if (!resource.resourceInfo().hidden()) {
                    this.renderResources.add(resource);
                }
            }
            this.renderResources.trimToSize();
        }
        return this.renderResourcesView;
    }

    public String getName() {
        return this.name;
    }

    public @Nullable VeilResourceFolder getFolder(String name) {
        return this.subFolders.get(name);
    }

    public @Nullable VeilResource<?> getResource(String name) {
        return this.resources.get(name);
    }
}
