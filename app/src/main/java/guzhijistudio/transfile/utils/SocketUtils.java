package guzhijistudio.transfile.utils;

import java.io.*;

public final class SocketUtils {

    public interface Progress {
        void onStart(File file);
        void onFinish(File file);
        void onProgress(File file, long progress, long total);
    }

    public static class BufPos {
        private int pos;

        public BufPos() {
            pos = 0;
        }

        public BufPos(int pos) {
            this.pos = pos;
        }

        public int getPos() {
            return pos;
        }

        public void setPos(int pos) {
            this.pos = pos;
        }
    }

    public static void print(byte value) {
        int m = 1 << 7;
        for (int i = 0; i < 8; i++) {
            System.out.print((m & value) == 0 ? "0" : "1");
            m >>>= 1;
        }
    }

    public static void println(byte value) {
        print(value);
        System.out.println();
    }

    public static void print(byte[] values) {
        for (int i = values.length - 1; i >= 0; i--)
            print(values[i]);
    }

    public static void println(byte[] values) {
        print(values);
        System.out.println();
    }

    public static void println(int value) {
        print(value);
        System.out.println();
    }

    public static void print(int value) {
        int m = 1 << 31;
        for (int i = 0; i < 32; i++) {
            System.out.print((m & value) == 0 ? "0" : "1");
            m >>>= 1;
        }
    }

    public static byte[] fromInt32(int value) {
        byte[] a = new byte[4];
        for (int i = 0; i < 4; i++) {
            a[i] = (byte) (value & 255);
            value >>>= 8;
        }
        return a;
    }

    public static byte[] fromInt64(long value) {
        byte[] a = new byte[8];
        for (int i = 0; i < 8; i++) {
            a[i] = (byte) (value & 255);
            value >>>= 8;
        }
        return a;
    }

    public static void writeInt32(OutputStream dest, int value) throws IOException {
        dest.write(fromInt32(value));
    }

    public static void writeInt32(byte[] buf, int start, int value) {
        writeInt32(buf, start, value, null);
    }

    public static void writeInt32(byte[] buf, int start, int value, BufPos pos) {
        if (start + 4 <= buf.length) {
            byte[] data = fromInt32(value);
            System.arraycopy(data, 0, buf, start, 4);
            if (pos != null) pos.setPos(start + 4);
        } else if (pos != null) {
            pos.setPos(start);
        }
    }

    public static void writeInt64(OutputStream dest, long value) throws IOException {
        dest.write(fromInt64(value));
    }

    public static void writeInt64(byte[] buf, int start, long value) {
        writeInt64(buf, start, value, null);
    }

    public static void writeInt64(byte[] buf, int start, long value, BufPos pos) {
        if (start + 8 <= buf.length) {
            byte[] data = fromInt64(value);
            System.arraycopy(data, 0, buf, start, 8);
            if (pos != null) pos.setPos(start + 8);
        } else if (pos != null) {
            pos.setPos(start);
        }
    }

    public static void writeString(OutputStream dest, String value) throws IOException {
        byte[] b = value.getBytes("utf-8");
        writeInt32(dest, b.length);
        dest.write(b);
    }

    public static void writeString(byte[] buf, String value) throws UnsupportedEncodingException {
        writeString(buf, value, null);
    }

    public static void writeString(byte[] buf, int start, String value) throws UnsupportedEncodingException {
        writeString(buf, value, new BufPos(start));
    }

    public static void writeString(byte[] buf, String value, BufPos pos) throws UnsupportedEncodingException {
        byte[] data = value == null ? new byte[0] : value.getBytes("utf-8");
        int start = 0;
        if (pos != null) start = pos.getPos();
        int len = buf.length - start - 4;
        if (len > 0) {
            if (len > data.length) len = data.length;
            writeInt32(buf, start, len);
            start += 4;
            System.arraycopy(data, 0, buf, start, len);
            if (pos != null) pos.setPos(start + len);
        } else if (pos != null) {
            pos.setPos(start);
        }
    }

    public static int toInt32(byte[] bytes) {
        return toInt32(bytes, 0);
    }

    public static int toInt32(byte[] bytes, int start) {
        int out = 0;
        for (int i = 3; i >= 0; i--) {
            byte b = bytes[start + i];
            out <<= 1;
            out |= (b & 128) >> 7;
            out <<= 7;
            out |= b & 127;
        }
        return out;
    }

    public static int toInt32b(byte[] bytes, int start) {
        int out = 0;
        for (int i = 3; i >= 0; i--) {
            int m = 1 << 7;
            byte b = bytes[start + i];
            for (int j = 7; j >= 0; j--) {
                out <<= 1;
                out |= (b & m) >> j;
                m >>= 1;
            }
        }
        return out;
    }

