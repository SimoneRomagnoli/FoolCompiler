package compiler;

import compiler.AST.*;
import compiler.lib.*;
import svm.ExecuteVM;
import compiler.exc.*;
import static compiler.lib.FOOLlib.*;

import java.util.ArrayList;
import java.util.List;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

  CodeGenerationASTVisitor() {}
  CodeGenerationASTVisitor(boolean debug) {super(false,debug);} //enables print for debugging

  private static List<List<String>> dispatchTables;
  
	@Override
	public String visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		dispatchTables = new ArrayList<>();
		String declCode = null;
		for (Node dec : n.declist) declCode=nlJoin(declCode,visit(dec));
		return nlJoin(
			"push 0",	
			declCode, // generate code for declarations (allocation)			
			visit(n.exp),
			"halt",
			getCode()
		);
	}

	@Override
	public String visitNode(ProgNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp),
			"halt"
		);
	}

	@Override
	public String visitNode(FunNode n) {
		if (print) printNode(n,n.id);
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.declist) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
			if( ((DecNode)dec).getType() instanceof ArrowTypeNode) {
				popDecl = nlJoin(popDecl,"pop");
			}
		}
		for (int i=0;i<n.parlist.size();i++) {
			popParl = nlJoin(popParl,"pop");
			if(n.parlist.get(i).getType() instanceof ArrowTypeNode) {
				popParl = nlJoin(popParl,"pop");
			}
		}
		String funl = freshFunLabel();
		putCode(
			nlJoin(
				funl+":",
				"cfp", 			// set $fp to $sp value
				"lra", 			// load $ra value
				declCode, 		// generate code for local declarations (they use the new $fp!!!)
				visit(n.exp), 	// generate code for function body expression
				"stm", 			// set $tm to popped value (function result)
				popDecl, 		// remove local declarations from stack
				"sra", 			// set $ra to popped value
				"pop", 			// remove Access Link from stack
				popParl, 		// remove parameters from stack
				"sfp", 			// set $fp to popped value (Control Link)
				"ltm", 			// load $tm value (function result)
				"lra", 			// load $ra value
				"js"  			// jump to to popped address
			)
		);
		return nlJoin("lfp", "push "+funl);		
	}

	@Override
	public String visitNode(VarNode n) {
		if (print) printNode(n,n.id);
		return visit(n.exp);
	}

	@Override
	public String visitNode(ClassNode n) {
		if (print) printNode(n,n.id);
		if(n.superID!=null) {
			//se eredito creo una nuova dispatch table copiando tutto il contenuto da superclasse
			dispatchTables.add(new ArrayList<>(dispatchTables.get(-n.superEntry.offset-2)));
		} else {			
			dispatchTables.add(new ArrayList<>());
		}
		
		for(MethodNode m:n.methods) {
			visit(m);
				//caso in cui non faccio override
			if(m.offset >= dispatchTables.get(dispatchTables.size()-1).size()) {
				dispatchTables.get(dispatchTables.size()-1).add(m.label);
			} else {	
				// se la label esiste già, sto facendo ovveriding
				dispatchTables.get(dispatchTables.size()-1).set(m.offset, m.label);
			}
		}
		
		String dispatchTablesOnHeap = null;
		for(String s: dispatchTables.get(dispatchTables.size()-1)) {
			//per ogni metodo scrivo la sua label sullo heap
			dispatchTablesOnHeap = nlJoin(
					dispatchTablesOnHeap,
					//METTERE s SULLO HEAP
					"push "+s,  //metto "s" sullo stack
					"lhp", 		//metto sullo stack il primo indirizzo libero dello heap
					"sw",		//scrivo "s" nel primo indirizzo libero dello hep
					
					//INCREMENTO IL REGISTRO $hp
					"lhp",      
					"push 1",
					"add",
					"shp"	
					);
		}
		
		return nlJoin(
				"lhp",
				dispatchTablesOnHeap
				);		
	}
	
	@Override
	public String visitNode(EmptyNode n) {
		if (print) printNode(n);
		//-1 e' diverso da qualsiasi object pointer
		return "push -1";
	}
	
	@Override
	public String visitNode(MethodNode n) {
		if (print) printNode(n,n.id);
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.declist) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i=0;i<n.parlist.size();i++) popParl = nlJoin(popParl,"pop");
		n.label = freshFunLabel();
		putCode(
			nlJoin(
				n.label+":",
				"cfp", 			// set $fp to $sp value
				"lra", 			// load $ra value
				declCode, 		// generate code for local declarations (they use the new $fp!!!)
				visit(n.exp), 	// generate code for function body expression
				"stm", 			// set $tm to popped value (function result)
				popDecl, 		// remove local declarations from stack
				"sra", 			// set $ra to popped value
				"pop", 			// remove Access Link from stack
				popParl, 		// remove parameters from stack
				"sfp", 			// set $fp to popped value (Control Link)
				"ltm", 			// load $tm value (function result)
				"lra", 			// load $ra value
				"js"  			// jump to to popped address
			)
		);
		return "";	
	}
		
	@Override
	public String visitNode(ClassCallNode n) {
		if (print) printNode(n,n.methodID);
		String argCode = null, getAR = null;
		for (int i=n.arglist.size()-1;i>=0;i--) argCode=nlJoin(argCode,visit(n.arglist.get(i)));
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
			"lfp", 			// carico il control link sullo stack
			argCode, 		// carico codice per gli argomenti (al contrario)
			"lfp", 			// recupero il valore dell'AR dove e' dichiarata la classe
			getAR, 			// con risalita della catena statica
			
			"push "+n.entry.offset,
			"add",			//calcolo l'indirizzo della dichiarazione dell'oggetto
			"lw",			//carico l'object pointer sullo stack (setto l'access link)
			
			"stm",
			"ltm",			//duplico il valore in tm
			
			"ltm",
			"lw",			//seguo l'access link (va al dispatch pointer)
			
			"push "+n.methodEntry.offset,
			"add",			//calcolo l'indirizzo del corpo del metodo
			"lw",			//va all'indirizzo
			"js"			//jump al metodo
		);
	}
	
	@Override
	public String visitNode(NewNode n) {
		if (print) printNode(n,n.id);
		String argCode = null, putArgsOnHeap = null;
		for (int i=0 ; i<n.arglist.size() ; i++) {
			argCode=nlJoin(argCode,visit(n.arglist.get(i)));
			putArgsOnHeap = nlJoin(putArgsOnHeap, 
					"lhp", 		
					"sw",		//copio sullo heap il valore che e' sulla cima dello stack
					
					"lhp",
					"push 1",
					"add",
					"shp"		//incremento il registro $hp
					);
		}
		return nlJoin(
				argCode,		//metto tutti gli argomenti sullo stack
				putArgsOnHeap,	//sposto tutti gli argomenti sullo heap
				
				"push "+(ExecuteVM.MEMSIZE+n.entry.offset),
				"lw",			//metto sullo stack l'indirizzo della classe e carico il dispatch pointer
				
				"lhp",
				"sw",			//carico il dispatch pointer sullo heap
				
				"lhp",			//copio object pointer sullo stack (valore di ritorno)
				
				"lhp",
				"push 1",
				"add",
				"shp"			//incremento $hp
				);
	}
	
	@Override
	public String visitNode(PrintNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp),
			"print"
		);
	}

	@Override
	public String visitNode(IfNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel();
	 	String l2 = freshLabel();		
		return nlJoin(
			visit(n.cond),
			"push 1",
			"beq "+l1,
			visit(n.el),
			"b "+l2,
			l1+":",
			visit(n.th),
			l2+":"
		);
	}

	@Override
	public String visitNode(EqualNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel();
	 	String l2 = freshLabel();
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"beq "+l1,
			"push 0",
			"b "+l2,
			l1+":",
			"push 1",
			l2+":"
		);
	}
	
	@Override
	public String visitNode(LessEqualNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel();
	 	String l2 = freshLabel();
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"bleq "+l1,
			"push 0",
			"b "+l2,
			l1+":",
			"push 1",
			l2+":"
		);
	}
	
	@Override
	public String visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel();
	 	String l2 = freshLabel();
		return nlJoin(
			visit(n.right),
			visit(n.left),
			"bleq "+l1,
			"push 0",
			"b "+l2,
			l1+":",
			"push 1",
			l2+":"
		);
	}

	@Override
	public String visitNode(TimesNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"mult"
		);	
	}
	
	@Override
	public String visitNode(DivNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"div"
		);	
	}

	@Override
	public String visitNode(PlusNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"add"				
		);
	}
	
	@Override
	public String visitNode(MinusNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"minus"
		);	
	}
	
	@Override
	public String visitNode(NotNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.val),
				"push -1",
				"mult",
				"push 1",
				"add"
		);	
	}
	
	@Override
	public String visitNode(AndNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"mult"
		);	
	}
	
	@Override
	public String visitNode(OrNode n) {
		if (print) printNode(n);
		return nlJoin(
			//DeMorgan
			visit(n.left),
			"push -1",
			"mult",
			"push 1",
			"add",
			visit(n.right),
			"push -1",
			"mult",
			"push 1",
			"add",
			"mult",
			"push -1",
			"mult",
			"push 1",
			"add"
		);	
	}

	@Override
	public String visitNode(CallNode n) {
		if (print) printNode(n,n.id);
		String argCode = null, getAR = null;
		for (int i=n.arglist.size()-1;i>=0;i--) argCode=nlJoin(argCode,visit(n.arglist.get(i)));
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
			"lfp", 			// carico il control link sullo stack
			argCode, 		// carico codice per gli argomenti (al contrario)
			"lfp", 			// recupero il valore dell'AR dove e' dichiarata la classe
			getAR, 			// con risalita della catena statica
							// se e' una chiamata a metodo  allora getAR = "lw" poiche'
			    			// siamo per forza all'interno della classe stessa
			
			n.entry.type instanceof MethodTypeNode 
				//se sto chiamando un metodo
				? nlJoin(			
						"stm", 			
						"ltm", 			//duplico l'access link (in $tm) 
						
						"ltm", 			
						"lw",			//seguo l'access link (va al dispatch pointer)
						
						"push "+n.entry.offset,
						"add",			//calcolo l'indirizzo del corpo del metodo
						"lw",			//va all'indirizzo
						"js"			//salto al metodo
						)
				//se la chiamata non e' a un metodo, resta invariato
				: nlJoin(
						"push "+n.entry.offset, 	
						"add",			//calcolo l'indirizzo di dichiarazione della funzione
						
						"stm", 			
						"ltm",			//copio in $tm l'indirizzo di dichiarazione della funzione 			
						
						"lw",			//seguo l'access link
						
						"ltm",			//riprendo l'indirizzo di dichiarazione della funzione 			
						"push 1", 
						"sub", 			//sottraggo 1 per raggiungere il corpo della funzinoe
						
						"lw", 			// carico l'indirizzo del corpo della funzione (label)
						"js"  			// salto al corpo della funzione
						)
			);
	}

	@Override
	public String visitNode(IdNode n) {
		if (print) printNode(n,n.id);
		String getAR = null;
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		String ret = nlJoin(
				"lfp",  // mette il valore di fp in cima allo stack
				getAR,  // raggiunge l'indirizzo del frame contentente la dichiarazione
				        // dell'id seguendo la catena statica degli access links
				"push "+n.entry.offset,   // pusha sullo stack l'offset
				"add"	// calcolo l'indirizzo di dichiarazione dell'id
			);
		
		//se la variabile e' una funzione bisogna recuperarne l'indirizzo a offset id - 1
		if(n.entry.type instanceof ArrowTypeNode) {
			ret = nlJoin(ret,	
					"stm",		
					"ltm",		//copio in $tm l'indirizzo di id
					
					"lw",		//carico il puntatore all'AR
					
					"ltm",      //riprendo il valore copiato in $tm (offset)
					"push 1",   
					"sub"       //sottraggo 1 per raggiungere il corpo della funzione con lw
					);
		} 
		return nlJoin(ret, "lw");
	}

	@Override
	public String visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+(n.val?1:0);
	}

	@Override
	public String visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+n.val;
	}
}