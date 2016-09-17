package fr.inria.spirals.c2spoon.treeBuilder;

import org.jdom.Element;
import spoon.reflect.reference.CtTypeReference;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by thomas on 04/02/16.
 */
public class TypeFoctory {
	private static Map<SrcmlTreeBuilder, CtTypeReference> lastTypeReference = new HashMap<>();

	public static CtTypeReference create(SrcmlTreeBuilder project, Element o) {
		Element nameElement = o.getChild("name", o.getNamespace());
		if(nameElement == null && o.getAttribute("ref").getValue().equals("prev")) {
			return lastTypeReference.get(project);
		}
		String value = nameElement.getValue();
		CtTypeReference<Object> reference = project.getSpoon().getFactory().Type().createReference(value);
		lastTypeReference.put(project, reference);
		return reference;
	}
}
