// ---------------------------------------------------------------------------
// ConsList.java
// A generic list with a functional interface.
// $Id: ConsList.java,v 1.1 2023/05/25 15:16:08 schreine Exp $
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
// a list of values of type T
// --------------------------------------------------------------------------
public final class ConsList<T> implements Iterable<T>
{
  // a node of the list
  private class Node
  {
    final T value;
    final Node next;
    Node(T value, Node next) { this.value = value; this.next = next; }
  }
  
  // the first node of the list
  private final Node head;
  
  // the number of nodes in the list
  private final int size;
  
  // support for-each loops
  private class ListIterator implements Iterator<T>
  {
    private Node next;
    private ListIterator(Node next) { this.next = next; }
    public boolean hasNext() { return next != null; }
    public T next() { T value = next.value; next = next.next; return value; }
  }
  public Iterator<T> iterator()  { return new ListIterator(head); }
  
  // create an empty list
  public ConsList() { this(null, 0); }

  // create a list with the denoted head node
  private ConsList(Node head, int size) { this.head = head; this.size = size; }

  // is the list empty?
  public boolean isEmpty() { return head == null; }
  
  // the size of the list
  public int size() { return size; }
  
  // get the head and tail of the list 
  public T head() { return head.value; }
  public ConsList<T> tail() { return new ConsList<T>(head.next, size-1); }
  
  // construct a new list by adding a value in front of this list
  public ConsList<T> cons(T value) { return new ConsList<T>(new Node(value, head), size+1); }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------