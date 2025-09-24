package qupath.ext.imglib2.immutablearrays;

import net.imglib2.img.basictypeaccess.array.FloatArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestImmutableFloatArray {

    @Test
    void Check_Cannot_Set_Value() {
        float[] data = new float[] {0, 1, 2};
        FloatArray array = new ImmutableFloatArray(data);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> array.setValue(0, 0));
    }

    @Test
    void Check_Storage_Array_Not_Modified() {
        float[] expectedData = new float[] {0, 1, 2};
        float[] data = expectedData.clone();
        FloatArray array = new ImmutableFloatArray(data);

        float[] storageArray = array.getCurrentStorageArray();
        storageArray[1] = 0;

        Assertions.assertArrayEquals(expectedData, data);
    }
}
