package fr.inria.spirals.c2spoon.treeBuilder;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;

import java.util.List;

public class ExpressionFactoryTest {

	private CtElement createSrcmlTreeBuilder(String filename) {
		SAXBuilder sxb = new SAXBuilder();
		Document document;
		try {
			document = sxb.build(ExpressionFactory.class.getResourceAsStream("/" + filename + ".xml"));
		}  catch(Exception e){
			throw new RuntimeException("Dataset XML not found", e);
		}

		SrcmlTreeBuilder SrcmlTreeBuilder = new SrcmlTreeBuilder();
		List children = document.getRootElement().getChildren();
		for (int i = 0; i < children.size(); i++) {
			if (!(children.get(i) instanceof Element)) {
				continue;
			}
			Element o = (Element) children.get(i);
			return SrcmlTreeBuilder.visit(o);
		}
		return null;
	}

	@Test
	public void createIfExpressionTest1() throws Exception {
		CtIf anIf = (CtIf) createSrcmlTreeBuilder("ifexample1");
		Assert.assertEquals("((str[i]) == 'a') || (((str[i]) == 'e') || (((str[i]) == 'i') || (((str[i]) == 'o') || (((str[i]) == 'u') || ((str[i]) == 'y')))))", anIf.getCondition().toString());
	}

	@Test
	public void createIfExpressionTest2() throws Exception {
		CtIf anIf = (CtIf) createSrcmlTreeBuilder("ifexample2");
		Assert.assertEquals("(score >= B) && (score < A)", anIf.getCondition().toString());
	}

	@Test
	public void createConditionExpressionTest1() throws Exception {
		CtExpression expr = (CtExpression) createSrcmlTreeBuilder("conditionexample1");
		Assert.assertEquals("((in[i]) == \"a\") || (((in[i]) == \"e\") || (((in[i]) == \"o\") || (((in[i]) == \"u\") || ((in[i]) == \"y\"))))", expr.toString());
	}

	@Test
	public void createConditionExpressionTest2() throws Exception {
		CtExpression expr = (CtExpression) createSrcmlTreeBuilder("conditionexample2");
		Assert.assertEquals("(x > (-10)) && (x < 0)", expr.toString());
	}

	@Test
	public void createAssignmentExpressionTest1() throws Exception {
		CtStatement expr = (CtStatement) createSrcmlTreeBuilder("assignmentexample1");
		Assert.assertEquals("digit = n % ((int) (pow(10, a)))", expr.toString());
	}

}