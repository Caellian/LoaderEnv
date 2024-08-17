package net.tinsvagelj.mc.loaderenv.javac;

import com.sun.tools.javac.tree.JCTree;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class JavacUtil {
    private JavacUtil() {}

    public static <T extends JCTree> String sourceRangeString(JCTree.JCCompilationUnit unit, T token) {
        int start = token.getStartPosition();
        int length = token.getEndPosition(unit.endPositions) - start;
        try (InputStream in = unit.getSourceFile().openInputStream()) {
            if (in.skip(start) != start) {
                return null;
            }
            return new String(in.readNBytes(length), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
