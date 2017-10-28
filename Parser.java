public class Parser {

    private Symtab symtab = new Symtab();

    // the first sets.
    // note: we cheat sometimes:
    // when there is only a single token in the set,
    // we generally just compare tkrep with the first token.
    
    TK f_declarations[] = {TK.VAR, TK.none};//////////////////TK.ID
    TK f_statement[] = {TK.ID, TK.PRINT, TK.PRINTNL, TK.IF, TK.DO, TK.FA, TK.SWAP, TK.none};
    TK f_swap[] = {TK.SWAP, TK.none};
    TK f_print[] = {TK.PRINT, TK.none};
    TK f_printnl[] = {TK.PRINTNL, TK.none};
    TK f_assignment[] = {TK.ID, TK.none};
    TK f_if[] = {TK.IF, TK.none};
    TK f_do[] = {TK.DO, TK.none};
    TK f_fa[] = {TK.FA, TK.none};
    TK f_expression[] = {TK.ID, TK.NUM, TK.LPAREN, TK.none};

    // tok is global to all these parsing methods;
    // scan just calls the scanner's scan method and saves the result in tok.
    private Token tok; // the current token
    private void scan() {
        tok = scanner.scan();
    }

    private Scan scanner;
    Parser(Scan scanner) {
        this.scanner = scanner;
        scan();
        program();
        if( tok.kind != TK.EOF )
            parse_error("junk after logical end of program");
        symtab.reportVariables();
    }

    // print something in the generated code
    private void gcprint(String str) {
        System.out.println(str);
    }
    // print identifier in the generated code
    // it prefixes x_ in case id conflicts with C keyword.
    private void gcprintid(String str) {
        System.out.println("x_"+str);
    }

    private void program() {
        // generate the E math functions:
        gcprint("int esquare(int x){ return x*x;}");
        gcprint("#include <math.h>");
        gcprint("int esqrt(int x){ double y; if (x < 0) return 0; y = sqrt((double)x); return (int)y;}");

        gcprint("#include <stdio.h>");
        gcprint("#include <stdlib.h>");
        gcprint("#include <string.h>");
        gcprint("void swap(int *x, int *y){int t; t= *x; *x = *y; *y = t;}");
        gcprint("int check(int x,char *name,int assign){char str[20];sprintf(str, \"%d\", x);if(strcmp(str,\"-12345\") == 0 && assign == 0){printf(\"\\n\");printf(name);printf(\" is unassigned before use\\n\");exit(1);}else{return x;}}");
        gcprint("int main() {");
	block();
        gcprint("return 0; }");
    }

    private void block() {
        symtab.begin_st_block();
	gcprint("{");
        if( first(f_declarations) ) {
            declarations();
        }
        statement_list();
        symtab.end_st_block();
	gcprint("}");
    }

    //13
    private void declarations() {
        mustbe(TK.VAR);
        if_id();
        while( is(TK.COMMA) ) {
            scan();
            if_id();
        }
    }
    
    private void if_assigned(String variable){
        String name = ",\"" + variable + "\"";
        gcprint("check(");
        gcprintid(variable);
        gcprint(name);
        gcprint(",");
        gcprint(variable + "_ass");
        gcprint(");");
    }

    private void if_id(){
        if( is(TK.ID) ){
           if( symtab.add_var_entry(tok.string,tok.lineNumber) ) {
                    gcprint("int");
                    gcprintid(tok.string);
                    gcprint("= -12345;");
                    gcprint("int");
                    gcprint(tok.string + "_ass");
                    gcprint("= 0;");
            }
            scan();
        }
        else
        {
            System.err.println( "mustbe: want " + TK.ID + ", got " +
                               tok);
            parse_error( "missing token (mustbe)" );
        }
    }
    
    private void statement_list(){
        while( first(f_statement) ) {
            statement();
        }
    }

    private void statement(){
        if( first(f_assignment) )
            assignment();
        else if( first(f_print) )
            print();
        else if( first(f_printnl) )
            printnl();
        else if( first(f_swap) )
            swap();
        else if( first(f_if) )
            ifproc();
        else if( first(f_do) )
            doproc();
        else if( first(f_fa) )
            fa();
        else
            parse_error("statement");
    }

    private void assignment(){
        String id = tok.string;
        int lno = tok.lineNumber; // save it too before mustbe!
        mustbe(TK.ID);
        referenced_id(id, true, lno);
        gcprintid(id);
        mustbe(TK.ASSIGN);
        gcprint("=");
        expression();
        gcprint(";");
        //gcprint("int ");
        gcprint(id + "_ass");
        gcprint("= 1;");
    }

    private void print(){
        mustbe(TK.PRINT);
        if(is(TK.ID)){
            if_assigned(tok.string);
        }
        gcprint("printf(\"%d\", ");
        expression();
        gcprint(");");
        while( is(TK.COMMA) ){
            scan();
            if(is(TK.ID)){
              if_assigned(tok.string);
            }
            gcprint("printf(\" %d\", ");
            expression();
            gcprint(");");
        }
        gcprint("printf(\"\\n\");");
    }

    //12
    private void printnl(){
        mustbe(TK.PRINTNL);
        gcprint("printf(\"\\n\");");
        
    }

    private void swap(){

        mustbe(TK.SWAP);
        gcprint("swap(");
        String id = tok.string;
        int lno = tok.lineNumber;
        mustbe(TK.ID);
        referenced_id(id, true, lno);
        gcprint("&");
        gcprintid(id);
        gcprint(",");
        String id_2 = tok.string;
        int lno_2 = tok.lineNumber;
        if(id.equals(id_2)){
            System.err.println( "swapping variable " + id + " with itself on line " + lno);
        }
        mustbe(TK.ID);
        referenced_id(id_2, true, lno_2);
        gcprint("&");
        gcprintid(id_2);
        gcprint(");");

        referenced_id(id, false, lno);
        referenced_id(id_2, false, lno_2);
        
        if_assigned(id);
        if_assigned(id_2);
    }

    private void ifproc(){
        mustbe(TK.IF);
        guarded_commands(TK.IF);
        mustbe(TK.FI);
    }

    private void doproc(){
        mustbe(TK.DO);
        gcprint("while(1){");
        guarded_commands(TK.DO);
        gcprint("}");
        mustbe(TK.OD);
    }

    private void fa(){
        mustbe(TK.FA);
        gcprint("for(");
        String id = tok.string;
        int lno = tok.lineNumber; // save it too before mustbe!
        mustbe(TK.ID);
        referenced_id(id, true, lno);
        gcprintid(id);
        mustbe(TK.ASSIGN);
        gcprint("=");
        expression();
        gcprint(";");
        
        if(is(TK.TO)){
            mustbe(TK.TO);
            gcprintid(id);
            gcprint("<=");
            expression();
            gcprint(";");
            gcprintid(id);
            gcprint("++)");
            }
         else if(is(TK.DOWNTO)){
             mustbe(TK.DOWNTO);
             gcprintid(id);
             gcprint(">=");
             expression();
             gcprint(";");
             gcprintid(id);
             gcprint("--)");
        
         }
        
        if( is(TK.ST) ) {
            gcprint("if( ");
            scan();
            expression();
            gcprint(")");
        }
        commands();
        mustbe(TK.AF);
    }

    private void guarded_commands(TK which){
        guarded_command();
        while( is(TK.BOX) ) {
            scan();
            gcprint("else");
            guarded_command();
        }
        if( is(TK.ELSE) ) {
            gcprint("else");
            scan();
            commands();
        }
        else if( which == TK.DO )
            gcprint("else break;");
    }

    private void guarded_command(){
        gcprint("if(");
        expression();
        gcprint(")");
        commands();
    }

    private void commands(){
        mustbe(TK.ARROW);
        gcprint("{");/* note: generate {} to simplify, e.g., st in fa. */
        block();
        gcprint("}");
    }

    private void expression(){
        simple();
        while( is(TK.EQ) || is(TK.LT) || is(TK.GT) ||
               is(TK.NE) || is(TK.LE) || is(TK.GE)) {
            if( is(TK.EQ) ) gcprint("==");
            else if( is(TK.NE) ) gcprint("!=");
            else if( is(TK.ID) )
            {
                if_assigned(tok.string);
                gcprint(tok.string);
            }
            else gcprint(tok.string);
            scan();
            simple();
        }
    }

    private void simple(){
        term();
        while( is(TK.PLUS) || is(TK.MINUS) ) {
            gcprint(tok.string);
            gcprint("(");
            scan();
            term();
            gcprint(")");
        }
    }

    private void term(){
        factor();
        while(  is(TK.TIMES) || is(TK.DIVIDE) ) {
            gcprint(tok.string);
            gcprint("(");
            scan();
            factor();
            gcprint(")");
        }
    }

    private void factor(){
        if( is(TK.LPAREN) ) {
            gcprint("(");
            scan();
            expression();
            mustbe(TK.RPAREN);
            gcprint(")");
        }
        else if( is(TK.SQUARE) ) {
            gcprint("esquare(");
            scan();
            expression();
            gcprint(")");
        }
        else if( is(TK.SQRT) ) {
            gcprint("esqrt(");
            scan();
            expression();
            gcprint(")");
        }
        else if( is(TK.ID) ) {
            String id = tok.string;
            int lno = tok.lineNumber;
            referenced_id(tok.string, false, tok.lineNumber);
            //gcprintid(tok.string);
            scan();
            if(!is(TK.ASSIGN)){
                if_assigned(tok.string);
            }
            gcprintid(id);
        }
        else if( is(TK.NUM) ) {
            gcprint(tok.string);
            scan();
        }
        else if( is(TK.LBRACE) ) {
            gcprint("(");
            scan();
            guarded_expressions();
            mustbe(TK.RBRACE);
            
            //scan();
        }
        else
            parse_error("factor");
    }
    
    private void guarded_expressions(){
        guarded_expression();
        //gcprint(":");
        while( is(TK.BOX) ) {
            gcprint(":(");
            scan();
            guarded_expression();
            
        }
        if(is(TK.ELSE)){
            gcprint(":");
            scan();
            if( is(TK.ID) )
            {
              if_assigned(tok.string);
            }
            
            expression();
        }
    }
    
    private void guarded_expression(){
        expression(); //scan()
        gcprint(") ?");
        mustbe(TK.ARROW); //scan()
        expression();
        if( is(TK.RBRACE) )
        {
            System.err.println("mustbe: want ELSE, got Token(RBRACE } 2)");
            parse_error( "missing token (mustbe)" );
        }
            
    }

    // be careful: use lno here, not tok.lineNumber
    // (which may have been advanced by now)
    private void referenced_id(String id, boolean assigned, int lno) {
        Entry e = symtab.search(id);
        if( e == null) {
            System.err.println("undeclared variable "+ id + " on line "
                               + lno);
            System.exit(1);
        }
        e.ref(assigned, lno);
    }

    // is current token what we want?
    private boolean is(TK tk) {
        return tk == tok.kind;
    }

    // ensure current token is tk and skip over it.
    private void mustbe(TK tk) {
        if( ! is(tk) ) {
            System.err.println( "mustbe: want " + tk + ", got " +
                                    tok);
            parse_error( "missing token (mustbe)" );
        }
        scan();
    }
    boolean first(TK [] set) {
        int k = 0;
        while(set[k] != TK.none && set[k] != tok.kind) {
            k++;
        }
        return set[k] != TK.none;
    }

    private void parse_error(String msg) {
        System.err.println( "can't parse: line "
                            + tok.lineNumber + " " + msg );
        System.exit(1);
    }
}
