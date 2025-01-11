package foundry.veil.impl.client.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import foundry.veil.api.client.editor.SingleWindowInspector;
import foundry.veil.api.client.imgui.CodeEditor;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.imgui.VeilLanguageDefinitions;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.compat.IrisCompat;
import foundry.veil.api.compat.SodiumCompat;
import foundry.veil.impl.client.imgui.VeilImGuiImpl;
import foundry.veil.impl.client.render.shader.program.ShaderUniformCache;
import foundry.veil.mixin.debug.accessor.DebugGameRendererAccessor;
import foundry.veil.mixin.debug.accessor.DebugLevelRendererAccessor;
import foundry.veil.mixin.debug.accessor.DebugPostChainAccessor;
import imgui.ImGui;
import imgui.ImGuiListClipper;
import imgui.callback.ImListClipperCallback;
import imgui.flag.*;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntRBTreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.lwjgl.opengl.ARBProgramInterfaceQuery.GL_SHADER_STORAGE_BLOCK;
import static org.lwjgl.opengl.ARBProgramInterfaceQuery.glGetProgramResourceiv;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL21C.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL40C.*;
import static org.lwjgl.opengl.GL43C.GL_BUFFER_BINDING;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

@ApiStatus.Internal
public class ShaderInspector extends SingleWindowInspector implements ResourceManagerReloadListener {

    public static final Component TITLE = Component.translatable("inspector.veil.shader.title");

    private static final Component REFRESH = Component.translatable("inspector.veil.shader.button.refresh");
    private static final Component SEARCH = Component.translatable("inspector.veil.shader.search");
    private static final Component SHADER_PROGRAMS = Component.translatable("inspector.veil.shader.shader_programs");
    private static final Component SHADER_DEFINITIONS = Component.translatable("inspector.veil.shader.definitions");
    private static final Component SHADER_DEFINITIONS_HINT = Component.translatable("inspector.veil.shader.definitions.hint");
    private static final Component OPEN_SOURCE = Component.translatable("inspector.veil.shader.open_source");
    private static final Component OPEN_SHADER_INFO = Component.translatable("inspector.veil.shader.open_shader_info");
    private static final Component SHADER_INFO_WINDOW = Component.translatable("inspector.veil.shader.shader_info");
    private static final Component SAMPLERS = Component.translatable("inspector.veil.shader.samplers");
    private static final Component UNIFORMS = Component.translatable("inspector.veil.shader.uniforms");
    private static final Component UNIFORM_BLOCKS = Component.translatable("inspector.veil.shader.uniform_blocks");
    private static final Component STORAGE_BLOCKS = Component.translatable("inspector.veil.shader.storage_blocks");

    private final CodeEditor codeEditor;
    private final ImBoolean shaderInfoVisible;
    private final Object2IntMap<ResourceLocation> shaders;
    private final ShaderUniformCache uniformCache;

    private final ImString programFilterText;
    private Pattern programFilter;
    private SelectedProgram selectedProgram;
    private int selectedTab;

    private final ImString addDefinitionText;
    private final Set<String> removedDefinitions;

    public ShaderInspector() {
        this.shaders = new Object2IntRBTreeMap<>((a, b) -> {
            int compare = a.getNamespace().compareTo(b.getNamespace());
            if (compare == 0) {
                return a.getPath().compareTo(b.getPath());
            }
            return compare;
        });
        this.uniformCache = new ShaderUniformCache(() -> this.selectedProgram.programId);

        this.codeEditor = new CodeEditor(TITLE, null);
        this.codeEditor.getEditor().setLanguageDefinition(VeilLanguageDefinitions.glsl());
        this.shaderInfoVisible = new ImBoolean(false);

        this.programFilterText = new ImString(128);
        this.programFilter = null;
        this.selectedProgram = null;
        this.selectedTab = 0;

        this.addDefinitionText = new ImString(128);
        this.removedDefinitions = new HashSet<>(1);
    }

    private void setSelectedProgram(@Nullable ResourceLocation name) {
        if (name != null && this.shaders.containsKey(name)) {
            int program = this.shaders.getInt(name);
            if (glIsProgram(program)) {
                int[] attachedShaders = new int[glGetProgrami(program, GL_ATTACHED_SHADERS)];
                glGetAttachedShaders(program, null, attachedShaders);

                Int2IntMap shaders = new Int2IntArrayMap(attachedShaders.length);
                for (int shader : attachedShaders) {
                    shaders.put(glGetShaderi(shader, GL_SHADER_TYPE), shader);
                }

                this.selectedProgram = new SelectedProgram(name, program, Int2IntMaps.unmodifiable(shaders));
                this.uniformCache.clear();
                return;
            } else {
                Veil.LOGGER.error("Compiled shader does not exist for program: {}", name);
            }
        }

        this.selectedProgram = null;
        this.uniformCache.clear();
    }

    private void setEditShaderSource(int shader) {
        this.codeEditor.show(null, glGetShaderSource(shader));
    }

