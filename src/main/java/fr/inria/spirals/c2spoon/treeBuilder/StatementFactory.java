package fr.inria.spirals.c2spoon.treeBuilder;

import fr.inria.spirals.c2spoon.spoon.PositionCtElement;
import org.jdom.Element;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.ArrayList;
import java.util.List;

public class StatementFactory {
	public static CtReturn createReturn(SrcmlTreeBuilder builder, Element o) {
		CtReturn<Object> ctReturn = builder.getSpoon().getFactory().Core()
				.createReturn();

		builder.getStack().add(ctReturn);

		CtElement expr = builder.visit(o.getChild("expr", o.getNamespace()));
		if(expr instanceof CtExpression) {
			ctReturn.setReturnedExpression((CtExpression<Object>) expr);
		} else {
			throw new RuntimeException("Invalid return expression " + expr);
		}

		builder.getStack().remove(ctReturn);

		return ctReturn;
	}

	public static CtStatement createStatement(SrcmlTreeBuilder project, Element o) {
		List<CtElement> elements = new ArrayList<>();
		List children = o.getChildren();
		for (int i = 0; i < children.size(); i++) {
			Object node = children.get(i);
			CtElement element = project.visit(node);
			if(element == null) {
				continue;
			}
			if(element instanceof PositionCtElement) {
				for (int j = 0; j < elements.size(); j++) {
					CtElement ctElement = elements.get(j);
					ctElement.setPosition(element.getPosition());
				}
				continue;
			}
			elements.add(element);
		}
		if(elements.size() == 1 && elements.get(0) instanceof CtStatement) {
			return (CtStatement) elements.get(0);
		}
		CtBlock<Object> block = project.getSpoon().getFactory().Core().createBlock();
		for (int i = 0; i < elements.size(); i++) {
			CtElement element = elements.get(i);
			if(element instanceof CtStatement) {
				block.addStatement((CtStatement) element);
			} else {
				for (int j = 0; j < children.size(); j++) {
					Object node = children.get(j);
					project.visit(node);
				}
				throw new RuntimeException("Invalid statement " + element);
			}
		}
		return block;
	}

	public static CtLocalVariable createLocalVariable(SrcmlTreeBuilder builder, Element o) {
		Factory factory = builder.getSpoon().getFactory();
		CtLocalVariable<Object> localVariable = factory
				.Core().createLocalVariable();
		builder.getStack().add(localVariable);

		Element nameElement = o.getChild("name", o.getNamespace());
		if(nameElement == null) {
			builder.getStack().remove(localVariable);
			return null;
		}
		String name = nameElement.getValue();
		CtTypeReference type = (CtTypeReference) builder.visit(o.getChild("type", o.getNamespace()));
		if (nameElement.getChild("index", o.getNamespace()) != null) {
			type = factory.Type().createArrayReference(type);
			name = nameElement.getChildText("name", o.getNamespace());

			localVariable.setSimpleName(name);
			localVariable.setType(type);

			CtNewArray<Object> newArray = factory.Core().createNewArray();
			newArray.setType(type);
			Element init = o.getChild("init", o.getNamespace());

			if(init != null) {
				Element expr = init.getChild("expr", o.getNamespace());
				Element block = expr.getChild("block", o.getNamespace());

				if(block == null) {
					CtExpression value = (CtExpression) builder.visit(expr);
					localVariable.setType(factory.Code().createCtTypeReference(String.class));
					localVariable.setDefaultExpression(value);
				} else {
					List children = block.getChildren();
					for (int i = 0; i < children.size(); i++) {
						CtElement element = builder.visit(children.get(i));
						if (element instanceof CtExpression) {
							newArray.addElement((CtExpression<?>) element);
						} else if (element instanceof PositionCtElement) {
							localVariable.setPosition(element.getPosition());
						}
					}
				}
			} else {
				CtElement index = builder
						.visit(nameElement.getChild("index", o.getNamespace()));
				if(index == null) {
					index = factory.Code().createLiteral(0);
				}
				newArray.addDimensionExpression((CtExpression<Integer>) index);
			}

			localVariable.setDefaultExpression(newArray);
		} else {
			localVariable.setSimpleName(name);
			localVariable.setType(type);

			CtElement init = builder.visit(o.getChild("init", o.getNamespace()));
			if(init != null) {
				localVariable.setDefaultExpression((CtExpression<Object>) init);
			}
		}

		builder.getStack().remove(localVariable);

		return localVariable;
	}

	public static CtElement createIf(SrcmlTreeBuilder builder, Element o) {
		CtIf anIf = builder.getSpoon().getFactory().Core().createIf();

		builder.getStack().add(anIf);
		builder.getStack().add(null);

		CtExpression condition = (CtExpression) builder.visit(o.getChild("condition", o.getNamespace()));

		builder.getStack().remove(null);
		anIf.setCondition(condition);

		CtStatement then = (CtStatement) builder.visit(o.getChild("then", o.getNamespace()));
		anIf.setThenStatement(then);

		CtStatement anElse = (CtStatement) builder.visit(o.getChild("else", o.getNamespace()));

		CtIf lastelseIf = anIf;

		List listElseIf = o.getChildren("elseif", o.getNamespace());
		for (int i = 0; i < listElseIf.size(); i++) {
			CtIf elseif = (CtIf) builder.visit(listElseIf.get(i));
			lastelseIf.setElseStatement(elseif);
			lastelseIf = elseif;
		}
		lastelseIf.setElseStatement(anElse);

		builder.getStack().remove(anIf);
		return anIf;
	}

