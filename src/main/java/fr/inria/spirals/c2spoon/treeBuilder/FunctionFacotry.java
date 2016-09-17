package fr.inria.spirals.c2spoon.treeBuilder;

import org.jdom.Element;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

import java.util.List;

public class FunctionFacotry {
	public static CtMethod create(SrcmlTreeBuilder project, Element o) {
		Factory factory = project.getSpoon().getFactory();
		CtMethod ctMethod = factory.Core().createMethod();
		ctMethod.setParent(project.getCurrentClass());

		project.getStack().add(ctMethod);

		String methodName = o.getChild("name", o.getNamespace()).getValue();
		ctMethod.setSimpleName(methodName);

		CtTypeReference<Object> returnType = (CtTypeReference<Object>) project.visit(o.getChild("type", o.getNamespace()));
		ctMethod.setType(returnType);

		Element parameter_list = o.getChild("parameter_list", o.getNamespace());
		List parameters = parameter_list.getChildren("parameter", o.getNamespace());
		for (int i = 0; i < parameters.size(); i++) {
			CtElement parm = project.visit(parameters.get(i));
			if(parm == null) {
				continue;
			}
			if(parm instanceof CtParameter) {
				ctMethod.addParameter((CtParameter<?>) parm);
			} else {
				throw new RuntimeException("Invalid parameter " + parm);
			}
		}

		CtElement block = project.visit(o.getChild("block", o.getNamespace()));
		ctMethod.setBody((CtBlock) block);

		project.getStack().remove(ctMethod);
		return ctMethod;
	}

	public static CtParameter createParameter(SrcmlTreeBuilder project, Element o) {
		Factory factory = project.getSpoon().getFactory();

		CtParameter ctParameter = factory.Core().createParameter();

		project.getStack().add(ctParameter);

		CtLocalVariable decl = (CtLocalVariable) project.visit(o.getChild("decl", o.getNamespace()));
		if(decl == null) {
			project.getStack().remove(ctParameter);
			return null;
		}
		ctParameter.setSimpleName(decl.getSimpleName());
		ctParameter.setType(decl.getType());

		project.getStack().remove(ctParameter);

		return ctParameter;
	}
}
