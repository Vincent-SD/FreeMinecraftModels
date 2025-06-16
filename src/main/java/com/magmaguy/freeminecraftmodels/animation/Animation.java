package com.magmaguy.freeminecraftmodels.animation;

import com.magmaguy.freeminecraftmodels.customentity.ModeledEntity;
import com.magmaguy.freeminecraftmodels.customentity.core.Bone;
import com.magmaguy.freeminecraftmodels.dataconverter.AnimationBlueprint;
import com.magmaguy.freeminecraftmodels.dataconverter.AnimationFrame;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class Animation {
    @Getter
    private final AnimationBlueprint animationBlueprint;
    @Getter
    private final HashMap<Bone, AnimationFrame[]> animationFrames = new HashMap<>();
    @Getter @Setter
    private int counter = 0;

    public void incrementCounter() {
        counter++;
    }

    public Animation(AnimationBlueprint animationBlueprint, ModeledEntity modeledEntity) {
        this.animationBlueprint = animationBlueprint;
        animationBlueprint.getAnimationFrames().forEach((key, value) -> {
            for (Bone bone : modeledEntity.getSkeleton().getBones())
                if (bone.getBoneBlueprint().equals(key)) {
                    animationFrames.put(bone, value);
                    break;
                }
        });
        modeledEntity.getSkeleton().getBones().forEach(bone -> {
            if (!animationFrames.containsKey(bone)) {
                animationFrames.put(bone, null);
            }
        });
    }

    public Animation(AnimationBlueprint animationBlueprint, Map<Bone, AnimationFrame[]> frames) {
        this.animationBlueprint = animationBlueprint;
        this.animationFrames.putAll(frames);
    }

    public void resetCounter() {
        counter = 0;
    }
}
