package guzhijistudio.transfile.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public final class SocketUtilsTest {

    @Test
    public void testInt32() {
        int[] ns = new int[] {-1, 0, 1, 10, 99, 876, 8474, 6387, 72837, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (int n : ns) {
            byte[] b = SocketUtils.fromInt32(n);
            int r = SocketUtils.toInt32(b);
            Assert.assertEquals(n, r);
        }
    }

    @Test
    public void testInt64() {
        long[] ns = new long[] {-1, 0, 1, 10, 99, 876, 8474, 6387, 72837, Long.MIN_VALUE, Long.MAX_VALUE};
        for (long n : ns) {
            byte[] b = SocketUtils.fromInt64(n);
            long r = SocketUtils.toInt64(b);
            Assert.assertEquals(n, r);
        }
    }

    @Test
    public void testString() throws IOException {
        byte[] buf = new byte[1024];
        String[] values = new String[] {
                "abc",
                "1lwekfjfds;lfkjsadfklasdllsdl",
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString() + ".jpg",
                "你好",
                "测试这个程序，再测试，再测试一下，再一下下"
        };
        for (String value : values) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            SocketUtils.writeString(baos, value);
            SocketUtils.writeString(baos, "   asdf  ");
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            String result = SocketUtils.readString(bais, buf);
            Assert.assertEquals(value, result);
        }
    }

}