    public static long toInt64(byte[] bytes) {
        return toInt64(bytes, 0);
    }

    public static long toInt64(byte[] bytes, int start) {
        long out = 0;
        for (int i = 7; i >= 0; i--) {
            byte b = bytes[start + i];
            out <<= 1;
            out |= (b & 128) >> 7;
            out <<= 7;
            out |= b & 127;
        }
        return out;
    }

    public static int readInt32(InputStream src) throws IOException {
        byte[] bytes = new byte[4];
        if (4 > src.read(bytes)) {
            return 0;
        }
        return toInt32(bytes);
    }

    public static long readInt64(InputStream src) throws IOException {
        byte[] bytes = new byte[8];
        if (8 > src.read(bytes)) {
            return 0;
        }
        return toInt64(bytes);
    }

    public static String toStr(byte[] bytes, byte[] buf) throws UnsupportedEncodingException {
        return toStr(bytes, buf, null);
    }

    public static String toStr(byte[] bytes, byte[] buf, BufPos pos) throws UnsupportedEncodingException {
        int start = 0;
        if (pos != null) start = pos.getPos();
        // cannot read len
        if (bytes.length < start + 4) return null;
        int len = toInt32(bytes, start);
        // untrue len
        if (len + 4 + start > bytes.length) return null;
        // truncate the string if buf space isn't big enough
        if (len > buf.length) len = buf.length;
        System.arraycopy(bytes, start + 4, buf, 0, len);
        if (pos != null) pos.setPos(start + 4 + len);
        return new String(buf, 0, len, "utf-8");
    }

    public static String readString(InputStream src, byte[] buf) throws IOException {
        int len = readInt32(src);
        if (len == 0 || len > buf.length) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int r, d, received = 0;
        do {
            d = len - received;
            r = src.read(buf, 0, d > buf.length ? buf.length : d);
            if (r > 0) {
                sb.append(new String(buf, 0, r, "utf-8"));
                received += r;
            }
        } while (r >= 0 && received < len);
        return sb.toString();
    }

    public static void readData(InputStream src, byte[] buf, OutputStream dest) throws IOException {
        readData(src, buf, dest, null);
    }

    public static void readData(InputStream src, byte[] buf, OutputStream dest, Progress pg) throws IOException {
        readData(src, buf, dest, pg, null);
    }

    public static void readData(InputStream src, byte[] buf, OutputStream dest, Progress pg, File relevantFile) throws IOException {
        long len = readInt64(src), t = System.currentTimeMillis();
        long received = 0, d;
        int r;
        do {
            d = len - received;
            r = src.read(buf, 0, d >= buf.length ? buf.length : (int) d);
            if (r > 0) {
                dest.write(buf, 0, r);
                received += r;
                if (pg != null && System.currentTimeMillis() - t > 500) {
                    pg.onProgress(relevantFile, received, len);
                    t = System.currentTimeMillis();
                }
            }
        } while (r >= 0 && received < len);
        if (pg != null) pg.onProgress(relevantFile, received, len);
    }

    public static String readFile(InputStream src, byte[] buf, File dir) throws IOException {
        return readFile(src, buf, dir, null);
    }

    public static String readFile(InputStream src, byte[] buf, File dir, Progress pg) throws IOException {
        String filename = readString(src, buf);
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        File file = new File(dir, filename);
        if (file.exists() || file.createNewFile()) {
            if (pg != null) pg.onStart(file);
            FileOutputStream dest = new FileOutputStream(file);
            try {
                readData(src, buf, dest, pg, file);
            } finally {
                dest.close();
                if (pg != null) pg.onFinish(file);
            }
            return filename;
        } else {
            return null;
        }
    }

    public static void writeFile(OutputStream dest, byte[] buf, String filename) throws IOException {
        writeFile(dest, buf, filename, null);
    }

    public static void writeFile(OutputStream dest, byte[] buf, String filename, Progress pg) throws IOException {
        File file = new File(filename);
        if (file.exists()) {
            if (pg != null) pg.onStart(file);
            writeString(dest, file.getName());
            writeInt64(dest, file.length());
            long t = System.currentTimeMillis(), sent = 0;
            int r;
            FileInputStream fis = new FileInputStream(file);
            try {
                do {
                    r = fis.read(buf);
                    if (r > 0) {
                        dest.write(buf, 0, r);
                        sent += r;
                        if (pg != null && System.currentTimeMillis() - t > 500) {
                            pg.onProgress(file, sent, file.length());
                            t = System.currentTimeMillis();
                        }
                    }
                } while (r >= 0);
                if (pg != null) {
                    pg.onProgress(file, sent, file.length());
                    pg.onFinish(file);
                }
            } finally {
                fis.close();
            }
        } else {
            writeInt32(dest, 0);
        }
    }

}
