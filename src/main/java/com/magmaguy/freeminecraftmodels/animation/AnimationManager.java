package com.magmaguy.freeminecraftmodels.animation;

import com.magmaguy.freeminecraftmodels.MetadataHandler;
import com.magmaguy.freeminecraftmodels.customentity.ModeledEntity;
import com.magmaguy.freeminecraftmodels.customentity.core.Bone;
import com.magmaguy.freeminecraftmodels.customentity.core.BoneTransformsSnapshot;
import com.magmaguy.freeminecraftmodels.dataconverter.AnimationBlueprintFake;
import com.magmaguy.freeminecraftmodels.dataconverter.AnimationFrame;
import com.magmaguy.freeminecraftmodels.dataconverter.AnimationsBlueprint;
import com.magmaguy.freeminecraftmodels.dataconverter.BoneBlueprint;
import com.magmaguy.freeminecraftmodels.utils.LoopType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.joml.Vector3d;

import java.util.*;
import java.util.function.Function;

public class AnimationManager {
    private static List<AnimationManager> loadedAnimations;
    private static List<AnimationManager> unloadedAnimations;
    @Getter
    private final Animations animations;
    private final ModeledEntity modeledEntity;
    private final HashSet<Animation> states = new HashSet<>();
    //There are some animation defaults that will activate automatically so long as the animations are adequately named
    private Animation idleAnimation = null;
    private Animation attackAnimation = null;
    private Animation walkAnimation = null;
    private Animation jumpAnimation = null;
    private Animation deathAnimation = null;
    private Animation spawnAnimation = null;
    private BukkitTask clock = null;
    //This one is used for preventing default animations other than death from playing for as long as it is true
    private boolean animationGracePeriod = false;

    public AnimationManager(ModeledEntity modeledEntity, AnimationsBlueprint animationsBlueprint) {
        this.modeledEntity = modeledEntity;
        this.animations = new Animations(animationsBlueprint, modeledEntity);

        idleAnimation = animations.getAnimations().get("idle");
        attackAnimation = animations.getAnimations().get("attack");
        walkAnimation = animations.getAnimations().get("walk");
        jumpAnimation = animations.getAnimations().get("jump");
        deathAnimation = animations.getAnimations().get("death");
        spawnAnimation = animations.getAnimations().get("spawn");
    }

    private static int getAdjustedAnimationPosition(Animation animation) {
        int adjustedAnimationPosition;
        if (animation.getCounter() >= animation.getAnimationBlueprint().getDuration() && animation.getAnimationBlueprint().getLoopType() == LoopType.HOLD)
            //Case where the animation is technically over but also is set to hold
            adjustedAnimationPosition = animation.getAnimationBlueprint().getDuration() - 1;
        else {
            //Normal case, looping
            adjustedAnimationPosition = (int) (animation.getCounter() - Math.floor(animation.getCounter() / (double) animation.getAnimationBlueprint().getDuration()) * animation.getAnimationBlueprint().getDuration());
        }
        return adjustedAnimationPosition;
    }

