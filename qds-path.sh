if [[ "$OS" == "Windows_NT" ]]; then
    PATH_SEP=";"
else
    PATH_SEP=":"
fi

QDS_LIBS_DIR="libs/3.347/"
QDS_SUFFIX="-3.347.jar"
QDS_PATH="."

for lib in "qds-tools" "qds" "dxlib" "qds-file" "mars" "qds-monitoring" "dxfeed-api"; do
  lib_path="${QDS_LIBS_DIR}${lib}${QDS_SUFFIX}"
  QDS_PATH="${QDS_PATH}${PATH_SEP}${lib_path}"
done

# add custom tools
QDS_PATH="${QDS_PATH}${PATH_SEP}libs/config-1.4.3.jar"