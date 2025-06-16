package com.magmaguy.freeminecraftmodels.customentity.core;

import com.magmaguy.freeminecraftmodels.config.DefaultConfig;
import com.magmaguy.freeminecraftmodels.dataconverter.BoneBlueprint;
import com.magmaguy.freeminecraftmodels.thirdparty.Floodgate;
import com.magmaguy.freeminecraftmodels.utils.VersionChecker;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Bone {
    @Getter
    private final BoneBlueprint boneBlueprint;
    @Getter
    private final List<Bone> boneChildren = new ArrayList<>();
    @Getter
    private final Bone parent;
    @Getter
    private final Skeleton skeleton;
    @Getter
    private final BoneTransforms boneTransforms;
    @Getter
    private Vector3f animationTranslation = new Vector3f();
    private int counter = 0;
    @Getter
    private Vector3f animationRotation = new Vector3f();

    public Bone(BoneBlueprint boneBlueprint, Bone parent, Skeleton skeleton) {
        this.boneBlueprint = boneBlueprint;
        this.parent = parent;
        this.skeleton = skeleton;
        this.boneTransforms = new BoneTransforms(this, parent);
        for (BoneBlueprint child : boneBlueprint.getBoneBlueprintChildren())
            boneChildren.add(new Bone(child, this, skeleton));
    }

    public void updateAnimationTranslation(float x, float y, float z) {
        animationTranslation = new Vector3f(x, y, z);
    }

    public void updateAnimationRotation(double x, double y, double z) {
        animationRotation = new Vector3f((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
    }

    public void updateAnimationRotationFromDirection(Vector vector) {

        final double _2PI = 2 * Math.PI;
        final double x = vector.getX();
        final double z = vector.getZ();
        float pitch;
        float yaw = 0;

        if (x == 0 && z == 0) {
            pitch = vector.getY() > 0 ? -90 : 90;
        }
        else{
            double theta = Math.atan2(-x, z);
            yaw = (float) Math.toDegrees((theta + _2PI) % _2PI);
            double x2 = NumberConversions.square(x);
            double z2 = NumberConversions.square(z);
            double xz = Math.sqrt(x2 + z2);
            pitch = (float) Math.toDegrees(Math.atan(-vector.getY() / xz));
        }
        animationRotation = new Vector3f((float) Math.toRadians(pitch), (float) Math.toRadians(yaw), 0.0f);
    }


    //Note that several optimizations might be possible here, but that syncing with a base entity is necessary.
    public void transform() {
        transform(true);
    }

    //Note that several optimizations might be possible here, but that syncing with a base entity is necessary.
    public void transform(boolean shouldHeadBeAnimated) {
        boneTransforms.transform(shouldHeadBeAnimated);
        boneChildren.forEach(bone -> bone.transform(shouldHeadBeAnimated));
        skeleton.getSkeletonWatchers().sendPackets(this);
    }

    public void generateDisplay() {
        boneTransforms.generateDisplay();
        boneChildren.forEach(Bone::generateDisplay);
    }

    public void setName(String name) {
        boneChildren.forEach(child -> child.setName(name));
    }

    public void setNameVisible(boolean visible) {
        boneChildren.forEach(child -> child.setNameVisible(visible));
    }

    public void getNametags(List<ArmorStand> nametags) {
        boneChildren.forEach(child -> child.getNametags(nametags));
    }

    public void remove() {
        if (boneTransforms.getPacketArmorStandEntity() != null) boneTransforms.getPacketArmorStandEntity().remove();
        if (boneTransforms.getPacketDisplayEntity() != null) boneTransforms.getPacketDisplayEntity().remove();
        boneChildren.forEach(Bone::remove);
    }

    protected void getAllChildren(HashMap<String, Bone> children) {
        boneChildren.forEach(child -> {
            children.put(child.getBoneBlueprint().getBoneName(), child);
            child.getAllChildren(children);
        });
    }

    public void sendUpdatePacket() {
        counter++;
        int reset = 20 * 60 * 2;
        if (counter > reset) {
            counter = 0;
            skeleton.getSkeletonWatchers().reset();
        }
        if (boneTransforms.getPacketArmorStandEntity() != null)
            boneTransforms.getPacketArmorStandEntity().sendLocationAndRotationPacket(
                    boneTransforms.getArmorStandTargetLocation(),
                    boneTransforms.getArmorStandEntityRotation());
        if (boneTransforms.getPacketDisplayEntity() != null) {
            boneTransforms.getPacketDisplayEntity().sendLocationAndRotationPacket(
                    boneTransforms.getDisplayEntityTargetLocation(),
                    boneTransforms.getDisplayEntityRotation());
        }
    }

    //VSD reset location for a bone
    public void resetLocation(){
        if (boneTransforms.getPacketArmorStandEntity() != null)
            boneTransforms.getPacketArmorStandEntity().sendLocationAndRotationPacket(
                    boneTransforms.getArmorStandTargetLocation(),
                    boneTransforms.getArmorStandEntityRotation());
        if (boneTransforms.getPacketDisplayEntity() != null) {
            boneTransforms.getPacketDisplayEntity().sendLocationAndRotationPacket(
                    boneTransforms.getDisplayEntityTargetLocation(),
                    boneTransforms.getDisplayEntityRotation());
        }
    }

    public void displayTo(Player player) {
        if (boneTransforms.getPacketArmorStandEntity() != null &&
                (!DefaultConfig.useDisplayEntitiesWhenPossible ||
                        Floodgate.isBedrock(player) ||
                        VersionChecker.serverVersionOlderThan(19, 4)))
            boneTransforms.getPacketArmorStandEntity().displayTo(player.getUniqueId());
        else if (boneTransforms.getPacketDisplayEntity() != null){
            //VSD ignore exception to avoid spamming
            try{
                boneTransforms.getPacketDisplayEntity().displayTo(player.getUniqueId());
            }
            catch (NullPointerException ignore){

            }
        }

    }

    public void hideFrom(UUID playerUUID) {
        if (boneTransforms.getPacketArmorStandEntity() != null)
            boneTransforms.getPacketArmorStandEntity().hideFrom(playerUUID);
        if (boneTransforms.getPacketDisplayEntity() != null)
            boneTransforms.getPacketDisplayEntity().hideFrom(playerUUID);
    }

    public void setHorseLeatherArmorColor(Color color) {
        if (boneTransforms.getPacketArmorStandEntity() != null)
            boneTransforms.getPacketArmorStandEntity().setHorseLeatherArmorColor(color);
        if (boneTransforms.getPacketDisplayEntity() != null)
            boneTransforms.getPacketDisplayEntity().setHorseLeatherArmorColor(color);
    }

    public void teleport() {
        boneTransforms.transform();
        boneChildren.forEach(Bone::transform);
        sendTeleportPacket();
        boneChildren.forEach(Bone::teleport);
    }

    //VSD Temporary workaround, modified private to public
    public void sendTeleportPacket() {
        counter++;
        int reset = 20 * 60 * 2;
        if (counter > reset) {
            counter = 0;
            skeleton.getSkeletonWatchers().reset();
        }
        if (boneTransforms.getPacketArmorStandEntity() != null) {
            boneTransforms.getPacketArmorStandEntity().teleport(boneTransforms.getArmorStandTargetLocation());
        }
        if (boneTransforms.getPacketDisplayEntity() != null) {
            boneTransforms.getPacketDisplayEntity().teleport(boneTransforms.getDisplayEntityTargetLocation());
        }
    }

    public BoneTransformsSnapshot getCurrentAnimationPose() {
        return new BoneTransformsSnapshot(
                this.animationRotation.get(0),
                this.animationRotation.get(1),
                this.animationRotation.get(2),
                this.animationTranslation.get(0),
                this.animationTranslation.get(1),
                this.animationTranslation.get(2)
        );
    }

    private float normalizeAngle(float angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }


    public BoneTransformsSnapshot getLivePose() {
        float[] rot = this.getBoneTransforms().getGlobalMatrix().getRotation();
        float[] pos = this.getBoneTransforms().getGlobalMatrix().getTranslation();

        return new BoneTransformsSnapshot(
                normalizeAngle(rot[0]),
                normalizeAngle(rot[1]),
                normalizeAngle(rot[2]),
                pos[0], pos[1], pos[2]
        );
    }

}
