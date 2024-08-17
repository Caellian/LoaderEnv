package net.tinsvagelj.mc.loaderenv.javac;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;

import java.util.LinkedList;
import java.util.function.Function;

public class FilterTransformer extends TreeTransformer {
    private final Function<Object, Boolean> filter;

    public FilterTransformer(Function<Object, Boolean> filter, TreeMaker treeMaker) {
        this.filter = filter;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T extends JCTree> T transform(T tree) {
        return switch (tree) {
            case JCTree.JCCompilationUnit unit -> {
                LinkedList<JCTree> retained = new LinkedList<>();
                for (JCTree def : unit.defs) {
                    if (filter.apply(def)) {
                        retained.add(def);
                    }
                }

                unit.defs = List.from(retained);
                yield (T) unit;
            }
            case JCTree.JCClassDecl clazz -> {
                LinkedList<JCTree> retained = new LinkedList<>();
                for (JCTree member : clazz.defs) {
                    if(filter.apply(member)) {
                        retained.add(member);
                    }
                }

                clazz.defs = List.from(retained);
                yield (T) clazz;
            }
            default -> tree;
        };
    }
}
