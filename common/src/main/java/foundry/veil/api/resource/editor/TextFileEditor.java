package foundry.veil.api.resource.editor;

import foundry.veil.Veil;
import foundry.veil.api.client.imgui.CodeEditor;
import foundry.veil.api.resource.VeilEditorEnvironment;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.api.resource.type.VeilTextResource;
import imgui.ImGui;
import imgui.extension.texteditor.TextEditor;
import imgui.extension.texteditor.TextEditorLanguageDefinition;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class TextFileEditor implements ResourceFileEditor<VeilTextResource<?>> {

    private static final Component SAVE = Component.translatable("gui.veil.save");

    private final VeilResourceManager resourceManager;
    private final VeilTextResource<?> resource;
    private final CodeEditor editor;

    public TextFileEditor(VeilEditorEnvironment environment, VeilTextResource<?> resource) {
        this.editor = new CodeEditor(SAVE);
        this.editor.show(resource.resourceInfo().fileName(), "");
        this.editor.setSaveCallback(null);
        this.resourceManager = environment.getResourceManager();
        this.resource = resource;
        this.loadFromDisk();
    }

    @Override
    public void render() {
        this.editor.renderWindow();
        if (ImGui.beginPopupModal("###open_failed")) {
            ImGui.text("Failed to open file");
            ImGui.endPopup();
        }
    }

    @Override
    public void loadFromDisk() {
        VeilResourceInfo info = this.resource.resourceInfo();
        TextEditorLanguageDefinition languageDefinition = resource.languageDefinition();
        TextEditor editor = this.editor.getEditor();

        editor.setReadOnly(true);
        editor.setColorizerEnable(false);

        info.getResource(this.resourceManager).ifPresentOrElse(data -> CompletableFuture.supplyAsync(() -> {
            try (InputStream stream = data.open()) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, Util.ioPool()).handleAsync((contents, error) -> {
            if (error != null) {
                this.editor.hide();
                ImGui.openPopup("###open_failed");
                Veil.LOGGER.error("Failed to open file", error);
                return null;
            }

            this.editor.show(info.fileName(), contents);
            this.editor.setSaveCallback((source, errorConsumer) -> this.save(source.getBytes(StandardCharsets.UTF_8), resourceManager, resource));

            editor.setReadOnly(info.isStatic());
            if (languageDefinition != null) {
                editor.setColorizerEnable(true);
                editor.setLanguageDefinition(languageDefinition);
            }
            return null;
        }, Minecraft.getInstance()), () -> {
            this.editor.hide();
            ImGui.openPopup("###open_failed");
        });
    }

    @Override
    public boolean isClosed() {
        return !this.editor.isOpen();
    }

    @Override
    public VeilTextResource<?> getResource() {
        return this.resource;
    }
}
