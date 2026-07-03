// ---------------------------------------------------------------------------
// Names.java
// Auxiliaries for name handling.
// $Id: Names.java,v 1.4 2024/03/25 14:24:02 schreine Exp $
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
package risctp.syntax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Names
{
  /****************************************************************************
   * Return name that is not used.
   * @param prefix a prefix for the name.
   * @param used the names that are already in use.
   * @return a name that does not occur in 'used'.
   **************************************************************************/
  public static String unused(String prefix, Collection<String> used)
  {
    List<String> result = unused(1, prefix, used);
    return result.get(0);
  }
  /****************************************************************************
   * Return list of names that are not used.
   * @param n the desired number of names.
   * @param prefix a prefix for the names.
   * @param used the names that are already in use.
   * @return a list of 'n' names that do not occur in 'used'.
   **************************************************************************/
  public static List<String> unused(int n, String prefix, Collection<String> used)
  {
    // strip trailing digits from prefix (probably previously generated)
    int len = prefix.length();
    while (len > 0)
    {
      char ch = prefix.charAt(len-1);
      if (!Character.isDigit(ch)) break;
      len--;
    }
    prefix = prefix.substring(0, len);
    List<String> result = new ArrayList<String>();
    int counter = -1;
    for (int i = 0; i < n; i++)
    {
      String name;
      while (true)
      {
        name = prefix + (counter == -1 ? "" : counter);
        counter++;
        if (!used.contains(name)) break;
      }
      result.add(name);
    }
    return result;
  }
}
//----------------------------------------------------------------------------
// end of file
//----------------------------------------------------------------------------
