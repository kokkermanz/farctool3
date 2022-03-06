package ennuo.toolkit.functions;

import ennuo.craftworld.memory.Bytes;
import ennuo.craftworld.memory.Data;
import ennuo.craftworld.resources.io.FileIO;
import ennuo.craftworld.memory.Output;
import ennuo.craftworld.memory.Resource;
import ennuo.craftworld.memory.ResourcePtr;
import ennuo.craftworld.resources.Mesh;
import ennuo.craftworld.resources.TranslationTable;
import ennuo.craftworld.resources.enums.RType;
import ennuo.craftworld.resources.io.MeshIO;
import ennuo.craftworld.resources.structs.Slot;
import ennuo.craftworld.resources.structs.UserCreatedDetails;
import ennuo.craftworld.things.InventoryMetadata;
import ennuo.craftworld.things.Serializer;
import ennuo.craftworld.types.FileEntry;
import ennuo.craftworld.types.Mod;
import ennuo.toolkit.utilities.Globals;
import ennuo.toolkit.windows.Toolkit;
import ennuo.toolkit.functions.ModCallbacks;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

public class ExportCallbacks {
    public static void exportOBJ(int channel) {
        File file = Toolkit.instance.fileChooser.openFile(
            Globals.lastSelected.header.substring(0, Globals.lastSelected.header.length() - 4) + ".obj",
            "obj",
            "Wavefront Object (.OBJ)",
            true
        );

        if (file != null)
            MeshIO.OBJ.export(file.getAbsolutePath(), Globals.lastSelected.entry.mesh, channel);
    }
    
    public static void exportGLB() {
        File file = Toolkit.instance.fileChooser.openFile(
            Globals.lastSelected.header.substring(0, Globals.lastSelected.header.length() - 4) + ".glb",
            "glb",
            "glTF Binary (.GLB)",
            true
        );
       
        if (file != null)
            MeshIO.GLB.FromMesh(Globals.lastSelected.entry.mesh).export(file.getAbsolutePath());
    }
    
    public static void exportAnimation() {
        File file = Toolkit.instance.fileChooser.openFile(
            Globals.lastSelected.header.substring(0, Globals.lastSelected.header.length() - 5) + ".glb",
            "glb",
            "glTF Binary (.GLB)",
            true
        );
       
        String GUID = JOptionPane.showInputDialog(Toolkit.instance, "Mesh GUID", "g0");
        if (GUID == null) return;
        GUID = GUID.replaceAll("\\s", "");
        
        long integer;
        try {
            if (GUID.toLowerCase().startsWith("0x"))
                integer = Long.parseLong(GUID.substring(2), 16);
            else if (GUID.toLowerCase().startsWith("g"))
                integer = Long.parseLong(GUID.substring(1));
            else
                integer = Long.parseLong(GUID);
        } catch (NumberFormatException e) {
            System.err.println("You inputted an invalid GUID!");
            return;
        }
        
        FileEntry entry = Globals.findEntry(integer);
        
        Mesh mesh = null;
        if (entry == null) {
            System.err.println("Couldn't find model!");
        } else {
            byte[] data = Globals.extractFile(integer);
            if (data == null) System.err.println("Couldn't find data for model in any archives.");
            else
                mesh = new Mesh(Paths.get(entry.path).getFileName().toString().replaceFirst("[.][^.]+$", ""), data);
        }
        
        if (file != null)
            MeshIO.GLB.FromAnimation(Globals.lastSelected.entry.animation, mesh).export(file.getAbsolutePath());
    }

    public static void exportTexture(String extension) {
        File file = Toolkit.instance.fileChooser.openFile(
            Globals.lastSelected.header.substring(0, Globals.lastSelected.header.length() - 4) + "." + extension,
            extension,
            "Image",
            true
        );

        if (file == null) return;

        if (Globals.lastSelected.entry.texture == null || !Globals.lastSelected.entry.texture.parsed) return;

        try {
            ImageIO.write(Globals.lastSelected.entry.texture.getImage(), extension, file);
        } catch (IOException ex) {
            System.err.println("There was an error exporting the image.");
            return;
        }

        System.out.println("Successfully exported textures!");
    }

