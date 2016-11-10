#! /bin/bash
# Usage:
#  run.sh -i input_dir -o output_dir

source common_settings.sh

${CMD} "$@"
exit $?
