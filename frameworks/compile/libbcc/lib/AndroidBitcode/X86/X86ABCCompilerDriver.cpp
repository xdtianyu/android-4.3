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

#include "X86/X86ABCCompilerDriver.h"

#include "bcc/Support/TargetCompilerConfigs.h"
#include "bcc/Support/TargetLinkerConfigs.h"

namespace {

static const char *X86NonPortableList[] = {
  "stat",
  "fstat",
  "lstat",
  "fstatat",
  "open",
  "ioctl",
  "fcntl",
  "epoll_ctl",
  "epoll_wait",

  NULL  // NUL-terminator
};

} // end anonymous namespace

namespace bcc {

CompilerConfig *X86ABCCompilerDriver::createCompilerConfig() const {
  // x86-64 is currently unsupported.
  return new (std::nothrow) X86_32CompilerConfig();
}

LinkerConfig *X86ABCCompilerDriver::createLinkerConfig() const {
  // x86-64 is currently unsupported.
  return new (std::nothrow) X86_32LinkerConfig();
}

const char **X86ABCCompilerDriver::getNonPortableList() const {
  return X86NonPortableList;
}

} // end namespace bcc
