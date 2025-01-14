package foundry.veil.impl.client.imgui;

import imgui.glfw.ImGuiImplGlfw;

public class VeilImGuiImplGlfw extends ImGuiImplGlfw {

    private final VeilImGuiImpl impl;

    public VeilImGuiImplGlfw(VeilImGuiImpl impl) {
        this.impl = impl;
    }

    @Override
    public void scrollCallback(long windowId, double xOffset, double yOffset) {
        try {
            this.impl.start();
            super.scrollCallback(windowId, xOffset, yOffset);
        } finally {
            this.impl.stop();
        }
    }

    @Override
    public void keyCallback(long windowId, int key, int scancode, int action, int mods) {
        try {
            this.impl.start();
            super.keyCallback(windowId, key, scancode, action, mods);
        } finally {
            this.impl.stop();
        }
    }

    @Override
    public void windowFocusCallback(long windowId, boolean focused) {
        try {
            this.impl.start();
            super.windowFocusCallback(windowId, focused);
        } finally {
            this.impl.stop();
        }
    }

    @Override
    public void charCallback(long windowId, int c) {
        try {
            this.impl.start();
            super.charCallback(windowId, c);
        } finally {
            this.impl.stop();
        }
    }
}