    public static void exportDDS() {
        File file = Toolkit.instance.fileChooser.openFile(
            Globals.lastSelected.header.substring(0, Globals.lastSelected.header.length() - 4) + "dds",
            "DDS",
            "Image",
            true
        );

        if (file == null) return;

        if (Globals.lastSelected.entry.texture == null || !Globals.lastSelected.entry.texture.parsed) return;

        FileIO.write(Globals.lastSelected.entry.texture.data, file.getAbsolutePath());
    }

    public static void exportTranslations() {
        if (Globals.LAMS == null)
            Globals.LAMS = new TranslationTable(new Data(Globals.lastSelected.entry.data));
        if (Globals.LAMS != null) {
            Output out = new Output(0xFEFF * Globals.LAMS.map.size());
            for (Map.Entry < Long, String > entry: Globals.LAMS.map.entrySet())
                out.string(entry.getKey() + "\n\t" + entry.getValue() + "\n");

            File file = Toolkit.instance.fileChooser.openFile(
                Globals.lastSelected.header.substring(0, Globals.lastSelected.header.length() - 5) + ".txt",
                "txt",
                "Text Document",
                true
            );

            if (file == null) return;
            out.shrinkToFit();
            FileIO.write(out.buffer, file.getAbsolutePath());
        }
    }

    public static void exportMod(boolean hashinate) {
        FileEntry entry = Globals.lastSelected.entry;
        String name = Paths.get(Globals.lastSelected.entry.path).getFileName().toString();
        if (Globals.lastSelected.entry.item != null)
            name = name.substring(0, name.length() - 5);
        else name = name.substring(0, name.length() - 4);

        File file = Toolkit.instance.fileChooser.openFile(name + ".mod", "mod", "Mod", true);
        if (file == null) return;

        Resource resource = new Resource(Globals.extractFile(entry.hash));
        Mod mod;
        if (hashinate)
            mod = resource.hashinate(entry);
        else mod = resource.recurse(entry);

        mod.title = name;

        byte[] compressed = mod.entries.get(mod.entries.size() - 1).data;

        if (entry.item != null) {
            resource.setData(compressed);
            resource.decompress(true);
            InventoryMetadata metadata = new Serializer(resource).DeserializeItem().metadata;
            if (Globals.LAMS != null) {
                metadata.translatedLocation = Globals.Translate(metadata.location);
                metadata.translatedCategory = Globals.Translate(metadata.category);
            }
            if (metadata == null) {
                metadata = new InventoryMetadata();
                metadata.userCreatedDetails = new UserCreatedDetails();
                metadata.userCreatedDetails.title = name;
            }
            metadata.resource = new ResourcePtr(Bytes.SHA1(compressed), RType.PLAN);
            mod.items.add(metadata);
        } else if (Globals.lastSelected.entry.path.toLowerCase().endsWith(".bin")) {
            Slot slot = new Slot();
            slot.root = new ResourcePtr(Bytes.SHA1(compressed), RType.LEVEL);
            slot.title = name;
            mod.slots.add(slot);
        }

        if (file.exists()) {
            int result = JOptionPane.showConfirmDialog(null, "This mod already exists, do you want to merge them?", "Existing mod!", JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                Mod oldMod = ModCallbacks.loadMod(file);
                if (oldMod != null) {
                    for (FileEntry e: oldMod.entries)
                        mod.add(e.path, e.data, e.GUID);
                    for (InventoryMetadata m: oldMod.items)
                        mod.items.add(m);
                    for (Slot slot: oldMod.slots)
                        mod.slots.add(slot);
                }
            } else if (result != JOptionPane.NO_OPTION) return;
        }

        mod.save(file.getAbsolutePath());
    }
}
