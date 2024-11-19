package foundry.veil.api.client.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * A class for storing data about the player model that can be used by poses.
 *
 * @see VeilPoseable
 */
public class PoseData {
    public float ageInTicks;
    public float walkTime;
    public float limbSwing;
    public float limbSwingAmount;
    public float headYaw;
    public float headPitch;
    public float useTime;
    public float maxUseTime;
    public ModelPart mainHand;
    public ModelPart offHand;

    public boolean swapped;

    public PoseStack stackPoseStack;
    public ItemStack stack;
    public InteractionHand hand;

    public float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    public float equipProgress;


    /**
     * PoseData constructor for a 3rd person pose.
     *
     * @param ageInTicks      the age of the player in ticks
     * @param walkTime        the time the player has been walking for
     * @param limbSwing       the limb swing of the player
     * @param limbSwingAmount the limb swing amount of the player
     * @param headYaw         the head yaw of the player
     * @param headPitch       the head pitch of the player
     * @param useTime         the time the player has been using an item for
     * @param maxUseTime      the max time the player can use an item for
     * @param mainHand        the main hand model part
     * @param offHand         the off hand model part
     * @param swapped         whether the player is using their off hand or not
     */
    public PoseData(float ageInTicks, float walkTime, float limbSwing, float limbSwingAmount, float headYaw, float headPitch, float useTime, float maxUseTime, ModelPart mainHand, ModelPart offHand, boolean swapped) {
        this.mainHand = mainHand;
        this.offHand = offHand;
        this.ageInTicks = ageInTicks;
        this.walkTime = walkTime;
        this.limbSwing = limbSwing;
        this.limbSwingAmount = limbSwingAmount;
        this.headYaw = headYaw;
        this.headPitch = headPitch;
        this.useTime = useTime;
        this.maxUseTime = maxUseTime;
        this.swapped = swapped;
    }

    /**
     * PoseData constructor for a 1st person pose.
     *
     * @param stackPoseStack  the pose stack of the player model
     * @param stack           the item stack of the item being rendered
     * @param limbSwingAmount the limb swing amount of the player
     * @param useTime         the time the player has been using an item for
     * @param maxUseTime      the max time the player can use an item for
     */
    public PoseData(PoseStack stackPoseStack, ItemStack stack, float limbSwingAmount, float useTime, float maxUseTime, InteractionHand hand, float equipProgress) {
        this.stackPoseStack = stackPoseStack;
        this.stack = stack;
        this.limbSwingAmount = limbSwingAmount;
        this.useTime = useTime;
        this.maxUseTime = maxUseTime;
        this.hand = hand;
        this.equipProgress = equipProgress;
    }
}