	public static CtWhile createWhile(SrcmlTreeBuilder builder, Element o) {
		CtWhile aWhile = builder.getSpoon().getFactory().Core().createWhile();

		builder.getStack().add(aWhile);

		CtExpression condition = (CtExpression) builder.visit(o.getChild("condition", o.getNamespace()));
		aWhile.setLoopingExpression(condition);

		CtStatement block = (CtStatement) builder.visit(o.getChild("block", o.getNamespace()));
		aWhile.setBody(block);

		builder.getStack().remove(aWhile);

		return aWhile;
	}

	public static CtDo createDo(SrcmlTreeBuilder project, Element o) {
		CtDo aDo = project.getSpoon().getFactory().Core().createDo();
		project.getStack().add(aDo);


		CtExpression condition = (CtExpression) project.visit(o.getChild("condition", o.getNamespace()));
		aDo.setLoopingExpression(condition);

		CtStatement block = (CtStatement) project.visit(o.getChild("block", o.getNamespace()));
		aDo.setBody(block);

		project.getStack().remove(aDo);

		return aDo;
	}

	public static CtSwitch createSwitch(SrcmlTreeBuilder project, Element o) {
		CtSwitch aSwitch = project.getSpoon().getFactory().Core().createSwitch();
		project.getStack().add(aSwitch);


		CtExpression condition = (CtExpression) project.visit(o.getChild("condition", o.getNamespace()));
		aSwitch.setSelector(condition);

		CtBlock block = (CtBlock) project.visit(o.getChild("block", o.getNamespace()));
		CtCase lastCase = null;
		for (int i = 0; i < block.getStatements().size(); i++) {
			CtStatement statement = block.getStatements().get(i);
			if(statement instanceof CtCase) {
				lastCase = (CtCase) statement;
			} else {
				lastCase.addStatement(statement);
			}
		}
		aSwitch.setCases(block.getElements(new TypeFilter<CtCase>(CtCase.class)));

		project.getStack().remove(aSwitch);

		return aSwitch;
	}

	public static CtCase createCase(SrcmlTreeBuilder project, Element o) {
		CtCase aCase = project.getSpoon().getFactory().Core().createCase();
		project.getStack().add(aCase);

		aCase.setCaseExpression((CtExpression) project.visit(o.getChild("expr", o.getNamespace())));

		project.getStack().remove(aCase);
		return aCase;
	}

	public static CtFor createFor(SrcmlTreeBuilder project, Element o) {
		CtFor ctFor = project.getSpoon().getFactory().Core().createFor();
		project.getStack().add(ctFor);

		Element control = o.getChild("control", o.getNamespace());

		CtStatement init = (CtStatement) project.visit(control.getChild("init", o.getNamespace()));
		if(init instanceof CtStatementList) {
			for (int i = 0;
				 i < ((CtStatementList) init).getStatements().size(); i++) {
				CtStatement ctStatement = ((CtStatementList) init)
						.getStatements().get(i);
				ctFor.addForInit(ctStatement);
			}
		} else {
			ctFor.addForInit(init);
		}

		CtStatement incr = (CtStatement) project.visit(control.getChild("incr", o.getNamespace()));
		if(incr instanceof CtStatementList) {
			for (int i = 0;
				 i < ((CtStatementList) incr).getStatements().size(); i++) {
				CtStatement ctStatement = ((CtStatementList) incr)
						.getStatements().get(i);
				ctFor.addForUpdate(ctStatement);
			}
		} else {
			ctFor.addForUpdate(incr);
		}

		CtExpression condition = (CtExpression) project.visit(control.getChild("condition", o.getNamespace()));
		ctFor.setExpression(condition);

		CtStatement block = (CtStatement) project.visit(o.getChild("block", o.getNamespace()));
		ctFor.setBody(block);

		project.getStack().remove(ctFor);

		return ctFor;
	}

	public static CtField createConstant(SrcmlTreeBuilder project, Element o) {
		CtField<Object> field = project.getSpoon().getFactory().Core()
				.createField();

		project.getStack().add(field);

		field.addModifier(ModifierKind.FINAL);
		field.addModifier(ModifierKind.STATIC);
		CtExpression value = (CtExpression) project.visit(o.getChild("value", o.getNamespace()));
		field.setType(value.getType());
		field.setDefaultExpression(value);

		String name = o.getChild("macro", o.getNamespace()).getValue();
		field.setSimpleName(name);


		project.getStack().remove(field);

		return field;
	}
}