    public void start() {
        if (spawnAnimation != null) {
            states.add(spawnAnimation);
            if (idleAnimation != null)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        animationGracePeriod = false;
                    }
                }.runTaskLater(MetadataHandler.PLUGIN, spawnAnimation.getAnimationBlueprint().getDuration());
        } else if (idleAnimation != null) states.add(idleAnimation);

        clock = new BukkitRunnable() {
            @Override
            public void run() {
                updateStates();
                states.forEach(animation -> playAnimationFrame(animation));
                boolean shouldHeadFollowAnimation = modeledEntity.getHeadRotationSupplier() == null;
                modeledEntity.getSkeleton().transform(shouldHeadFollowAnimation);
            }
        }.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);

    }

    private void updateStates() {
        if (modeledEntity.getLivingEntity() == null) return;
        if (modeledEntity.getLivingEntity().isDead()) {
            if (deathAnimation != null) {
                animationGracePeriod = true;
                overrideStates(deathAnimation);
                return;
            } else
                modeledEntity.remove();
        }
        if (animationGracePeriod) return;
        if (jumpAnimation != null && !modeledEntity.getLivingEntity().isOnGround()) {
            overrideStates(jumpAnimation);
            return;
        }
        if (walkAnimation != null && modeledEntity.getLivingEntity().getVelocity().length() > .08) {
            overrideStates(walkAnimation);
            return;
        }
        overrideStates(idleAnimation);
        //Jump
        //if (!modeledEntity.getEntity().isDead())
    }

    private void overrideStates(Animation animation) {
        if (!states.contains(animation)) {
            states.clear();
            animation.resetCounter();
            states.add(animation);
        }
    }

    private BukkitTask graceResetTask = null;

    public boolean playAnimation(String animationName, boolean blendAnimation, int firstFrame) {
        Animation animation = animations.getAnimations().get(animationName);
        if (animation == null) return false;
        if (graceResetTask != null) graceResetTask.cancel();
        if (!blendAnimation) {
            states.clear();
            animationGracePeriod = true;
            graceResetTask = new BukkitRunnable() {
                @Override
                public void run() {
                    animationGracePeriod = false;
                }
            }.runTaskLater(MetadataHandler.PLUGIN, animation.getAnimationBlueprint().getDuration() - firstFrame);
        }
        animation.resetCounter();
        animation.setCounter(firstFrame);
        states.add(animation);
        return true;
    }

    public boolean playAnimation(String animationName, boolean blendAnimation) {
        return playAnimation(animationName, blendAnimation, 0);
    }

    private void playAnimationFrame(Animation animation) {
        if (!animation.getAnimationBlueprint().getLoopType().equals(LoopType.LOOP) && animation.getCounter() >= animation.getAnimationBlueprint().getDuration()) {
            //Case where the animation doesn't loop, and it's over
            states.remove(animation);
            if (animation == deathAnimation)
                modeledEntity.remove();
            return;
        }
        //VSD Temporary workaround, reset idle animation at the end of each loop to avoid LE PUTAIN DE BUG QUI N'A AUCUN SENS
        else if (animation.getAnimationBlueprint().getAnimationName().equals("idle") && animation.getCounter() % animation.getAnimationBlueprint().getDuration() == 1){
            modeledEntity.getSkeleton().getBones().forEach(Bone::sendTeleportPacket);
        }
        int adjustedAnimationPosition = getAdjustedAnimationPosition(animation);
        //Handle rotations
        animation.getAnimationFrames().forEach((key, value) -> {
            if (value == null)
                key.updateAnimationRotation(0, 0, 0);
            else{
                key.updateAnimationRotation(
                        value[adjustedAnimationPosition].xRotation,
                        value[adjustedAnimationPosition].yRotation,
                        value[adjustedAnimationPosition].zRotation);
            }

        });

        //Handle translations
        animation.getAnimationFrames().forEach((key, value) -> {
            if (value == null)
                key.updateAnimationTranslation(0, 0, 0);
            else{
                key.updateAnimationTranslation(
                        value[adjustedAnimationPosition].xPosition,
                        value[adjustedAnimationPosition].yPosition,
                        value[adjustedAnimationPosition].zPosition);
            }

        });

        animation.incrementCounter();
    }

    public boolean playAnimation(String animationName, int blendDuration) {
        Animation target = animations.getAnimations().get(animationName);

        if (target == null) return false;

        //default value if blend duration > animation duration
        if (blendDuration >= target.getAnimationBlueprint().getDuration()) {
            blendDuration =  (int) (target.getAnimationBlueprint().getDuration() * 0.8);
        }

        Map<Bone, BoneTransformsSnapshot> fromPose = captureCurrentAnimationPose();
        if (fromPose == null) return false;

        Animation blendAnimation = generateBlendAnimation(target, fromPose, blendDuration);

        states.clear();
        blendAnimation.resetCounter();
        states.add(blendAnimation);

        animationGracePeriod = true;
        if (graceResetTask != null) graceResetTask.cancel();

        int finalBlendDuration = blendDuration;
        graceResetTask = new BukkitRunnable() {
            @Override
            public void run() {
                animationGracePeriod = false;
                playAnimation(animationName, false, finalBlendDuration);
            }
        }.runTaskLater(MetadataHandler.PLUGIN, blendDuration);

        return true;
    }


    private Animation generateBlendAnimation(Animation target, Map<Bone, BoneTransformsSnapshot> fromPose, int blendDuration) {
        Map<Bone, AnimationFrame[]> blendedFrames = new HashMap<>();

        for (Bone bone : modeledEntity.getSkeleton().getBones()) {
            BoneTransformsSnapshot from = fromPose.getOrDefault(bone, new BoneTransformsSnapshot());
            AnimationFrame[] targetFrames = target.getAnimationFrames().get(bone);

            int targetFrameIndex = Math.min(blendDuration, target.getAnimationBlueprint().getDuration() - 1);
            AnimationFrame targetFrame = (targetFrames != null && targetFrameIndex < targetFrames.length) ? targetFrames[targetFrameIndex] : null;

            BoneTransformsSnapshot to = (targetFrame != null)
                    ? new BoneTransformsSnapshot(
                    targetFrame.xRotation, targetFrame.yRotation, targetFrame.zRotation,
                    targetFrame.xPosition, targetFrame.yPosition, targetFrame.zPosition)
                    : new BoneTransformsSnapshot();

            AnimationFrame[] blendSequence = new AnimationFrame[blendDuration];
            for (int i = 0; i < blendDuration; i++) {
                double progress = (double) i / blendDuration;
                BoneTransformsSnapshot interp = from.interpolateTo(to, progress);
                blendSequence[i] = new AnimationFrame(
                        (float) interp.xRotation, (float) interp.yRotation, (float) interp.zRotation,
                        (float) interp.xTranslation, (float) interp.yTranslation, (float) interp.zTranslation
                );
            }

            blendedFrames.put(bone, blendSequence);
        }

        Map<BoneBlueprint, AnimationFrame[]> blueprintFrames = new HashMap<>();
        for (Map.Entry<Bone, AnimationFrame[]> entry : blendedFrames.entrySet()) {
            BoneBlueprint blueprint = entry.getKey().getBoneBlueprint();
            if (blueprint != null) {
                blueprintFrames.put(blueprint, entry.getValue());
            }
        }

        AnimationBlueprintFake blueprint = new AnimationBlueprintFake("blendTemp", blendDuration, LoopType.HOLD, blueprintFrames);
        return new Animation(blueprint, modeledEntity);
    }


    public Map<Bone, BoneTransformsSnapshot> captureCurrentAnimationPose() {
        Map<Bone, BoneTransformsSnapshot> pose = new HashMap<>();
        Optional<Animation> animationOptional = states.stream().findFirst();
        if (animationOptional.isEmpty()){
            MetadataHandler.PLUGIN.getLogger().warning("Could not blend animation because no animation is " +
                    "currently running.");
            return null;
        }
        Animation animation = animationOptional.get();
        int adjustedAnimationPosition = getAdjustedAnimationPosition(animation);
        animation.getAnimationFrames().forEach((bone, frames) -> {
            BoneTransformsSnapshot currentSnapshot;
            if (frames == null){
                currentSnapshot = new BoneTransformsSnapshot();
            }
            else{
                AnimationFrame currentFrame = frames[adjustedAnimationPosition];
                currentSnapshot = new BoneTransformsSnapshot(
                        currentFrame.xRotation, currentFrame.yRotation, currentFrame.zRotation,
                        currentFrame.xPosition, currentFrame.yPosition, currentFrame.zPosition
                );
            }
            pose.put(bone, currentSnapshot);
        });
        return pose;
    }



    public void stop() {
        states.clear();
        animationGracePeriod = false;
        overrideStates(idleAnimation);
    }

    public boolean hasAnimation(String animationName) {
        return animations.getAnimations().containsKey(animationName);
    }

    public void end() {
        if (clock != null) clock.cancel();
    }
}
