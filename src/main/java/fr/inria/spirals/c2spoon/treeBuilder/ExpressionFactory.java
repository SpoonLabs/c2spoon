package fr.inria.spirals.c2spoon.treeBuilder;

import fr.inria.spirals.c2spoon.expression.TreeNode;
import fr.inria.spirals.c2spoon.spoon.CloseCtElement;
import fr.inria.spirals.c2spoon.spoon.OpenCtElement;
import fr.inria.spirals.c2spoon.spoon.PositionCtElement;
import org.jdom.Element;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.factory.CodeFactory;
import spoon.reflect.factory.CoreFactory;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExpressionFactory {

	public static CtElement createPosition(SrcmlTreeBuilder builder, final Element o) {
		PositionCtElement positionCtElement = new PositionCtElement();
		positionCtElement.setPosition(new SourcePosition() {
			@Override
			public File getFile() {
				return null;
			}

			@Override
			public CompilationUnit getCompilationUnit() {
				return null;
			}

			@Override
			public int getLine() {
				return Integer.parseInt(o.getAttributeValue("line", o.getNamespace()));
			}

			@Override
			public int getEndLine() {
				return -1;
			}

			@Override
			public int getColumn() {
				return Integer.parseInt(o.getAttributeValue("column", o.getNamespace()));
			}

			@Override
			public int getEndColumn() {
				return 0;
			}

			@Override
			public int getSourceEnd() {
				return -1;
			}

			@Override
			public int getSourceStart() {
				return -1;
			}
		});
		return positionCtElement;
	}

	public static CtExpression createExpression(SrcmlTreeBuilder builder, Element o) {
		List<CtElement> elements = new ArrayList<>();
		List<Element> children = o.getChildren();
		CtElement lastElement = null;
		for (Element child : children) {
			String text = child.getTextTrim();
			if (text.equals("(")) {
				elements.add(new OpenCtElement());
				continue;
			} else if (text.equals(")")) {
				elements.add(new CloseCtElement());
				continue;
			}
			CtElement element = builder.visit(child);
			if (lastElement != null) {
				builder.getStack().remove(lastElement);
			}
			lastElement = element;

			if (element != null) {
				builder.getStack().add(element);
				elements.add(element);
			}
		}
		if (lastElement != null) {
			builder.getStack().remove(lastElement);
		}

		if(elements.size() == 1 && elements.get(0) instanceof CtExpression) {
			return (CtExpression) elements.get(0);
		}
		TreeNode treeNode = createTreeNode(elements);
		CtExpression ctExpression = groupExpressions(treeNode);

		return ctExpression;
	}

	private static CtExpression groupExpressions(TreeNode treeNode) {
		CtElement expression = treeNode.getExpression();
		if (expression instanceof OpenCtElement) {
			if (treeNode.getChildren().size() > 1) {
				throw new IllegalArgumentException("No valid expression tree");
			}
			return groupExpressions(treeNode.getChildren().get(0));
		}
		if (expression == null || expression.getFactory() == null) {
			return null;
		}
		expression = expression.getFactory().Core().clone(expression);
		if (expression instanceof CtBinaryOperator) {
			if (treeNode.getChildren().size() < 2) {
				throw new IllegalArgumentException("No valid expression tree");
			}
			CtBinaryOperator binaryOperator = (CtBinaryOperator) expression;
			CtExpression left = groupExpressions(treeNode.getChildren().get(0));
			CtExpression right = groupExpressions(treeNode.getChildren().get(1));
			binaryOperator.setLeftHandOperand(left);
			binaryOperator.setRightHandOperand(right);
			return binaryOperator;
		} else if (expression instanceof CtAssignment) {
			CtAssignment ctAssignment = (CtAssignment) expression;
			CtExpression left = groupExpressions(treeNode.getChildren().get(0));
			CtExpression right = groupExpressions(treeNode.getChildren().get(1));
			ctAssignment.setAssigned(left);
			ctAssignment.setAssignment(right);
			return ctAssignment;
		} else if (expression instanceof CtUnaryOperator) {
			CtUnaryOperator unaryOperator = (CtUnaryOperator) expression;
			CtExpression exp = groupExpressions(treeNode.getChildren().get(0));
			unaryOperator.setOperand(exp);
			return unaryOperator;
		} else if (expression instanceof CtTypeReference) {
			if (treeNode.getChildren().size() < 1) {
				throw new IllegalArgumentException("No valid expression tree");
			}
			CtTypeReference typeReference = (CtTypeReference) expression;
			CtExpression exp = groupExpressions(treeNode.getChildren().get(0));
			exp.addTypeCast(typeReference);
			return exp;
		}
		if (treeNode.getChildren().isEmpty()) {
			return (CtExpression) expression;
		}
		if (expression instanceof CtLiteral) {
			StringBuilder sb = new StringBuilder();
			sb.append(expression.toString());
			for (int i = 0; i < treeNode.getChildren().size(); i++) {
				TreeNode node = treeNode.getChildren().get(i);
				sb.append(node.getExpression().toString());
			}
			return ((CtLiteral) expression).setValue(sb.toString());
		}
		throw new IllegalArgumentException("No valid expression tree");
	}

	private static TreeNode createTreeNode(List<CtElement> elements) {
		TreeNode root = new TreeNode(elements.get(0), null);
		TreeNode current = root;
		for (int i = 1; i < elements.size(); i++) {
			CtElement ctElement = elements.get(i);
			if (current == null) {
				System.out.println(ctElement);
			}
			if (ctElement instanceof CtBinaryOperator || ctElement instanceof CtAssignment) {
				if (getPriority(ctElement) > getPriority(current.getExpression()) && !current.getChildren().isEmpty()) {
					current = current.getChildren().get(current.getChildren().size() - 1);
				}
				TreeNode treeNode = new TreeNode(ctElement, current.getParent());
				treeNode.addChild(current);
				if (current == root) {
					root = treeNode;
				}
				current = treeNode;
			} else if (ctElement instanceof CtUnaryOperator || ctElement instanceof CtTypeReference) {
				if (ctElement instanceof CtUnaryOperator
						&& (((CtUnaryOperator) ctElement).getKind() == UnaryOperatorKind.POSTDEC
							|| ((CtUnaryOperator) ctElement).getKind() == UnaryOperatorKind.POSTINC)) {
					TreeNode treeNode = new TreeNode(ctElement, current.getParent());
					treeNode.addChild(current);
					if (current == root) {
						root = treeNode;
					}
					current = treeNode;
				} else {
					TreeNode treeNode = new TreeNode(ctElement, current);
					current.addChild(treeNode);
					current = treeNode;
				}
			} else if (ctElement instanceof OpenCtElement) {
				TreeNode treeNode = new TreeNode(ctElement, current);
				current.addChild(treeNode);
				current = treeNode;
			} else if (ctElement instanceof CloseCtElement) {
				do {
					if (current.getParent() != null) {
						current = current.getParent();
					}
				} while(current != root && !(current.getExpression() instanceof OpenCtElement));
				TreeNode newChild = current.getChildren().get(0);
				if (current.getChildren().size() == 1) {
					if (newChild.getChildren().isEmpty()) {
						if (current == root) {
							root = newChild;
						} else {
							current.getParent().replaceChild(current, newChild);
						}
						current = newChild;
					}
				}

				if (!(current.getExpression() instanceof CtUnaryOperator || current.getExpression() instanceof CtTypeReference)) {
					TreeNode currentParent = current.getParent();
					if (currentParent != null) {
						if (currentParent.getExpression() instanceof CtBinaryOperator
								|| currentParent.getExpression() instanceof CtAssignment) {
							current = currentParent;
						}
					}
				}
			} else {
				TreeNode treeNode = new TreeNode(ctElement, current);
				current.addChild(treeNode);
				if (current.getExpression() instanceof OpenCtElement) {
					current = treeNode;
				}
				if (current != root && current.getExpression() instanceof CtUnaryOperator) {
					current = current.getParent();
				}
			}
		}
		return root;
	}

	private static int getPriority(CtElement ctElement) {
		if (ctElement instanceof CtTypeReference) {
			return 99;
		}
		if (ctElement instanceof CtUnaryOperator) {
			return 99;
		}
		if (ctElement instanceof CtAssignment) {
			return -1;
		}

		if (ctElement instanceof CtBinaryOperator) {
			switch (((CtBinaryOperator) ctElement).getKind()) {
			case OR:
				return 2;
			case AND:
				return 3;
			case BITOR:
				return 0;
			case BITXOR:
				return 0;
			case BITAND:
				return 1;
			case EQ:
			case NE:
				return 4;
			case LT:
			case GT:
			case LE:
			case GE:
				return 4;
			case SL:
				return 1;
			case SR:
				return 1;
			case USR:
				return 1;
			case PLUS:
			case MINUS:
				return 5;
			case MUL:
			case DIV:
				return 6;
			case MOD:
				return 3;
			case INSTANCEOF:
				return 0;
			}
		}
		if (ctElement instanceof OpenCtElement) {
			return 99;
		}
		return -1;
	}

	public static CtLiteral createLiteral(SrcmlTreeBuilder builder, Element o) {
		String valueStr = o.getTextTrim();

		CodeFactory code = builder.getSpoon().getFactory().Code();
		TypeFactory type = builder.getSpoon().getFactory().Type();
		if(valueStr.startsWith("'") && valueStr.replaceAll("\\\\", "").length() == 3) {
			valueStr = o.getValue().substring(1, valueStr.length() - 1).replaceAll("\\\\n", "\n").replaceAll("\\\\'", "'");
			CtLiteral literal = code.createLiteral(valueStr.charAt(0));
			literal.setType(code.createCtTypeReference(char.class));
			return literal;
		}

		if(valueStr.startsWith("\"")) {
			valueStr = o.getValue().substring(1, valueStr.length() - 1).replaceAll("\\\\n", "\n").replaceAll("\\\\'", "'");
			CtLiteral literal = code.createLiteral(valueStr);
			literal.setType(type.stringType());
			return literal;
		}
		if(valueStr.contains(".")) {
			CtLiteral literal = code.createLiteral(Double.parseDouble(valueStr.replaceAll(";", "")));
			literal.setType(type.doublePrimitiveType());
			return literal;
		}
		try {
			CtLiteral literal = code.createLiteral(Integer.parseInt(valueStr.replaceAll(";", "")));
			literal.setType(type.integerPrimitiveType());
			return literal;
		} catch (NumberFormatException e) {
			CtLiteral literal = code.createLiteral(
					Long.parseLong(valueStr.replaceAll(";", "")));
			literal.setType(type.longPrimitiveType());
			return literal;
		}
	}

	public static CtElement createOperator(SrcmlTreeBuilder builder, Element o) {
		CoreFactory core = builder.getSpoon().getFactory().Core();
		TypeFactory type = builder.getSpoon().getFactory().Type();

		String valueStr = o.getValue();
		CtElement lastCtElement = builder.getStack().lastElement();
		switch (valueStr) {
		case "(":
		case ")":
			break;
		case "<":
			CtBinaryOperator binaryOperator = core.createBinaryOperator();
			binaryOperator.setKind(BinaryOperatorKind.LT);
			binaryOperator.setType(type.booleanPrimitiveType());
			return binaryOperator;
		case "<=":
			binaryOperator = core.createBinaryOperator();
			binaryOperator.setKind(BinaryOperatorKind.LE);
			binaryOperator.setType(type.booleanPrimitiveType());
			return binaryOperator;
		case ">":
			binaryOperator = core.createBinaryOperator();
			binaryOperator.setKind(BinaryOperatorKind.GT);
			binaryOperator.setType(type.booleanPrimitiveType());
			return binaryOperator;
		case ">=":
			binaryOperator = core.createBinaryOperator();
			binaryOperator.setKind(BinaryOperatorKind.GE);
			binaryOperator.setType(type.booleanPrimitiveType());
			return binaryOperator;
		case "==":
			binaryOperator = core.createBinaryOperator();
			binaryOperator.setKind(BinaryOperatorKind.EQ);
			binaryOperator.setType(type.booleanPrimitiveType());
			return binaryOperator;
		case "!=":
			binaryOperator = core.createBinaryOperator();
			binaryOperator.setKind(BinaryOperatorKind.NE);
			binaryOperator.setType(type.booleanPrimitiveType());
			return binaryOperator;
		case "&&":
			binaryOperator = core.createBinaryOperator();
			binaryOperator.setKind(BinaryOperatorKind.AND);
			binaryOperator.setType(type.booleanPrimitiveType());
			return binaryOperator;
		case "||":
			binaryOperator = core.createBinaryOperator();
			binaryOperator.setKind(BinaryOperatorKind.OR);
			binaryOperator.setType(type.booleanPrimitiveType());
			return binaryOperator;
		case "%":
			binaryOperator = core.createBinaryOperator();
			binaryOperator.setKind(BinaryOperatorKind.MOD);
			binaryOperator.setType(type.integerPrimitiveType());
			return binaryOperator;
		case "+":
			if(lastCtElement == null
					|| (lastCtElement instanceof CtStatement && !(lastCtElement instanceof CtExpression))
					|| lastCtElement instanceof CtBinaryOperator
					|| lastCtElement instanceof CtAssignment) {
				CtUnaryOperator<Object> unaryOperator = core.createUnaryOperator();
				unaryOperator.setKind(UnaryOperatorKind.POS);
				return unaryOperator;
			}
			binaryOperator = core.createBinaryOperator();
			binaryOperator.setKind(BinaryOperatorKind.PLUS);
			binaryOperator.setType(type.createReference(Number.class));
			return binaryOperator;
		case "-":
			if(lastCtElement == null
					|| (lastCtElement instanceof CtStatement && !(lastCtElement instanceof CtExpression))
					|| lastCtElement instanceof CtBinaryOperator
					|| lastCtElement instanceof CtAssignment) {
				CtUnaryOperator<Object> unaryOperator = core.createUnaryOperator();
				unaryOperator.setKind(UnaryOperatorKind.NEG);
				return unaryOperator;
			}
			binaryOperator = core.createBinaryOperator();
			binaryOperator.setKind(BinaryOperatorKind.MINUS);
			binaryOperator.setType(type.createReference(Number.class));
			return binaryOperator;
		case "*":
			if(lastCtElement == null
					|| lastCtElement instanceof CtStatement
					|| lastCtElement instanceof CtBinaryOperator
					|| lastCtElement instanceof CtAssignment
					|| lastCtElement instanceof CtUnaryOperator) {
				// C pointer
				return null;
			}
			binaryOperator = core.createBinaryOperator();
			binaryOperator.setKind(BinaryOperatorKind.MUL);
			binaryOperator.setType(type.createReference(Number.class));
			return binaryOperator;
		case "/":
			binaryOperator = core.createBinaryOperator();
			binaryOperator.setKind(BinaryOperatorKind.DIV);
			binaryOperator.setType(type.createReference(Number.class));
			return binaryOperator;
		case "!":
			CtUnaryOperator unaryOperator = core.createUnaryOperator();
			unaryOperator.setKind(UnaryOperatorKind.NOT);
			unaryOperator.setType(type.booleanPrimitiveType());
			return unaryOperator;
		case "++":
			unaryOperator = core.createUnaryOperator();
			unaryOperator.setKind(UnaryOperatorKind.POSTINC);
			unaryOperator.setType(type.createReference(Number.class));
			return unaryOperator;
		case "--":
			unaryOperator = core.createUnaryOperator();
			unaryOperator.setKind(UnaryOperatorKind.POSTDEC);
			unaryOperator.setType(type.createReference(Number.class));
			return unaryOperator;
		case "=":
			CtIf aIf = builder.getElementInStack(CtIf.class);
			if (aIf != null && aIf.getCondition() == null) {
				binaryOperator = core.createBinaryOperator();
				binaryOperator.setKind(BinaryOperatorKind.EQ);
				binaryOperator.setType(type.booleanPrimitiveType());
				return binaryOperator;
			}
			CtAssignment<Object, Object> assignment = core.createAssignment();
			return assignment;
		case "-=":
			CtOperatorAssignment<Object, Object> operatorAssignment = core
					.createOperatorAssignment();
			operatorAssignment.setKind(BinaryOperatorKind.MINUS);
			return operatorAssignment;
		case "+=":
			operatorAssignment = core
					.createOperatorAssignment();
			operatorAssignment.setKind(BinaryOperatorKind.PLUS);
			return operatorAssignment;
		case "*=":
			operatorAssignment = core
					.createOperatorAssignment();
			operatorAssignment.setKind(BinaryOperatorKind.MUL);
			return operatorAssignment;
		case "/=":
			operatorAssignment = core
					.createOperatorAssignment();
			operatorAssignment.setKind(BinaryOperatorKind.DIV);
			return operatorAssignment;
		case "%=":
			operatorAssignment = core
					.createOperatorAssignment();
			operatorAssignment.setKind(BinaryOperatorKind.MOD);
			return operatorAssignment;
		case "&=":
			operatorAssignment = core
					.createOperatorAssignment();
			operatorAssignment.setKind(BinaryOperatorKind.AND);
			return operatorAssignment;
		case "|=":
			operatorAssignment = core
					.createOperatorAssignment();
			operatorAssignment.setKind(BinaryOperatorKind.OR);
			return operatorAssignment;
		}
		return null;
	}

	public static CtInvocation createInvocation(SrcmlTreeBuilder builder, Element o) {
		CoreFactory core = builder.getSpoon().getFactory().Core();
		CtInvocation<Object> invocation = core.createInvocation();

		builder.getStack().add(invocation);

		String name = o.getChild("name", o.getNamespace()).getValue();

		CtExecutableReference<Object> executableReference = core.createExecutableReference();
		executableReference.setSimpleName(name);
		invocation.setExecutable(executableReference);

		builder.getStack().push(null);
		List argument_list = o.getChild("argument_list", o.getNamespace()).getChildren();
		for (int i = 0; i < argument_list.size(); i++) {
			CtElement element =  builder.visit(argument_list.get(i));
			if(element instanceof CtExpression) {
				invocation.addArgument((CtExpression<?>) element);
			} else if(element instanceof PositionCtElement) {
				invocation.setPosition(element.getPosition());
			} else {
				throw new RuntimeException("Invalid argument " + element);
			}
		}
		builder.getStack().remove(null);
		builder.getStack().remove(invocation);

		return invocation;
	}

	public static CtExpression createCondition(SrcmlTreeBuilder project, Element o) {
		return (CtExpression) project.visit(o.getChild("expr", o.getNamespace()));
	}

	public static CtElement createName(SrcmlTreeBuilder builder, Element o) {
		String name = o.getValue();
		switch (name) {
		case "int":
			return builder.getSpoon().createFactory()
					.Type().integerPrimitiveType();
		case "float":
			return builder.getSpoon().createFactory()
					.Type().floatPrimitiveType();
		case "double":
			return builder.getSpoon().createFactory()
					.Type().doublePrimitiveType();
		case "byte":
			return builder.getSpoon().createFactory()
					.Type().bytePrimitiveType();
		case "char":
			return builder.getSpoon().createFactory()
					.Type().characterPrimitiveType();
		}
		return createVariableRead(builder, o);
	}

	public static CtExpression createVariableRead(SrcmlTreeBuilder builder, Element o) {
		Factory factory = builder.getSpoon().getFactory();
		if(o.getChild("index", o.getNamespace()) != null) {
			return createArrayRead(builder, o);
		}
		CtVariableAccess variableRead = factory.Core().createVariableRead();

		builder.getStack().add(variableRead);

		CtVariableReference variableReference = factory.Core().createLocalVariableReference();
		variableReference.setSimpleName(o.getValue());
		variableReference.setType(factory.Type().nullType());

		CtVariable variableFromStack = getVariableFromStack(builder, o.getValue());
		if(variableFromStack != null) {
			variableReference = variableFromStack.getReference();
			variableRead.setType(variableFromStack.getType());
		} else if (variableReference.getSimpleName().equals("stdin")) {
			CtFieldReference<Object> fieldReference = factory.Core().createFieldReference();
			fieldReference.setDeclaringType(factory.Code().createCtTypeReference(System.class));
			fieldReference.setStatic(true);
			fieldReference.setSimpleName("in");
			fieldReference.setType(factory.Code().createCtTypeReference(InputStream.class));
			variableReference = fieldReference;
			builder.getStack().remove(variableRead);
			variableRead = factory.Code().createVariableRead(variableReference, true);
			variableRead.setType(factory.Code().createCtTypeReference(InputStream.class));
		}
		variableRead.setVariable(variableReference);

		builder.getStack().remove(variableRead);

		return variableRead;
	}

	private static CtArrayRead<Object> createArrayRead(SrcmlTreeBuilder builder, Element o) {
		Factory factory = builder.getSpoon().getFactory();
		CtArrayRead<Object> variableRead = factory.Core().createArrayRead();

		builder.getStack().add(variableRead);

		String name = o.getChildText("name", o.getNamespace());

		CtVariable variableFromStack = getVariableFromStack(builder, name);
		CtVariableReference variableReference = factory.Core().createLocalVariableReference();
		if(variableFromStack != null) {
			variableReference = variableFromStack.getReference();
			variableRead.setType(factory.Code().createCtTypeReference(variableFromStack.getType().getActualClass().getComponentType()));
		}
		variableRead.setTarget(factory.Code().createVariableRead(variableReference, false));
		variableReference.setSimpleName(name);
		variableRead.setIndexExpression((CtExpression<Integer>) builder.visit(o.getChild("index", o.getNamespace())));

		return variableRead;
	}

	private static CtVariable getVariableFromStack(SrcmlTreeBuilder builder, final String name) {
		for (int i = builder.getStack().size() - 1; i >= 0; i--) {
			CtElement element = builder.getStack().get(i);
			if (element == null) {
				continue;
			}
			List<CtVariable> elements = element
					.getElements(new TypeFilter<CtVariable>(CtVariable.class) {
						@Override
						public boolean matches(CtVariable element) {
							if (super.matches(element)) {
								String simpleName = element.getSimpleName();
								return name.equals(simpleName);
							}
							return false;
						}
					});
			if(elements.size() == 1) {
				return elements.get(0);
			}
		}
		return null;
	}

	public static CtElement createSizeOf(SrcmlTreeBuilder builder, Element o) {
		Factory factory = builder.getSpoon().createFactory();
		CtFieldRead<Integer> fieldRead = factory.Core().createFieldRead();

		builder.getStack().add(fieldRead);

		CtExpression ctExpression = (CtExpression) builder
				.visit(o.getChild("argument_list", o.getNamespace())
						.getChild("argument", o.getNamespace()));

		fieldRead.setTarget(ctExpression);
		fieldRead.setType(factory.Type().integerPrimitiveType());
		fieldRead.setVariable(factory.Field().createReference(ctExpression.getType(), factory.Type().integerPrimitiveType(), "length"));

		builder.getStack().remove(fieldRead);

		return fieldRead;
	}
}
