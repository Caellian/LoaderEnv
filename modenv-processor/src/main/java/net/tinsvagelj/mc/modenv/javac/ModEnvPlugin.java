package net.tinsvagelj.mc.modenv.javac;

import com.sun.source.tree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.JCTree;
import net.tinsvagelj.mc.modenv.ModEnv;
import net.tinsvagelj.mc.modenv.NotForLoader;
import net.tinsvagelj.mc.modenv.OnlyForLoader;

import javax.tools.*;
import java.util.*;
import java.util.List;

public class ModEnvPlugin implements Plugin {
    private Context ctx;
    private Options options;
    private Log log;
    private TreeMaker treeMaker;

    private ModEnv[] currentEnvironment = null;
    private JavacTrees trees;

    @Override
    public String getName() {
        return ModEnvPlugin.class.getSimpleName();
    }

    @Override
    public void init(JavacTask task, String... args) {
        this.ctx = ((BasicJavacTask) task).getContext();
        this.options = Options.instance(ctx);
        this.log = Log.instance(ctx);
        this.treeMaker = TreeMaker.instance(ctx);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            log.error("System compiler not available.");
            return;
        }

        if (args.length != 0) {
            currentEnvironment = Arrays.stream(args).map(ModEnv::parse).filter(Objects::nonNull).toArray(ModEnv[]::new);
        } else if (options.isSet("modenv.loader")) {
            String forceEnv = options.get("modenv.loader");
            currentEnvironment = Arrays.stream(forceEnv.split(";")).map(ModEnv::parse).filter(Objects::nonNull).toArray(ModEnv[]::new);
        }

        if (currentEnvironment == null || currentEnvironment.length == 0) {
            System.out.println("Unable to detect any mod loaders in environment");
            return;
        } else {
            System.out.println("Keeping code for mod loaders: " + String.join(", ", Arrays.stream(currentEnvironment).map(ModEnv::toString).toArray(String[]::new)));
        }

        task.addTaskListener(new ModEnvFilterTask(task));
    }

    JCTree.JCCompilationUnit currentUnit = null;

    private class ModEnvFilterTask implements TaskListener {
        JavacTask task;

        public ModEnvFilterTask(JavacTask task) {
            this.task = task;
        }

        @Override
        public void finished(TaskEvent e) {
            if (e.getKind() != TaskEvent.Kind.PARSE) {
                return;
            }
            currentUnit = (JCTree.JCCompilationUnit) e.getCompilationUnit();
            FilterTransformer transformer = new FilterTransformer(ModEnvPlugin.this::isAllowed, treeMaker);
            transformer.translate(currentUnit);
        }
    }

    private ModEnv parseModEnv(ExpressionTree it) {
        if (it instanceof JCTree.JCFieldAccess fieldAccess) {
            String path = JavacUtil.sourceRangeString(currentUnit, fieldAccess.selected);
            if (!Objects.equals(path, ModEnv.class.getSimpleName())) {
                throw new RuntimeException("ModEnv annotation argument not a ModEnv value; actual type: " + path);
            }
            //throw new RuntimeException(memberReference.getName().toString());
            return ModEnv.valueOf(fieldAccess.name.toString());
        }
        throw new RuntimeException("Unhandled ModEnv[] argument value type: " + it.getClass().getCanonicalName());
    }
    private ModEnv[] parseAnnotationArgs(List<? extends ExpressionTree> args) {
        if (args.size() != 1) {
            throw new RuntimeException("Unhandled number of ModEnv annotation arguments");
        }
        ExpressionTree expr = args.getFirst();
        if (expr instanceof NewArrayTree arr) {
            return arr.getInitializers().stream().map(this::parseModEnv).toArray(ModEnv[]::new);
        }
        throw new RuntimeException("Unhandled ModEnv[] argument type: " + expr.getClass().getCanonicalName());
    }

    private boolean envHas(ModEnv... loaders) {
        for (ModEnv loader : loaders) {
            for (ModEnv modEnv : this.currentEnvironment) {
                if (loader == modEnv) return true;
            }
        }
        return false;
    }

    private String typeName(JCTree type) {
        return switch (Objects.requireNonNull(type)) {
            case JCTree.JCIdent identifier -> identifier.name.toString();
            case JCTree.JCFieldAccess fieldAccess -> fieldAccess.name.toString();
            default -> {
                throw new UnsupportedOperationException("Can't get type name for " + type.getClass().getCanonicalName());
            }
        };
    }

    private boolean annotationsAllow(com.sun.tools.javac.util.List<JCTree.JCAnnotation> annotations) {
        for (JCTree.JCAnnotation annotation : annotations) {
            String name = typeName(annotation.annotationType);
            if (name.equals(NotForLoader.class.getSimpleName())) {
                ModEnv[] loaders = parseAnnotationArgs(((AnnotationTree) annotation).getArguments());
                if (envHas(loaders)) {
                    return false;
                }
            } else if (name.equals(OnlyForLoader.class.getSimpleName())) {
                ModEnv[] loaders = parseAnnotationArgs(((AnnotationTree) annotation).getArguments());
                if (!envHas(loaders)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isAllowed(Object obj) {
        return switch (obj) {
            case JCTree.JCClassDecl clazz -> annotationsAllow(clazz.mods.annotations);
            case JCTree.JCVariableDecl variable -> annotationsAllow(variable.mods.annotations);
            case JCTree.JCMethodDecl method -> annotationsAllow(method.mods.annotations);
            case null, default -> true;
        };
    }
}
