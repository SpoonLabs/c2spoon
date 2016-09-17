package fr.inria.spirals.c2spoon.treeBuilder;

import org.jdom.Element;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;

import java.util.List;

class BlockFactory {
	/**
	 * Create a CtBlock
	 * @param builder the main builder of the project
	 * @param o the srcml block
	 * @return the CtBlock
	 */
	protected static CtBlock create(SrcmlTreeBuilder builder, Element o) {
		CtBlock<Object> block = builder.getSpoon().getFactory().Core().createBlock();
		block.setParent(builder.getStack().lastElement());
		builder.getStack().add(block);

		List children = o.getChildren();
		for (int i = 0; i < children.size(); i++) {
			Object child = children.get(i);
			CtElement element = builder.visit(child);
			if(element == null) {
				continue;
			}
			if(element instanceof CtBlock) {
				List<CtStatement> statements = ((CtBlock) element).getStatements();
				for (int j = 0; j < statements.size(); j++) {
					CtStatement ctStatement = statements.get(j);
					block.addStatement(ctStatement);
				}
			} else if(element instanceof CtStatement) {
				block.addStatement((CtStatement) element);
			}
		}

		builder.getStack().remove(block);
		return block;
	}
}
