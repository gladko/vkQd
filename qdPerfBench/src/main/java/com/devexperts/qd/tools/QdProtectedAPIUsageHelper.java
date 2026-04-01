package com.devexperts.qd.tools;

import java.util.List;

public class QdProtectedAPIUsageHelper {
    public static List<String> generateSymbols(int totalSymbols) {
        return SymbolGenerator.generateSymbols(totalSymbols);
    }
}
