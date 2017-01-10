DIR="./"

  find "${DIR}" -name 'messages.xml' -print | while read f; do
    sed -i -e 's,    <methodMessages/>,    <methodMessages>\n        <logMessage>\n            <text>Success</text>\n        </logMessage>\n    </methodMessages>\n,g' "$f";
    
  done
