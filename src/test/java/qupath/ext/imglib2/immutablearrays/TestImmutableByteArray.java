package qupath.ext.imglib2.immutablearrays;

import net.imglib2.img.basictypeaccess.array.ByteArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestImmutableByteArray {

    @Test
    void Check_Cannot_Set_Value() {
        byte[] data = new byte[] {0, 1, 2};
        ByteArray array = new ImmutableByteArray(data);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> array.setValue(0, (byte) 0));
    }

    @Test
    void Check_Storage_Array_Not_Modified() {
        byte[] expectedData = new byte[] {0, 1, 2};
        byte[] data = expectedData.clone();
        ByteArray array = new ImmutableByteArray(data);

        byte[] storageArray = array.getCurrentStorageArray();
        storageArray[1] = 0;

        Assertions.assertArrayEquals(expectedData, data);
    }
}
