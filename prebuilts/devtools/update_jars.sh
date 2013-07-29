#!/bin/bash

set -e            # fail on errors

if [[ $(uname) == "Darwin" ]]; then
  PROG_DIR=$(dirname "$0")
else
  PROG_DIR=$(readlink -f $(dirname "$0"))
fi
cd "$PROG_DIR"

DRY="echo"        # default to dry mode unless -f is specified
FILTER=""         # name of a specific jar to update (default: update them all)
MK_MERGE_MSG="1"  # 1 to update the MERGE_MSG, empty to do not generate it
MERGE_MSG1=""     # msg to generate
MERGE_MSG2=""     # msg to generate

while [[ -n "$1" ]]; do
  if [[ "$1" == "-f" ]]; then
    DRY=""
  elif [[ "$1" == "-m" ]]; then
    MK_MERGE_MSG=""
  elif [[ $1 =~ ^[a-z]+ ]]; then
    FILTER="$FILTER ${1/.jar/} "
  else
    echo "Unknown argument: $1"
    echo "Usage: $0 [project_to_update] [-f] [-m]"
    echo "       -f: force actually generating/modifying files."
    echo "       -m: do NOT generate a .git/MERGE_MSG"
    echo "      (default: updates all jars.)"
    exit 1
  fi
  shift
done


# Define projects to build and files to copy.
function list_projects() {
  add_project assetstudio
  add_project common
  add_project ddmlib
  add_project dvlib
  add_project jobb            "etc/jobb|tools/jobb" "etc/jobb.bat|tools/jobb.bat"
  add_project layoutlib_api
  add_project lint_api
  add_project lint            "cli/etc/lint|tools/lint" "cli/etc/lint.bat|tools/lint.bat"
  add_project lint_checks
  add_project manifmerger
  #add_project rule_api -- TODO do this one next
  add_project sdk_common
  add_project sdklib
  add_project sdkuilib        in:tools/swt
  add_project swtmenubar      in:tools/swt
}

# ----
# List of targets to build, e.g. :jobb:jar
BUILD_LIST_base=""
BUILD_LIST_swt=""
# List of files to copy.
# Syntax:
#     relative/dir              (copy, relative to src & dest)
#     src/rel/dir|dst/rel/dir   (copy, with different destination name)
#     @relative_script          (executes script in dest/proj dir)
COPY_LIST_base=""
COPY_LIST_swt=""

function get_map() {
  #$1=map name (BUILD_LIST or COPY_LIST)
  #$2=map key  (base or swt)
  eval local V=\$$1_$2
  echo $V
}

function append_map() {
  #$1=map name (BUILD_LIST or COPY_LIST)
  #$2=map key  (base or swt)
  #$3=value to append (will be space separated)
  eval local V=\$$1_$2
  eval $1_$2=\"$V $3\"
}

function add_project() {
  # $1=project name
  # $2=optional in:tools/swt repo (default: tools/base)
  # $2...=optional files to copy (relative to project dir)
  local proj=$1 src dst f
  shift

  if [[ -n "$FILTER" && "${FILTER/ $proj /}" == "$FILTER" ]]; then
    echo "## Skipping project $proj"
    return
  fi

  local repo="base"
  if [[ "$1" == "in:tools/swt" ]]; then
    repo="swt"
    shift
  fi

  echo "## Updating project tools/$repo/$proj"
  # Request to build the jar for that project
  append_map BUILD_LIST $repo ":$proj:prebuiltJar"

  # Copy all the optional files
  while [[ -n "$1" ]]; do
    f=$1
    src="${f%%|*}"    # strip part after  | if any
    dst="${f##*|}"    # strip part before | if any
    if [[ ${src:0:1} != "/" ]]; then src=$proj/$src; fi
    append_map COPY_LIST $repo "$src|$dst"
    shift
  done
  return 0
}

function build() {
  echo
  local repo=$1
  local buildlist=$(get_map BUILD_LIST $repo)

  if [[ -n "$buildlist" ]]; then
    local names=${buildlist//:/}
    names=${names//prebuiltJar/}
    MERGE_MSG1="$MERGE_MSG1
tools/$repo: Changed $names"
    local SHA1=$( cd ../../tools/$repo ; git show-ref --head --hash HEAD )
    MERGE_MSG2="$MERGE_MSG2
tools/$repo @ $SHA1"
  fi

  # To build tools/swt, we'll need to first locally publish some
  # libs from tools/base.
  if [[ "$repo" == "base" && -n $(get_map BUILD_LIST swt) ]]; then
    echo "## PublishLocal in tools/base (needed for tools/swt)"
    buildlist="$buildlist publishLocal"
  fi

  if [[ -z "$buildlist" ]]; then
    echo "## WARNING: nothing to build in tools/$repo."
    return 1
  else
    echo "## Building tools/$repo: $buildlist"
    ( D="$PWD" ; cd ../../tools/$repo ; $DRY ./gradlew -PprebuiltPath="$D" $buildlist )
    return 0
  fi
}

function copy_files() {
  local repo=$1 src dst dstdir
  echo
  local copylist=$(get_map COPY_LIST $repo)
  if [[ -z "$copylist" ]]; then
    echo "## WARNING: nothing to copy in tools/$repo."
  else
    for f in $copylist; do
      if [[ "${f/@//}" == "$f" ]]; then
        src="${f%%|*}"    # strip part after  | if any
        dst="${f##*|}"    # strip part before | if any
        if [[ ${src:0:1} != "/" ]]; then src=../../tools/$repo/$src; fi
        dstdir=$(dirname $dst)
        if [[ ! -d $dstdir ]]; then $DRY mkdir -p $dstdir; fi
        $DRY cp -v $src $dst
      else
        # syntax is proj/@script_name
        d="${f%%@*}"      # string part after @, that's the proj dir name
        f="${f##*@}"      # strip part before @, script name is what's left.
        echo "## Execute $d => $f"
        ( cd "$d" && pwd && $DRY $f )
      fi
    done
  fi
}

function merge_msg() {
  local dst=.git/MERGE_MSG
  if [[ -n $DRY ]]; then
    echo "The following would be output to $dst (use -m to prevent this):"
    dst=/dev/stdout
  fi
  cat >> $dst <<EOMSG
Update SDK prebuilts.
$MERGE_MSG1

Origin:$MERGE_MSG2
EOMSG
}

list_projects
for r in base swt; do
  if build $r; then
    copy_files $r
  fi
done
if [[ $MK_MERGE_MSG ]]; then merge_msg; fi
if [[ -n $DRY ]]; then
  echo
  echo "## WARNING: DRY MODE. Run with -f to actually copy files."
fi

