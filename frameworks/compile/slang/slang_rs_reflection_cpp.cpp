/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <sys/stat.h>
#include <stdio.h>
#include <stdlib.h>

#include <cstdarg>
#include <cctype>

#include <algorithm>
#include <sstream>
#include <string>
#include <utility>

#include "os_sep.h"
#include "slang_rs_context.h"
#include "slang_rs_export_var.h"
#include "slang_rs_export_foreach.h"
#include "slang_rs_export_func.h"
#include "slang_rs_reflect_utils.h"
#include "slang_version.h"
#include "slang_utils.h"

#include "slang_rs_reflection_cpp.h"

using namespace std;

namespace slang {

#define RS_TYPE_ITEM_CLASS_NAME          "Item"

static const char *GetMatrixTypeName(const RSExportMatrixType *EMT) {
  static const char *MatrixTypeCNameMap[] = {
    "rs_matrix2x2",
    "rs_matrix3x3",
    "rs_matrix4x4",
  };
  unsigned Dim = EMT->getDim();

  if ((Dim - 2) < (sizeof(MatrixTypeCNameMap) / sizeof(const char*)))
    return MatrixTypeCNameMap[ EMT->getDim() - 2 ];

  slangAssert(false && "GetMatrixTypeName : Unsupported matrix dimension");
  return NULL;
}


static std::string GetTypeName(const RSExportType *ET, bool Brackets = true) {
  switch (ET->getClass()) {
    case RSExportType::ExportClassPrimitive: {
      return RSExportPrimitiveType::getRSReflectionType(
          static_cast<const RSExportPrimitiveType*>(ET))->c_name;
    }
    case RSExportType::ExportClassPointer: {
      const RSExportType *PointeeType =
          static_cast<const RSExportPointerType*>(ET)->getPointeeType();

      if (PointeeType->getClass() != RSExportType::ExportClassRecord)
        return "android::sp<android::RSC::Allocation>";
      else
        return PointeeType->getElementName();
    }
    case RSExportType::ExportClassVector: {
      const RSExportVectorType *EVT =
          static_cast<const RSExportVectorType*>(ET);
      std::stringstream VecName;
      VecName << EVT->getRSReflectionType(EVT)->rs_c_vector_prefix
              << EVT->getNumElement();
      return VecName.str();
    }
    case RSExportType::ExportClassMatrix: {
      return GetMatrixTypeName(static_cast<const RSExportMatrixType*>(ET));
    }
    case RSExportType::ExportClassConstantArray: {
      // TODO: Fix this for C arrays!
      const RSExportConstantArrayType* CAT =
          static_cast<const RSExportConstantArrayType*>(ET);
      std::string ElementTypeName = GetTypeName(CAT->getElementType());
      if (Brackets) {
        ElementTypeName.append("[]");
      }
      return ElementTypeName;
    }
    case RSExportType::ExportClassRecord: {
      // TODO: Fix for C structs!
      return ET->getElementName() + "." RS_TYPE_ITEM_CLASS_NAME;
    }
    default: {
      slangAssert(false && "Unknown class of type");
    }
  }

  return "";
}


RSReflectionCpp::RSReflectionCpp(const RSContext *con)
    : RSReflectionBase(con) {
  clear();
}

RSReflectionCpp::~RSReflectionCpp() {
}

bool RSReflectionCpp::reflect(const string &OutputPathBase,
                              const string &InputFileName,
                              const string &OutputBCFileName) {
  mInputFileName = InputFileName;
  mOutputPath = OutputPathBase;
  mOutputBCFileName = OutputBCFileName;
  mClassName = string("ScriptC_") + stripRS(InputFileName);

  makeHeader("android::RSC::ScriptC");
  std::vector< std::string > header(mText);
  mText.clear();

  makeImpl("android::RSC::ScriptC");
  std::vector< std::string > cpp(mText);
  mText.clear();


  writeFile(mClassName + ".h", header);
  writeFile(mClassName + ".cpp", cpp);


  return true;
}


#define RS_TYPE_CLASS_NAME_PREFIX        "ScriptField_"



bool RSReflectionCpp::makeHeader(const std::string &baseClass) {
  startFile(mClassName + ".h");

  write("");
  write("#include \"RenderScript.h\"");
  write("using namespace android::RSC;");
  write("");

  // Imports
  //for(unsigned i = 0; i < (sizeof(Import) / sizeof(const char*)); i++)
      //out() << "import " << Import[i] << ";" << std::endl;
  //out() << std::endl;

  if (!baseClass.empty()) {
    write("class " + mClassName + " : public " + baseClass + " {");
  } else {
    write("class " + mClassName + " {");
  }

  write("private:");
  uint32_t slot = 0;
  incIndent();
  for (RSContext::const_export_var_iterator I = mRSContext->export_vars_begin(),
         E = mRSContext->export_vars_end(); I != E; I++, slot++) {
    const RSExportVar *ev = *I;
    RSReflectionTypeData rtd;
    ev->getType()->convertToRTD(&rtd);
    if (!ev->isConst()) {
      write(GetTypeName(ev->getType()) + " __" + ev->getName() + ";");
    }
  }
  decIndent();

  write("public:");
  incIndent();
  write(mClassName + "(android::sp<android::RSC::RS> rs," +
          " const char *cacheDir, size_t cacheDirLength);");
  write("virtual ~" + mClassName + "();");
  write("");


  // Reflect export variable
  slot = 0;
  for (RSContext::const_export_var_iterator I = mRSContext->export_vars_begin(),
         E = mRSContext->export_vars_end(); I != E; I++, slot++) {
    genExportVariable(*I);
  }

  // Reflect export for each functions
  for (RSContext::const_export_foreach_iterator
           I = mRSContext->export_foreach_begin(),
           E = mRSContext->export_foreach_end(); I != E; I++) {
    const RSExportForEach *ef = *I;
    if (ef->isDummyRoot()) {
      write("// No forEach_root(...)");
      continue;
    }

    ArgTy Args;
    stringstream ss;
    ss << "void forEach_" << ef->getName() << "(";

    if (ef->hasIn()) {
      Args.push_back(std::make_pair(
          "android::sp<const android::RSC::Allocation>", "ain"));
    }

    if (ef->hasOut() || ef->hasReturn()) {
      Args.push_back(std::make_pair(
          "android::sp<const android::RSC::Allocation>", "aout"));
    }

    const RSExportRecordType *ERT = ef->getParamPacketType();
    if (ERT) {
      for (RSExportForEach::const_param_iterator i = ef->params_begin(),
           e = ef->params_end(); i != e; i++) {
        RSReflectionTypeData rtd;
        (*i)->getType()->convertToRTD(&rtd);
        Args.push_back(std::make_pair(rtd.type->c_name, (*i)->getName()));
      }
    }
    makeArgs(ss, Args);
    ss << ");";
    write(ss);
  }


  // Reflect export function
  for (RSContext::const_export_func_iterator
        I = mRSContext->export_funcs_begin(),
        E = mRSContext->export_funcs_end(); I != E; I++) {
    const RSExportFunc *ef = *I;

    stringstream ss;
    makeFunctionSignature(ss, false, ef);
    write(ss);
  }

  decIndent();
  write("};");
  return true;
}

bool RSReflectionCpp::writeBC() {
  FILE *pfin = fopen(mOutputBCFileName.c_str(), "rb");
  if (pfin == NULL) {
    fprintf(stderr, "Error: could not read file %s\n",
            mOutputBCFileName.c_str());
    return false;
  }

  unsigned char buf[16];
  int read_length;
  write("static const unsigned char __txt[] = {");
  incIndent();
  while ((read_length = fread(buf, 1, sizeof(buf), pfin)) > 0) {
    string s;
    for (int i = 0; i < read_length; i++) {
      char buf2[16];
      snprintf(buf2, sizeof(buf2), "0x%02x,", buf[i]);
      s += buf2;
    }
    write(s);
  }
  decIndent();
  write("};");
  write("");
  return true;
}

bool RSReflectionCpp::makeImpl(const std::string &baseClass) {
  startFile(mClassName + ".cpp");

  write("");
  write("#include \"" + mClassName + ".h\"");
  write("");

  writeBC();

  // Imports
  //for(unsigned i = 0; i < (sizeof(Import) / sizeof(const char*)); i++)
      //out() << "import " << Import[i] << ";" << std::endl;
  //out() << std::endl;

  write("\n");
  stringstream ss;
  ss << mClassName << "::" << mClassName
     << "(android::sp<android::RSC::RS> rs, const char *cacheDir, "
        "size_t cacheDirLength) :\n"
        "        ScriptC(rs, __txt, sizeof(__txt), \""
     << mClassName << "\", " << mClassName.length()
     << ", cacheDir, cacheDirLength) {";
  write(ss);
  incIndent();
  //...
  decIndent();
  write("}");
  write("");

  write(mClassName + "::~" + mClassName + "() {");
  write("}");
  write("");

  // Reflect export for each functions
  uint32_t slot = 0;
  for (RSContext::const_export_foreach_iterator
       I = mRSContext->export_foreach_begin(),
       E = mRSContext->export_foreach_end(); I != E; I++, slot++) {
    const RSExportForEach *ef = *I;
    if (ef->isDummyRoot()) {
      write("// No forEach_root(...)");
      continue;
    }

    stringstream tmp;
    ArgTy Args;
    tmp << "void " << mClassName << "::forEach_" << ef->getName() << "(";

    if (ef->hasIn()) {
      Args.push_back(std::make_pair(
          "android::sp<const android::RSC::Allocation>", "ain"));
    }

    if (ef->hasOut() || ef->hasReturn()) {
      Args.push_back(std::make_pair(
          "android::sp<const android::RSC::Allocation>", "aout"));
    }

    const RSExportRecordType *ERT = ef->getParamPacketType();
    if (ERT) {
      for (RSExportForEach::const_param_iterator i = ef->params_begin(),
           e = ef->params_end(); i != e; i++) {
        RSReflectionTypeData rtd;
        (*i)->getType()->convertToRTD(&rtd);
        Args.push_back(std::make_pair(rtd.type->c_name, (*i)->getName()));
      }
    }
    makeArgs(tmp, Args);

    tmp << ") {";
    write(tmp);
    tmp.str("");

    std::string FieldPackerName = ef->getName() + "_fp";
    if (ERT) {
      if (genCreateFieldPacker(ERT, FieldPackerName.c_str())) {
        genPackVarOfType(ERT, NULL, FieldPackerName.c_str());
      }
    }
    tmp << "    forEach(" << slot << ", ";

    if (ef->hasIn()) {
      tmp << "ain, ";
    } else {
      tmp << "NULL, ";
    }

    if (ef->hasOut() || ef->hasReturn()) {
      tmp << "aout, ";
    } else {
      tmp << "NULL, ";
    }

    tmp << "NULL, 0);";
    write(tmp);

    write("}");
    write("");
  }

  slot = 0;
  // Reflect export function
  for (RSContext::const_export_func_iterator
       I = mRSContext->export_funcs_begin(),
       E = mRSContext->export_funcs_end(); I != E; I++) {
    const RSExportFunc *ef = *I;

    stringstream ss;
    makeFunctionSignature(ss, true, ef);
    write(ss);
    ss.str("");
    const RSExportRecordType *params = ef->getParamPacketType();
    size_t param_len = 0;
    if (params) {
      param_len = RSExportType::GetTypeAllocSize(params);
      if (genCreateFieldPacker(params, "__fp")) {
        genPackVarOfType(params, NULL, "__fp");
      }
    }

    ss.str("");
    ss << "    invoke(" << slot;
    if (params) {
      ss << ", __fp.getData(), " << param_len << ");";
    } else {
      ss << ", NULL, 0);";
    }
    write(ss);

    write("}");
    write("");

    slot++;
  }

  decIndent();
  return true;
}

void RSReflectionCpp::genExportVariable(const RSExportVar *EV) {
  const RSExportType *ET = EV->getType();

  switch (ET->getClass()) {
    case RSExportType::ExportClassPrimitive: {
      genPrimitiveTypeExportVariable(EV);
      break;
    }
    case RSExportType::ExportClassPointer: {
      genPointerTypeExportVariable(EV);
      break;
    }
    case RSExportType::ExportClassVector: {
      genVectorTypeExportVariable(EV);
      break;
    }
    case RSExportType::ExportClassMatrix: {
      genMatrixTypeExportVariable(EV);
      break;
    }
    case RSExportType::ExportClassConstantArray: {
      genConstantArrayTypeExportVariable(EV);
      break;
    }
    case RSExportType::ExportClassRecord: {
      genRecordTypeExportVariable(EV);
      break;
    }
    default: {
      slangAssert(false && "Unknown class of type");
    }
  }
}


void RSReflectionCpp::genPrimitiveTypeExportVariable(const RSExportVar *EV) {
  RSReflectionTypeData rtd;
  EV->getType()->convertToRTD(&rtd);

  if (!EV->isConst()) {
    write(string("void set_") + EV->getName() + "(" + rtd.type->c_name +
          " v) {");
    stringstream tmp;
    tmp << getNextExportVarSlot();
    write(string("    setVar(") + tmp.str() + ", &v, sizeof(v));");
    write(string("    __") + EV->getName() + " = v;");
    write("}");
  }
  write(string(rtd.type->c_name) + " get_" + EV->getName() + "() const {");
  if (EV->isConst()) {
    const clang::APValue &val = EV->getInit();
    bool isBool = !strcmp(rtd.type->c_name, "bool");
    write(string("    return ") + genInitValue(val, isBool) + ";");
  } else {
    write(string("    return __") + EV->getName() + ";");
  }
  write("}");
  write("");
}

void RSReflectionCpp::genPointerTypeExportVariable(const RSExportVar *EV) {
  const RSExportType *ET = EV->getType();

  slangAssert((ET->getClass() == RSExportType::ExportClassPointer) &&
              "Variable should be type of pointer here");

  std::string TypeName = GetTypeName(ET);
  std::string VarName = EV->getName();

  RSReflectionTypeData rtd;
  EV->getType()->convertToRTD(&rtd);
  uint32_t slot = getNextExportVarSlot();

  if (!EV->isConst()) {
    write(string("void bind_") + VarName + "(" + TypeName +
          " v) {");
    stringstream tmp;
    tmp << slot;
    write(string("    bindAllocation(v, ") + tmp.str() + ");");
    write(string("    __") + VarName + " = v;");
    write("}");
  }
  write(TypeName + " get_" + VarName + "() const {");
  if (EV->isConst()) {
    const clang::APValue &val = EV->getInit();
    bool isBool = !strcmp(TypeName.c_str(), "bool");
    write(string("    return ") + genInitValue(val, isBool) + ";");
  } else {
    write(string("    return __") + VarName + ";");
  }
  write("}");
  write("");

}

void RSReflectionCpp::genVectorTypeExportVariable(const RSExportVar *EV) {
  slangAssert(false);
}

void RSReflectionCpp::genMatrixTypeExportVariable(const RSExportVar *EV) {
  slangAssert(false);
}

void RSReflectionCpp::genConstantArrayTypeExportVariable(
    const RSExportVar *EV) {
  slangAssert(false);
}

void RSReflectionCpp::genRecordTypeExportVariable(const RSExportVar *EV) {
  slangAssert(false);
}


void RSReflectionCpp::makeFunctionSignature(
    std::stringstream &ss,
    bool isDefinition,
    const RSExportFunc *ef) {
  ss << "void ";
  if (isDefinition) {
    ss << mClassName << "::";
  }
  ss << "invoke_" << ef->getName() << "(";

  if (ef->getParamPacketType()) {
    bool FirstArg = true;
    for (RSExportFunc::const_param_iterator i = ef->params_begin(),
         e = ef->params_end(); i != e; i++) {
      RSReflectionTypeData rtd;
      (*i)->getType()->convertToRTD(&rtd);
      if (!FirstArg) {
        ss << ", ";
      } else {
        FirstArg = false;
      }
      ss << rtd.type->c_name << " " << (*i)->getName();
    }
  }

  if (isDefinition) {
    ss << ") {";
  } else {
    ss << ");";
  }
}

void RSReflectionCpp::makeArgs(std::stringstream &ss, const ArgTy& Args) {
  bool FirstArg = true;

  for (ArgTy::const_iterator I = Args.begin(), E = Args.end(); I != E; I++) {
    if (!FirstArg) {
      ss << ", ";
    } else {
      FirstArg = false;
    }

    ss << I->first << " " << I->second;
  }
}

bool RSReflectionCpp::genCreateFieldPacker(const RSExportType *ET,
                                           const char *FieldPackerName) {
  size_t AllocSize = RSExportType::GetTypeAllocSize(ET);

  if (AllocSize > 0) {
    std::stringstream ss;
    ss << "    FieldPacker " << FieldPackerName << "("
       << AllocSize << ");";
    write(ss);
    return true;
  }

  return false;
}

void RSReflectionCpp::genPackVarOfType(const RSExportType *ET,
                                       const char *VarName,
                                       const char *FieldPackerName) {
  std::stringstream ss;
  switch (ET->getClass()) {
    case RSExportType::ExportClassPrimitive:
    case RSExportType::ExportClassVector:
    case RSExportType::ExportClassPointer:
    case RSExportType::ExportClassMatrix: {
      RSReflectionTypeData rtd;
      ET->convertToRTD(&rtd);
      ss << "    " << FieldPackerName << ".add(" << VarName << ");";
      write(ss);
      break;
    }
    case RSExportType::ExportClassConstantArray: {
      /*const RSExportConstantArrayType *ECAT =
          static_cast<const RSExportConstantArrayType *>(ET);

      // TODO(zonr): more elegant way. Currently, we obtain the unique index
      //             variable (this method involves recursive call which means
      //             we may have more than one level loop, therefore we can't
      //             always use the same index variable name here) name given
      //             in the for-loop from counting the '.' in @VarName.
      unsigned Level = 0;
      size_t LastDotPos = 0;
      std::string ElementVarName(VarName);

      while (LastDotPos != std::string::npos) {
        LastDotPos = ElementVarName.find_first_of('.', LastDotPos + 1);
        Level++;
      }
      std::string IndexVarName("ct");
      IndexVarName.append(llvm::utostr_32(Level));

      C.indent() << "for (int " << IndexVarName << " = 0; " <<
                          IndexVarName << " < " << ECAT->getSize() << "; " <<
                          IndexVarName << "++)";
      C.startBlock();

      ElementVarName.append("[" + IndexVarName + "]");
      genPackVarOfType(C, ECAT->getElementType(), ElementVarName.c_str(),
                       FieldPackerName);

      C.endBlock();*/
      break;
    }
    case RSExportType::ExportClassRecord: {
      const RSExportRecordType *ERT =
          static_cast<const RSExportRecordType*>(ET);
      // Relative pos from now on in field packer
      unsigned Pos = 0;

      for (RSExportRecordType::const_field_iterator I = ERT->fields_begin(),
               E = ERT->fields_end();
           I != E;
           I++) {
        const RSExportRecordType::Field *F = *I;
        std::string FieldName;
        size_t FieldOffset = F->getOffsetInParent();
        size_t FieldStoreSize = RSExportType::GetTypeStoreSize(F->getType());
        size_t FieldAllocSize = RSExportType::GetTypeAllocSize(F->getType());

        if (VarName != NULL)
          FieldName = VarName + ("." + F->getName());
        else
          FieldName = F->getName();

        if (FieldOffset > Pos) {
          ss.str("");
          ss << "    " << FieldPackerName << ".skip("
             << (FieldOffset - Pos) << ");";
          write(ss);
        }

        genPackVarOfType(F->getType(), FieldName.c_str(), FieldPackerName);

        // There is padding in the field type
        if (FieldAllocSize > FieldStoreSize) {
          ss.str("");
          ss << "    " << FieldPackerName << ".skip("
             << (FieldAllocSize - FieldStoreSize) << ");";
          write(ss);
        }

        Pos = FieldOffset + FieldAllocSize;
      }

      // There maybe some padding after the struct
      if (RSExportType::GetTypeAllocSize(ERT) > Pos) {
        ss.str("");
        ss << "    " << FieldPackerName << ".skip("
           << RSExportType::GetTypeAllocSize(ERT) - Pos << ");";
        write(ss);
      }
      break;
    }
    default: {
      slangAssert(false && "Unknown class of type");
    }
  }
}

}  // namespace slang
