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

    private final CodeEditor editor;

    public TextFileEditor() {
        this.editor = new CodeEditor(SAVE);
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
    public void open(VeilEditorEnvironment environment, VeilTextResource<?> resource) {
        VeilResourceInfo info = resource.resourceInfo();
        TextEditorLanguageDefinition languageDefinition = resource.languageDefinition();
        VeilResourceManager resourceManager = environment.getResourceManager();

        this.editor.show(info.fileName(), "");
        this.editor.setSaveCallback(null);
        this.editor.getEditor().setReadOnly(true);
        this.editor.getEditor().setColorizerEnable(false);

        resourceManager.resources(info).getResource(info.location()).ifPresentOrElse(data -> CompletableFuture.supplyAsync(() -> {
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
            this.editor.setSaveCallback((source, errorConsumer) -> this.save(source.getBytes(StandardCharsets.UTF_8), resource));

            TextEditor textEditor = this.editor.getEditor();
            textEditor.setReadOnly(resource.resourceInfo().isStatic());
            if (languageDefinition != null) {
                textEditor.setColorizerEnable(true);
                textEditor.setLanguageDefinition(languageDefinition);
            }
            return null;
        }, Minecraft.getInstance()), () -> {
            this.editor.hide();
            ImGui.openPopup("###open_failed");
        });
    }
}
