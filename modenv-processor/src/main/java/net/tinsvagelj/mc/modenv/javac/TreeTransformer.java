package net.tinsvagelj.mc.modenv.javac;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;

public class TreeTransformer extends TreeTranslator {
    protected <T extends JCTree> T transform(T tree) {
        return tree;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends JCTree> T translate(T tree) {
        if (tree == null) {
            return null;
        } else {
            return super.translate(transform(tree));
        }
    }

    @Override
    public <T extends JCTree> List<T> translate(List<T> trees) {
        if (trees == null) {
            return null;
        } else {
            for(List<T> l = trees; l.nonEmpty(); l = l.tail) {
                l.head = this.translate(l.head);
                while (l.head == null && l.nonEmpty()) {
                    l = l.tail;
                    l.head = this.translate(l.head);
                }
            }
            return trees;
        }
    }

    @Override
    public List<JCTree.JCVariableDecl> translateVarDefs(List<JCTree.JCVariableDecl> trees) {
        return this.translate(trees);
    }

    @Override
    public List<JCTree.JCTypeParameter> translateTypeParams(List<JCTree.JCTypeParameter> trees) {
        return this.translate(trees);
    }

    @Override
    public List<JCTree.JCCase> translateCases(List<JCTree.JCCase> trees) {
        return this.translate(trees);
    }

    @Override
    public List<JCTree.JCCatch> translateCatchers(List<JCTree.JCCatch> trees) {
        return this.translate(trees);
    }

    @Override
    public List<JCTree.JCAnnotation> translateAnnotations(List<JCTree.JCAnnotation> trees) {
        return this.translate(trees);
    }
}
