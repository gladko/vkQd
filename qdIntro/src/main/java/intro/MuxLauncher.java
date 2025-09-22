package intro;

import com.devexperts.qd.tools.Multiplexor;

// Analogue of `./qds Multiplexor :7000 :8000`
public class MuxLauncher {
    public static void main(String[] args) {
        Multiplexor.main(new String[] {":7000", ":8000"});
    }
}
