package ennuo.craftworld.memory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Output {
    public static int ENCODED_REVISION = 0x271;

    public int offset = 0;
    public int revision = 0x271;
    public byte[] buffer;

    public ArrayList < ResourcePtr > dependencies = new ArrayList < ResourcePtr > ();

    public Output(int size) {
        buffer = new byte[size];
    }
    public Output(int size, int rev) {
        buffer = new byte[size];
        revision = rev;
    }
    
    public boolean isEncoded() {
        return this.revision > ENCODED_REVISION && !(this.revision >= 0x273 && this.revision <= 0x297);
    }

    private boolean hasDependency(ResourcePtr res) {
        for (ResourcePtr ptr: dependencies)
            if (ptr.equals(res))
                return true;
        return false;
    }

    public void str16(String str) {
        if (str == null || str.equals("")) {
            int32(0);
            return;
        }
        int size = str.length();
        if (isEncoded()) size *= 2;
            
        int32(size);
        bytes(str.getBytes(StandardCharsets.UTF_16BE));

    }

    public void str8(String str) {
        if (str == null || str.equals("")) {
            int32(0);
            return;
        }
        int size = str.length();
        if (isEncoded()) size *= 2;
        int32(size);
        string(str);
    }

    public void string(String str, int size) {
        string(str);
        pad(size - str.length());

    }
    public void string(String str) {
        bytes(str.getBytes());
    }

    public void bytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; ++offset, ++i)
            buffer[offset] = bytes[i];
    }

    public void bool(boolean value) {
        int8(value == true ? 1 : 0);
    }

    public void pad(int size) {
        for (int i = 0; i < size; ++i)
            int8((byte) 0);
    }

    public void int8(int value) {
        int8((byte) value);
    }
    public void int8(byte value) {
        buffer[offset] = value;
        offset += 1;
    }

    public void int32(int value) {
        if (isEncoded())
            varint(value);
        else int32f(value);
    }
    
    public void u32a(long[] values) {
        int32(values.length);
        for (int i = 0; i < values.length; ++i)
            uint32(values[i]);
    }

    public void uint32(long value) {
        if (isEncoded())
            varint(value);
        else uint32f(value);
    }

    public void int16(short value) {
        if (value == 0) {
            bytes(new byte[] {
                0,
                0
            });
            return;
        }
        bytes(Bytes.toBytes(value));
    }
    
    public void int16le(short value) {
        if (value == 0) { int16((short) 0); return; }
        byte[] s = Bytes.toBytes(value);
        int8(s[1]); int8(s[0]);
    }

    public void int32f(int value) {
        if (value == 0) {
            bytes(new byte[] {
                0,
                0,
                0,
                0
            });
            return;
        }
        bytes(Bytes.toBytes(value));
    }

    public void uint32f(long value) {
        if (value == 0) {
            bytes(new byte[] {
                0,
                0,
                0,
                0
            });
            return;
        }
        bytes(Bytes.toBytes(value));
    }

    public void varint(int value) {
        varint((long) value);
    }
    public void varint(long value) {
        if (value == -1 || value == Long.MAX_VALUE) {
            bytes(new byte[] {  -1, -1, -1, -1, 0xf });
            return;
        } else if (value == 0) {
            int8((byte) 0);
            return;
        }
        while (value > 0) {
            byte b = (byte)(value & 0x7fL);
            value >>= 7L;
            if (value > 0L) b |= 128L;
            int8(b);
        }
    }

    public void matrix(float[] value) {
        float[] dMatrix = new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
        int flags = 0xFFFF;
        if (isEncoded()) {
            flags = 0;
            for (int i = 0; i < 16; ++i)
                if (value[i] != dMatrix[i])
                    flags |= (1 << i);
            int16((short) flags);
        }

        for (int i = 0; i < 16; ++i)
            if (((flags >>> i) & 1) != 0)
                float32(value[i]);

    }

    public void resource(ResourcePtr res) {
        resource(res, false);
    }
    public void resource(ResourcePtr res, boolean bit) {
        byte HASH = 1, GUID = 2;
        if (revision <= 0x18B) {
            HASH = 2;
            GUID = 1;
        }


        if (revision < 0x230) bit = true;
        if (((revision >= 0x230 && revision <= 0x26e) || (this.revision >= 0x273 && this.revision <= 0x297)) && !bit) int8(0);
        if (bit) {
            if (res == null) int8(0);
            else if (res.hash != null) int8(HASH);
            else if (res.GUID != -1) int8(GUID);
            else int8(0);
        } else if (isEncoded()) {
            if (res == null) int16((short) 0);
            else if (res.hash != null) int16((short) HASH);
            else if (res.GUID != -1) int16((short) GUID);
            else int16((short) 0);
        } else {
            if (res == null) int32(0);
            else if (res.hash != null) int32(HASH);
            else if (res.GUID != -1) int32(GUID);
            else int32(0);
        }
        if (res == null) return;
        if (res.hash != null) bytes(res.hash);
        else if (res.GUID != -1) uint32(res.GUID);

        if (!hasDependency(res))
            dependencies.add(res);
    }

    public void float32(float value) {
        bytes(Bytes.toBytes(Float.floatToIntBits(value)));
    }
    
    public void float32le(float value) {
        int bits = Float.floatToRawIntBits(value);
        byte[] bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(bits).array();
        bytes(bb);
    }

    public void float32arr(float[] value) {
        if (value == null) {
            int32(0);
            return;
        }
        int32(value.length);
        for (float f: value)
            float32(f);
    }

    public void v2(Vector2f value) {
        float32(value.x);
        float32(value.y);
    }

    public void v3(Vector3f value) {
        float32(value.x);
        float32(value.y);
        float32(value.z);
    }

    public void v4(Vector4f value) {
        float32(value.x);
        float32(value.y);
        float32(value.z);
        float32(value.w);
    }

    public void shrinkToFit() {
        buffer = Arrays.copyOfRange(buffer, 0, offset);
    }
}
