// ---------------------------------------------------------------------------
// Symbol.java
// Symbols, i.e., the meanings of identifiers.
// $Id: Symbol.java,v 1.23 2022/06/30 08:23:41 schreine Exp $
//
// Author: Wolfgang Schreiner <Wolfgang.Schreiner@risc.jku.at>
// Copyright (C) 2022-, Research Institute for Symbolic Computation (RISC)
// Johannes Kepler University, Linz, Austria, https://www.risc.jku.at
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
// ----------------------------------------------------------------------------
package risctp.types;

import java.util.*;

import risctp.Main;
import risctp.syntax.AST.*;
import risctp.syntax.AST.Decl.Axiom;
import risctp.syntax.AST.Decl.DataType;
import risctp.syntax.AST.Decl.Function;
import risctp.syntax.AST.Decl.Theorem;
import risctp.syntax.AST.Decl.TypeDecl;

public abstract class Symbol
{
  public final Id id;
  public Symbol(Id id) { this.id = id; }

  public static Symbol getSymbol(Decl decl)
  {
    if (decl instanceof TypeDecl)
    {
      TypeDecl decl0 = (TypeDecl)decl;
      return decl0.id.getSymbol();
    }
    else if (decl instanceof DataType)
    {
      DataType decl0 = (DataType)decl;
      return decl0.items[0].id.getSymbol();
    }
    else if (decl instanceof Function)
    {
      Function decl0 = (Function)decl;
      return decl0.id.getSymbol();
    }
    else if (decl instanceof Axiom)
    {
      Axiom decl0 = (Axiom)decl;
      return decl0.id.getSymbol();
    }
    else if (decl instanceof Theorem)
    {
      Theorem decl0 = (Theorem)decl;
      return decl0.id.getSymbol();
    }
    else
      Main.getMain().error("unknown declaration");
    return null;
  }
  
  public final static class TypeSymbol extends Symbol
  {
    // describing type expression
    public final Type type;
  
    // the base type
    public final TypeSymbol base;
    
    // associated subtype predicate (may be null)
    private FunctionSymbol pred; 
    
    // the root type (without subtype predicate)
    public final TypeSymbol root;
    
    // if base is null, the root type symbol is the new symbol itself
    public TypeSymbol(Id id, Type type, TypeSymbol base) 
    { 
      super(id);
      this.type = type;  
      this.base = base == null ? this : base;
      this.root = base == null ? this : base.root;
    }
    
    // clone a type symbol (without subtype predicate)
    public TypeSymbol(TypeSymbol tsymbol)
    {
      this(tsymbol.id, tsymbol.type, tsymbol.root);
    }
    
    // get/set type predicate
    public FunctionSymbol getPred() { return pred; }
    public void setPred(FunctionSymbol pred) { this.pred = pred; }
  
    // is type predicate necessarily satisfiable?
    private boolean sat = false;
    public boolean isSat() { return sat; }
    public void setSat(boolean sat) { this.sat = sat; }
    
    // true if this type is subtype of given type
    public boolean isSubType(TypeSymbol tsymbol)
    {
      TypeSymbol tsymbol0 = this;
      while (true)
      {
        if (tsymbol0 == tsymbol) return true;
        if (tsymbol0 == tsymbol0.base) return false;
        tsymbol0 = tsymbol0.base;
      }
    }
  }
  
  public final static class VariableSymbol extends Symbol
  {
    // the variable type
    public final TypeSymbol tsymbol;
    public VariableSymbol(Id id, TypeSymbol tsymbol) 
    { super(id); this.tsymbol = tsymbol; }
  }
  
  public final static class FunctionSymbol extends Symbol
  {
    // argument types and result type
    public final List<TypeSymbol> tsymbols; public final TypeSymbol tsymbol;
    public FunctionSymbol(Id id, List<TypeSymbol> tsymbols, TypeSymbol tsymbol) 
    { super(id); this.tsymbols = tsymbols; this.tsymbol = tsymbol; }
  }
  
  public final static class FormulaSymbol extends Symbol
  {
    // is the formula an axiom?
    public final boolean axiom;
    public FormulaSymbol(Id id, boolean axiom) { super(id); this.axiom = axiom; }
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------
