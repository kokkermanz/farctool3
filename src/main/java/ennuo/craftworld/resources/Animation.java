package ennuo.craftworld.resources;

import ennuo.craftworld.memory.Bytes;
import ennuo.craftworld.memory.Data;
import ennuo.craftworld.resources.structs.animation.AnimatedMorph;
import ennuo.craftworld.resources.structs.animation.AnimationBone;
import ennuo.craftworld.resources.structs.animation.Locator;
import java.util.ArrayList;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Animation {
    public int numFrames, FPS, loopStart;
    public int morphCount, boneCount, rotBonesAnimatedCount, posBonesAnimatedCount,
    scaledBonesAnimatedCount, morphsAnimatedCount;
    public int locatorKeysCount;
    public Vector4f posOffset, posScale;
    public boolean fat;
    
    public AnimationBone[] bones;
  
    public Locator[] locators;
    
    public short[] rotBonesAnimated;
    public short[] posBonesAnimated;
    public short[] scaledBonesAnimated;
    public short[] morphsAnimated;
    
    
    public ArrayList<Float> initialMorphs = new ArrayList<Float>();
    
    public AnimatedMorph[] morphs;
    
    public Animation(Data data) {
        if (data.revision > 0x2ae) 
            processLBP3(data);
        else processLBP1(data);
        
        System.out.println(String.format("numFrames = %d, FPS = %d, loopStart = %ds", numFrames, FPS, loopStart));
        
        System.out.println("counts: ");
        System.out.println("\tmorphs = " + morphCount);
        System.out.println("\tbones = " + boneCount);
        System.out.println("\trotBonesAnimated = " + rotBonesAnimatedCount);
        System.out.println("\tposBonesAnimated = " + posBonesAnimatedCount);
        System.out.println("\tscaledBonesAnimated = " + scaledBonesAnimatedCount);
        System.out.println("\tmorphsAnimated = " + morphsAnimatedCount);
        System.out.println("\tlocatorKeys = " + locatorKeysCount);
    }
    
    private void processLBP1(Data data) {
        bones = AnimationBone.array(data);
        boneCount = bones.length;
        numFrames = data.int32();
        FPS = data.int32();
        morphCount = data.int32();
        
        rotBonesAnimatedCount = data.int32();
        rotBonesAnimated = new short[rotBonesAnimatedCount];
        for (int i = 0; i < rotBonesAnimatedCount; ++i)
            rotBonesAnimated[i] = data.int16();
       
        posBonesAnimatedCount = data.int32();
        posBonesAnimated = new short[posBonesAnimatedCount];
        for (int i = 0; i < posBonesAnimatedCount; ++i)
            posBonesAnimated[i] = data.int16();
        
        scaledBonesAnimatedCount = data.int32();
        scaledBonesAnimated = new short[scaledBonesAnimatedCount];
        for (int i = 0; i < scaledBonesAnimatedCount; ++i)
            scaledBonesAnimated[i] = data.int16();
        
        morphsAnimatedCount = data.int32();
        morphsAnimated = new short[morphsAnimatedCount];
        for (int i = 0; i < morphsAnimatedCount; ++i)
            morphsAnimated[i] = data.int16();
        
        
        Vector4f[] rotation;
        Vector4f[] position;
        Vector4f[] scale;
    
        int rotationCount = data.int32();
        for (int i = 0; i < boneCount; ++i)
            bones[i].initialRotation = data.v4();
        rotation = new Vector4f[rotationCount - boneCount];
        for (int i = 0; i < rotationCount - boneCount; ++i)
            rotation[i] = data.v4();
        
        
        int positionCount = data.int32();
        for (int i = 0; i < boneCount; ++i)
            bones[i].initialPosition = data.v4();
        position = new Vector4f[positionCount - boneCount];
        for (int i = 0; i < positionCount - boneCount; ++i)
            position[i] = data.v4();
        
        int scaleCount = data.int32();
        for (int i = 0; i < boneCount; ++i)
            bones[i].initialScale = data.v4();
        scale = new Vector4f[scaleCount - boneCount];
        for (int i = 0; i < scaleCount - boneCount; ++i)
            scale[i] = data.v4();
        
        int morphC = data.int32();
        morphs = new AnimatedMorph[this.morphCount];
        for (int i = 0; i < morphCount; ++i) {
            morphs[i] = new AnimatedMorph(data.float32(), this.numFrames - 1);
            initialMorphs.add(morphs[i].value);
        }
        float[] morph = new float[morphC - morphCount];
        for (int i = 0; i < morphC - morphCount; ++i)
            morph[i] = data.float32();
       
        
        for (AnimationBone bone : bones) {
            bone.rotations = new Vector4f[numFrames - 1];
            bone.positions = new Vector4f[numFrames - 1];
            bone.scales = new Vector4f[numFrames - 1];
        }
        
        for (int i = 0; i < numFrames - 1; ++i) {
            for (int j = 0; j < rotBonesAnimated.length; ++j) {
                short bone = rotBonesAnimated[j];
                bones[bone].rotations[i] = rotation[(i * rotBonesAnimated.length) + j];
            }
        }
        
        for (int i = 0; i < numFrames - 1; ++i) {
            for (int j = 0; j < posBonesAnimated.length; ++j) {
                short bone = posBonesAnimated[j];
                bones[bone].positions[i] = position[(i * posBonesAnimated.length) + j];
            }
        }
        
        for (int i = 0; i < numFrames - 1; ++i) {
            for (int j = 0; j < scaledBonesAnimated.length; ++j) {
                short bone = scaledBonesAnimated[j];
                bones[bone].scales[i] = scale[(i * scaledBonesAnimated.length) + j];
            }
        }
        
        for (int i = 0; i < numFrames - 1; ++i) {
            for (int j = 0; j < morphsAnimated.length; ++j) {
                short m = morphsAnimated[j];
                morphs[m].isAnimated = true;
                morphs[m].values[i] = morph[(i * morphsAnimated.length) + j];
            }
        }
    }
    
    private void processLBP3(Data data) {
        numFrames = data.int16();
        FPS = data.int16();
        loopStart = data.int16();
        morphCount = data.int8();
        boneCount = data.int8();
        rotBonesAnimatedCount = data.int8();
        posBonesAnimatedCount = data.int8();
        scaledBonesAnimatedCount = data.int8();
        morphsAnimatedCount = data.int8();
        locatorKeysCount = data.int16();
        
        if (data.revision >= 0x3d9) {
            posOffset = data.v4();
            posScale = data.v4();
        
            fat = data.bool();
        }
                
        byte[] animData = data.bytes(data.int32());
        
        processAnimationDataLBP3(animData);
        
        System.out.println("dataSize = 0x" + Bytes.toHex(animData.length));
        
        locators = Locator.array(data);
    }
    
    private void processAnimationDataLBP3(byte[] buffer) {
        Data data = new Data(buffer, 0x3f9);
        
        bones = new AnimationBone[boneCount];
        for (int i = 0; i < boneCount; ++i)
            bones[i] = new AnimationBone(data);
        
        rotBonesAnimated = new short[rotBonesAnimatedCount];
        for (int i = 0; i < rotBonesAnimatedCount; ++i)
            rotBonesAnimated[i] = data.int8();
        
        posBonesAnimated = new short[posBonesAnimatedCount];
        for (int i = 0; i < posBonesAnimatedCount; ++i)
            posBonesAnimated[i] = data.int8();
        
        scaledBonesAnimated = new short[scaledBonesAnimatedCount];
        for (int i = 0; i < scaledBonesAnimatedCount; ++i)
            scaledBonesAnimated[i] = data.int8();
        
        morphsAnimated = new short[morphsAnimatedCount];
        for (int i = 0; i < morphsAnimatedCount; ++i)
            morphsAnimated[i] = data.int8();
        
        if ((rotBonesAnimatedCount + posBonesAnimatedCount + scaledBonesAnimatedCount + morphsAnimatedCount) % 2 != 0)
            data.int8(); 
        
        Vector4f[] rotation = new Vector4f[rotBonesAnimatedCount * (numFrames - 1)];
        Vector4f[] position = new Vector4f[posBonesAnimatedCount * (numFrames - 1)];
        Vector4f[] scale = new Vector4f[scaledBonesAnimatedCount * (numFrames - 1)];
        float[] morph = new float[morphsAnimatedCount * (numFrames - 1)];
        
        for (int i = 0; i < boneCount; ++i) {
            short x = data.int16();
            short y = data.int16();
            short z = data.int16();
            Vector3f rot = new Vector3f(Math.max(x / 32767.0f, -1.0f), Math.max(y / 32767.0f, -1.0f), Math.max(z / 32767.0f, -1.0f));
            
            bones[i].initialRotation = new Vector4f(rot.x, rot.y, rot.z, (float) Math.sqrt(1 - ((Math.pow(rot.x, 2)) + (Math.pow(rot.y, 2) + (Math.pow(rot.z, 2))))));   
        }
        
        
        for (int i = 0; i < rotation.length; ++i) {
            short x = data.int16();
            short y = data.int16();
            short z = data.int16();
            Vector3f rot = new Vector3f(Math.max(x / 32767.0f, -1.0f), Math.max(y / 32767.0f, -1.0f), Math.max(z / 32767.0f, -1.0f));
            rotation[i] = new Vector4f(rot.x, rot.y, rot.z, (float) Math.sqrt(1 - ((Math.pow(rot.x, 2)) + (Math.pow(rot.y, 2) + (Math.pow(rot.z, 2))))));   
        }
        
        for (int i = 0; i < boneCount; ++i)
            bones[i].initialPosition = new Vector4f(data.f16(), data.f16(), data.f16(), 1.0f);
        for (int i = 0; i < position.length; ++i)
           position[i] = new Vector4f(data.f16(), data.f16(), data.f16(), 1.0f);
        
        for (int i = 0; i < boneCount; ++i)
            bones[i].initialScale = new Vector4f(data.f16(), data.f16(), data.f16(), 1.0f);
        for (int i = 0; i < scale.length; ++i)
           scale[i] = new Vector4f(data.f16(), data.f16(), data.f16(), 1.0f);
        
        morphs = new AnimatedMorph[morphCount];
        for (int i = 0; i < morphCount; ++i) {
            morphs[i] = new AnimatedMorph(data.f16(), numFrames - 1);
            initialMorphs.add(morphs[i].value);
        }
        for (int i = 0; i < morph.length; ++i)
            morph[i] = data.f16();
            
        for (AnimationBone bone : bones) {
            bone.rotations = new Vector4f[numFrames - 1];
            bone.positions = new Vector4f[numFrames - 1];
            bone.scales = new Vector4f[numFrames - 1];
        }
        
        for (int i = 0; i < numFrames - 1; ++i) {
            for (int j = 0; j < rotBonesAnimated.length; ++j) {
                short bone = rotBonesAnimated[j];
                bones[bone].rotations[i] = rotation[(i * rotBonesAnimated.length) + j];
            }
        }
        
        for (int i = 0; i < numFrames - 1; ++i) {
            for (int j = 0; j < posBonesAnimated.length; ++j) {
                short bone = posBonesAnimated[j];
                bones[bone].positions[i] = position[(i * posBonesAnimated.length) + j];
            }
        }
        
        for (int i = 0; i < numFrames - 1; ++i) {
            for (int j = 0; j < scaledBonesAnimated.length; ++j) {
                short bone = scaledBonesAnimated[j];
                bones[bone].scales[i] = scale[(i * scaledBonesAnimated.length) + j];
            }
        }
        
        for (int i = 0; i < numFrames - 1; ++i) {
            for (int j = 0; j < morphsAnimated.length; ++j) {
                short m = morphsAnimated[j];
                morphs[m].isAnimated = true;
                morphs[m].values[i] = morph[(i * morphsAnimated.length) + j];
            }
        }
        
    }
    
    
    
}
