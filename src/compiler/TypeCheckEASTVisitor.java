package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;
import static compiler.TypeRels.*;

import java.util.HashMap;

//visitNode(n) fa il type checking di un Node n e ritorna:
//- per una espressione, il suo tipo (oggetto BoolTypeNode o IntTypeNode)
//- per una dichiarazione, "null"; controlla la correttezza interna della dichiarazione
//(- per un tipo: "null"; controlla che il tipo non sia incompleto) 
//
//visitSTentry(s) ritorna, per una STentry s, il tipo contenuto al suo interno
public class TypeCheckEASTVisitor extends BaseEASTVisitor<TypeNode,TypeException> {

	TypeCheckEASTVisitor() { super(true); } // enables incomplete tree exceptions 
	TypeCheckEASTVisitor(boolean debug) { super(true,debug); } // enables print for debugging

	//checks that a type object is visitable (not incomplete) 
	private TypeNode ckvisit(TypeNode t) throws TypeException {
		visit(t);
		return t;
	} 
	
	@Override
	public TypeNode visitNode(ProgLetInNode n) throws TypeException {
		if (print) printNode(n);
		TypeRels.superType = new HashMap<>();
		for (Node dec : n.declist)
			try {
				visit(dec);
			} catch (IncomplException e) { 
			} catch (TypeException e) {
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		return visit(n.exp);
	}

	@Override
	public TypeNode visitNode(ProgNode n) throws TypeException {
		if (print) printNode(n);
		return visit(n.exp);
	}

	@Override
	public TypeNode visitNode(FunNode n) throws TypeException {
		if (print) printNode(n,n.id);
		for (Node dec : n.declist)
			try {
				visit(dec);
			} catch (IncomplException e) { 
			} catch (TypeException e) {
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		if ( !isSubtype(visit(n.exp),ckvisit(n.retType)) ) 
			throw new TypeException("Wrong return type for function " + n.id,n.getLine());
		return null;
	}

	@Override
	public TypeNode visitNode(VarNode n) throws TypeException {
		if (print) printNode(n,n.id);
		if ( !isSubtype(visit(n.exp),ckvisit(n.getType())) ) {
			System.out.println(TypeRels.superType.toString());
			throw new TypeException("Incompatible value for variable " + n.id,n.getLine());
		}
		return null;
	}

	@Override
	public TypeNode visitNode(ClassNode n) throws TypeException {
		if (print) printNode(n,n.id);
		
		// visita dei metodi
		for (MethodNode m : n.methods) visit(m);
		
		if(n.superID != null) {
			//aggiornamento di supertipe(mappa classe-superclasse)
			TypeRels.superType.put(n.id, n.superID);
			ClassTypeNode parentCT = (ClassTypeNode)n.superEntry.type;
			for(FieldNode f: n.fields) {
				int position = -f.offset-1;
				// se entro in questo if vuole dire che sto facendo overriding
				if(position < parentCT.allFields.size()) {
					//controllo che il campo nella stessa posizione del padre sia sopratipo
					if(!isSubtype(f.getType(), parentCT.allFields.get(position))) {
						throw new TypeException("Fields must be subtype of the ones declared in superclass",n.getLine());
					}
				}
			}
			for(MethodNode m:n.methods) {
				int position = m.offset;
				if(position < parentCT.allMethods.size()) {
					if(!isSubtype(m.getType(), parentCT.allMethods.get(position))) {
						throw new TypeException("Methods must be subtype of the ones declared in superclass",n.getLine());
					}
				}
			}
		}
		return null;
	}
	
	//identico a funNode
	@Override
	public TypeNode visitNode(MethodNode n) throws TypeException {
		if (print) printNode(n,n.id);
		for (Node dec : n.declist)
			try {
				visit(dec);
			} catch (IncomplException e) { 
			} catch (TypeException e) {
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		if ( !isSubtype(visit(n.exp), ckvisit(n.retType)) ) 
			throw new TypeException("Wrong return type for method " + n.id,n.getLine());
		return null;
	}
	
	// il controllo che ID1.ID2() fosse corretto, era già stato fatto nella visita della sym table. 
	@Override
	public TypeNode visitNode(ClassCallNode n) throws TypeException {
		if (print) printNode(n,n.classID);
		TypeNode t = visit(n.methodEntry); 
		if ( !(t instanceof MethodTypeNode) )
			throw new TypeException("Invocation of a non-method "+n.methodID,n.getLine());
		ArrowTypeNode at = ((MethodTypeNode) t).fun;
		if ( !(at.parlist.size() == n.arglist.size()) )
			throw new TypeException("Wrong number of parameters in the invocation of "+n.methodID,n.getLine());
		for (int i = 0; i < n.arglist.size(); i++)
			if ( !(isSubtype(visit(n.arglist.get(i)),at.parlist.get(i))) )
				throw new TypeException("Wrong type for "+(i+1)+"-th parameter in the invocation of "+n.methodID,n.getLine());
		return at.ret;
	}
	
	@Override
	public TypeNode visitNode(NewNode n) throws TypeException {
		if (print) printNode(n,n.id);
		if ( !(n.arglist.size() == n.arglist.size()) )
			throw new TypeException("Wrong number of parameters in the creation of "+n.id,n.getLine());
		for (int i = 0; i < n.arglist.size(); i++)
			if ( !(isSubtype(visit(n.arglist.get(i)), ((ClassTypeNode)n.entry.type).allFields.get(i)) ) )
				throw new TypeException("Wrong type for "+(i+1)+"-th parameter in the creation of "+n.id,n.getLine());
		return new RefTypeNode(n.id);
	}
	
	@Override
	public TypeNode visitNode(EmptyNode n) throws TypeException {
		if (print) printNode(n);
		return new EmptyTypeNode();
	}
	
	@Override
	public TypeNode visitNode(PrintNode n) throws TypeException {
		if (print) printNode(n);
		return visit(n.exp);
	}

	@Override
	public TypeNode visitNode(IfNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.cond), new BoolTypeNode())) )
			throw new TypeException("Non boolean condition in if",n.getLine());
		TypeNode t = visit(n.th);
		TypeNode e = visit(n.el);
		//if (isSubtype(t, e)) return e;
		//if (isSubtype(e, t)) return t;
		TypeNode ref = TypeRels.lowestCommonAncestor(t, e);
		if(ref == null ) {			
			throw new TypeException("Incompatible types in then-else branches",n.getLine());
		} else {
			return ref;
		}
	}

	@Override
	public TypeNode visitNode(EqualNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.left);
		TypeNode r = visit(n.right);
		if (l instanceof ArrowTypeNode || r instanceof ArrowTypeNode)
			throw new TypeException("Cannot compare functional types ", n.getLine());
		if ( !(isSubtype(l, r) || isSubtype(r, l)) )
			throw new TypeException("Incompatible types in equal",n.getLine());
		return new BoolTypeNode();
	}
	
	@Override
	public TypeNode visitNode(GreaterEqualNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.left);
		TypeNode r = visit(n.right);
		if (l instanceof ArrowTypeNode || r instanceof ArrowTypeNode)
			throw new TypeException("Cannot compare functional types ", n.getLine());
		if ( !(isSubtype(l, r) || isSubtype(r, l)) )
			throw new TypeException("Incompatible types in equal",n.getLine());
		return new BoolTypeNode();
	}
	
	@Override
	public TypeNode visitNode(LessEqualNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.left);
		TypeNode r = visit(n.right);
		if (l instanceof ArrowTypeNode || r instanceof ArrowTypeNode)
			throw new TypeException("Cannot compare functional types ", n.getLine());
		if ( !(isSubtype(l, r) || isSubtype(r, l)) )
			throw new TypeException("Incompatible types in equal",n.getLine());
		return new BoolTypeNode();
	}
	
	@Override
	public TypeNode visitNode(AndNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.left);
		TypeNode r = visit(n.right);
		if ( !(isSubtype(l, new BoolTypeNode()) && isSubtype(r, new BoolTypeNode())) )
			throw new TypeException("Incompatible types in equal",n.getLine());
		return new BoolTypeNode();
	}
	
	@Override
	public TypeNode visitNode(OrNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.left);
		TypeNode r = visit(n.right);
		if ( !(isSubtype(l, new BoolTypeNode()) && isSubtype(r, new BoolTypeNode())) )
			throw new TypeException("Incompatible types in equal",n.getLine());
		return new BoolTypeNode();
	}
	
	@Override
	public TypeNode visitNode(NotNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.val);
		if ( !(isSubtype(l, new BoolTypeNode()) ))
			throw new TypeException("Incompatible types in equal",n.getLine());
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(TimesNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in multiplication",n.getLine());
		return new IntTypeNode();
	}
	
	@Override
	public TypeNode visitNode(DivNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in multiplication",n.getLine());
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(PlusNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in sum",n.getLine());
		return new IntTypeNode();
	}
	
	@Override
	public TypeNode visitNode(MinusNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in sum",n.getLine());
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(CallNode n) throws TypeException {
		if (print) printNode(n,n.id);
		TypeNode t = visit(n.entry); 
		if ( !(t instanceof ArrowTypeNode || t instanceof MethodTypeNode) )
			throw new TypeException("Invocation of a non-function "+n.id,n.getLine());
		ArrowTypeNode at = t instanceof ArrowTypeNode ? (ArrowTypeNode) t : ((MethodTypeNode)t).fun;
		if ( !(at.parlist.size() == n.arglist.size()) )
			throw new TypeException("Wrong number of parameters in the invocation of "+n.id,n.getLine());
		for (int i = 0; i < n.arglist.size(); i++)
			if ( !(isSubtype(visit(n.arglist.get(i)),at.parlist.get(i))) )
				throw new TypeException("Wrong type for "+(i+1)+"-th parameter in the invocation of "+n.id,n.getLine());
		return at.ret;
	}

	@Override
	public TypeNode visitNode(IdNode n) throws TypeException {
		if (print) printNode(n,n.id);
		if(n.entry.type instanceof MethodTypeNode)
			throw new TypeException("Wrong usage of method " + n.id,n.getLine());
		TypeNode t = visit(n.entry); 
		// controllo che ID non sia il nome di una classe
		if (t instanceof ClassTypeNode)
			throw new TypeException("Wrong usage of class identifier " + n.id,n.getLine());
		return t;
	}

	@Override
	public TypeNode visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return new IntTypeNode();
	}

// gestione tipi incompleti	(se lo sono lancia eccezione)
	
	@Override
	public TypeNode visitNode(ArrowTypeNode n) throws TypeException {
		if (print) printNode(n);
		for (Node par: n.parlist) visit(par);
		visit(n.ret,"->"); //marks return type
		return null;
	}
	
	@Override
	public TypeNode visitNode(MethodTypeNode n) throws TypeException {
		if (print) printNode(n);
		visit(n.fun);
		return null;
	}
	
	@Override
	public TypeNode visitNode(RefTypeNode n) {
		if (print) printNode(n);
		return n;
	}

	@Override
	public TypeNode visitNode(BoolTypeNode n) {
		if (print) printNode(n);
		return null;
	}

	@Override
	public TypeNode visitNode(IntTypeNode n) {
		if (print) printNode(n);
		return null;
	}
	
	@Override
	public TypeNode visitNode(ClassTypeNode n) throws TypeException {
		if (print) printNode(n);
		for(Node f:n.allFields) visit(f);
		for(Node m:n.allMethods) visit(m);
		return null;
	}
	
	@Override
	public TypeNode visitNode(EmptyTypeNode n) {
		if (print) printNode(n);
		return null;
	}

	@Override
	public TypeNode visitSTentry(STentry entry) throws TypeException {
		if (print) printSTentry("type");
		return ckvisit(entry.type); 
	}

}