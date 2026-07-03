// ---------------------------------------------------------------------------
// TypeTable.java
// A table of canonical type symbols for applications of type constructors.
// $Id: TypeTable.java,v 1.28 2022/06/30 10:51:04 schreine Exp $
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
import java.util.function.*;

import risctp.syntax.AST;
import risctp.syntax.AST.*;
import risctp.types.Symbol.*;

public class TypeTable
{
  // maps of canonical type symbols and canonical type expressions
  private final SigMap<TypeSymbol> symbolMap;
  private final SigMap<Type> typeMap;
  
  // the symbols for the builtin types
  private final TypeSymbol boolSymbol;
  private final TypeSymbol intSymbol;

  public TypeTable() 
  {
    symbolMap = new SigMap<TypeSymbol>();
    typeMap = new SigMap<Type>();
    Id boolId = Id.create(AST.BOOLNAME);
    boolSymbol = getTypeSymbol(boolId, new ArrayList<TypeSymbol>(), true);
    boolId.setSymbol(boolSymbol);
    Id intId = Id.create(AST.INTNAME);
    intSymbol = getTypeSymbol(intId, new ArrayList<TypeSymbol>(), true);
    intId.setSymbol(intSymbol);
  }
  
  public TypeSymbol getBool() { return boolSymbol; }
  public TypeSymbol getInt() { return intSymbol; }
 
  /****************************************************************************
   * Get canonical version of type.
   * @param id the identifier of the type constructor.
   * @param asymbols the symbols for the type arguments.
   * @return the symbol for the canonical type.
   ***************************************************************************/
  public TypeSymbol getTypeSymbol(Id id, List<TypeSymbol> asymbols)
  {
    return getTypeSymbol(id, asymbols, false);
  }
  
  /****************************************************************************
   * Get canonical version of type.
   * @param id the identifier of the type constructor.
   * @param asymbols the symbols for the type arguments.
   * @param root true if for the argument types the root types
   *        are to be considered in the table lookup.
   * @return the symbol for the canonical type.
   ***************************************************************************/
  private TypeSymbol getTypeSymbol(Id id, List<TypeSymbol> asymbols, boolean root)
  {
    String key = id.toString();
    int n = asymbols.size();
    List<TypeSymbol> dsymbols = new ArrayList<TypeSymbol>(n);
    List<Type> dtypes = new ArrayList<Type>(n);
    boolean isroot = true;
    for (TypeSymbol asymbol : asymbols)
    {
      TypeSymbol dsymbol = root ? asymbol.root : asymbol;
      if (dsymbol != asymbol.root) isroot = false;
      dsymbols.add(dsymbol);
      dtypes.add(dsymbol.type);
    }
    boolean isroot0 = isroot;
    return symbolMap.get(key, dsymbols, ()->
    {
      Type type = typeMap.get(key, dtypes, ()->Type.create(id, dtypes));
      StringBuilder builder = new StringBuilder();
      builder.append(id);
      if (n != 0)
      {
        builder.append("[");
        int i = 0;
        for (Type dtype : dtypes)
        {
          builder.append(dtype);
          i++; if (i < n) builder.append(",");
        }
        builder.append("]");
      }
      String string = builder.toString();
      string = string.replace("\'","");
      Id id0 = Id.create(string);
      TypeSymbol base = root || isroot0 ? null : getTypeSymbol(id, asymbols, true);
      TypeSymbol result = new TypeSymbol(id0, type, base);
      id0.setSymbol(result);
      return result;
    });
  }
  
  // signatures (args,result) with components of type T
  private static class Sig<T>
  {
    public final List<T> args; public final T result;
    public Sig(List<T> args, T result)
    { this.args = args; this.result = result; }
  }
  
  // maps of strings to list of signatures of type T
  private static class SigMap<T>
  {
    private final Map<String, List<Sig<T>>> map;
    public SigMap()
    {
      map = new HashMap<String, List<Sig<T>>>();
    }
    // get from map the result that is associated to 'key' for 'args'
    // if there is none, put result of 'supplier' there and return it as result
    public T get(String key, List<T> args, Supplier<T> supplier)
    {
      List<Sig<T>> sigs = map.get(key);
      if (sigs == null)
      {
        sigs = new ArrayList<Sig<T>>();
        map.put(key, sigs);
      }
      else
      {
        for (Sig<T> sig : sigs)
        {
          if (args.equals(sig.args)) return sig.result;
        }
      }
      T result = supplier.get();
      Sig<T> sig = new Sig<T>(args, result);
      sigs.add(sig);
      return result;
    }
  }
}
//---------------------------------------------------------------------------
// end of file
//---------------------------------------------------------------------------