    private void reloadShaders() {
        this.shaders.clear();
        this.uniformCache.clear();
        TabSource.values()[this.selectedTab].addShaders(this.shaders::put);
        if (this.isShaderInvalid() || (this.selectedProgram != null && !this.shaders.containsKey(this.selectedProgram.name))) {
            this.setSelectedProgram(null);
        }
    }

    private boolean isShaderInvalid() {
        return this.selectedProgram == null || this.selectedProgram.isShaderInvalid();
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public Component getGroup() {
        return RENDERER_GROUP;
    }

    @Override
    protected void renderComponents() {
        this.codeEditor.getEditor().setReadOnly(true);
        this.removedDefinitions.clear();

        ImGui.beginChild("##shader_programs", ImGui.getContentRegionAvailX() * 2 / 3, 0);
        VeilImGuiUtil.component(SHADER_PROGRAMS);

        TabSource[] sources = TabSource.values();
        if (ImGui.beginTabBar("##controls")) {
            if (ImGui.tabItemButton(REFRESH.getString())) {
                this.reloadShaders();
            }
            for (TabSource source : sources) {
                ImGui.beginDisabled(!source.active.getAsBoolean());
                if (ImGui.beginTabItem(source.displayName.getString())) {
                    if (this.selectedTab != source.ordinal()) {
                        this.selectedTab = source.ordinal();
                        this.setSelectedProgram(null);
                        this.reloadShaders();
                    }
                    ImGui.endTabItem();
                }
                ImGui.endDisabled();
            }
            ImGui.endTabBar();
        }

        // Deselect the tab if it is no longer active
        while (!sources[this.selectedTab].active.getAsBoolean() && this.selectedTab > 0) {
            this.selectedTab--;
            this.setSelectedProgram(null);
            this.reloadShaders();
        }

        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
        if (ImGui.inputTextWithHint("##search", SEARCH.getString(), this.programFilterText)) {
            String regex = this.programFilterText.get();
            this.programFilter = null;
            if (!regex.isBlank()) {
                try {
                    this.programFilter = Pattern.compile(regex);
                } catch (PatternSyntaxException ignored) {
                }
            }
        }

        if (ImGui.beginListBox("##programs", ImGui.getContentRegionAvailX(), -Float.MIN_VALUE)) {
            for (Object2IntMap.Entry<ResourceLocation> entry : this.shaders.object2IntEntrySet()) {
                ResourceLocation name = entry.getKey();
                boolean selected = this.selectedProgram != null && name.equals(this.selectedProgram.name);

                if (this.programFilter != null && !this.programFilter.matcher(name.toString()).find()) {
                    if (selected) {
                        this.setSelectedProgram(null);
                    }
                    continue;
                }

                if (ImGui.selectable("##" + name.toString(), selected)) {
                    this.setSelectedProgram(name);
                }

                ImGui.sameLine();
                VeilImGuiUtil.resourceLocation(name);

                ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, ImGui.getStyle().getItemSpacingY());
                ImGui.sameLine();
                ImGui.text(" (" + entry.getIntValue() + ")");
                ImGui.popStyleVar();
            }

            ImGui.endListBox();
        }
        ImGui.endChild();

        ShaderPreDefinitions definitions = VeilRenderSystem.renderer().getShaderDefinitions();
        ImGui.sameLine();
        if (ImGui.beginChild("##panel", 0, ImGui.getContentRegionAvailY())) {
            if (ImGui.beginChild("##open_source", 0, ImGui.getContentRegionAvailY() / 2)) {
                VeilImGuiUtil.component(OPEN_SOURCE);

                this.openShaderButton(GL_FRAGMENT_SHADER);
                this.openShaderButton(GL_VERTEX_SHADER);
                this.openShaderButton(GL_COMPUTE_SHADER);
                this.openShaderButton(GL_GEOMETRY_SHADER);
                this.openShaderButton(GL_TESS_CONTROL_SHADER);
                this.openShaderButton(GL_TESS_EVALUATION_SHADER);

                ImGui.beginDisabled(this.shaderInfoVisible.get());
                if (ImGui.button(OPEN_SHADER_INFO.getString())) {
                    this.shaderInfoVisible.set(true);
                }
                ImGui.endDisabled();
            }
            ImGui.endChild();

            if (ImGui.beginChild("##shader_definitions", 0, ImGui.getContentRegionAvailY())) {
                VeilImGuiUtil.component(SHADER_DEFINITIONS);
                ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
                if (ImGui.inputTextWithHint("##add_definition", SHADER_DEFINITIONS_HINT.getString(), this.addDefinitionText, ImGuiInputTextFlags.EnterReturnsTrue)) {
                    String[] parts = this.addDefinitionText.get().split("=", 2);
                    definitions.set(parts[0].trim(), parts.length > 1 ? parts[1].trim() : null);
                    this.addDefinitionText.clear();
                }
                if (ImGui.beginListBox("##definitions", -Float.MIN_VALUE, ImGui.getContentRegionAvailY())) {
                    for (Map.Entry<String, String> entry : definitions.getDefinitions().entrySet()) {
                        String name = entry.getKey();
                        String value = entry.getValue();

                        ImGui.pushID(name);
                        ImGui.text(value);

                        float size = ImGui.getTextLineHeightWithSpacing();
                        ImGui.sameLine();
                        ImGui.dummy(ImGui.getContentRegionAvailX() - ImGui.getStyle().getCellPaddingX() * 2 - size, 0);
                        ImGui.sameLine();
                        if (ImGui.button("X", size, size)) {
                            this.removedDefinitions.add(name);
                        }

                        ImGui.popID();
                    }

                    ImGui.endListBox();
                }
            }
            ImGui.endChild();
        }
        ImGui.endChild();

        for (String name : this.removedDefinitions) {
            definitions.remove(name);
        }
    }

