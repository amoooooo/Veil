package foundry.veil.api.client.necromancer;

import java.util.List;
import java.util.Map;

// W.I.P. replacement for graveyard
// dooooon't use this. i'm still working on it...
//
//      ~ your best friend,
//          cappin  >_o

public class Skeleton {
    Bone root;
    Map<String, Integer> nameToId;
    List<Bone> bones;
    List<Constraint> constraints;

    public void update(float deltaTime) {
        for (Bone bone : this.bones) {
            bone.update(deltaTime);
        }
    }
}
