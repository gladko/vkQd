package intro.qd;

import com.devexperts.qd.tools.Multiplexor;

// Analogue of `./qds multiplexor --stat 10s :7000 :8000`
public class MuxLauncher {
    public static void main(String[] args) {
        Multiplexor.main(new String[] {"--stat", "10s", ":7000", ":8000"});
    }
}