    @Override
    public void render() {
        ImGui.setNextWindowSizeConstraints(600, 400, Float.MAX_VALUE, Float.MAX_VALUE);

        super.render();

        this.codeEditor.renderWindow();
        if (this.shaderInfoVisible.get()) {
            ImGui.setNextWindowSizeConstraints(500, 600, Float.MAX_VALUE, Float.MAX_VALUE);
            if (ImGui.begin(SHADER_INFO_WINDOW.getString(), this.shaderInfoVisible, ImGuiWindowFlags.NoSavedSettings)) {
                boolean invalid = this.isShaderInvalid();
                ImGui.beginDisabled(invalid);
                int program = invalid ? 0 : this.selectedProgram.programId;

                if (ImGui.collapsingHeader(SAMPLERS.getString(), ImGuiTreeNodeFlags.DefaultOpen) && !invalid) {
                    ImGui.indent();
                    for (CharSequence sampler : this.uniformCache.getSamplers()) {
                        ImGui.selectable(sampler.toString());
                    }
                    ImGui.unindent();
                }

                if (ImGui.collapsingHeader(UNIFORMS.getString(), ImGuiTreeNodeFlags.DefaultOpen) && !invalid) {
                    ImGui.indent();
                    List<Map.Entry<String, ShaderUniformCache.Uniform>> sorted = this.uniformCache.getUniforms()
                            .entrySet()
                            .stream()
                            .filter(entry -> !this.uniformCache.getSamplers().contains(entry.getKey()))
                            .sorted(Comparator.comparingInt(entry -> entry.getValue().location()))
                            .toList();
                    for (Map.Entry<String, ShaderUniformCache.Uniform> entry : sorted) {
                        String name = entry.getKey();
                        ShaderUniformCache.Uniform uniform = entry.getValue();
                        ImGui.selectable(this.formatUniform(name, program, uniform));
                    }
                    ImGui.unindent();
                }

                if (ImGui.collapsingHeader(UNIFORM_BLOCKS.getString(), ImGuiTreeNodeFlags.DefaultOpen) && !invalid) {
                    ImGui.indent();
                    List<Map.Entry<String, ShaderUniformCache.UniformBlock>> sorted = this.uniformCache.getUniformBlocks()
                            .entrySet()
                            .stream()
                            .sorted(Comparator.comparingInt(entry -> entry.getValue().index()))
                            .toList();
                    for (Map.Entry<String, ShaderUniformCache.UniformBlock> entry : sorted) {
                        String blockName = entry.getKey();
                        if (ImGui.collapsingHeader(blockName)) {
                            ImGui.indent();

                            ShaderUniformCache.UniformBlock block = entry.getValue();
                            int buffer = glGetIntegeri(GL_UNIFORM_BUFFER_BINDING, glGetActiveUniformBlocki(program, block.index(), GL_UNIFORM_BLOCK_BINDING));
                            RenderSystem.glBindBuffer(GL_COPY_READ_BUFFER, buffer);
                            ByteBuffer data = glMapBuffer(GL_COPY_READ_BUFFER, GL_READ_ONLY, block.size(), null);
                            for (ShaderUniformCache.Uniform field : block.fields()) {
                                String name = field.name().startsWith(blockName) ? field.name().substring(blockName.length() + 1) : field.name();
                                ImGui.selectable(data != null ? this.formatBuffer(name, data, field, 0) : name);
                            }
                            glUnmapBuffer(GL_COPY_READ_BUFFER);
                            ImGui.unindent();
                        }
                    }
                    ImGui.unindent();
                }

                ImGui.beginDisabled(!VeilRenderSystem.shaderStorageBufferSupported());
                if (ImGui.collapsingHeader(STORAGE_BLOCKS.getString(), ImGuiTreeNodeFlags.DefaultOpen) && !invalid) {
                    ImGui.indent();
                    List<Map.Entry<String, ShaderUniformCache.StorageBlock>> sorted = this.uniformCache.getStorageBlocks()
                            .entrySet()
                            .stream()
                            .sorted(Comparator.comparingInt(entry -> entry.getValue().index()))
                            .toList();
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        IntBuffer properties = stack.ints(GL_BUFFER_BINDING);
                        IntBuffer buffer = stack.mallocInt(1);

                        for (Map.Entry<String, ShaderUniformCache.StorageBlock> entry : sorted) {
                            String blockName = entry.getKey();
                            if (ImGui.collapsingHeader(blockName)) {
                                ImGui.indent();

                                ShaderUniformCache.StorageBlock block = entry.getValue();
                                glGetProgramResourceiv(program, GL_SHADER_STORAGE_BLOCK, block.index(), properties, null, buffer);
                                RenderSystem.glBindBuffer(GL_COPY_READ_BUFFER, buffer.get(0));
                                int size = block.array() ? glGetBufferParameteri(GL_COPY_READ_BUFFER, GL_BUFFER_SIZE) : block.size();
                                ByteBuffer data = glMapBuffer(GL_COPY_READ_BUFFER, GL_READ_ONLY, size, null);

                                ShaderUniformCache.Uniform[] fields = block.fields();
                                for (int i = 0; i < fields.length; i++) {
                                    ShaderUniformCache.Uniform field = fields[i];
                                    String name = field.name().startsWith(blockName) ? field.name().substring(blockName.length() + 1) : field.name();

                                    if (block.array() && i >= fields.length - 1 && data != null) {
                                        ImGuiListClipper.forEach((data.limit() - field.offset()) / block.arrayStride(), (int) ImGui.getTextLineHeightWithSpacing(), new ImListClipperCallback() {
                                            @Override
                                            public void accept(int index) {
                                                ImGui.selectable(ShaderInspector.this.formatBuffer(name, data, field, index));
                                            }
                                        });
                                        continue;
                                    }

                                    ImGui.selectable(data != null ? this.formatBuffer(name, data, field, 0) : name);
                                }
                                glUnmapBuffer(GL_COPY_READ_BUFFER);
                                ImGui.unindent();
                            }
                        }
                    }
                    ImGui.unindent();
                }
                ImGui.endDisabled();

                ImGui.endDisabled();
            }
            ImGui.end();
        }
    }

    //<editor-fold desc="Uniform Formatting">

    private String formatUniform(String name, int program, ShaderUniformCache.Uniform uniform) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return switch (uniform.type()) {
                case GL_FLOAT -> this.formatUniformFloats(stack, "float " + name, program, uniform.location(), 1, 1);
                case GL_FLOAT_VEC2 ->
                        this.formatUniformFloats(stack, "vec2 " + name, program, uniform.location(), 2, 1);
                case GL_FLOAT_VEC3 ->
                        this.formatUniformFloats(stack, "vec3 " + name, program, uniform.location(), 3, 1);
                case GL_FLOAT_VEC4 ->
                        this.formatUniformFloats(stack, "vec4 " + name, program, uniform.location(), 4, 1);
                case GL_DOUBLE -> this.formatUniformDoubles(stack, "double " + name, program, uniform.location(), 1, 1);
                case GL_DOUBLE_VEC2 ->
                        this.formatUniformDoubles(stack, "dvec2 " + name, program, uniform.location(), 2, 1);
                case GL_DOUBLE_VEC3 ->
                        this.formatUniformDoubles(stack, "dvec3 " + name, program, uniform.location(), 3, 1);
                case GL_DOUBLE_VEC4 ->
                        this.formatUniformDoubles(stack, "dvec4 " + name, program, uniform.location(), 4, 1);
                case GL_INT -> this.formatUniformInts(stack, "int " + name, program, uniform.location(), 1);
                case GL_INT_VEC2 -> this.formatUniformInts(stack, "ivec2 " + name, program, uniform.location(), 2);
                case GL_INT_VEC3 -> this.formatUniformInts(stack, "ivec3 " + name, program, uniform.location(), 3);
                case GL_INT_VEC4 -> this.formatUniformInts(stack, "ivec4 " + name, program, uniform.location(), 4);
                case GL_UNSIGNED_INT -> this.formatUniformUInts(stack, "uint " + name, program, uniform.location(), 1);
                case GL_UNSIGNED_INT_VEC2 ->
                        this.formatUniformUInts(stack, "uvec2 " + name, program, uniform.location(), 2);
                case GL_UNSIGNED_INT_VEC3 ->
                        this.formatUniformUInts(stack, "uvec3 " + name, program, uniform.location(), 3);
                case GL_UNSIGNED_INT_VEC4 ->
                        this.formatUniformUInts(stack, "uvec4 " + name, program, uniform.location(), 4);
                case GL_FLOAT_MAT2 ->
                        this.formatUniformFloats(stack, "mat2 " + name, program, uniform.location(), 2, 2);
                case GL_FLOAT_MAT3 ->
                        this.formatUniformFloats(stack, "mat3 " + name, program, uniform.location(), 3, 3);
                case GL_FLOAT_MAT4 ->
                        this.formatUniformFloats(stack, "mat4 " + name, program, uniform.location(), 4, 4);
                case GL_FLOAT_MAT2x3 ->
                        this.formatUniformFloats(stack, "mat2x3 " + name, program, uniform.location(), 2, 3);
                case GL_FLOAT_MAT2x4 ->
                        this.formatUniformFloats(stack, "mat2x4 " + name, program, uniform.location(), 2, 4);
                case GL_FLOAT_MAT3x2 ->
                        this.formatUniformFloats(stack, "mat3x2 " + name, program, uniform.location(), 3, 2);
                case GL_FLOAT_MAT3x4 ->
                        this.formatUniformFloats(stack, "mat3x4 " + name, program, uniform.location(), 3, 4);
                case GL_FLOAT_MAT4x2 ->
                        this.formatUniformFloats(stack, "mat4x2 " + name, program, uniform.location(), 4, 2);
                case GL_FLOAT_MAT4x3 ->
                        this.formatUniformFloats(stack, "mat4x3 " + name, program, uniform.location(), 4, 3);
                case GL_DOUBLE_MAT2 ->
                        this.formatUniformDoubles(stack, "dmat2 " + name, program, uniform.location(), 2, 2);
                case GL_DOUBLE_MAT3 ->
                        this.formatUniformDoubles(stack, "dmat3 " + name, program, uniform.location(), 3, 3);
                case GL_DOUBLE_MAT4 ->
                        this.formatUniformDoubles(stack, "dmat4 " + name, program, uniform.location(), 4, 4);
                case GL_DOUBLE_MAT2x3 ->
                        this.formatUniformDoubles(stack, "dmat2x3 " + name, program, uniform.location(), 2, 3);
                case GL_DOUBLE_MAT2x4 ->
                        this.formatUniformDoubles(stack, "dmat2x4 " + name, program, uniform.location(), 2, 4);
                case GL_DOUBLE_MAT3x2 ->
                        this.formatUniformDoubles(stack, "dmat3x2 " + name, program, uniform.location(), 3, 2);
                case GL_DOUBLE_MAT3x4 ->
                        this.formatUniformDoubles(stack, "dmat3x4 " + name, program, uniform.location(), 3, 4);
                case GL_DOUBLE_MAT4x2 ->
                        this.formatUniformDoubles(stack, "dmat4x2 " + name, program, uniform.location(), 4, 2);
                case GL_DOUBLE_MAT4x3 ->
                        this.formatUniformDoubles(stack, "dmat4x3 " + name, program, uniform.location(), 4, 3);
                default -> name;
            };
        }
    }

    private String formatBuffer(String name, ByteBuffer buffer, ShaderUniformCache.Uniform uniform, int offset) {
        return switch (uniform.type()) {
            case GL_FLOAT ->
                    this.formatFloats((uniform.offset() + offset) + ": float " + name, i -> buffer.getFloat(uniform.offset() + offset + (i << 2)), 1, 1);
            case GL_FLOAT_VEC2 ->
                    this.formatFloats((uniform.offset() + offset) + ": vec2 " + name, i -> buffer.getFloat(uniform.offset() + offset + (i << 2)), 2, 1);
            case GL_FLOAT_VEC3 ->
                    this.formatFloats((uniform.offset() + offset) + ": vec3 " + name, i -> buffer.getFloat(uniform.offset() + offset + (i << 2)), 3, 1);
            case GL_FLOAT_VEC4 ->
                    this.formatFloats((uniform.offset() + offset) + ": vec4 " + name, i -> buffer.getFloat(uniform.offset() + offset + (i << 2)), 4, 1);
            case GL_DOUBLE ->
                    this.formatDoubles((uniform.offset() + offset) + ": double " + name, i -> buffer.getDouble(uniform.offset() + offset + (i << 3)), 1, 1);
            case GL_DOUBLE_VEC2 ->
                    this.formatDoubles((uniform.offset() + offset) + ": dvec2 " + name, i -> buffer.getDouble(uniform.offset() + offset + (i << 3)), 2, 1);
            case GL_DOUBLE_VEC3 ->
                    this.formatDoubles((uniform.offset() + offset) + ": dvec3 " + name, i -> buffer.getDouble(uniform.offset() + offset + (i << 3)), 3, 1);
            case GL_DOUBLE_VEC4 ->
                    this.formatDoubles((uniform.offset() + offset) + ": dvec4 " + name, i -> buffer.getDouble(uniform.offset() + offset + (i << 3)), 4, 1);
            case GL_INT ->
                    this.formatInts((uniform.offset() + offset) + ": int " + name, i -> buffer.getInt(uniform.offset() + offset + (i << 2)), 1);
            case GL_INT_VEC2 ->
                    this.formatInts((uniform.offset() + offset) + ": ivec2 " + name, i -> buffer.getInt(uniform.offset() + offset + (i << 2)), 2);
            case GL_INT_VEC3 ->
                    this.formatInts((uniform.offset() + offset) + ": ivec3 " + name, i -> buffer.getInt(uniform.offset() + offset + (i << 2)), 3);
            case GL_INT_VEC4 ->
                    this.formatInts((uniform.offset() + offset) + ": ivec4 " + name, i -> buffer.getInt(uniform.offset() + offset + (i << 2)), 4);
            case GL_UNSIGNED_INT ->
                    this.formatUInts((uniform.offset() + offset) + ": uint " + name, i -> buffer.getInt(uniform.offset() + offset + (i << 2)), 1);
            case GL_UNSIGNED_INT_VEC2 ->
                    this.formatUInts((uniform.offset() + offset) + ": uvec2 " + name, i -> buffer.getInt(uniform.offset() + offset + (i << 2)), 2);
            case GL_UNSIGNED_INT_VEC3 ->
                    this.formatUInts((uniform.offset() + offset) + ": uvec3 " + name, i -> buffer.getInt(uniform.offset() + offset + (i << 2)), 3);
            case GL_UNSIGNED_INT_VEC4 ->
                    this.formatUInts((uniform.offset() + offset) + ": uvec4 " + name, i -> buffer.getInt(uniform.offset() + offset + (i << 2)), 4);
            case GL_FLOAT_MAT2 ->
                    this.formatFloats((uniform.offset() + offset) + ": mat2 " + name, i -> buffer.getFloat(uniform.offset() + offset + (i << 2)), 2, 2);
            case GL_FLOAT_MAT3 ->
                    this.formatFloats((uniform.offset() + offset) + ": mat3 " + name, i -> buffer.getFloat(uniform.offset() + offset + (i << 2)), 3, 3);
            case GL_FLOAT_MAT4 ->
                    this.formatFloats((uniform.offset() + offset) + ": mat4 " + name, i -> buffer.getFloat(uniform.offset() + offset + (i << 2)), 4, 4);
            case GL_FLOAT_MAT2x3 ->
                    this.formatFloats((uniform.offset() + offset) + ": mat2x3 " + name, i -> buffer.getFloat(uniform.offset() + offset + (i << 2)), 2, 3);
            case GL_FLOAT_MAT2x4 ->
                    this.formatFloats((uniform.offset() + offset) + ": mat2x4 " + name, i -> buffer.getFloat(uniform.offset() + offset + (i << 2)), 2, 4);
            case GL_FLOAT_MAT3x2 ->
                    this.formatFloats((uniform.offset() + offset) + ": mat3x2 " + name, i -> buffer.getFloat(uniform.offset() + offset + (i << 2)), 3, 2);
            case GL_FLOAT_MAT3x4 ->
                    this.formatFloats((uniform.offset() + offset) + ": mat3x4 " + name, i -> buffer.getFloat(uniform.offset() + offset + (i << 2)), 3, 4);
            case GL_FLOAT_MAT4x2 ->
                    this.formatFloats((uniform.offset() + offset) + ": mat4x2 " + name, i -> buffer.getFloat(uniform.offset() + offset + (i << 2)), 4, 2);
            case GL_FLOAT_MAT4x3 ->
                    this.formatFloats((uniform.offset() + offset) + ": mat4x3 " + name, i -> buffer.getFloat(uniform.offset() + offset + (i << 2)), 4, 3);
            case GL_DOUBLE_MAT2 ->
                    this.formatDoubles((uniform.offset() + offset) + ": dmat2 " + name, i -> buffer.getDouble(uniform.offset() + offset + (i << 3)), 2, 2);
            case GL_DOUBLE_MAT3 ->
                    this.formatDoubles((uniform.offset() + offset) + ": dmat3 " + name, i -> buffer.getDouble(uniform.offset() + offset + (i << 3)), 3, 3);
            case GL_DOUBLE_MAT4 ->
                    this.formatDoubles((uniform.offset() + offset) + ": dmat4 " + name, i -> buffer.getDouble(uniform.offset() + offset + (i << 3)), 4, 4);
            case GL_DOUBLE_MAT2x3 ->
                    this.formatDoubles((uniform.offset() + offset) + ": dmat2x3 " + name, i -> buffer.getDouble(uniform.offset() + offset + (i << 3)), 2, 3);
            case GL_DOUBLE_MAT2x4 ->
                    this.formatDoubles((uniform.offset() + offset) + ": dmat2x4 " + name, i -> buffer.getDouble(uniform.offset() + offset + (i << 3)), 2, 4);
            case GL_DOUBLE_MAT3x2 ->
                    this.formatDoubles((uniform.offset() + offset) + ": dmat3x2 " + name, i -> buffer.getDouble(uniform.offset() + offset + (i << 3)), 3, 2);
            case GL_DOUBLE_MAT3x4 ->
                    this.formatDoubles((uniform.offset() + offset) + ": dmat3x4 " + name, i -> buffer.getDouble(uniform.offset() + offset + (i << 3)), 3, 4);
            case GL_DOUBLE_MAT4x2 ->
                    this.formatDoubles((uniform.offset() + offset) + ": dmat4x2 " + name, i -> buffer.getDouble(uniform.offset() + offset + (i << 3)), 4, 2);
            case GL_DOUBLE_MAT4x3 ->
                    this.formatDoubles((uniform.offset() + offset) + ": dmat4x3 " + name, i -> buffer.getDouble(uniform.offset() + offset + (i << 3)), 4, 3);
            default -> name;
        };
    }

    private String formatUniformFloats(MemoryStack stack, String key, int program, int location, int cols, int rows) {
        FloatBuffer values = stack.mallocFloat(rows * cols);
        glGetUniformfv(program, location, values);
        return this.formatFloats(key, values::get, cols, rows);
    }

    private String formatUniformDoubles(MemoryStack stack, String key, int program, int location, int cols, int rows) {
        DoubleBuffer values = stack.mallocDouble(rows * cols);
        glGetUniformdv(program, location, values);
        return this.formatDoubles(key, values::get, cols, rows);
    }

    private String formatUniformInts(MemoryStack stack, String key, int program, int location, int cols) {
        IntBuffer values = stack.mallocInt(cols);
        glGetUniformiv(program, location, values);
        return this.formatInts(key, values::get, cols);
    }

    private String formatUniformUInts(MemoryStack stack, String key, int program, int location, int cols) {
        IntBuffer values = stack.mallocInt(cols);
        glGetUniformuiv(program, location, values);
        return this.formatUInts(key, values::get, cols);
    }

    private String formatFloats(String key, Function<Integer, Float> deserializer, int cols, int rows) {
        StringBuilder matrix = new StringBuilder(key + ":" + (rows > 1 ? '\n' : ' '));

        int[] colWidths = new int[cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int size = ("%+.4f").formatted(deserializer.apply(col * rows + row)).length();
                if (size > colWidths[col]) {
                    colWidths[col] = size;
                }
            }
        }

        for (int row = 0; row < rows; row++) {
            matrix.append('[');
            for (int col = 0; col < cols; col++) {
                String value = ("%+" + colWidths[col] + ".4f").formatted(deserializer.apply(col * rows + row));
                matrix.append(value).append(", ");
            }
            matrix.replace(matrix.length() - 2, matrix.length(), "]\n");
        }
        matrix.delete(matrix.length() - 1, matrix.length());
        return matrix.toString();
    }

    private String formatDoubles(String key, Function<Integer, Double> deserializer, int cols, int rows) {
        StringBuilder matrix = new StringBuilder(key + ":" + (rows > 1 ? '\n' : ' '));

        int[] colWidths = new int[cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int size = ("%+.4f").formatted(deserializer.apply(col * rows + row)).length();
                if (size > colWidths[col]) {
                    colWidths[col] = size;
                }
            }
        }

        for (int row = 0; row < rows; row++) {
            matrix.append('[');
            for (int col = 0; col < cols; col++) {
                String value = ("%+" + colWidths[col] + ".4f").formatted(deserializer.apply(col * rows + row));
                matrix.append(value).append(", ");
            }
            matrix.replace(matrix.length() - 2, matrix.length(), "]\n");
        }
        matrix.delete(matrix.length() - 1, matrix.length());
        return matrix.toString();
    }

    private String formatInts(String key, Function<Integer, Integer> deserializer, int cols) {
        StringBuilder matrix = new StringBuilder(key + ": [");

        int[] colWidths = new int[cols];
        for (int col = 0; col < cols; col++) {
            int size = ("%+d").formatted(deserializer.apply(col)).length();
            if (size > colWidths[col]) {
                colWidths[col] = size;
            }
        }

        for (int col = 0; col < cols; col++) {
            String value = ("%+" + colWidths[col] + "d").formatted(deserializer.apply(col));
            matrix.append(value).append(", ");
        }
        matrix.replace(matrix.length() - 2, matrix.length(), "]");
        return matrix.toString();
    }

    private String formatUInts(String key, Function<Integer, Integer> deserializer, int cols) {
        StringBuilder matrix = new StringBuilder(key + ": [");

        int[] colWidths = new int[cols];
        for (int col = 0; col < cols; col++) {
            int size = ("%+d").formatted(Integer.toUnsignedLong(deserializer.apply(col))).length();
            if (size > colWidths[col]) {
                colWidths[col] = size;
            }
        }

        for (int col = 0; col < cols; col++) {
            String value = ("%+" + colWidths[col] + "d").formatted(Integer.toUnsignedLong(deserializer.apply(col)));
            matrix.append(value).append(", ");
        }
        matrix.replace(matrix.length() - 2, matrix.length(), "]");
        return matrix.toString();
    }

    //</editor-fold>

    private void openShaderButton(int type) {
        boolean disabled = this.isShaderInvalid() || !this.selectedProgram.shaders.containsKey(type);
        ImGui.beginDisabled(disabled);

        if (disabled) {
            ImGui.pushStyleColor(ImGuiCol.Button, ImGui.getColorU32(ImGuiCol.FrameBg));
        }

        if (ImGui.button(DeviceInfoViewer.getShaderName(type).getString())) {
            this.setEditShaderSource(this.selectedProgram.shaders.get(type));
        }

        if (disabled) {
            ImGui.popStyleColor();
        }

        ImGui.endDisabled();
    }

    @Override
    public void onShow() {
        super.onShow();
        this.reloadShaders();
    }

    @Override
    public void onHide() {
        super.onHide();
        this.shaders.clear();
    }

    @Override
    public void free() {
        super.free();
        this.codeEditor.free();
        this.uniformCache.clear();
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        if (this.isOpen()) {
            this.reloadShaders();
        }
    }

    private record SelectedProgram(ResourceLocation name, int programId, Int2IntMap shaders) {
        public boolean isShaderInvalid() {
            return this.programId == 0 || !glIsProgram(this.programId);
        }
    }

    private enum TabSource {
        VANILLA(Component.translatable("inspector.veil.shader.source.vanilla")) {
            @Override
            public void addShaders(ObjIntConsumer<ResourceLocation> registry) {
                DebugGameRendererAccessor accessor = (DebugGameRendererAccessor) Minecraft.getInstance().gameRenderer;
                Map<String, ShaderInstance> shaders = accessor.getShaders();
                for (ShaderInstance shader : shaders.values()) {
                    String name = shader.getName().isBlank() ? Integer.toString(shader.getId()) : shader.getName();
                    registry.accept(ResourceLocation.parse(name), shader.getId());
                }

                ShaderInstance blitShader = accessor.getBlitShader();
                registry.accept(ResourceLocation.parse(blitShader.getName()), blitShader.getId());
            }
        },
        VANILLA_POST(Component.translatable("inspector.veil.shader.source.vanilla_post")) {
            @Override
            public void addShaders(ObjIntConsumer<ResourceLocation> registry) {
                DebugLevelRendererAccessor levelRendererAccessor = (DebugLevelRendererAccessor) Minecraft.getInstance().levelRenderer;
                this.addChainPasses(registry, levelRendererAccessor.getEntityEffect());
                this.addChainPasses(registry, levelRendererAccessor.getTransparencyChain());
                DebugGameRendererAccessor gameRendererAccessor = (DebugGameRendererAccessor) Minecraft.getInstance().gameRenderer;
                this.addChainPasses(registry, gameRendererAccessor.getPostEffect());
            }

            private void addChainPasses(ObjIntConsumer<ResourceLocation> registry, @Nullable PostChain chain) {
                if (chain == null) {
                    return;
                }

                List<PostPass> passes = ((DebugPostChainAccessor) chain).getPasses();
                for (PostPass pass : passes) {
                    EffectInstance effect = pass.getEffect();
                    registry.accept(ResourceLocation.parse(effect.getName()), effect.getId());
                }
            }
        },
        VEIL(Component.translatable("inspector.veil.shader.source.veil")) {
            @Override
            public void addShaders(ObjIntConsumer<ResourceLocation> registry) {
                Map<ResourceLocation, ShaderProgram> shaders = VeilRenderSystem.renderer().getShaderManager().getShaders();
                for (ShaderProgram shader : shaders.values()) {
                    registry.accept(shader.getName(), shader.getProgram());
                }
                VeilImGuiImpl.get().addImguiShaders(registry);
            }
        },
        IRIS(Component.translatable("inspector.veil.shader.source.iris"), () -> IrisCompat.INSTANCE != null) {
            @Override
            public void addShaders(ObjIntConsumer<ResourceLocation> registry) {
                for (ShaderInstance shader : IrisCompat.INSTANCE.getLoadedShaders()) {
                    String name = shader.getName().isBlank() ? Integer.toString(shader.getId()) : shader.getName();
                    registry.accept(ResourceLocation.parse(name), shader.getId());
                }
            }
        },
        SODIUM(Component.translatable("inspector.veil.shader.source.sodium"), () -> SodiumCompat.INSTANCE != null) {
            @Override
            public void addShaders(ObjIntConsumer<ResourceLocation> registry) {
                for (Object2IntMap.Entry<ResourceLocation> entry : SodiumCompat.INSTANCE.getLoadedShaders().object2IntEntrySet()) {
                    registry.accept(entry.getKey(), entry.getIntValue());
                }
            }
        },
        OTHER(Component.translatable("inspector.veil.shader.source.unknown")) {
            @Override
            public void addShaders(ObjIntConsumer<ResourceLocation> registry) {
                IntSet programs = new IntOpenHashSet();
                for (int i = 1; i < 10000; i++) {
                    if (!glIsProgram(i)) {
                        continue;
                    }

                    programs.add(i);
                }

                for (TabSource value : TabSource.values()) {
                    if (value == this || !value.active.getAsBoolean()) {
                        continue;
                    }

                    value.addShaders((name, id) -> programs.remove(id));
                }

                for (int program : programs) {
                    registry.accept(ResourceLocation.fromNamespaceAndPath("unknown", Integer.toString(program)), program);
                }
            }
        };

        private final Component displayName;
        private final BooleanSupplier active;

        TabSource(Component displayName) {
            this(displayName, () -> true);
        }

        TabSource(Component displayName, BooleanSupplier active) {
            this.displayName = displayName;
            this.active = active;
        }

        public abstract void addShaders(ObjIntConsumer<ResourceLocation> registry);
    }
}
