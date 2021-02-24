package compiler;

import java.util.List;
import java.util.Map;

import compiler.AST.*;
import compiler.lib.*;

public class TypeRels {
	
	static Map<String,String> superType;
	
	public static TypeNode lowestCommonAncestor(TypeNode a, TypeNode b) {
		if(a instanceof RefTypeNode || a instanceof EmptyTypeNode && b instanceof RefTypeNode || b instanceof EmptyTypeNode) {
			if (a instanceof EmptyTypeNode) {
				return b;
			}
			if (b instanceof EmptyTypeNode) {
				return a;
			}
			return isSuperOptimized((RefTypeNode)a, (RefTypeNode)b);
		}
		if(a instanceof IntTypeNode || a instanceof BoolTypeNode && b instanceof IntTypeNode || b instanceof BoolTypeNode) {
			if(a instanceof IntTypeNode || b instanceof IntTypeNode) {
				return new IntTypeNode();
			} else {
				return new BoolTypeNode();
			}
		}
		if(a instanceof ArrowTypeNode && b instanceof ArrowTypeNode && ((ArrowTypeNode)a).parlist.size() == ((ArrowTypeNode)b).parlist.size()) {
			ArrowTypeNode a1 = (ArrowTypeNode)a;
			ArrowTypeNode b1 = (ArrowTypeNode)b;
			TypeNode lowComAnc = lowestCommonAncestor(a1.ret, b1.ret);
			if(lowComAnc != null && checkParameters(b1.parlist, a1.parlist)) {
				return new ArrowTypeNode(b1.parlist, lowComAnc);
			}
		}
		return null;
	}

	// valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
	public static boolean isSubtype(TypeNode a, TypeNode b) {
		if(a instanceof EmptyTypeNode && b instanceof RefTypeNode) {
			return true;
		}
		if(a instanceof RefTypeNode && b instanceof RefTypeNode) {
			RefTypeNode a1 = (RefTypeNode)a;
			RefTypeNode b1 = (RefTypeNode)b;
			return a1.id.equals(b1.id) || superType.containsKey(a1.id) && isSuperType(a1.id, b1.id);		
		} 			
		if(a instanceof EmptyTypeNode && (b instanceof RefTypeNode || b instanceof EmptyTypeNode)) {
			return true;
		}
		if(a instanceof ArrowTypeNode && b instanceof ArrowTypeNode) {
			ArrowTypeNode a1 = (ArrowTypeNode)a;
			ArrowTypeNode b1 = (ArrowTypeNode)b;
			return a1.parlist.size() == b1.parlist.size() && isSubtype(a1.ret, b1.ret) && checkParameters(b1.parlist, a1.parlist);
		}
		return a.getClass().equals(b.getClass()) || ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode));
		
	}
	
	//controllo che fra i parametri ci sia una relazione di contro-varianza(li ho invertiti nella chiamata a checkparameter)
	private static boolean checkParameters(List<TypeNode> a, List<TypeNode> b) {
		boolean eq = true;
		for(int i=0; i<a.size() && eq; i++) {
			eq = eq && isSubtype(a.get(i), b.get(i));
		}
		return eq;
	}
	
	private static RefTypeNode isSuperOptimized(RefTypeNode a, RefTypeNode b) {
		if(isSubtype(a, b)) {
			return a;
		} else {
			if(superType.containsKey(a.id)) {
				return isSuperOptimized(new RefTypeNode(superType.get(a.id)), b);
			} else {
				return null;
			}
		}
	}
	
	private static boolean isSuperType(String a, String b) {
		return superType.get(a).equals(b) || (superType.containsKey(superType.get(a)) ? isSuperType(superType.get(a), b) : false);
	}

}
