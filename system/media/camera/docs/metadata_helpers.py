#
# Copyright (C) 2012 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
A set of helpers for rendering Mako templates with a Metadata model.
"""

import metadata_model
from collections import OrderedDict

_context_buf = None

def _is_sec_or_ins(x):
  return isinstance(x, metadata_model.Section) or    \
         isinstance(x, metadata_model.InnerNamespace)

##
## Metadata Helpers
##

def find_all_sections(root):
  """
  Find all descendants that are Section or InnerNamespace instances.

  Args:
    root: a Metadata instance

  Returns:
    A list of Section/InnerNamespace instances

  Remarks:
    These are known as "sections" in the generated C code.
  """
  return root.find_all(_is_sec_or_ins)

def find_parent_section(entry):
  """
  Find the closest ancestor that is either a Section or InnerNamespace.

  Args:
    entry: an Entry or Clone node

  Returns:
    An instance of Section or InnerNamespace
  """
  return entry.find_parent_first(_is_sec_or_ins)

# find uniquely named entries (w/o recursing through inner namespaces)
def find_unique_entries(node):
  """
  Find all uniquely named entries, without recursing through inner namespaces.

  Args:
    node: a Section or InnerNamespace instance

  Yields:
    A sequence of MergedEntry nodes representing an entry

  Remarks:
    This collapses multiple entries with the same fully qualified name into
    one entry (e.g. if there are multiple entries in different kinds).
  """
  if not isinstance(node, metadata_model.Section) and    \
     not isinstance(node, metadata_model.InnerNamespace):
      raise TypeError("expected node to be a Section or InnerNamespace")

  d = OrderedDict()
  # remove the 'kinds' from the path between sec and the closest entries
  # then search the immediate children of the search path
  search_path = isinstance(node, metadata_model.Section) and node.kinds \
                or [node]
  for i in search_path:
      for entry in i.entries:
          d[entry.name] = entry

  for k,v in d.iteritems():
      yield v.merge()

def path_name(node):
  """
  Calculate a period-separated string path from the root to this element,
  by joining the names of each node and excluding the Metadata/Kind nodes
  from the path.

  Args:
    node: a Node instance

  Returns:
    A string path
  """

  isa = lambda x,y: isinstance(x, y)
  fltr = lambda x: not isa(x, metadata_model.Metadata) and \
                   not isa(x, metadata_model.Kind)

  path = node.find_parents(fltr)
  path = list(path)
  path.reverse()
  path.append(node)

  return ".".join((i.name for i in path))

##
## Filters
##

# abcDef.xyz -> ABC_DEF_XYZ
def csym(name):
  """
  Convert an entry name string into an uppercase C symbol.

  Returns:
    A string

  Example:
    csym('abcDef.xyz') == 'ABC_DEF_XYZ'
  """
  newstr = name
  newstr = "".join([i.isupper() and ("_" + i) or i for i in newstr]).upper()
  newstr = newstr.replace(".", "_")
  return newstr

# abcDef.xyz -> abc_def_xyz
def csyml(name):
  """
  Convert an entry name string into a lowercase C symbol.

  Returns:
    A string

  Example:
    csyml('abcDef.xyz') == 'abc_def_xyz'
  """
  return csym(name).lower()

# pad with spaces to make string len == size. add new line if too big
def ljust(size, indent=4):
  """
  Creates a function that given a string will pad it with spaces to make
  the string length == size. Adds a new line if the string was too big.

  Args:
    size: an integer representing how much spacing should be added
    indent: an integer representing the initial indendation level

  Returns:
    A function that takes a string and returns a string.

  Example:
    ljust(8)("hello") == 'hello   '

  Remarks:
    Deprecated. Use pad instead since it works for non-first items in a
    Mako template.
  """
  def inner(what):
    newstr = what.ljust(size)
    if len(newstr) > size:
      return what + "\n" + "".ljust(indent + size)
    else:
      return newstr
  return inner

def _find_new_line():

  if _context_buf is None:
    raise ValueError("Context buffer was not set")

  buf = _context_buf
  x = -1 # since the first read is always ''
  cur_pos = buf.tell()
  while buf.tell() > 0 and buf.read(1) != '\n':
    buf.seek(cur_pos - x)
    x = x + 1

  buf.seek(cur_pos)

  return int(x)

# Pad the string until the buffer reaches the desired column.
# If string is too long, insert a new line with 'col' spaces instead
def pad(col):
  """
  Create a function that given a string will pad it to the specified column col.
  If the string overflows the column, put the string on a new line and pad it.

  Args:
    col: an integer specifying the column number

  Returns:
    A function that given a string will produce a padded string.

  Example:
    pad(8)("hello") == 'hello   '

  Remarks:
    This keeps track of the line written by Mako so far, so it will always
    align to the column number correctly.
  """
  def inner(what):
    wut = int(col)
    current_col = _find_new_line()

    if len(what) > wut - current_col:
      return what + "\n".ljust(col)
    else:
      return what.ljust(wut - current_col)
  return inner

# int32 -> TYPE_INT32, byte -> TYPE_BYTE, etc. note that enum -> TYPE_INT32
def ctype_enum(what):
  """
  Generate a camera_metadata_type_t symbol from a type string.

  Args:
    what: a type string

  Returns:
    A string representing the camera_metadata_type_t

  Example:
    ctype_enum('int32') == 'TYPE_INT32'
    ctype_enum('int64') == 'TYPE_INT64'
    ctype_enum('float') == 'TYPE_FLOAT'

  Remarks:
    An enum is coerced to a byte since the rest of the camera_metadata
    code doesn't support enums directly yet.
  """
  return 'TYPE_%s' %(what.upper())
