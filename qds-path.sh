

QDS_LIBS_DIR="libs/3.347/"
QDS_SUFFIX="-3.347.jar"
QDS_PATH="."

for lib in "qds-tools" "qds" "dxlib" "qds-file" "mars" "qds-monitoring" "dxfeed-api"; do
  QDS_PATH="$QDS_PATH;$QDS_LIBS_DIR$lib$QDS_SUFFIX"
done

# add custom tools
QDS_PATH="$QDS_PATH;libs/config-1.4.3.jar"