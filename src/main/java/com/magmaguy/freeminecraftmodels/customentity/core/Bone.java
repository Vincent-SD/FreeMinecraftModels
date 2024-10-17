package com.magmaguy.freeminecraftmodels.customentity.core;

import com.magmaguy.freeminecraftmodels.config.DefaultConfig;
import com.magmaguy.freeminecraftmodels.dataconverter.BoneBlueprint;
import com.magmaguy.freeminecraftmodels.thirdparty.Floodgate;
import com.magmaguy.freeminecraftmodels.utils.VersionChecker;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
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

    //Note that several optimizations might be possible here, but that syncing with a base entity is necessary.
    public void transform() {
        boneTransforms.transform();
        boneChildren.forEach(Bone::transform);
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
        else if (boneTransforms.getPacketDisplayEntity() != null)
            boneTransforms.getPacketDisplayEntity().displayTo(player.getUniqueId());
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
}
