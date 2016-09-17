package fr.inria.spirals.c2spoon.spoon;

import spoon.reflect.visitor.CtVisitor;
import spoon.support.reflect.declaration.CtElementImpl;

public class OpenCtElement extends CtElementImpl {
	@Override
	public void accept(CtVisitor ctVisitor) {
	}

	@Override
	public String toString() {
		return "(";
	}
}