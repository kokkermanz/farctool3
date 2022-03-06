package ennuo.craftworld.things.parts;

import ennuo.craftworld.resources.structs.FieldLayoutDetails;
import ennuo.craftworld.resources.structs.ScriptObject;
import ennuo.craftworld.things.Part;
import ennuo.craftworld.things.Serializer;
import java.util.ArrayList;

public class ScriptInstance implements Part {
    FieldLayoutDetails[] fieldLayout;
    public int instanceSize;
    public ScriptObject[] objects;
    
    @Override
    public void Serialize(Serializer serializer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void Deserialize(Serializer serializer) {
        int count = serializer.input.int32();
        if (count != 0) {
            fieldLayout = new FieldLayoutDetails[count];
            for (int i = 0; i < count; ++i)
                fieldLayout[i] = new FieldLayoutDetails(serializer.input, serializer.partsRevision);
        }
        instanceSize = serializer.input.int32();
        
        serializer.input.int8();
        
        getObjects(serializer);
    }
    
    private void getObjects(Serializer serializer) {
        ArrayList<ScriptObject> objects = new ArrayList<ScriptObject>(fieldLayout.length);
        for (FieldLayoutDetails field : fieldLayout) {
            if ((field.modifiers | 4096) == field.modifiers) continue;
            ScriptObject object = new ScriptObject();
            object.type = field.machineType;
            switch (field.machineType) {
                case MATRIX:
                    object.matrix = serializer.input.matrix();
                    break;
                case BOOL:
                    object.bool = serializer.input.bool();
                    break;
                case FLOAT:
                    object.f = serializer.input.float32();
                    break;
                case INT:
                    object.integer = serializer.input.int32();
                    break;
                case SAFE_PTR:
                    object.thing = serializer.deserializeThing();
                    break;
                case OBJECT: {
                    int objectType = serializer.input.int32();
                    switch (objectType) {
                        case 13:
                            object.part = serializer.deserializePart("Script");
                            break;
                    }
                    break;
                }
            }
            objects.add(object);
        }
        
        this.objects = new ScriptObject[objects.size()];
        this.objects = objects.toArray(this.objects);
    }
    
}
