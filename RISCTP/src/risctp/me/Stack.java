// ---------------------------------------------------------------------------
// Stack.java
// A generic stack.
// $Id: Stack.java,v 1.3 2023/03/23 15:49:41 schreine Exp $
//
// Author: Wolfgang Schreiner <Wolfgang.Schreiner@risc.jku.at>
// Copyright (C) 2023-, Research Institute for Symbolic Computation (RISC)
// Johannes Kepler University, Linz, Austria, https://www.risc.jku.at
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General public final License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General public final License for more details.
//
// You should have received a copy of the GNU General public final License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
// ----------------------------------------------------------------------------
package risctp.me;

import java.util.*;

// --------------------------------------------------------------------------
// a stack of values of type T
// --------------------------------------------------------------------------
public final class Stack<T> extends ArrayList<T>
{
  // make Java happy
  static final long serialVersionUID = 20230313;

  // remember the top element for quick access (may be null)
  private T top;

  // create an empty stack
  public Stack()
  {
    top = null;
  }

  // get the top of the stack without removing it (null for an empty stack)
  public T top() 
  { 
    return top; 
  }

  // push a new substitution on the stack
  public void push(T val)
  {
    add(val);
    top = val;
  }

  // pop the top element from the (non-empty) stack and return it
  public T pop()
  {
    int n = size();
    remove(n-1);
    T top0 = top;
    top = n == 1 ? null : get(n-2);
    return top0;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------