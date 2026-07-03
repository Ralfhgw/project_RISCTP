// ---------------------------------------------------------------------------
// FragmentHandler.java
// A thread-safe response handler that delivers documents in fragments.
// $Id: FragmentHandler.java,v 1.14 2023/12/14 09:30:58 schreine Exp $
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
package risctp.web;

import java.util.*;
import java.io.*;
import java.util.function.*;

import org.w3c.dom.*;

// --------------------------------------------------------------------------
//
// a response handler that allows to create and return document in fragments
//
// --------------------------------------------------------------------------
public class FragmentHandler implements Server.Handler
{ 
  // the root tag of every fragment
  private static final String ROOT = "root";
  
  // the maximum number of children to be added to a document fragment
  // (thus the browser does not block on processing huge document fragments)
  private static final int SIZE = 500;
   
  // the maximum length of a queue before the handler will throttle down
  // (thus we bound the memory required for the queue)
  private static final int LENGTH = 3;
  
  // the (non-empty) queue of document fragments to be delivered
  private Queue<Document> documents;
  
  // the last document in the queue
  private Document current;
  
  // the number of children added to the current document
  private int size;
  
  /**************************************************************************
   * Create handler with root document.
   *************************************************************************/
  public FragmentHandler()
  {
    init();
  }
  
  /**************************************************************************
   * Initialize handler.
   *************************************************************************/
  public synchronized void init()
  {
    documents = new LinkedList<Document>();
    newDocument();
  }
 
  /***************************************************************************
   * Create a new document at the end of the queue.
   **************************************************************************/
  private void newDocument()
  {
    current = DOM.createDocument(ROOT);
    documents.add(current);
    size = 0;
  }
  
  /**************************************************************************
   * Respond to request to denoted service.
   * @param params a mapping of parameter names to parameter values.
   * @param out the stream to write the response to.
   *************************************************************************/
  public void handle(String service, Map<String,String> params, PrintWriter out)
  {
    Document document = getDocument();
    DOM.printDocument(document, false, out);
  }

  /**************************************************************************
   * Get the current document in a thread-safe way.
   * @return the denoted document
   *************************************************************************/
  private synchronized Document getDocument()
  {
    Document document = documents.remove();
    if (documents.isEmpty()) newDocument();
    return document;
  }
  
  /**************************************************************************
   * Append a child to an element in current document in a thread safe-way.
   * @param parentId the id of the element.
   * @param creator the function that creates the child.
   *************************************************************************/
  public void appendChild(String parentId, Function<Document,Node> creator) 
  {
    while (true)
    {
      boolean okay = appendChildSync(parentId, creator);
      if (okay) return;
      try { Thread.sleep(1); } catch(InterruptedException e) { }
    }
  }
  private synchronized boolean appendChildSync(String parentId, 
    Function<Document,Node> creator) 
  {
    if (size > SIZE) 
    {
      if (documents.size() > LENGTH) return false;
      newDocument();
    }
    size++;
    Element root = current.getDocumentElement();
    Element parent = current.createElement("div");
    root.appendChild(parent);
    parent.setAttribute("id", parentId);
    parent.setAttribute("class", "appendChild");
    Node child = creator.apply(current);
    parent.appendChild(child);
    return true;
  }

  /**************************************************************************
   * Replace children of an element in current document in a thread safe-way.
   * @param parentId the id of the element.
   * @param creator the function that creates the new child.
   *************************************************************************/
  public void replaceChildren(String parentId, Function<Document,Node> creator) 
  {
    while (true)
    {
      boolean okay = replaceChildrenSync(parentId, creator);
      if (okay) return;
      try { Thread.sleep(1); } catch(InterruptedException e) { }
    }
  }
  private synchronized boolean replaceChildrenSync(String parentId,
    Function<Document,Node> creator) 
  {
    if (size > SIZE) 
    {
      if (documents.size() > LENGTH) return false;
      newDocument();
    }
    size++;
    Element root = current.getDocumentElement();
    Element parent = current.createElement("div");
    root.appendChild(parent);
    parent.setAttribute("id", parentId);
    parent.setAttribute("class", "replaceChildren");
    Node child = creator.apply(current);
    parent.appendChild(child);
    return true;
  }
  
  /****************************************************************************
   * Signal termination of execution with denoted success status.
   * @param okay true if computation was successful.
   ***************************************************************************/
  public synchronized void signalTermination(boolean okay)
  {
    Element root = current.getDocumentElement();
    root.setAttribute("done", Boolean.toString(okay));
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------