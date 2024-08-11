package net.tinsvagelj.mc.modenv.checker;

import net.tinsvagelj.mc.modenv.ModEnv;

public class Main {
    public static void main(String[] args) {
        boolean printedAny = false;
        for (ModEnv env : ModEnv.getAvailable()) {
            if (printedAny) System.out.print(';');
            System.out.print(env.prettyName);
            printedAny = true;
        }
    }
}
