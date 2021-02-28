package compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import compiler.AST.*;
import compiler.lib.*;

public class TypeRels {
	
	static Map<String,String> superType;
	
	//Ritorna il TypeNode che e' supertipo comune tra "a" e "b", che sono i tipi delle espressioni in if-then-else.
	public static TypeNode lowestCommonAncestor(TypeNode a, TypeNode b) {
		if(a instanceof RefTypeNode || a instanceof EmptyTypeNode && b instanceof RefTypeNode || b instanceof EmptyTypeNode) {
			// se uno tra a e b e'un EmptyTypeNode torna l'altro
			if (a instanceof EmptyTypeNode) {
				return b;
			}
			if (b instanceof EmptyTypeNode) {
				return a;
			}
			
			return isSuperOptimized((RefTypeNode)a, (RefTypeNode)b);
		}
		// per tipi bool/int ritorna int se almeno uno dei due e'int, bool altrimenti
		if(a instanceof IntTypeNode || a instanceof BoolTypeNode && b instanceof IntTypeNode || b instanceof BoolTypeNode) {
			if(a instanceof IntTypeNode || b instanceof IntTypeNode) {
				return new IntTypeNode();
			} else {
				return new BoolTypeNode();
			}
		}
		
		if(a instanceof ArrowTypeNode && b instanceof ArrowTypeNode && ((ArrowTypeNode)a).parlist.size() == ((ArrowTypeNode)b).parlist.size()) {
			ArrowTypeNode a1 = (ArrowTypeNode) a;
			ArrowTypeNode b1 = (ArrowTypeNode) b;
			
			// lowestCommonAncestor per il tipo di ritorno (covarianza)
			TypeNode lca = lowestCommonAncestor(a1.ret, b1.ret);
			
			//il tipo dell'i-esimo paramentro del LowestComonAncestor e' il tipo che e' sottotipo dell'altro (controvarianza)
			List<TypeNode> subTypes = new ArrayList<>();
			for(int i = 0; i < a1.parlist.size(); i++) {
				if (isSubtype(a1.parlist.get(i), b1.parlist.get(i))) {
					subTypes.add(a1.parlist.get(i));
				} else if (isSubtype(b1.parlist.get(i), a1.parlist.get(i))) {
					subTypes.add(b1.parlist.get(i));
				}
			}
			
			//se i controlli sono andati bene allora ritorna un tipo funzionale che ha come tipo di ritorno il risultato della
			//chiamata ricorsiva (covarianza) e come tipo di parametro i-esimo il
			//tipo che è sottotipo dell'altro (controvarianza)
			if(lca != null && subTypes.size() == a1.parlist.size()) {
				return new ArrowTypeNode(subTypes, lca);
			} else {
				return null;
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
			// controllo che vi sia una relazione di co-varianza sul tipo di ritorno.
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
		// considera la classe di a controllando che le sue superclassi siano sottotipo di b
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
