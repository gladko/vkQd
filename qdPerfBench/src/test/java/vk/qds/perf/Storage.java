package vk.qds.perf;

import java.io.*;

public class Storage {
    public static <T> T loadObject(String dir, String objId) {
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(
                new FileInputStream(dir + "/" + objId))))
        {
            return (T) in.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void storeObject(String dir, String objId, Object data) throws IOException {
        new File(dir).mkdirs();
        try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(
                new FileOutputStream(dir + "/" + objId))))
        {
            out.writeObject(data);
            out.flush();
        }
    }
}
