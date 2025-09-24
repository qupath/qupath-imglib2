package qupath.ext.imglib2.immutablearrays;

import net.imglib2.img.basictypeaccess.array.ShortArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestImmutableShortArray {

    @Test
    void Check_Cannot_Set_Value() {
        short[] data = new short[] {0, 1, 2};
        ShortArray array = new ImmutableShortArray(data);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> array.setValue(0, (short) 0));
    }

    @Test
    void Check_Storage_Array_Not_Modified() {
        short[] expectedData = new short[] {0, 1, 2};
        short[] data = expectedData.clone();
        ShortArray array = new ImmutableShortArray(data);

        short[] storageArray = array.getCurrentStorageArray();
        storageArray[1] = 0;

        Assertions.assertArrayEquals(expectedData, data);
    }
}
