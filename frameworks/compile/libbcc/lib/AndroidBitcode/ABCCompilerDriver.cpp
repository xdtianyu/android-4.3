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

#include "bcc/AndroidBitcode/ABCCompilerDriver.h"

#include <llvm/IR/Module.h>
#include <llvm/Pass.h>
#include <llvm/Support/MemoryBuffer.h>
#include <llvm/Support/raw_ostream.h>
#include <mcld/Config/Config.h>

#include "bcc/Config/Config.h"
#include "bcc/Script.h"
#include "bcc/Source.h"
#include "bcc/Support/CompilerConfig.h"
#include "bcc/Support/LinkerConfig.h"
#include "bcc/Support/Log.h"
#include "bcc/Support/OutputFile.h"
#include "bcc/Support/TargetLinkerConfigs.h"
#include "bcc/Support/TargetCompilerConfigs.h"

#if defined(PROVIDE_ARM_CODEGEN)
# include "ARM/ARMABCCompilerDriver.h"
#endif
#if defined(PROVIDE_MIPS_CODEGEN)
# include "Mips/MipsABCCompilerDriver.h"
#endif
#if defined(PROVIDE_X86_CODEGEN)
# include "X86/X86ABCCompilerDriver.h"
#endif

namespace bcc {

ABCCompilerDriver::ABCCompilerDriver()
  : mContext(), mCompiler(*this), mLinker(),
    mCompilerConfig(NULL), mLinkerConfig(NULL), mAndroidSysroot("/") {
}

ABCCompilerDriver::~ABCCompilerDriver() {
  delete mCompilerConfig;
  delete mLinkerConfig;
}

bool ABCCompilerDriver::configCompiler() {
  if (mCompilerConfig != NULL) {
    return true;
  }

  mCompilerConfig = createCompilerConfig();
  if (mCompilerConfig == NULL) {
    ALOGE("Out of memory when create the compiler configuration!");
    return false;
  }

  // Set PIC mode for relocatables.
  mCompilerConfig->setRelocationModel(llvm::Reloc::PIC_);

  // Set optimization level to -O1.
  mCompilerConfig->setOptimizationLevel(llvm::CodeGenOpt::Less);

  Compiler::ErrorCode result = mCompiler.config(*mCompilerConfig);

  if (result != Compiler::kSuccess) {
    ALOGE("Failed to configure the compiler! (detail: %s)",
          Compiler::GetErrorString(result));
    return false;
  }

  return true;
}

bool ABCCompilerDriver::configLinker() {
  if (mLinkerConfig != NULL) {
    return true;
  }

  mLinkerConfig = createLinkerConfig();
  if (mLinkerConfig == NULL) {
    ALOGE("Out of memory when create the linker configuration!");
    return false;
  }

  // FIXME: how can we get the soname if input/output is file descriptor?
  mLinkerConfig->setSOName("");

  mLinkerConfig->setDyld("/system/bin/linker");
  mLinkerConfig->setSysRoot(mAndroidSysroot);
  mLinkerConfig->addSearchDir("=/system/lib");

  // Add non-portable function list. For each function X, linker will rename
  // it to X_portable. And X_portable" is implemented in libportable to solve
  // portable issues.
  const char **non_portable_func = getNonPortableList();
  if (non_portable_func != NULL) {
    while (*non_portable_func != NULL) {
      mLinkerConfig->addPortable(*non_portable_func);
      non_portable_func++;
    }
  }

  // -shared
  mLinkerConfig->setShared(true);

  // -Bsymbolic.
  mLinkerConfig->setBsymbolic(true);

  // Set kRelro for -z relro
  // Not set kExecStack for -z noexecstack
  // Not set kLazy for -z now
  mLinkerConfig->setZOption(LinkerConfig::kRelro);

  // Config the linker.
  Linker::ErrorCode result = mLinker.config(*mLinkerConfig);
  if (result != Linker::kSuccess) {
    ALOGE("Failed to configure the linker! (%s)",
          Linker::GetErrorString(result));
    return false;
  }

  return true;
}

//------------------------------------------------------------------------------

Script *ABCCompilerDriver::prepareScript(int pInputFd) {
  Source *source = Source::CreateFromFd(mContext, pInputFd);
  if (source == NULL) {
    ALOGE("Failed to load LLVM module from file descriptor `%d'", pInputFd);
    return NULL;
  }

  Script *script = new (std::nothrow) Script(*source);
  if (script == NULL) {
    ALOGE("Out of memory when create script for file descriptor `%d'!",
          pInputFd);
    delete source;
    return NULL;
  }

  return script;
}

bool ABCCompilerDriver::compile(Script &pScript, llvm::raw_ostream &pOutput) {
  // Config the compiler.
  if (!configCompiler()) {
    return false;
  }

  // Run the compiler.
  Compiler::ErrorCode result = mCompiler.compile(pScript, pOutput);
  if (result != Compiler::kSuccess) {
    ALOGE("Fatal error during compilation (%s)!",
          Compiler::GetErrorString(result));
    return false;
  }

  return true;
}

bool ABCCompilerDriver::link(const Script &pScript,
                             const std::string &input_relocatable,
                             int pOutputFd) {
  // Config the linker.
  if (!configLinker()) {
    return false;
  }

  // Prepare output file.
  Linker::ErrorCode result = mLinker.setOutput(pOutputFd);

  if (result != Linker::kSuccess) {
    ALOGE("Failed to open the output file! (file descriptor `%d': %s)",
          pOutputFd, Linker::GetErrorString(result));
    return false;
  }

  mLinker.addObject(mAndroidSysroot + "/system/lib/crtbegin_so.o");

  // Prepare the relocatables.
  //
  // FIXME: Ugly const_cast here.
  mLinker.addObject(const_cast<char *>(input_relocatable.data()),
                    input_relocatable.size());

  // TODO: Refactor libbcc/runtime/ to libcompilerRT.so and use it.
  mLinker.addNameSpec("bcc");

  mLinker.addObject(mAndroidSysroot + "/system/lib/crtend_so.o");

  // Perform linking.
  result = mLinker.link();
  if (result != Linker::kSuccess) {
    ALOGE("Failed to link the shared object (detail: %s)",
          Linker::GetErrorString(result));
    return false;
  }

  return true;
}

//------------------------------------------------------------------------------

ABCCompilerDriver *ABCCompilerDriver::Create(const std::string &pTriple) {
  std::string error;
  const llvm::Target *target =
      llvm::TargetRegistry::lookupTarget(pTriple, error);

  if (target == NULL) {
    ALOGE("Unsupported target '%s' (detail: %s)!", pTriple.c_str(),
          error.c_str());
    return NULL;
  }

  switch (llvm::Triple::getArchTypeForLLVMName(target->getName())) {
#if defined(PROVIDE_ARM_CODEGEN)
    case llvm::Triple::arm: {
      return new ARMABCCompilerDriver(/* pInThumbMode */false);
    }
    case llvm::Triple::thumb: {
      return new ARMABCCompilerDriver(/* pInThumbMode */true);
    }
#endif
#if defined(PROVIDE_MIPS_CODEGEN)
    case llvm::Triple::mipsel: {
      return new MipsABCCompilerDriver();
    }
#endif
#if defined(PROVIDE_X86_CODEGEN)
    case llvm::Triple::x86: {
      return new X86ABCCompilerDriver();
    }
#endif
    default: {
      ALOGE("Unknown architecture '%s' supplied in %s!", target->getName(),
            pTriple.c_str());
      break;
    }
  }

  return NULL;
}

bool ABCCompilerDriver::build(int pInputFd, int pOutputFd) {
  //===--------------------------------------------------------------------===//
  // Prepare the input.
  //===--------------------------------------------------------------------===//
  Script *script = prepareScript(pInputFd);
  if (script == NULL) {
    return false;
  }

  //===--------------------------------------------------------------------===//
  // Prepare the output.
  //===--------------------------------------------------------------------===//
  std::string output_relocatable;
  llvm::raw_ostream *output =
      new (std::nothrow) llvm::raw_string_ostream(output_relocatable);
  if (output == NULL) {
    ALOGE("Failed to prepare the output for compile the input from %d into "
          "relocatable object!", pInputFd);
    delete script;
    return false;
  }

  //===--------------------------------------------------------------------===//
  // Compile.
  //===--------------------------------------------------------------------===//
  if (!compile(*script, *output)) {
    delete output;
    delete script;
    return false;
  }

  //===--------------------------------------------------------------------===//
  // Close the output.
  //===--------------------------------------------------------------------===//
  delete output;

  //===--------------------------------------------------------------------===//
  // Link.
  //===--------------------------------------------------------------------===//
  if (!link(*script, output_relocatable, pOutputFd)) {
    delete script;
    return false;
  }

  //===--------------------------------------------------------------------===//
  // Clean up.
  //===--------------------------------------------------------------------===//
  delete script;

  return true;
}

} // namespace bcc
