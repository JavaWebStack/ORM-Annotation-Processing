package org.javawebstack.orm.annotation.processor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import org.javawebstack.orm.annotation.BelongsTo;
import org.javawebstack.orm.util.Helper;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import java.lang.reflect.Modifier;
import java.util.Set;

@SupportedAnnotationTypes("org.javawebstack.orm.annotation.BelongsTo")
public class AnnotationProcessor extends AbstractProcessor {

    private Trees trees;
    private TreeMaker make;
    private Names names;

    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        trees = Trees.instance(env);
        Context context = ((JavacProcessingEnvironment) env).getContext();
        make = TreeMaker.instance(context);
        names = Names.instance(context);
    }

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(!roundEnv.processingOver()) {
            try {
                for(Element element : roundEnv.getRootElements())
                    processElement(null, element);
            }catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return true;
    }

    private void processElement(JCTree.JCClassDecl classDecl, Element element) {
        if(element.getKind() == ElementKind.FIELD) {
            BelongsTo[] belongsTos = element.getAnnotationsByType(BelongsTo.class);
            for(BelongsTo belongsTo : belongsTos)
                processBelongsTo(classDecl, element.getSimpleName().toString(), belongsTo);
            return;
        }
        if(element.getKind() == ElementKind.CLASS) {
            JCTree.JCClassDecl clDecl = (JCTree.JCClassDecl) trees.getTree(element);
            for(Element child : element.getEnclosedElements())
                processElement(clDecl, child);
            return;
        }
        for(Element child : element.getEnclosedElements())
            processElement(classDecl, child);
    }

    private JCTree.JCExpression makeType(String typeName) {
        String[] spl = typeName.split("\\.");
        JCTree.JCExpression expr = make.Ident(names.fromString(spl[0]));
        for(int i=1; i<spl.length; i++)
            expr = make.Select(expr, names.fromString(spl[i]));
        return expr;
    }

    public void processBelongsTo(JCTree.JCClassDecl classDecl, String fieldName, BelongsTo belongsTo) {
        String name = belongsTo.name().length() > 0 ? belongsTo.name() : null;
        if(name == null) {
            name = fieldName;
            if(name.endsWith("Id")) {
                name = name.substring(0, name.length() - 2);
            } else if(name.endsWith("UUID") || name.endsWith("Uuid")) {
                name = name.substring(0, name.length() - 4);
            }
        }

        JCTree.JCExpression type = makeType(getClassName(() -> belongsTo.value()));
        JCTree.JCExpression queryType = make.TypeApply(makeType("org.javawebstack.orm.query.Query"), List.of(type));
        classDecl.defs = classDecl.defs.append(make.MethodDef(
                make.Modifiers(Modifier.PUBLIC),                        // Modifier
                names.fromString(name),                               // Method Name
                queryType,                    // Return Type
                List.nil(),                                             // Generics
                List.nil(),                                             // Parameters
                List.nil(),                                             // Exceptions
                make.Block(0, List.of(                                  // Code
                        make.Return(
                                make.Apply(List.nil(), make.Ident(names.fromString("belongsTo")), List.of(
                                        make.Select(type, names._class),
                                        make.Literal(TypeTag.CLASS, fieldName)
                                ))
                        )
                )),
                null                                                    // Default Value ?!?!
        ));
        classDecl.defs = classDecl.defs.append(make.MethodDef(
                make.Modifiers(Modifier.PUBLIC),                        // Modifier
                names.fromString("get" + Helper.camelToPascalCase(name)),                               // Method Name
                type,                    // Return Type
                List.nil(),                                             // Generics
                List.nil(),                                             // Parameters
                List.nil(),                                             // Exceptions
                make.Block(0, List.of(                                  // Code
                        make.Return(
                                make.Apply(List.nil(), make.Select(make.Apply(List.nil(), make.Ident(names.fromString(name)), List.nil()), names.fromString("first")), List.nil())
                        )
                )),
                null                                                    // Default Value ?!?!
        ));
        classDecl.defs = classDecl.defs.append(make.MethodDef(
                make.Modifiers(Modifier.PUBLIC),                        // Modifier
                names.fromString("set" + Helper.camelToPascalCase(name)),// Method Name
                make.TypeIdent(TypeTag.VOID),                    // Return Type
                List.nil(),                                             // Generics
                List.of(make.VarDef(make.Modifiers(Flags.PARAMETER), names.fromString(name), type, null)),                                             // Parameters
                List.nil(),                                             // Exceptions
                make.Block(0, List.of(
                        make.Exec(make.Apply(List.nil(), make.Ident(names.fromString("assignTo")), List.of(
                                make.Select(type, names._class),
                                make.Ident(names.fromString(name)),
                                make.Literal(TypeTag.CLASS, fieldName)
                        )))
                )),
                null                                                    // Default Value ?!?!
        ));
    }

    private String getClassName(Runnable access) {
        try {
            access.run();
        } catch (MirroredTypeException ex) {
            return ex.getTypeMirror().toString();
        }
        return null;
    }

}
