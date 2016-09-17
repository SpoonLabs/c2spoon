package fr.inria.spirals.c2spoon;

import fr.inria.spirals.c2spoon.treeBuilder.SrcmlTreeBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.FileReader;
import java.util.List;

public class Main {
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("The first parameter is the path to the srcml file.");
			return;
		}
		SAXBuilder sxb = new SAXBuilder();
		Document document;
		try {
			document = sxb.build(new FileReader(args[0]));
		}  catch(Exception e){
			throw new RuntimeException("Dataset XML not found", e);
		}

		List children = document.getRootElement().getChildren();
		for (int i = 0; i < children.size(); i++) {
			if (!(children.get(i) instanceof Element)) {
				continue;
			}
			Element o = (Element) children.get(i);
			SrcmlTreeBuilder builder = new SrcmlTreeBuilder();

			builder.build(o.getAttribute("filename").getValue(), o);

			builder.getSpoon().prettyprint();
		}
	}
}
