#! /bin/bash
# Usage:
#  run.sh -i input_file -l load_tag -o output_file -e export_tag

source common_settings.sh

${CMD} "$@"
exit $?
