package fr.inria.spirals.c2spoon.treeBuilder;

import fr.inria.spirals.c2spoon.spoon.PositionCtElement;
import org.jdom.Element;
import org.jdom.Text;
import spoon.Launcher;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class SrcmlTreeBuilder {

	private Launcher spoon;
	private CtClass currentClass;
	private Stack<CtElement> stack = new Stack<>();

	public SrcmlTreeBuilder(Launcher spoon) {
		this.spoon = spoon;
	}

	public SrcmlTreeBuilder() {
		this(new Launcher());
	}

	public CtElement build(String name, Element e) {
		currentClass = getSpoon().getFactory().Class().create(name);
		return visit(e);
	}

	CtElement visit(Object o) {
		if (o == null) {
			return null;
		}
		if(o instanceof Text) {
			return visit((Text) o);
		}
		if(o instanceof Element) {
			return visit((Element) o);
		}
		if(o instanceof List) {
			return visit((List) o);
		}
		throw new RuntimeException("Unknown node " + o);
	}


	private CtElement visit(List o) {
		CtElement position = null;
		List<CtElement> elements = new ArrayList<>();
		for (int i = 0; i < o.size(); i++) {
			CtElement o1 = visit(o.get(i));
			if (o1 instanceof PositionCtElement) {
				position = o1;
				continue;
			}
			if (o1 != null) {
				elements.add(o1);
			}
		}
		if (position != null) {
			for (CtElement element : elements) {
				element.setPosition(position.getPosition());
			}
		}
		if (elements.size() == 1) {
			return elements.get(0);
		}
		if(elements.isEmpty()) {
			return null;
		}
		CtBlock ctBlock = spoon.getFactory().Core().createBlock();
		for (CtElement element : elements) {
			ctBlock.addStatement((CtStatement) element);
		}
		return ctBlock;
	}

	private CtElement visit(Text o) {
		String value = o.getValue().trim();
		if(value.isEmpty()) {
			return null;
		}
		return spoon.getFactory().Code().createLiteral(value);
	}

	private CtElement visit(Element o) {
		if(o == null) {
			return null;
		}
		switch (o.getName()) {
		case "function_decl":
		case "empty_stmt":
		case "comment":
		case "macro":
		case "include":
			// ignore
			return null;
		case "define":
			CtField constant = StatementFactory.createConstant(this, o);
			currentClass.addField(constant);
			constant.setParent(currentClass);
			return constant;
		case "type":
			return TypeFoctory.create(this, o);
		case "operator":
			return ExpressionFactory.createOperator(this, o);
		case "value":
		case "literal":
			return ExpressionFactory.createLiteral(this, o);
		case "call":
			return ExpressionFactory.createInvocation(this, o);
		case "condition":
			return ExpressionFactory.createCondition(this, o);
		case "expr":
			return ExpressionFactory.createExpression(this, o);
		case "while":
			return StatementFactory.createWhile(this, o);
		case "do":
			return StatementFactory.createDo(this, o);
		case "for":
			return StatementFactory.createFor(this, o);
		case "switch":
			return StatementFactory.createSwitch(this, o);
		case "case":
			return StatementFactory.createCase(this, o);
		case "default":
			return spoon.getFactory().Core().createCase();
		case "if":
			return StatementFactory.createIf(this, o);
		case "index":
		case "incr":
		case "init":
		case "elseif":
		case "else":
		case "then":
			return visit(o.getChildren());
		case "expr_stmt":
		case "decl_stmt":
			return StatementFactory.createStatement(this, o);
		case "decl":
			return StatementFactory.createLocalVariable(this, o);
		case "return":
			return StatementFactory.createReturn(this, o);
		case "argument":
			return ExpressionFactory.createExpression(this, o);
		case "parameter":
			return FunctionFacotry.createParameter(this, o);
		case "unit":
			List content = o.getContent();
			for (Iterator iterator = content.iterator(); iterator.hasNext(); ) {
				Object next = iterator.next();
				visit(next);
			}
			return currentClass;
		case "block":
			CtBlock<Object> block = BlockFactory.create(this, o);
			return block;
		case "function":
			CtMethod ctMethod = FunctionFacotry.create(this, o);
			currentClass.addMethod(ctMethod);
			return ctMethod;
		case "name":
			return ExpressionFactory.createName(this, o);
		case "break":
			return this.spoon.getFactory().Core().createBreak();
		case "continue":
			return this.spoon.getFactory().Core().createContinue();
		case "sizeof":
			return ExpressionFactory.createSizeOf(this, o);
		case "position":
			return ExpressionFactory.createPosition(this, o);
		default:
			throw new RuntimeException("Unknown Element " + o.getNamespace() + o.getName());
		}
	}

	public spoon.Launcher getSpoon() {
		return spoon;
	}

	public CtClass<Object> getCurrentClass() {
		return currentClass;
	}

	public Stack<CtElement> getStack() {
		return stack;
	}

	public <T> T getElementInStack(Class<T> aClass) {
		for (int i = stack.size() -1; i >= 0; i--) {
			CtElement ctElement = stack.get(i);
			if (ctElement != null && aClass.isAssignableFrom(ctElement.getClass())) {
				return (T) ctElement;
			}
		}
		return null;
	}
}
