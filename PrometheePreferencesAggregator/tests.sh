#! /bin/bash

CMD="./run.sh"

NB_TESTS=$(find tests -maxdepth 1 -type d -regex '.*/in[0-9]*$' | wc -l)

mkdir -p tests_tmp
for i in $(seq 1 ${NB_TESTS}); do
    IN="tests/in${i}"
    REFERENCE_OUT="tests/out${i}"
    OUT=$(mktemp --tmpdir=. -d tests_tmp/out.XXX)
    echo "${IN}"
    ${CMD} -i "${IN}" -o "${OUT}"
    diff -x README -ruBw "${REFERENCE_OUT}" "${OUT}"
    ret_diff=$?
    if [ $ret_diff -ne 0 ]; then
        echo "FAILED: ${IN}"
    else
        rm -r ${OUT}
    fi
done
