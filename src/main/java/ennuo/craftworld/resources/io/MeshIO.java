package ennuo.craftworld.resources.io;

import de.javagl.jgltf.impl.v2.Accessor;
import de.javagl.jgltf.impl.v2.Animation;
import de.javagl.jgltf.impl.v2.AnimationChannel;
import de.javagl.jgltf.impl.v2.AnimationChannelTarget;
import de.javagl.jgltf.impl.v2.AnimationSampler;
import de.javagl.jgltf.impl.v2.Asset;
import de.javagl.jgltf.impl.v2.Buffer;
import de.javagl.jgltf.impl.v2.BufferView;
import de.javagl.jgltf.impl.v2.GlTF;
import de.javagl.jgltf.impl.v2.Image;
import de.javagl.jgltf.impl.v2.Material;
import de.javagl.jgltf.impl.v2.MaterialNormalTextureInfo;
import de.javagl.jgltf.impl.v2.MaterialOcclusionTextureInfo;
import de.javagl.jgltf.impl.v2.MaterialPbrMetallicRoughness;
import de.javagl.jgltf.impl.v2.Mesh;
import de.javagl.jgltf.impl.v2.MeshPrimitive;
import de.javagl.jgltf.impl.v2.Node;
import de.javagl.jgltf.impl.v2.Sampler;
import de.javagl.jgltf.impl.v2.Scene;
import de.javagl.jgltf.impl.v2.Skin;
import de.javagl.jgltf.impl.v2.TextureInfo;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.io.v2.GltfAssetWriterV2;
import de.javagl.jgltf.model.io.v2.GltfReaderV2;
import ennuo.craftworld.memory.Bytes;
import ennuo.craftworld.memory.Output;
import ennuo.craftworld.memory.Resource;
import ennuo.craftworld.resources.GfxMaterial;
import ennuo.craftworld.resources.Texture;
import ennuo.craftworld.resources.structs.animation.AnimatedMorph;
import ennuo.craftworld.resources.structs.animation.AnimationBone;
import ennuo.craftworld.resources.structs.gfxmaterial.Box;
import ennuo.craftworld.resources.structs.gfxmaterial.Wire;
import ennuo.craftworld.resources.structs.mesh.Bone;
import ennuo.craftworld.types.FileEntry;
import ennuo.toolkit.utilities.Globals;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class MeshIO {
    public static class OBJ {
        public static void export(String path, ennuo.craftworld.resources.Mesh mesh) { export(path, mesh, 0); }
        public static void export(String path, ennuo.craftworld.resources.Mesh mesh, int channel) {
            StringBuilder builder = new StringBuilder((mesh.vertices.length * 82) + (mesh.uvCount * 42) + (mesh.faces.length * 40));
            for (int j = 0; j < mesh.vertices.length; ++j)
                builder.append("v " + mesh.vertices[j].x + " " + mesh.vertices[j].y + " " + mesh.vertices[j].z + '\n');
            for (int j = 0; j < mesh.vertices.length; ++j)
                builder.append("vn " + mesh.weights[j].normal.x + " " + mesh.weights[j].normal.y + " " + mesh.weights[j].normal.z + '\n');
            for (int i = 0; i < mesh.uvCount; ++i)
                builder.append("vt " + mesh.attributes[i][channel].x + " " + (1.0f - mesh.attributes[i][channel].y) + '\n');
            for (int i = -1, j = 1; i < mesh.faces.length; ++i, ++j) {
                if (i == -1 || mesh.faces[i] == -1) {
                    String str = "f ";
                    str += (mesh.faces[i + 1] + 1) + "/" + (mesh.faces[i + 1] + 1) + "/" + (mesh.faces[i + 1] + 1) + " ";
                    str += (mesh.faces[i + 2] + 1) + "/" + (mesh.faces[i + 2] + 1) + "/" + (mesh.faces[i + 2] + 1) + " ";
                    str += (mesh.faces[i + 3] + 1) + "/" + (mesh.faces[i + 3] + 1) + "/" + (mesh.faces[i + 3] + 1) + '\n';

                    builder.append(str);
                    i += 3;
                    j = 0;
                } else {
                    if ((j & 1) == 1) {
                        String str = "f ";
                        str += (mesh.faces[i - 2] + 1) + "/" + (mesh.faces[i - 2] + 1) + "/" + (mesh.faces[i - 2] + 1) + " ";
                        str += (mesh.faces[i] + 1) + "/" + (mesh.faces[i] + 1) + "/" + (mesh.faces[i] + 1) + " ";
                        str += (mesh.faces[i - 1] + 1) + "/" + (mesh.faces[i - 1] + 1) + "/" + (mesh.faces[i - 1] + 1) + '\n';
                        builder.append(str);
                    } else {
                        String str = "f ";
                        str += (mesh.faces[i - 2] + 1) + "/" + (mesh.faces[i - 2] + 1) + "/" + (mesh.faces[i - 2] + 1) + " ";
                        str += (mesh.faces[i - 1] + 1) + "/" + (mesh.faces[i - 1] + 1) + "/" + (mesh.faces[i - 1] + 1) + " ";
                        str += (mesh.faces[i] + 1) + "/" + (mesh.faces[i] + 1) + "/" + (mesh.faces[i] + 1) + '\n';
                        builder.append(str);
                    }
                }
            }
            FileIO.write(builder.toString().getBytes(), path);
        }
    }
    
    public static class GLB {
        GlTF gltf = new GlTF();
        byte[] buffer = null;
        
        HashMap<String, Integer> bufferViews = new HashMap<String, Integer>();
        HashMap<String, Integer> materials = new HashMap<String, Integer>();
        HashMap<String, Integer> textures = new HashMap<String, Integer>();
        int accessorCount = 0;
        
        public static GLB FromAnimation(ennuo.craftworld.resources.Animation animation, ennuo.craftworld.resources.Mesh mesh) {
            GLB glb;
            
            if (mesh == null) {
                glb = new GLB();
                byte[] dataBuffer = glb.getBufferFromAnimation(animation);
                Buffer buffer = new Buffer();
                buffer.setByteLength(dataBuffer.length);
                glb.gltf.addBuffers(buffer);
                glb.buffer = dataBuffer;
                
                glb.setAsset("CRAFTWORLD", "2.0");


                Node root = new Node();
                root.setName("Armature");
                root.addChildren(1);
                root.setSkin(0);

                glb.gltf.addNodes(root);

                for (AnimationBone bone : animation.bones) {
                    Node child = new Node();
                    child.setTranslation(new float[] { bone.initialPosition.x, bone.initialPosition.y, bone.initialPosition.z });
                    child.setRotation(new float[] { bone.initialRotation.x, bone.initialRotation.y, bone.initialRotation.z, bone.initialRotation.w });
                    child.setScale(new float[] { bone.initialScale.x, bone.initialScale.y, bone.initialScale.z });
                    glb.gltf.addNodes(child);
                }

                Skin skin = new Skin();

                for (int i = 0; i < animation.bones.length; ++i) {
                    skin.addJoints(i + 1);
                    if (animation.bones[i].parent != -1)
                        glb.gltf.getNodes().get(animation.bones[i].parent + 1).addChildren(i + 1);
                }

                glb.gltf.addSkins(skin);
            } else {
                glb = GLB.FromMesh(mesh);
                byte[] dataBuffer = glb.getBufferFromAnimation(animation);
                glb.buffer = Bytes.Combine(glb.buffer, dataBuffer);
                glb.gltf.getBuffers().get(0).setByteLength(glb.buffer.length);
                for (AnimationBone bone : animation.bones) {
                    System.out.println(bone.animHash);
                    System.out.println(mesh.getBoneName(bone.animHash));
                    Node node = glb.getNode(mesh.getBoneName(bone.animHash));
                    node.setTranslation(new float[] { bone.initialPosition.x, bone.initialPosition.y, bone.initialPosition.z });
                    node.setRotation(new float[] { bone.initialRotation.x, bone.initialRotation.y, bone.initialRotation.z, bone.initialRotation.w });
                    node.setScale(new float[] { bone.initialScale.x, bone.initialScale.y, bone.initialScale.z });
                }
            }
            
            if (animation.morphCount != 0)
                for (Node node : glb.gltf.getNodes()) {
                    if (node.getMesh() != null)
                        glb.gltf.getMeshes().get(node.getMesh()).setWeights(animation.initialMorphs);
                }
            
            int time = glb.createAccessor("TIME", 5126, "SCALAR", 0, animation.numFrames - 1);
            
            Animation glAnim = new Animation();
            
            int samplerIndex = 0;
            
            if (animation.morphsAnimatedCount != 0) {
                for (int i = 0; i < glb.gltf.getNodes().size(); ++i) {
                    Node node = glb.gltf.getNodes().get(i);
                    if (node.getMesh() == null) continue;
                    AnimationChannel channel = new AnimationChannel();
                    AnimationChannelTarget target = new AnimationChannelTarget();
                    target.setNode(i);
                    target.setPath("weights");
                    channel.setTarget(target);
                    AnimationSampler sampler = new AnimationSampler();
                    sampler.setInput(time);
                    sampler.setInterpolation("LINEAR");
                    sampler.setOutput(glb.createAccessor("MORPHS_ANIMATED", 5126, "SCALAR", 0, animation.morphCount * (animation.numFrames - 1)));
                    channel.setSampler(samplerIndex);
                    samplerIndex++;
                    glAnim.addChannels(channel);
                    glAnim.addSamplers(sampler);
                }
            }
            
            for (short rot : animation.posBonesAnimated) {
                AnimationChannel channel = new AnimationChannel();
                AnimationChannelTarget target = new AnimationChannelTarget();
                if (mesh != null)
                    target.setNode(glb.getNodeIndex(mesh.getBoneName(animation.bones[rot].animHash)));
                else target.setNode(rot + 1);
                target.setPath("translation");
                channel.setTarget(target);
                AnimationSampler sampler = new AnimationSampler();
                sampler.setInput(time);
                sampler.setInterpolation("LINEAR");
                sampler.setOutput(glb.createAccessor("BONE_TRANSLATION_" + animation.bones[rot].animHash, 5126, "VEC3", 0, animation.numFrames - 1));
                channel.setSampler(samplerIndex);
                samplerIndex++;
                glAnim.addChannels(channel);
                glAnim.addSamplers(sampler);
            }
            
            for (short rot : animation.scaledBonesAnimated) {
                AnimationChannel channel = new AnimationChannel();
                AnimationChannelTarget target = new AnimationChannelTarget();
                if (mesh != null)
                    target.setNode(glb.getNodeIndex(mesh.getBoneName(animation.bones[rot].animHash)));
                else target.setNode(rot + 1);
                target.setPath("scale");
                channel.setTarget(target);
                AnimationSampler sampler = new AnimationSampler();
                sampler.setInput(time);
                sampler.setInterpolation("LINEAR");
                sampler.setOutput(glb.createAccessor("BONE_SCALE_" + animation.bones[rot].animHash, 5126, "VEC3", 0, animation.numFrames - 1));
                channel.setSampler(samplerIndex);
                samplerIndex++;
                glAnim.addChannels(channel);
                glAnim.addSamplers(sampler);
            }
            
            for (short rot : animation.rotBonesAnimated) {
                AnimationChannel channel = new AnimationChannel();
                AnimationChannelTarget target = new AnimationChannelTarget();
                if (mesh != null)
                    target.setNode(glb.getNodeIndex(mesh.getBoneName(animation.bones[rot].animHash)));
                else target.setNode(rot + 1);
                target.setPath("rotation");
                channel.setTarget(target);
                AnimationSampler sampler = new AnimationSampler();
                sampler.setInput(time);
                sampler.setInterpolation("LINEAR");
                sampler.setOutput(glb.createAccessor("BONE_ROTATION_" + animation.bones[rot].animHash, 5126, "VEC4", 0, animation.numFrames - 1));
                channel.setSampler(samplerIndex);
                samplerIndex++;
                glAnim.addChannels(channel);
                glAnim.addSamplers(sampler);
            }
            
            glb.gltf.addAnimations(glAnim);
            
            
            
            
            return glb;
        }
       
        public static GLB FromMesh(ennuo.craftworld.resources.Mesh mesh) {
            GLB glb = new GLB();
            
            
            byte[] dataBuffer = glb.getBufferFromMesh(mesh);
            Buffer buffer = new Buffer();
            buffer.setByteLength(dataBuffer.length);
            glb.gltf.addBuffers(buffer);
            glb.buffer = dataBuffer;
            
            glb.setAsset("CRAFTWORLD", "2.0");
           
            ennuo.craftworld.resources.structs.mesh.MeshPrimitive[][] subMeshes = mesh.getSubmeshes();
            for (int m = 0; m < subMeshes.length; ++m) {
                Mesh glMesh = new Mesh();
                if (mesh.morphs != null && mesh.morphs.length != 0) {
                    HashMap<String, Object> extras = new HashMap<String, Object>();
                    String[] morphs = new String[mesh.morphs.length];
                    for (int i = 0; i < morphs.length; ++i)
                        morphs[i] = mesh.morphNames[i];
                    extras.put("targetNames", morphs);
                    glMesh.setExtras(extras);
                }
                for (int i = 0; i < subMeshes[m].length; ++i) {
                    ennuo.craftworld.resources.structs.mesh.MeshPrimitive primitive = subMeshes[m][i];
                    MeshPrimitive glPrimitive = new MeshPrimitive();
                    
                    glPrimitive.addAttributes("POSITION", 
                            glb.createAccessor(
                                    "VERTICES", 
                                    5126, 
                                    "VEC3", 
                                    primitive.minVert * 0xC, 
                                    primitive.maxVert - primitive.minVert + 1)
                    );

                    glPrimitive.addAttributes("NORMAL", 
                            glb.createAccessor(
                                    "NORMAL", 
                                    5126, 
                                    "VEC3", 
                                    primitive.minVert * 0xC, 
                                    primitive.maxVert - primitive.minVert + 1)
                    );

                    for (int j = 0; j < mesh.attributeCount; ++j) {
                        glPrimitive.addAttributes("TEXCOORD_" + j, 
                                glb.createAccessor(
                                        "TEXCOORD_" + j, 
                                        5126, 
                                        "VEC2", 
                                        primitive.minVert * 0x8, 
                                        primitive.maxVert - primitive.minVert + 1)
                        );
                    }

                    if (mesh.morphs != null) {
                        for (int j = 0; j < mesh.morphs.length; ++j) {
                            HashMap<String, Integer> target = new HashMap<String, Integer>();
                            target.put("POSITION", glb.createAccessor(
                                    "MORPH_" + j,
                                    5126,
                                    "VEC3",
                                    primitive.minVert * 0xC, 
                                    primitive.maxVert - primitive.minVert + 1)
                            );
                            target.put("NORMAL", glb.createAccessor(
                                    "MORPH_NORMAL_" + j,
                                    5126,
                                    "VEC3",
                                    primitive.minVert * 0xC, 
                                    primitive.maxVert - primitive.minVert + 1)
                            );
                            glPrimitive.addTargets(target);
                        }
                    }

                    glPrimitive.addAttributes("JOINTS_0", 
                            glb.createAccessor(
                                    "JOINTS", 
                                    5121, 
                                    "VEC4", 
                                    primitive.minVert * 0x4, 
                                    primitive.maxVert - primitive.minVert + 1
                            )
                    );

                   glPrimitive.addAttributes("WEIGHTS_0", 
                            glb.createAccessor(
                                    "WEIGHTS", 
                                    5126, 
                                    "VEC4", 
                                    primitive.minVert * 0x10, 
                                    primitive.maxVert - primitive.minVert + 1
                            )
                    );

                    glPrimitive.setIndices(
                            glb.createAccessor(
                                    "INDICES_" + m + "_" + i, 
                                    5123, 
                                    "SCALAR", 
                                    0, 
                                    glb.gltf.getBufferViews().get(glb.getBufferView("INDICES_" + m + "_" + i)).getByteLength() / 2)
                    );

                    String materialName = "DIFFUSE";
                    if (primitive.material != null) {
                        FileEntry entry = Globals.findEntry(primitive.material);
                        if (entry != null) {
                            materialName = Paths.get(entry.path).getFileName().toString().replaceFirst("[.][^.]+$", "");
                            byte[] data = Globals.extractFile(entry.hash);
                            if (data != null) {
                                Resource res = new Resource(data);
                                res.decompress(true);
                                glPrimitive.setMaterial(glb.createMaterial(materialName, new GfxMaterial(res)));   
                            }
                            else glPrimitive.setMaterial(glb.createMaterial(materialName));
                        }
                        else  {
                            materialName = primitive.material.toString();
                            glPrimitive.setMaterial(glb.createMaterial(materialName));
                        }
                    }

                    glPrimitive.setMode(4);

                    glMesh.addPrimitives(glPrimitive);
                }
                
                glb.gltf.addMeshes(glMesh);
            }
            
            Node root = new Node();
            root.setName(mesh.name);
            
            glb.createSkeleton(mesh);
            Skin skin = new Skin();
            for (Bone bone : mesh.bones) {
                int index = glb.getNodeIndex(bone.name);
                skin.addJoints(index);
                if (bone.parent == -1 || mesh.bones[bone.parent] == bone)
                    root.addChildren(index);
            }
            skin.setInverseBindMatrices(glb.createAccessor("MATRIX", 5126, "MAT4", 0, mesh.bones.length));
            glb.gltf.addSkins(skin);
            
            for (int i = 0; i < subMeshes.length; ++i) {
                Node child = new Node();
                child.setMesh(i);
                child.setSkin(0);
                glb.gltf.addNodes(child);
                root.addChildren(glb.gltf.getNodes().size() - 1);
            }
            
            glb.gltf.addNodes(root);
            
            glb.gltf.setScene(0);
            
            Scene scene = new Scene();
            scene.setName("Scene");
            scene.addNodes(glb.gltf.getNodes().size() - 1);
            glb.gltf.addScenes(scene);

            return glb;
        }
        
        private int addTexture(String name, byte[] buffer) {
            if (getBufferView("TEXTURE_" + name) != -1) return this.textures.get(name);
            Image image = new Image();
            image.setBufferView(createBufferView("TEXTURE_" + name, 0, buffer.length));
            image.setMimeType("image/png");
            image.setName(name);
            this.buffer = Bytes.Combine(this.buffer, buffer);
            this.gltf.getBuffers().get(0).setByteLength(this.buffer.length);
            de.javagl.jgltf.impl.v2.Texture texture = new de.javagl.jgltf.impl.v2.Texture();
            this.gltf.addImages(image);
            texture.setSource(this.gltf.getImages().size() - 1);
            texture.setName(name);
            this.gltf.addTextures(texture);
            int index = this.gltf.getTextures().size() - 1;
            this.textures.put(name, index);
            return index;
        }
        
        private void createSkeleton(ennuo.craftworld.resources.Mesh mesh) {
            if (mesh.bones.length == 0) return;
            for (Bone bone : mesh.bones) {
                if (bone.parent == -1 || mesh.bones[bone.parent] == bone)
                    createChildren(mesh, bone);
            }
        }
        
        private Matrix4f getMatrix(float[] f) {
            return new Matrix4f(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10], f[11], f[12], f[13], f[14], f[15]);
        }
        
        private int createChildren(ennuo.craftworld.resources.Mesh mesh, Bone bone) {
            Matrix4f transform;
            if (bone.parent == -1 || mesh.bones[bone.parent] == bone)
                transform = getMatrix(bone.skinPoseMatrix);
            else {
                transform = getMatrix(mesh.bones[bone.parent].skinPoseMatrix)
                        .invert()
                        .mul(getMatrix(bone.skinPoseMatrix));
            }
            
            int index = createNode(bone.name, transform);
            Node root = this.gltf.getNodes().get(index);
            Bone[] children = mesh.getBoneChildren(bone);
            if (children.length == 0) return index;
            for (Bone child : children)
                root.addChildren(createChildren(
                        mesh, 
                        child
                ));
            return index;
        }
        
        private int createNode(String name, Matrix4f transform) {
            Node node = new Node();
            node.setName(name);
            
            Vector3f translation = new Vector3f();
            transform.getTranslation(translation);
            node.setTranslation(new float[] { translation.x, translation.y, translation.z });
            
            Quaternionf rotation = new Quaternionf().setFromUnnormalized(transform);
            
            node.setRotation(new float[] { rotation.x, rotation.y, rotation.z, rotation.w });
            
            Vector3f scale = new Vector3f();
            transform.getScale(scale);
            node.setScale(new float[] { scale.x, scale.y, scale.z });
            
            this.gltf.addNodes(node);
            return this.gltf.getNodes().size() - 1;
        }
        
        private Node getNode(int index) {
            List<Node> nodes = this.gltf.getNodes();
            if (nodes.size() >= index) return null;
            return nodes.get(index);
        }
        
        
        private int getBoneIndex(int node) {
            List<Integer> joints = this.gltf.getSkins().get(0).getJoints();
            for (int i = 0; i < joints.size(); ++i)
                if (joints.get(i) == node)
                    return i;
            return -1;
        }
        
        private int getNodeIndex(String name) {
            List<Node> nodes = this.gltf.getNodes();
            for (int i = 0; i < nodes.size(); ++i)
                if (nodes.get(i).getName().equals(name))
                    return i;
            return -1;
        }
        
        private Node getNode(String name) {
            List<Node> nodes = this.gltf.getNodes();
            for (Node node : nodes)
                if (node.getName().equals(name))
                    return node;
            return null;
        }
        
        private int createMaterial(String name, GfxMaterial gmat) {
            if (this.materials.containsKey(name))
                return this.materials.get(name);
            
            Material material = new Material();
            material.setName(name);
            
            material.setDoubleSided(true);
            
            MaterialPbrMetallicRoughness pbr = new MaterialPbrMetallicRoughness();
            
            boolean foundDiffuse = false;
            boolean foundBump = false;
            int outputBox = gmat.getOutputBox();
            for (int i = 0; i < gmat.boxes.length; ++i) {
                if (foundBump && foundDiffuse) break;
                Box box = gmat.boxes[i];
                /*
                
                DIFFUSE : 0,
                SPECULAR: 2,
                BUMP: 3,
                GLOW: 4,
                REFLECTION: 6
                */
                
                if (box.type == Box.BoxType.TEXTURE_SAMPLE) {
                    float[] textureScale = new float[] { Float.intBitsToFloat((int) box.params[0]), Float.intBitsToFloat((int) box.params[1]) };
                    float[] textureOffset = new float[] { Float.intBitsToFloat((int) box.params[2]), Float.intBitsToFloat((int) box.params[3]) };
                    int channel = (int) box.params[4];
                    int textureIndex = (int) box.params[5];
                    FileEntry entry = Globals.findEntry(gmat.textures[textureIndex]);
                    if (entry == null) continue;
                    byte[] texture = gmat.extractTexture(textureIndex);
                    if (texture == null) continue;
                    int source = addTexture(Paths.get(entry.path).getFileName().toString().replaceFirst("[.][^.]+$", ""), texture);
                    
                    ByteArrayInputStream png = new ByteArrayInputStream(texture);
                    BufferedImage image = null;
                    try {
                        image = ImageIO.read(png);
                    } catch (IOException ex) {
                        Logger.getLogger(MeshIO.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    HashMap<String, float[]> transforms = new HashMap<String, float[]>();
                    transforms.put("offset", textureOffset);
                    transforms.put("scale", textureScale);
                    
                    Wire wire = gmat.findWireFrom(i);
                    while (wire.boxTo != outputBox)
                        wire = gmat.findWireFrom(wire.boxTo);
                    
                    TextureInfo textureInfo = new TextureInfo();
                    textureInfo.addExtensions("KHR_texture_transform", transforms);
                    textureInfo.setTexCoord(channel);
                    textureInfo.setIndex(source);
                    switch (wire.portTo) {
                        case 0:                     
                            if (entry.path.contains("dirt")) {
                                MaterialOcclusionTextureInfo occInfo = new MaterialOcclusionTextureInfo();
                                occInfo.addExtensions("KHR_texture_transform", transforms);
                                occInfo.setIndex(source);
                                occInfo.setTexCoord(channel);
                                material.setOcclusionTexture(occInfo);
                                continue;
                            }
                            if (foundDiffuse) continue;
                            System.out.println(String.format("%s:%d", Paths.get(entry.path).getFileName().toString().replaceFirst("[.][^.]+$", ""), image.getTransparency()));
                            if (name.toLowerCase().contains("decal"))
                                material.setAlphaMode("BLEND");
                            foundDiffuse = true;
                            pbr.setBaseColorTexture(textureInfo);
                            if (material.getExtensions() != null && material.getExtensions().containsKey("KHR_materials_pbrSpecularGlossiness")) {
                                HashMap<String, Object> map = (HashMap<String, Object>) material.getExtensions().get("KHR_materials_pbrSpecularGlossiness");  
                                if (!map.containsKey("diffuseTexture"))
                                    map.put("diffuseTexture", textureInfo);
                            } 
                            continue;
                        case 2:
                            /*
                            HashMap<String, TextureInfo> extension = new HashMap<String, TextureInfo>();
                            extension.put("specularGlossinessTexture", textureInfo);
                            material.addExtensions("KHR_materials_pbrSpecularGlossiness", extension);
                            */
                            continue;
                        case 3:
                            if (foundBump) continue;
                            foundBump = true;
                            
                            if (image != null) {
                                for (int x = 0; x < image.getWidth(); ++x) {
                                    for (int y = 0; y < image.getHeight(); ++y) {
                                        Color c = new Color(image.getRGB(x, y), true);
                                        
                                        Color output = new Color(c.getAlpha(), c.getGreen(), 255, 255);
                                       
                                        image.setRGB(x, y, output.getRGB());
                                    }
                                }
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                try {
                                    ImageIO.write(image, "png", baos);
                                } catch (IOException ex) {
                                    Logger.getLogger(GfxMaterial.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                
                                source = addTexture(Paths.get(entry.path).getFileName().toString().replaceFirst("[.][^.]+$", "") + "_converted", baos.toByteArray());
                            }
                            
                            MaterialNormalTextureInfo normal = new MaterialNormalTextureInfo();
                            normal.addExtensions("KHR_texture_transform", transforms);
                            normal.setIndex(source);
                            normal.setTexCoord(channel);
                            material.setNormalTexture(normal);
                            continue;

                    }
                } else if (box.type == Box.BoxType.COLOR) {
                    Wire wire = gmat.findWireFrom(i);
                    if (wire.boxTo == outputBox) {
                        float[] color = new float[] { Float.intBitsToFloat((int) box.params[0]) / 255f, Float.intBitsToFloat((int) box.params[1]) / 255f, Float.intBitsToFloat((int) box.params[2]) / 255f, Float.intBitsToFloat((int) box.params[3]) / 255f};
                        if (wire.portTo == 0) {
                            if (material.getExtensions() != null && material.getExtensions().containsKey("KHR_materials_pbrSpecularGlossiness")) {
                                HashMap<String, Object> map = (HashMap<String, Object>) material.getExtensions().get("KHR_materials_pbrSpecularGlossiness");  
                                if (!map.containsKey("diffuseFactor"))
                                    map.put("diffuseFactor", color);
                            } 
                            foundDiffuse = true;
                            pbr.setBaseColorFactor(color);
                        } else if (wire.portTo == 2) {
                            HashMap<String, Object> extension = new HashMap<String, Object>();
                            extension.put("specularFactor", new float[] { color[0], color[1], color[2] });
                            if (pbr.getBaseColorFactor() != null)
                                extension.put("diffuseFactor", pbr.getBaseColorFactor());
                            if (pbr.getBaseColorTexture() != null)
                                extension.put("diffuseTexture", pbr.getBaseColorTexture());
                            material.addExtensions("KHR_materials_pbrSpecularGlossiness", extension);
                        }
                    }
                }
            }
            
            if (!foundDiffuse)
                pbr.setBaseColorFactor(new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
            
            material.setPbrMetallicRoughness(pbr);
            
            this.gltf.addMaterials(material);
            int index = this.materials.size();
            this.materials.put(name, index);
            return index;
        }
        
        private int createMaterial(String name) {
            if (this.materials.containsKey(name))
                return this.materials.get(name);
            
            Material material = new Material();
            material.setName(name);
            MaterialPbrMetallicRoughness pbr = new MaterialPbrMetallicRoughness();
            pbr.setBaseColorFactor(new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
            material.setPbrMetallicRoughness(pbr);
            
            this.gltf.addMaterials(material);
            int index = this.materials.size();
            this.materials.put(name, index);
            return index; 
        }
        
        private void setAsset(String generator, String version) {
            Asset asset = new Asset();
            asset.setGenerator(generator);
            asset.setVersion(version);
            this.gltf.setAsset(asset);
        }
        
        private int createAccessor(String bufferView, int componentType, String type, int offset, int count) {
            Accessor accessor = new Accessor();
            accessor.setBufferView(getBufferView(bufferView));
            accessor.setByteOffset(offset);
            accessor.setComponentType(componentType);
            accessor.setType(type);
            accessor.setCount(count);
            this.accessorCount++;
            this.gltf.addAccessors(accessor);
            return this.accessorCount - 1;
        }
        
        private int createBufferView(String name, int offset, int length) {
            if (this.buffer != null)
                offset += this.buffer.length;
            BufferView view = new BufferView();
            view.setBuffer(0);
            view.setByteOffset(offset);
            view.setByteLength(length);
            this.gltf.addBufferViews(view);
            int index = this.bufferViews.size();
            this.bufferViews.put(name, index);
            return index;
        }
        
        public int getBufferView(String name) {
            if (this.bufferViews.containsKey(name))
                return this.bufferViews.get(name);
            return -1;
        }
        
        private byte[] getBufferFromAnimation(ennuo.craftworld.resources.Animation animation) {
            float timestep = 1.0f / ((float) animation.FPS);
            
            Output output = new Output(animation.numFrames * animation.FPS * animation.boneCount + (animation.posBonesAnimatedCount + animation.rotBonesAnimatedCount + animation.scaledBonesAnimatedCount) * 0x50 + 0xFF0);
            
            float step = 0.0f;
            for (int i = 0; i < animation.numFrames - 1; ++i, step += timestep)
                output.float32le(step);
            createBufferView("TIME", 0, output.offset);
            for (int i = 0; i < animation.bones.length; ++i) {
                AnimationBone bone = animation.bones[i];
                
                if (bone.positions[0] != null) {
                    int posStart = output.offset;
                    for (Vector4f pos : bone.positions) {
                        output.float32le(pos.x);
                        output.float32le(pos.y);
                        output.float32le(pos.z);
                    }
                    createBufferView("BONE_TRANSLATION_" + String.valueOf(bone.animHash), posStart, output.offset - posStart);
                }
                
                if (bone.rotations[0] != null) {
                    int rotStart = output.offset;
                    for (Vector4f rot : bone.rotations) {
                        output.float32le(rot.x);
                        output.float32le(rot.y);
                        output.float32le(rot.z);
                        output.float32le(rot.w);
                    }
                    createBufferView("BONE_ROTATION_" + String.valueOf(bone.animHash), rotStart, output.offset - rotStart);
                }
                
                if (bone.scales[0] != null) {
                    int scaleStart = output.offset;
                    for (Vector4f scale : bone.scales) {
                        output.float32le(scale.x);
                        output.float32le(scale.y);
                        output.float32le(scale.z);
                    }
                    createBufferView("BONE_SCALE_" + String.valueOf(bone.animHash), scaleStart, output.offset - scaleStart);
                }
            }
            
            
            if (animation.morphsAnimatedCount != 0) {
                int morphStart = output.offset;
                for (int i = 0; i < animation.numFrames - 1; ++i) {
                    for (int j = 0; j < animation.morphCount; ++j)
                        output.float32le(animation.morphs[j].getValueAtFrame(i));
                }
                createBufferView("MORPHS_ANIMATED",  morphStart, output.offset - morphStart);
            }
            
            output.shrinkToFit();
            return output.buffer;
            
        }
        
        private byte[] getBufferFromMesh(ennuo.craftworld.resources.Mesh mesh) {
            int morphSize = (mesh.morphs == null) ? 0 : mesh.morphs.length;
            Output output = new Output( (mesh.vertices.length * 0x40) + (mesh.triangulate().length * 8) + (mesh.attributeCount * mesh.uvCount * 8) + (morphSize * mesh.vertices.length * 0x18) + (mesh.bones.length * 0x40));
            for (Vector3f vertex : mesh.vertices) {
                output.float32le(vertex.x);
                output.float32le(vertex.y);
                output.float32le(vertex.z);
            }
            createBufferView("VERTICES", 0, output.offset);
            
            ennuo.craftworld.resources.structs.mesh.MeshPrimitive[][] subMeshes = mesh.getSubmeshes();
            for (int i = 0; i < subMeshes.length; ++i) {
                for (int j = 0; j < subMeshes[i].length; ++j) {
                    int triangleStart = output.offset;
                    ennuo.craftworld.resources.structs.mesh.MeshPrimitive primitive
                            = subMeshes[i][j];
                    short[] triangles = mesh.triangulate(primitive);
                    primitive.minVert = getMin(triangles);
                    primitive.maxVert = getMax(triangles);
                    for (short triangle : triangles)
                        output.int16le((short) (triangle - primitive.minVert));
                    createBufferView("INDICES_" + String.valueOf(i) + "_" + String.valueOf(j), triangleStart, output.offset - triangleStart);
                }
            }
            
            int normalStart = output.offset;
            for (int i = 0; i < mesh.weights.length; ++i) {
                output.float32le(mesh.weights[i].normal.x);
                output.float32le(mesh.weights[i].normal.y);
                output.float32le(mesh.weights[i].normal.z);
            }
            createBufferView("NORMAL", normalStart, output.offset - normalStart);
            for (int i = 0; i < mesh.attributeCount; ++i) {
                int uvStart = output.offset;
                for (int j = 0; j < mesh.uvCount; ++j) {
                    output.float32le(mesh.attributes[j][i].x);
                    output.float32le(mesh.attributes[j][i].y);
                }
                createBufferView("TEXCOORD_" + String.valueOf(i), uvStart, output.offset - uvStart);
            }
            if (mesh.morphs != null) {
                for (int i = 0; i < mesh.morphs.length; ++i) {
                    int morphStart = output.offset;
                    for (int j = 0; j < mesh.vertices.length; ++j) {
                        output.float32le(mesh.morphs[i].vertices[j].x);
                        output.float32le(mesh.morphs[i].vertices[j].y);
                        output.float32le(mesh.morphs[i].vertices[j].z);
                    }
                    createBufferView("MORPH_" + String.valueOf(i), morphStart, output.offset - morphStart);
                }
                
                for (int i = 0; i < mesh.morphs.length; ++i) {
                    int morphStart = output.offset;
                    for (int j = 0; j < mesh.vertices.length; ++j) {
                        output.float32le(mesh.morphs[i].normals[j].x  - mesh.weights[j].normal.x);
                        output.float32le(mesh.morphs[i].normals[j].y  - mesh.weights[j].normal.y);
                        output.float32le(mesh.morphs[i].normals[j].z - mesh.weights[j].normal.z);
                    }
                    createBufferView("MORPH_NORMAL_" + String.valueOf(i), morphStart, output.offset - morphStart);
                }
            }

            int matrixStart = output.offset;
            for (int i = 0; i < mesh.bones.length; ++i)
                for (int j = 0; j < 16; ++j)
                    output.float32le(mesh.bones[i].invSkinPoseMatrix[j]);
            createBufferView("MATRIX", matrixStart, output.offset - matrixStart);

            int jointStart = output.offset;
            for (int i = 0; i < mesh.weights.length; ++i)
                for (int j = 0; j < 4; ++j)
                    output.int8(mesh.weights[i].boneIndex[j]);
            
            createBufferView("JOINTS", jointStart, output.offset - jointStart);

            int weightStart = output.offset;
            for (int i = 0; i < mesh.weights.length; ++i) {
                output.float32le(mesh.weights[i].weights.x);
                output.float32le(mesh.weights[i].weights.y);
                output.float32le(mesh.weights[i].weights.z);
                output.float32le(mesh.weights[i].weights.w);
            }
            
            createBufferView("WEIGHTS", weightStart, output.offset - weightStart);
   
            output.shrinkToFit();
            
            return output.buffer;
        }
        
        public static short getMin(short[] triangles) {
            short minValue = triangles[0];
            for (int i = 1; i < triangles.length; ++i)
                if (triangles[i] < minValue)
                    minValue = triangles[i];
            return minValue;
        }
        
        public static short getMax(short[] triangles) {
            short maxValue = triangles[0];
            for (int i = 1; i < triangles.length; ++i)
                if (triangles[i] > maxValue)
                    maxValue = triangles[i];
            return maxValue;
        }
        
        public void export(String path) {
            
            ByteBuffer buffer = null;
            if (this.buffer != null)
                buffer = ByteBuffer.wrap(this.buffer);
            
            GltfAssetV2 gltfAssetV2 = new GltfAssetV2(this.gltf, buffer);
            GltfAssetWriterV2 writer = new GltfAssetWriterV2();
            
            try {
                FileOutputStream stream = new FileOutputStream(path);
                writer.writeBinary(gltfAssetV2, stream);
                stream.close();  
            } catch (IOException ex) {
                Logger.getLogger(MeshIO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}