package foundry.veil.api.client.render.framebuffer;

import foundry.veil.impl.client.render.AdvancedFboImpl;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.NativeResource;

/**
 * An attachment added to an {@link AdvancedFboImpl}.
 *
 * @author Ocelot
 */
public interface AdvancedFboAttachment extends NativeResource, Cloneable {

    /**
     * Creates the attachment and initializes it with the default properties.
     */
    void create();

    /**
     * Attaches this attachment to the provided target under the specified attachment point.
     *
     * @param attachment The attachment point to add this attachment to
     */
    void attach(int attachment);

    /**
     * Binds this attachment.
     */
    void bindAttachment();

    /**
     * Unbinds this attachment.
     */
    void unbindAttachment();

    /**
     * Returns the OpenGL attachment point. One of: <br>
     * <table>
     *     <caption>All possible OpenGL attachment points</caption>
     *     <tr>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT0 COLOR_ATTACHMENT0}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT1 COLOR_ATTACHMENT1}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT2 COLOR_ATTACHMENT2}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT3 COLOR_ATTACHMENT3}</td>
     *     </tr>
     *     <tr>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT4 COLOR_ATTACHMENT4}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT5 COLOR_ATTACHMENT5}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT6 COLOR_ATTACHMENT6}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT7 COLOR_ATTACHMENT7}</td>
     *     </tr>
     *     <tr>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT8 COLOR_ATTACHMENT8}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT9 COLOR_ATTACHMENT9}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT10 COLOR_ATTACHMENT10}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT11 COLOR_ATTACHMENT11}</td>
     *     </tr>
     *     <tr>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT12 COLOR_ATTACHMENT12}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT13 COLOR_ATTACHMENT13}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT14 COLOR_ATTACHMENT14}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT15 COLOR_ATTACHMENT15}</td>
     *     </tr>
     *     <tr>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT16 COLOR_ATTACHMENT16}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT17 COLOR_ATTACHMENT17}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT18 COLOR_ATTACHMENT18}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT19 COLOR_ATTACHMENT19}</td>
     *     </tr>
     *     <tr>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT20 COLOR_ATTACHMENT20}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT21 COLOR_ATTACHMENT21}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT22 COLOR_ATTACHMENT22}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT23 COLOR_ATTACHMENT23}</td>
     *     </tr>
     *     <tr>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT24 COLOR_ATTACHMENT24}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT25 COLOR_ATTACHMENT25}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT26 COLOR_ATTACHMENT26}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT27 COLOR_ATTACHMENT27}</td>
     *     </tr>
     *     <tr>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT28 COLOR_ATTACHMENT28}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT29 COLOR_ATTACHMENT29}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT30 COLOR_ATTACHMENT30}</td>
     *         <td>{@link GL30C#GL_COLOR_ATTACHMENT31 COLOR_ATTACHMENT31}</td>
     *     </tr>
     *     <tr>
     *         <td>{@link GL30C#GL_DEPTH_ATTACHMENT DEPTH_ATTACHMENT}</td>
     *         <td>{@link GL30C#GL_STENCIL_ATTACHMENT STENCIL_ATTACHMENT}</td>
     *         <td>{@link GL30C#GL_DEPTH_STENCIL_ATTACHMENT DEPTH_STENCIL_ATTACHMENT}</td>
     *     </tr>
     * </table>
     *
     * @return OpenGL attachment point
     */
    int getAttachmentType();

    /**
     * @return The OpenGL format for this attachment
     */
    int getFormat();

    /**
     * @return Texture targets return mipmaps and render targets return samples.
     */
    int getLevels();

    /**
     * @return Whether this attachment can be read from
     */
    boolean canSample();

    /**
     * @return The custom name of this attachment or <code>null</code> to use <code>DiffuseSampler#</code>
     */
    @Nullable String getName();

    /**
     * @return A new identical attachment to this one
     */
    AdvancedFboAttachment clone();
}
