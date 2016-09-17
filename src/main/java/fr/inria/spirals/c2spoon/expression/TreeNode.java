package fr.inria.spirals.c2spoon.expression;

import fr.inria.spirals.c2spoon.spoon.OpenCtElement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
	private List<TreeNode> children = new ArrayList<>();
	private TreeNode parent;
	private CtElement expression;

	public TreeNode(CtElement expression, TreeNode parent) {
		this.expression = expression;
		this.parent = parent;
	}

	public CtElement getExpression() {
		return expression;
	}

	public TreeNode getParent() {
		return parent;
	}

	public void setParent(TreeNode parent) {
		this.parent = parent;
	}

	public List<TreeNode> getChildren() {
		return children;
	}

	public void replaceChild(TreeNode old, TreeNode newChild) {
		int i = this.getChildren().indexOf(old);
		if (i > -1) {
			this.getChildren().set(i, newChild);
		}
		newChild.setParent(this);
	}
	public void addChild(TreeNode e) {
		children.add(e);
		if (e.getParent() != null && e.getParent() != this) {
			e.getParent().replaceChild(e, this);
		}
		e.setParent(this);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.getExpression() instanceof OpenCtElement) {
			sb.append("(");
			for (TreeNode treeNode : children) {
				sb.append(treeNode.toString());
			}
			sb.append(")");
			return sb.toString();
		}
		if (expression instanceof CtTypeReference) {
			sb.append("(");
			sb.append(expression.toString());
			sb.append(")");
			if (!getChildren().isEmpty()) {
				sb.append(getChildren().get(0).toString());
			} else {
				sb.append("?");
			}
			return sb.toString();
		}
		switch (this.getChildren().size()) {
		case 0:
			return getExpression().toString();
		case 1:
			sb.append(getExpression().toString());
			sb.append("(");
			sb.append(getChildren().get(0).toString());
			sb.append(")");
			return sb.toString();
		case 2:
			sb.append("(");
			sb.append(getChildren().get(0).toString());
			sb.append(" ");
			sb.append(getExpression().toString());
			sb.append(" ");
			sb.append(getChildren().get(1).toString());
			sb.append(")");
			return sb.toString();
		}
		return super.toString();
	}
}
