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
			dispatchTables.add(new ArrayList<>(dispatchTables.get(-n.superEntry.offset-2)));
		} else {			
			dispatchTables.add(new ArrayList<>());
		}
		
		for(MethodNode m:n.methods) {
			visit(m);
			if(m.offset >= dispatchTables.get(dispatchTables.size()-1).size()) {
				dispatchTables.get(dispatchTables.size()-1).add(m.label);
			} else {				
				dispatchTables.get(dispatchTables.size()-1).set(m.offset, m.label);
			}
		}
		String dispatchTablesOnHeap = null;
		for(String s: dispatchTables.get(dispatchTables.size()-1)) {
			dispatchTablesOnHeap = nlJoin(
					dispatchTablesOnHeap,
					"push "+s,
					"lhp", 		
					"sw",		//in this way I am writing the string s on the heap
					
					"lhp",
					"push 1",
					"add",
					"shp"		//take hp value, increment it, and put it in hp		
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
			"lfp", 			// load Control Link (pointer to frame of function "id" caller)
			argCode, 		// generate code for argument expressions in reversed order
			"lfp", getAR, 	// retrieve address of frame containing "id" declaration
                          	// by following the static chain (of Access Links)
			
			"push "+n.entry.offset,
			"add",
			"lw",			//load value of id variable
			
			"stm",
			"ltm",			//duplicate the value
			
			"ltm",
			"lw",			//follow the access link
			"push "+n.methodEntry.offset,
			"add",
			"lw",			//retrieve the address of the called method
			"js"			//jump to the method
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
					"sw",		//in this way I am writing the value on the stack on the heap
					
					"lhp",
					"push 1",
					"add",
					"shp"		//take hp value, increment it, and put it in hp
					);
		}
		return nlJoin(
				argCode,
				putArgsOnHeap,
				
				"push "+(ExecuteVM.MEMSIZE+n.entry.offset),
				"lw",
				"lhp",
				"sw",		//write in hp the dispatch pointer
				
				"lhp",		//copy object pointer (to be returned) on the stack
				
				"lhp",
				"push 1",
				"add",
				"shp"		//increment hp
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
			"lfp", 			// load Control Link (pointer to frame of function "id" caller)
			argCode, 		// generate code for argument expressions in reversed order
			"lfp", 			//load object ar (if method) or first ring of the chain (if function)
			
			//change code if 
			n.entry.type instanceof MethodTypeNode 
				? nlJoin(
						"lw",
						"stm", 			// set $tm to popped value (with the aim of duplicating top of stack)
						"ltm", 			// load Access Link (pointer to frame of function "id" declaration)
						"ltm", 			// duplicate top of stack
						"lw",
						"push "+n.entry.offset,
						"add",
						"lw",
						"js"
						)
				: nlJoin(
						getAR, 			// retrieve address of frame containing "id" declaration
                      					// by following the static chain (of Access Links)
						"push "+n.entry.offset, 
						"add",
						
						"stm", 			
						"ltm", 			
						"lw",
						"ltm", 			
						"push 1", 
						"sub", 			
						"lw", 			// load address of "id" function
						"js"  			// jump to popped address (saving address of subsequent instruction in $ra)
						)
			);
	}

	@Override
	public String visitNode(IdNode n) {
		if (print) printNode(n,n.id);
		String getAR = null;
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		String ret = nlJoin(
				"lfp", 
				getAR,  // retrieve address of frame containing "id" declaration
	              		// by following the static chain (of Access Links)
				"push "+n.entry.offset, 
				"add" // compute address of "id" declaration
			);
		
		//se la variabile è una funzione bisogna recuperarne l'indirizzo a offset id - 1
		if(n.entry.type instanceof ArrowTypeNode) {
			ret = nlJoin(ret,	
					"stm",
					"ltm",
					"lw",
					"ltm",
					"push 1",
					"sub"
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