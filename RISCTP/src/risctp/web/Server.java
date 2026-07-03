// ---------------------------------------------------------------------------
// Server.java
// A web server providing a GUI.
// $Id: Server.java,v 1.19 2024/07/04 11:33:39 schreine Exp $
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
import java.nio.charset.*;
import java.net.*;
import java.util.concurrent.*;
import com.sun.net.httpserver.*;

import risctp.Main;

public class Server
{
  // maximum number of connections allowed by server (0: default)
  private static final int BACKLOG = 0;
  
  // number of threads handling requests
  private static final int THREADS = 2;
  
  // the port on which the server listens
  private int port; 
  
  // a mapping of service names to handlers
  private Map<String,Handler> handlers;

  // the actual server
  private HttpServer server;
  
  /****************************************************************************
   * Create a server listening on the denoted port.
   * @param port the port number.
   ***************************************************************************/
  public Server(int port)
  {
    this.port = port;
    this.handlers = new HashMap<String,Handler>();
    this.server = null;
  }
  
  /****************************************************************************
   * Unregister all handlers.
   ***************************************************************************/
  public void unregisterAll()
  {
    handlers = new HashMap<String,Handler>();
  }
  
  /****************************************************************************
   * Register a handler for the denoted resource path.
   * @param service the name of the service to handle.
   * @param handler the handler.
   ***************************************************************************/
  public void register(String service, Handler handler)
  {
    handlers.put(service, handler);
  }

  /****************************************************************************
   * Start the server in the background.
   * @return error message (null if none)
   ***************************************************************************/
  public String run()
  {
    if (server != null) return null;
    try
    {
      server = HttpServer.create(new InetSocketAddress(port), BACKLOG);
      server.createContext("/", new ServerHandler());
      ExecutorService executor = Executors.newFixedThreadPool(THREADS);
      server.setExecutor(executor);
      server.start();
      return null;
    }
    catch(IllegalArgumentException e)
    {
      return e.getMessage();
    }
    catch(IOException e)
    {
      return e.getMessage();
    }
  }

  // --------------------------------------------------------------------------
  //
  // The handler for the web server.
  //
  // --------------------------------------------------------------------------
  private class ServerHandler implements HttpHandler 
  {
    public void handle(HttpExchange request) throws IOException 
    {
      try
      {
        URI uri = request.getRequestURI();
        String path = uri.getPath();
        int end = path.indexOf('?');
        String service = end == -1 ? path : path.substring(0, end);
        Handler handler = handlers.get(service);
        if (handler == null) return;
        PrintWriter out = new PrintWriter(request.getResponseBody(), true);
        request.sendResponseHeaders(200, 0);
        String method = request.getRequestMethod();
        Map<String,String> params = 
            method.equals("GET") ? getParameters(uri.getQuery()) :
              postParameters(request.getRequestBody());
        handler.handle(service, params, out);
        out.close();
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
  }
  
  /***************************************************************************
   * Get parameters of GET request.
   * @param query the query part of the request path.
   * @param return a mapping of parameter names to parameter values.
   **************************************************************************/
  private static Map<String,String> getParameters(String query)
  {
    Map<String,String> map = new HashMap<String,String>();
    if (query != null)
    {
      String[] params = query.split("&");
      for (String param : params)
      {
        String[] parts = param.split("=");
        String name = parts[0];
        String value = parts.length < 2 ? "" : parts[1];
        map.put(name, value);
      } 
    }
    return map;
  }
  
  /***************************************************************************
   * Get parameters of URL-encoded POST request.
   * @param stream the body of the request.
   * @param return a mapping of parameter names to parameter values.
   **************************************************************************/
  private static Map<String,String> postParameters(InputStream stream)
  {
    Charset charset = Main.CHAR_SET; 
    String string;
    try { string = new String(stream.readAllBytes(), charset); } 
    catch(IOException e) { string = ""; }
    String[] params = string.split("&");
    Map<String,String> map = new HashMap<String,String>();
    for (String param : params)
    {
      String[] parts = param.split("=");
      String name = parts[0];
      String value = parts.length < 2 ? "" : URLDecoder.decode(parts[1], charset);
      map.put(name, value);
    } 
    return map;
  }

  // --------------------------------------------------------------------------
  //
  // A handler for the web server.
  //
  // --------------------------------------------------------------------------
  public static interface Handler
  {
    /**************************************************************************
     * Respond to request to denoted service.
     * @param params a mapping of parameter names to parameter values.
     * @param out the stream to write the response to.
     *************************************************************************/
    public void handle(String service, Map<String,String> params, PrintWriter out);
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------