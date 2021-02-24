package compiler;

import java.util.*;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {
	
	
	private Map<String, Map<String,STentry>> classTable = new HashMap<>();
	private List<Map<String, STentry>> symTable = new ArrayList<>();
	private Set<String> optimizer;
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level 
	int stErrors=0;

	SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) {super(debug);} // enables print for debugging

	private STentry stLookup(String id) {
		int j = nestingLevel;
		STentry entry = null;
		while (j >= 0 && entry == null) 
			entry = symTable.get(j--).get(id);	
		return entry;
	}

	@Override
	public Void visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = new HashMap<>();
		symTable.add(hm);
	    for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		symTable.remove(0);
		return null;
	}

	@Override
	public Void visitNode(ProgNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}
	
	@Override
	public Void visitNode(FunNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		List<TypeNode> parTypes = new ArrayList<>();  
		for (ParNode par : n.parlist) parTypes.add(par.getType()); 
		n.setType(new ArrowTypeNode(parTypes, n.retType));
		
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes,n.retType),decOffset--);
		decOffset--;
		
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		} 
		//creare una nuova hashmap per la symTable
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level 
		decOffset=-2;
		
		int parOffset=1;
		for (ParNode par : n.parlist) {
			if(par.getType() instanceof ArrowTypeNode) parOffset++;
			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}
		}
		for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		//rimuovere la hashmap corrente poiche' esco dallo scope               
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level 
		return null;
	}
	
	@Override
	public Void visitNode(VarNode n) {
		if (print) printNode(n);
		visit(n.exp);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		STentry entry = new STentry(nestingLevel,n.getType(),decOffset--);
		if(n.getType() instanceof ArrowTypeNode) decOffset--;
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Var id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}
		return null;
	}
	
	@Override
	public Void visitNode(ClassNode n) {
		if (print) printNode(n);
		if(nestingLevel!=0) {
			System.out.println("Class id " + n.id + " at line "+ n.getLine() +" declared out of global env");
			stErrors++;
		}
		
		this.optimizer = new HashSet<>();
		
		//visito la classe dichiarata
		Map<String, STentry> hm = symTable.get(nestingLevel);
		STentry entry = null;
		n.setType(new ClassTypeNode(new ArrayList<>(), new ArrayList<>()));
		
		//se eredita, il tipo della classe viene creato copiando quello ereditato
		if(n.superID != null) {
			
			// entry della classe ereditata
			n.setSuperEntry(hm.get(n.superID));
			
			ClassTypeNode clone = (ClassTypeNode)n.superEntry.type;
			//((ClassTypeNode)n.getType()).allFields.addAll(0, clone.allFields);
			//((ClassTypeNode)n.getType()).allMethods.addAll(0, clone.allMethods);
			entry = new STentry(nestingLevel, new ClassTypeNode(clone), decOffset--);
		} else {
			entry = new STentry(nestingLevel, new ClassTypeNode(new ArrayList<>(), new ArrayList<>()) ,decOffset--);
		}
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Class id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}
		
		Map<String, STentry> virtualTable;
		if(n.superID != null) {
			//creo una copia di tutto il contenuto della virtual table della classe da cui sto ereditando
			virtualTable = new HashMap<>(classTable.get(n.superID));
		} else {
			virtualTable = new HashMap<>();
		}
		classTable.put(n.id, virtualTable);
		
		//creo un nuovo livello per la symbol table
		nestingLevel++;
		symTable.add(virtualTable);
		
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level 
		decOffset=n.superID!=null ? ((ClassTypeNode)entry.type).allMethods.size() : 0;
		int fieldsOffset= n.superID!=null ? -((ClassTypeNode)entry.type).allFields.size()-1 : -1;
		
		for (FieldNode f : n.fields) {
			if(optimizer.contains(f.id)) {
				System.out.println("Field id " + f.id + " already declared in class "+n.id);
				stErrors++;
			} else {
				optimizer.add(f.id);
			}
			if(virtualTable.containsKey(f.id)) {
				//se la virtual table contiene gi� un campo con quel nome vuol dire che:
				// 1. sto ereditando
				// 2. sto facendo overriding
				if(virtualTable.get(f.id).type instanceof MethodTypeNode) {
					System.out.println("Cannot override method id " + f.id + " with a field at line "+ n.getLine());
					stErrors++;
				} else {
					//sostituisco nuova STentry alla vecchia preservando l�offset che era nella vecchia STentry
					f.offset = virtualTable.get(f.id).offset;
					virtualTable.put(f.id, new STentry(nestingLevel,f.getType(),f.offset));
					((ClassTypeNode)entry.type).allFields.set(-f.offset-1, f.getType());
				}
			} else {
				((ClassTypeNode)entry.type).allFields.add(f.getType());
				f.offset = fieldsOffset;
				virtualTable.put(f.id, new STentry(nestingLevel,f.getType(),fieldsOffset--));
			}
		}
		
		for (MethodNode m: n.methods) {
			if(optimizer.contains(m.id)) {
				System.out.println("Method id " + m.id + " already declared in class "+n.id);
				stErrors++;
			} else {
				optimizer.add(m.id);
			}
			visit(m);
			//se l'offset � minore della lunghezza di allMethods vuol dire che ho visitato un metodo gi� dichiarato
			//quindi sto facendo overriding
			if(m.offset < ((ClassTypeNode)entry.type).allMethods.size()) {
				((ClassTypeNode)entry.type).allMethods.set(m.offset, (MethodTypeNode) m.getType());
			} else {				
				((ClassTypeNode)entry.type).allMethods.add( (MethodTypeNode) m.getType());
			}
		}
		
		//rimuovere la hashmap corrente poiche' esco dallo scope               
		symTable.remove(nestingLevel--);
		decOffset = prevNLDecOffset;
		return null;
	}
	
	@Override
	public Void visitNode(MethodNode n) {
		if (print) printNode(n);
		Map<String, STentry> virtualTable = symTable.get(nestingLevel);
		List<TypeNode> parTypes = new ArrayList<>();  
		for (ParNode par : n.parlist) parTypes.add(par.getType()); 
		
		if(virtualTable.containsKey(n.id)) {
			//se la virtual table contiene gi� un metodo con quel nome vuol dire che:
			// 1. sto ereditando
			// 2. sto facendo overriding
			if(!(virtualTable.get(n.id).type instanceof MethodTypeNode)) {
				System.out.println("Cannot override method id " + n.id + " with a field at line "+ n.getLine());
				stErrors++;
			} else {
				//sostituisco nuova STentry alla vecchia preservando l�offset che era nella vecchia STentry
				n.offset = virtualTable.get(n.id).offset;
				virtualTable.put(n.id, new STentry(nestingLevel, n.getType(), n.offset));
			}
		} else {
			//decOffset settato in precedenza al primo slot libero (0 se non eredita, n se eredita)
			n.offset = decOffset;
			virtualTable.put(n.id, new STentry(nestingLevel, n.getType(),decOffset++));
		}
		
		//creare una nuova hashmap per la symTable
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level 
		decOffset=-2;
		
		int parOffset=1;
		for (ParNode par : n.parlist)
			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}
		for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		//rimuovere la hashmap corrente poiche' esco dallo scope               
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level 
		return null;
	}

	@Override
	public Void visitNode(ClassCallNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.classID);
		if (entry == null) {
			System.out.println("Class id " + n.classID + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
		}
		STentry methodEntry = classTable.get(((RefTypeNode)entry.type).id).get(n.methodID);
		if (methodEntry == null) {
			System.out.println("Method id " + n.methodID + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.methodEntry = methodEntry;
			n.nl = nestingLevel;
		}
		for (Node arg : n.arglist) visit(arg);
		return null;
	}
	
	@Override
	public Void visitNode(NewNode n) {
		if (print) printNode(n);
		if(!classTable.containsKey(n.id)) {
			System.out.println("Class id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			STentry entry = symTable.get(0).get(n.id);
			n.entry = entry;
			n.nl = nestingLevel;
		}
		for (Node arg : n.arglist) visit(arg);
		return null;
	}
	
	@Override
	public Void visitNode(PrintNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
		if (print) printNode(n);
		visit(n.cond);
		visit(n.th);
		visit(n.el);
		return null;
	}
	
	@Override
	public Void visitNode(EqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(LessEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(NotNode n) {
		if (print) printNode(n);
		visit(n.val);
		return null;
	}
	
	@Override
	public Void visitNode(TimesNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(DivNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(PlusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(MinusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(OrNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(AndNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(CallNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		for (Node arg : n.arglist) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Var or Par id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}
	
	@Override
	public Void visitNode(EmptyNode n) {
		if (print) printNode(n, "null");
		return null;
	}
}
