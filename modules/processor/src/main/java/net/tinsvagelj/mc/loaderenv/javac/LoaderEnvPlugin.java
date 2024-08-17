package net.tinsvagelj.mc.loaderenv.javac;

import com.sun.source.tree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.JCTree;
import net.tinsvagelj.mc.loaderenv.Loader;
import net.tinsvagelj.mc.loaderenv.NotForLoader;
import net.tinsvagelj.mc.loaderenv.OnlyForLoader;

import javax.tools.*;
import java.util.*;
import java.util.List;

public class LoaderEnvPlugin implements Plugin {
    private Context ctx;
    private Options options;
    private Log log;
    private TreeMaker treeMaker;

    private Loader[] currentEnvironment = null;
    private JavacTrees trees;

    @Override
    public String getName() {
        return LoaderEnvPlugin.class.getSimpleName();
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
            currentEnvironment = Arrays.stream(args).map(Loader::parse).filter(Objects::nonNull).toArray(Loader[]::new);
        } else if (options.isSet("loaderenv.loader")) {
            String forceEnv = options.get("loaderenv.loader");
            currentEnvironment = Arrays.stream(forceEnv.split(";")).map(Loader::parse).filter(Objects::nonNull).toArray(Loader[]::new);
        }

        if (currentEnvironment == null || currentEnvironment.length == 0) {
            System.out.println("Unable to detect any mod loaders in environment");
            return;
        } else {
            System.out.println("Keeping code for mod loaders: " + String.join(", ", Arrays.stream(currentEnvironment).map(Loader::toString).toArray(String[]::new)));
        }

        task.addTaskListener(new LoaderEnvFilterTask(task));
    }

    JCTree.JCCompilationUnit currentUnit = null;

    private class LoaderEnvFilterTask implements TaskListener {
        JavacTask task;

        public LoaderEnvFilterTask(JavacTask task) {
            this.task = task;
        }

        @Override
        public void finished(TaskEvent e) {
            if (e.getKind() != TaskEvent.Kind.PARSE) {
                return;
            }
            currentUnit = (JCTree.JCCompilationUnit) e.getCompilationUnit();
            FilterTransformer transformer = new FilterTransformer(LoaderEnvPlugin.this::isAllowed, treeMaker);
            transformer.translate(currentUnit);
        }
    }

    private Loader parseLoaderEnv(ExpressionTree it) {
        if (it instanceof JCTree.JCFieldAccess fieldAccess) {
            String path = JavacUtil.sourceRangeString(currentUnit, fieldAccess.selected);
            if (!Objects.equals(path, Loader.class.getSimpleName())) {
                throw new RuntimeException("LoaderEnv annotation argument not a Loader value; actual type: " + path);
            }
            //throw new RuntimeException(memberReference.getName().toString());
            return Loader.valueOf(fieldAccess.name.toString());
        }
        throw new RuntimeException("Unhandled Loader[] argument value type: " + it.getClass().getCanonicalName());
    }
    private Loader[] parseAnnotationArgs(List<? extends ExpressionTree> args) {
        if (args.size() != 1) {
            throw new RuntimeException("Unhandled number of LoaderEnv annotation arguments");
        }
        ExpressionTree expr = args.getFirst();
        if (expr instanceof NewArrayTree arr) {
            return arr.getInitializers().stream().map(this::parseLoaderEnv).toArray(Loader[]::new);
        }
        throw new RuntimeException("Unhandled Loader[] argument type: " + expr.getClass().getCanonicalName());
    }

    private boolean envHas(Loader... loaders) {
        for (Loader loader : loaders) {
            for (Loader it : this.currentEnvironment) {
                if (loader == it) return true;
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
                Loader[] loaders = parseAnnotationArgs(((AnnotationTree) annotation).getArguments());
                if (envHas(loaders)) {
                    return false;
                }
            } else if (name.equals(OnlyForLoader.class.getSimpleName())) {
                Loader[] loaders = parseAnnotationArgs(((AnnotationTree) annotation).getArguments());
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
